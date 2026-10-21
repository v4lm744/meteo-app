# BUG-006 — Une alerte vue pour une ville masque celle d'une autre ville

| Champ | Valeur |
|---|---|
| Statut | Publié en v1.11.0 |
| Gravité | Mineure |
| Composant | `notifications/WeatherAlertWorker.kt`, `notifications/AlertSeenStore.kt` (nouveau) |
| Détecté le | 2026-10-20 (audit) |
| Corrigé le | 2026-10-20 |
| Commit / PR | PR #85, publiée en v1.11.0 |

## Symptôme

L'utilisateur reçoit une alerte pluie pour Paris, puis passe à Lyon (ville
courante des notifications). Une alerte pluie pour Lyon dans les 12 h
suivantes n'est jamais notifiée.

## Cause racine

L'anti-spam (`WeatherAlertWorker.filterAlreadySeen`) enregistrait l'heure
d'envoi dans les préférences `meteo_alert_seen` sous la seule clé du type
d'alerte (`rain`, `wind`, `frost`), sans la ville. L'identifiant de
notification était déjà propre à chaque ville (vague 2), mais pas
l'anti-spam.

## Décisions

| Question | Option retenue | Option écartée |
|---|---|---|
| Inclure ce bug au lot | **Oui** (validé par le propriétaire du projet) | Le laisser au backlog |
| Clé d'anti-spam | Coordonnées arrondies à 0,01° + type (`48.85,2.35:rain`), comme les clés de `WeatherCache` | Nom de la ville : ambigu (homonymes) et dépendant de la langue |
| Anciennes entrées (clé sans ville) | Purgées à leur expiration, sans migration | Migration explicite : inutile, elles expirent en 12 h au plus |

## Correctif

- Nouveau `AlertSeenStore.filterUnseen(context, lat, lon, alerts, nowMs)` :
  clé par ville et par type, fenêtre de 12 h inchangée, purge des entrées
  expirées à chaque passage (les préférences ne grossissent plus avec le
  nombre de villes).
- `WeatherAlertWorker` l'utilise à la place de sa fonction privée.

## Tests de non-régression

`AlertSeenStoreTest` (nouveau) :

- `sameAlertForSameCityIsSilencedWithinWindow` ;
- `sameAlertForAnotherCityIsNotSilenced` (cas du bug) ;
- `alertIsNotifiedAgainAfterWindow` ;
- `expiredEntriesAndLegacyKeysArePurged`.

## Impact et risques

- Juste après la mise à jour, une alerte déjà notifiée dans les 12 h peut
  être renvoyée une fois, car l'ancienne clé n'est plus reconnue. C'est
  acceptable et cela ne se produit qu'une fois.
- Relevé pendant la correction et ajouté au backlog : les alertes sont
  marquées vues avant la vérification de la permission de notification.
