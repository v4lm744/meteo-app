# ADR-001 — Processus de documentation et de livraison

| Champ | Valeur |
|---|---|
| Statut | Acceptée |
| Date | 2026-10-20 |
| Décideurs | Propriétaire du projet, Claude Code |

## Contexte

Le projet n'avait ni CHANGELOG ni documentation des modifications : seul
l'historique Git gardait la trace des corrections. Le projet est désormais
mené en mode projet (pratiques ITIL et Agile), avec une validation explicite
de chaque décision et une trace écrite de chaque modification.

## Options étudiées

1. **CHANGELOG + `docs/`** (fiches d'incident et ADR) : traçabilité
   complète (cause racine, décisions, tests), un peu plus de rédaction.
2. **CHANGELOG seul** : léger, mais ni les causes racines ni les
   décisions n'y figurent.
3. **Section dans le README** : visible, mais le README devient un journal
   et perd sa fonction de présentation.

## Décision

Option 1 :

- `CHANGELOG.md` au format Keep a Changelog ;
- `docs/incidents/` : une fiche par bug (format ITIL) ;
- `docs/decisions/` : une ADR par décision structurante ;
- `docs/backlog.md` : travail identifié non planifié.

Livraison : **un commit par incident** (code, tests, fiche, CHANGELOG) sur
une branche dédiée, regroupés dans **une pull request** relue avant fusion.

## Conséquences

- Chaque commit est autonome et peut être relu ou annulé seul.
- Les décisions validées restent consultables sans fouiller les PR.
- La rédaction des fiches fait partie de la définition de « terminé ».
