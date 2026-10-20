# Documentation projet — Cirrus Skynap

Ce dossier garde la trace des évolutions du projet : incidents corrigés,
décisions prises et travail restant. Chaque modification du code est livrée
avec sa documentation dans le **même commit**.

## Organisation

| Emplacement | Rôle | Inspiration |
|---|---|---|
| [`../CHANGELOG.md`](../CHANGELOG.md) | Journal des versions, orienté utilisateur | Keep a Changelog |
| [`incidents/`](incidents/) | Une fiche par bug : symptôme, cause racine, correctif, tests | ITIL — gestion des incidents et des problèmes |
| [`decisions/`](decisions/) | Une fiche par décision structurante (ADR) | Architecture Decision Records |
| [`backlog.md`](backlog.md) | Travail identifié mais pas encore planifié | Product backlog (Agile) |

## Processus

1. **Identification** : le problème est consigné dans le backlog ou
   directement dans une fiche d'incident (`incidents/BUG-NNN-titre.md`, à
   partir de [`incidents/TEMPLATE.md`](incidents/TEMPLATE.md)).
2. **Décision** : quand un correctif implique un choix (comportement visible,
   compromis technique), l'option retenue est validée avec le propriétaire
   du projet puis consignée dans la fiche d'incident. Si le choix engage
   l'architecture au-delà du bug, une ADR est rédigée
   (`decisions/ADR-NNN-titre.md`, à partir de
   [`decisions/TEMPLATE.md`](decisions/TEMPLATE.md)).
3. **Réalisation** : un commit par incident, contenant le code, les tests de
   non-régression, la fiche et l'entrée du CHANGELOG (section
   `[Non publié]`).
4. **Revue et livraison** : les commits sont regroupés dans une pull request
   relue puis fusionnée ; la CI (tests unitaires, detekt, tests
   instrumentés) doit être verte.
5. **Publication** : à la release, la section `[Non publié]` du CHANGELOG
   devient la nouvelle version.

## Index des incidents

| ID | Titre | Gravité | Statut |
|---|---|---|---|
| [BUG-001](incidents/BUG-001-unite-vitesse-vent.md) | Vent Open-Meteo affiché 3,6 fois trop fort | Majeure | Corrigé (non publié) |
| [BUG-002](incidents/BUG-002-cumul-pluie-alertes.md) | Cumul de pluie et fenêtre des alertes faux | Majeure | Corrigé (non publié) |
| [BUG-003](incidents/BUG-003-cache-http-inoperant.md) | Cache HTTP OkHttp inopérant | Mineure | Corrigé (non publié) |

## Index des décisions

| ID | Titre | Statut |
|---|---|---|
| [ADR-001](decisions/ADR-001-processus-documentation.md) | Processus de documentation et de livraison | Acceptée |
| [ADR-002](decisions/ADR-002-politique-cache-http.md) | Politique de cache HTTP | Acceptée |
