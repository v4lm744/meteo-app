# Changelog

Toutes les modifications notables du projet sont consignées ici.

Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) et le
projet adopte le [versionnage sémantique](https://semver.org/lang/fr/).
Le détail de chaque correctif (cause racine, décisions, tests) se trouve dans
[`docs/incidents/`](docs/incidents/).

## [Non publié]

### Corrigé

- Vitesse du vent Open-Meteo affichée 3,6 fois trop forte (et fausses
  alertes « vent violent ») ; les caches météo de l'ancien format sont
  invalidés ([BUG-001](docs/incidents/BUG-001-unite-vitesse-vent.md)).

### Ajouté

- Documentation projet : CHANGELOG, fiches d'incident, registre des
  décisions et backlog dans `docs/`
  ([ADR-001](docs/decisions/ADR-001-processus-documentation.md)).

## [1.10.1] - version courante

Versions antérieures : voir les
[releases GitHub](https://github.com/v4lm744/meteo-app/releases) et
l'historique Git.
