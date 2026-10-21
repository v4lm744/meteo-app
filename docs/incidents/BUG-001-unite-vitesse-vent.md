# BUG-001 — Vent Open-Meteo affiché 3,6 fois trop fort

| Champ | Valeur |
|---|---|
| Statut | Publié en v1.11.0 |
| Gravité | Majeure |
| Composant | `data/api/OpenMeteoApi.kt`, `data/WeatherCache.kt` |
| Détecté le | 2026-10-20 (audit) |
| Corrigé le | 2026-10-20 |
| Commit / PR | PR #85, publiée en v1.11.0 |

## Symptôme

Depuis le passage à Open-Meteo comme source principale, la vitesse du vent
est fortement surestimée sur tous les écrans et widgets. Par exemple, un vent
réel de 30 km/h s'affiche à environ 108 km/h. Les alertes « vent violent »
se déclenchent dès 17 km/h réels environ, au lieu de 60 km/h (seuil par
défaut).

## Cause racine

- Open-Meteo renvoie `wind_speed_10m` en **km/h** par défaut. La requête
  (`OpenMeteoApi.getForecast`) ne précisait pas l'unité.
- `OpenMeteoMapper` recopie la valeur telle quelle dans `windSpeed`.
- Tout le modèle de l'app (`WeatherUtils.formatWindSpeed`,
  `WeatherAlertEvaluator`, la boussole) suppose des **m/s**, l'unité
  d'OpenWeatherMap, et multiplie par 3,6 pour afficher des km/h.

La valeur subissait donc deux conversions : km/h × 3,6.

## Décisions

| Question | Option retenue | Option écartée |
|---|---|---|
| Où corriger l'unité ? | Demander des m/s à l'API (`wind_speed_unit=ms`) : un seul point de vérité, cohérent avec OpenWeatherMap | Diviser par 3,6 dans le mapper : conversion supplémentaire et risque d'oubli sur d'autres champs |
| Que faire des caches existants (jusqu'à 3 jours, servis hors ligne) qui contiennent des km/h ? | **Invalider le cache** en versionnant le nom des fichiers (validé par le propriétaire du projet) | Laisser expirer : vent faux possible hors ligne pendant 3 jours |

## Correctif

- `OpenMeteoApi` : ajout de `wind_speed_unit=ms` à la requête de prévisions.
- `WeatherCache` : nom de fichier versionné (`weather_v2_<lat>_<lon>.json`,
  constante `CACHE_VERSION`). `trim()`, appelé à chaque sauvegarde, supprime
  les fichiers de l'ancien format (`weather_<lat>_<lon>.json`), qui ne sont
  plus lus.

## Tests de non-régression

- `OpenMeteoRepositoryTest.getWeather_requestsWindSpeedInMetersPerSecond` :
  la requête porte `wind_speed_unit=ms` et la valeur reçue n'est pas
  convertie.
- `WeatherCacheTest.trim_purgesFilesFromPreviousCacheVersion` et
  `load_ignoresFileFromPreviousCacheVersion` : les anciens fichiers sont
  ignorés puis purgés.
- Tests existants du cache adaptés au nouveau préfixe.

## Impact et risques

- Au premier lancement après la mise à jour, le cache est vide : un appel
  réseau de plus. Hors ligne à ce moment-là, l'app affiche l'erreur réseau
  au lieu de l'ancienne météo (qui était fausse pour le vent).
- Les seuils d'alerte vent configurés par l'utilisateur restent valables :
  ils sont exprimés en km/h et comparés à `windSpeed × 3,6`, désormais
  correct.
