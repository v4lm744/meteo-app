# BUG-002 — Cumul de pluie et fenêtre des alertes faux

| Champ | Valeur |
|---|---|
| Statut | Publié en v1.11.0 |
| Gravité | Majeure |
| Composant | `notifications/WeatherAlertEvaluator.kt` |
| Détecté le | 2026-10-20 (audit) |
| Corrigé le | 2026-10-20 |
| Commit / PR | PR #85, publiée en v1.11.0 |

## Symptôme

- **Avec Open-Meteo** (source principale) : le cumul de pluie annoncé dans
  l'alerte « Fortes pluies attendues : X mm cumulés sur 12 h » est trois
  fois trop élevé. Une alerte part dès 0,7 mm réels pour le seuil par défaut
  de 2 mm.
- **Avec le repli OpenWeatherMap** : l'évaluation porte sur 36 h au lieu de
  12 h, et le cumul est lui aussi triplé.

## Cause racine

`WeatherAlertEvaluator` datait de l'époque où OpenWeatherMap était la seule
source :

```kotlin
val window = weather.hourly.take(12)                              // 12 créneaux
val rainMm = window.sumOf { it.rainVolume + it.snowVolume } * 3.0  // × 3
```

- `take(12)` compte des créneaux, pas des heures : 12 h au pas de 1 h
  (Open-Meteo), 36 h au pas de 3 h (OpenWeatherMap).
- `× 3.0` suppose des intensités horaires à étendre sur 3 h. Or
  `rainVolume` est déjà le cumul du créneau (1 h pour Open-Meteo, 3 h pour
  OpenWeatherMap) : la multiplication est fausse dans les deux cas.

## Décisions

| Question | Option retenue | Option écartée |
|---|---|---|
| Fenêtre d'évaluation | **12 h réelles**, quel que soit le fournisseur (validé par le propriétaire du projet) | 24 h : plus d'anticipation, mais seuils et messages à revoir |
| Calcul du cumul | Somme des volumes réels des créneaux de la fenêtre, sans facteur | Facteur selon le fournisseur : fragile si un pas change |
| Créneau à cheval sur « maintenant » | Retenu tant qu'il n'est pas terminé (son volume entier est compté) | Prorata : complexité sans gain notable pour une alerte |

## Correctif

- La fenêtre devient temporelle : `alertWindow` garde les créneaux qui
  recoupent `[maintenant, maintenant + 12 h[`. Le pas est déduit de l'écart
  entre deux créneaux (1 h par défaut).
- Le cumul de pluie est la somme simple des volumes (pluie + neige).
- `evaluate` accepte un paramètre `nowSeconds` (par défaut l'heure
  courante) pour rendre la logique testable.
- La règle s'applique aussi au vent et au gel, qui portent désormais
  sur 12 h réelles avec OpenWeatherMap.
- Effet de bord bénéfique : des données en cache entièrement passées ne
  déclenchent plus d'alerte.

## Tests de non-régression

Dans `WeatherAlertEvaluatorTest` :

- `hourly open-meteo rainfall is not tripled` : 12 × 0,15 mm = 1,8 mm, pas
  d'alerte (l'ancien calcul donnait 5,4 mm) ;
- `three-hour steps only count the next twelve hours` : la pluie au-delà de
  12 h est ignorée au pas de 3 h ;
- `three-hour rain volumes are summed without scaling` ;
- `window keeps the current slot and drops finished ones` ;
- `no alert from stale data entirely in the past` ;
- les tests existants sont adaptés (créneaux horodatés, instant fixé).

## Impact et risques

- Moins d'alertes pluie : c'est l'effet attendu, les précédentes étaient
  surestimées. Les utilisateurs qui avaient relevé leur seuil pour
  compenser peuvent le redescendre.
- Le seuil « mm cumulés sur 12 h » (`AlertPrefs`) garde le même sens.
