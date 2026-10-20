# BUG-004 — Hors ligne sans clé API : pas de repli sur le cache

| Champ | Valeur |
|---|---|
| Statut | Corrigé (non publié) |
| Gravité | Majeure |
| Composant | `data/WeatherRepository.kt` |
| Détecté le | 2026-10-20 (audit) |
| Corrigé le | 2026-10-20 |
| Commit / PR | Branche `ccr-0558170e-6d9gd4` |

## Symptôme

Pour un utilisateur sans clé OpenWeatherMap (cas normal depuis qu'Open-Meteo
est la source principale), une perte de réseau affiche « Clé API non
configurée » et un écran vide, au lieu de la dernière météo connue avec la
bannière hors ligne. Les widgets et les notifications sont touchés de la
même façon.

## Cause racine

Dans `WeatherRepository.fetchWeather`, après l'échec d'Open-Meteo :

```kotlin
if (!isApiKeyConfigured) {
    return WeatherResult.Error(getString(R.string.error_api_key_not_configured))
}
```

Le repli sur le cache (`cache.load`) n'existait que dans le `catch` du
chemin OpenWeatherMap, jamais atteint sans clé. Le message était en plus
trompeur : la clé est facultative, et la vraie cause est le réseau.

## Décisions

| Question | Option retenue | Option écartée |
|---|---|---|
| Message sans réseau, sans clé et sans cache | **« Erreur réseau »**, message existant (validé par le propriétaire du projet) | Garder « Clé API non configurée » : trompeur, pousse l'utilisateur vers une configuration inutile |

## Correctif

- Nouvelle fonction privée `staleCacheOrNetworkError` : dernière météo en
  cache, quel que soit son âge (`fromCache = true`, `stale = true`, donc
  bannière hors ligne), sinon erreur réseau.
- Utilisée quand Open-Meteo échoue sans clé, et dans le `catch` générique
  du chemin OpenWeatherMap, qui faisait déjà la même chose.

## Tests de non-régression

Dans `OpenMeteoRepositoryTest` :

- `getWeather_servesStaleCacheWhenOpenMeteoFailsWithoutApiKey` : cache
  servi et marqué `stale`, aucun appel à OpenWeatherMap ;
- `getWeather_networkErrorWhenOpenMeteoFailsWithoutApiKeyNorCache` : le
  message est l'erreur réseau.

## Impact et risques

- Aucun changement pour les utilisateurs qui ont une clé.
- La chaîne `error_api_key_not_configured` reste utilisée par la recherche
  de ville, la minimap et le géocodage inverse, qui exigent encore la clé
  (voir le backlog : « Clé OpenWeatherMap facultative »).
