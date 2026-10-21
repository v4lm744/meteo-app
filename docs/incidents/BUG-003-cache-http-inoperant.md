# BUG-003 — Cache HTTP OkHttp inopérant

| Champ | Valeur |
|---|---|
| Statut | Publié en v1.11.0 |
| Gravité | Mineure (performance, consommation de données) |
| Composant | `data/api/ApiClient.kt`, `data/api/*Api.kt`, `data/WeatherRepository.kt` |
| Détecté le | 2026-10-20 (audit) |
| Corrigé le | 2026-10-20 |
| Commit / PR | PR #85, publiée en v1.11.0 |

## Symptôme

Pas de symptôme visible : chaque requête repartait sur le réseau, alors
que le cache HTTP de 10 Mo introduit en vague 2 (commit `f79c2cc`) devait
l'éviter pendant 5 min. Effets : consommation de données et de batterie
plus élevée, quota d'API plus sollicité (widgets, workers, minimap,
qualité de l'air).

## Cause racine

`ApiClient` ajoutait l'en-tête `Cache-Control: public, max-age=300` aux
réponses via `addInterceptor`, un intercepteur **applicatif**. Dans OkHttp,
la décision de stockage est prise par le `CacheInterceptor` interne, placé
**avant** les intercepteurs réseau et **après** les intercepteurs
applicatifs. L'en-tête ajouté arrivait donc trop tard : la réponse sans
`Cache-Control` n'était jamais stockée.

## Décisions

Voir [ADR-002](../decisions/ADR-002-politique-cache-http.md). En résumé, le
propriétaire du projet a validé : **réparer le cache et le contourner sur
pull-to-refresh**, plutôt que le supprimer ou le rendre strict.

## Correctif

- L'intercepteur devient un **intercepteur réseau**
  (`addNetworkInterceptor`). Il ne s'applique qu'aux réponses réussies, pour
  ne pas mettre en cache une erreur serveur.
- La construction du client est extraite dans
  `ApiClient.buildOkHttpClient(cacheDir)`, testable.
- `OpenMeteoApi.getForecast`, `OpenWeatherApi.getCurrentWeather` et
  `OpenWeatherApi.getForecast` acceptent `@Header("Cache-Control")`.
  `WeatherRepository` passe `ApiClient.NO_CACHE` quand `forceRefresh` vaut
  `true`.

## Tests de non-régression

- `ApiClientHttpCacheTest` (nouveau) : réponse réussie servie depuis le
  cache, contournement par `no-cache`, erreurs non mises en cache, en-tête
  serveur respecté.
- `OpenMeteoRepositoryTest.getWeather_forceRefreshSendsNoCacheHeader` et
  `getWeather_withoutForceRefreshLetsHttpCacheAnswer`.

## Impact et risques

- Qualité de l'air et villes voisines peuvent dater de 5 min au plus après
  un pull-to-refresh (voir ADR-002).
- Le cache HTTP vit dans le répertoire de cache de l'app : Android peut le
  vider en cas de manque d'espace, sans effet fonctionnel.
