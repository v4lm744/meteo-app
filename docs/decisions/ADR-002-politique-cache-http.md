# ADR-002 — Politique de cache HTTP

| Champ | Valeur |
|---|---|
| Statut | Acceptée |
| Date | 2026-10-20 |
| Décideurs | Propriétaire du projet, Claude Code |
| Origine | [BUG-003](../incidents/BUG-003-cache-http-inoperant.md) |

## Contexte

L'app a deux niveaux de cache :

1. **`WeatherCache`** (applicatif) : météo complète par ville, fraîche
   pendant 10 min et servie hors ligne pendant 3 jours au plus.
2. **Cache HTTP OkHttp** (10 Mo) : toutes les requêtes (prévisions, qualité
   de l'air, villes voisines, géocodage). Les API ne renvoient pas
   d'en-tête `Cache-Control` ; une fraîcheur de 5 min est ajoutée côté
   client.

Le cache HTTP ne fonctionnait pas (BUG-003). Une fois réparé, un
pull-to-refresh dans les 5 min aurait reçu la réponse en cache au lieu de
données nouvelles.

## Options étudiées

1. **Réparer le cache et contourner sur rafraîchissement explicite** :
   intercepteur réseau, et le pull-to-refresh envoie
   `Cache-Control: no-cache`.
2. **Supprimer le cache HTTP** : `WeatherCache` suffit pour la météo ; plus
   simple, mais qualité de l'air, minimap et géocodage ne sont plus
   mis en cache.
3. **Réparer sans contournement** : cache strict de 5 min, y compris pour
   un rafraîchissement demandé par l'utilisateur.

## Décision

Option 1.

- Fraîcheur client de 5 min (`public, max-age=300`), ajoutée par un
  **intercepteur réseau**, uniquement aux réponses réussies (2xx) sans
  `Cache-Control` serveur. Un en-tête fourni par le serveur est respecté.
- Les appels de prévisions (Open-Meteo et OpenWeatherMap) acceptent un
  en-tête de requête `Cache-Control` ; `WeatherRepository` y passe
  `no-cache` quand `forceRefresh = true` (pull-to-refresh, nouvelle
  tentative après une erreur 429).

## Conséquences

- Un geste explicite de l'utilisateur donne toujours des prévisions
  à jour.
- Qualité de l'air, villes voisines et géocodage profitent du cache
  pendant 5 min, y compris après un pull-to-refresh. C'est acceptable :
  ces données évoluent au mieux toutes les heures.
- Toute nouvelle requête qui doit pouvoir être forcée doit exposer le
  paramètre `@Header("Cache-Control")`.
