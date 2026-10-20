# BUG-005 — Chargements concurrents : une réponse tardive écrase la ville affichée

| Champ | Valeur |
|---|---|
| Statut | Corrigé (non publié) |
| Gravité | Mineure |
| Composant | `ui/WeatherViewModel.kt`, `util/LatestJobRunner.kt` (nouveau) |
| Détecté le | 2026-10-20 (audit) |
| Corrigé le | 2026-10-20 |
| Commit / PR | Branche `ccr-0558170e-6d9gd4` |

## Symptôme

1. L'utilisateur lance la géolocalisation puis touche une ville favorite
   avant la fin. Quand la position arrive, elle remplace la ville choisie et
   devient la « ville courante » (widgets, notifications).
2. *(Trouvé pendant la correction.)* Lors d'un changement rapide de ville,
   la qualité de l'air ou la minimap de la ville précédente peut s'afficher
   sur la nouvelle ville si sa réponse arrive après.

## Cause racine

- `loadWeatherForCity` et `refresh` annulaient `weatherJob` avant d'en
  lancer un nouveau, mais `loadForCurrentLocation` lançait sa coroutine
  directement dans `viewModelScope`. Elle n'était ni annulée par les autres
  chargements, ni capable de les annuler.
- `loadRegionCities` et `loadAirQuality` lançaient des coroutines
  indépendantes dans `viewModelScope`, jamais annulées.

## Décisions

| Question | Option retenue | Option écartée |
|---|---|---|
| Portée du correctif | Inclure le cas 2 (même cause racine), signalé au propriétaire du projet | Fiche séparée : même correctif, double travail |
| Mécanisme | Petite classe `LatestJobRunner` (« le dernier gagne ») partagée par les trois points d'entrée, testable sans Android | Ajouter `weatherJob?.cancel()` dans `loadForCurrentLocation` : corrige le cas 1, mais le prochain point d'entrée ajouté risque de l'oublier |

## Correctif

- `util/LatestJobRunner` : `launch { }` annule la coroutine précédente
  avant d'en lancer une nouvelle.
- `WeatherViewModel` : `loadWeatherForCity`, `refresh` et
  `loadForCurrentLocation` passent tous par ce lanceur.
- `loadRegionCities` et `loadAirQuality` deviennent des extensions de
  `CoroutineScope`. Leurs coroutines sont des enfants du chargement en
  cours et sont annulées avec lui.
- Chaque point d'entrée fixe explicitement `loading` et `refreshing`. Sans
  cela, un chargement annulé aurait pu laisser l'indicateur de chargement
  affiché indéfiniment.

## Tests de non-régression

`LatestJobRunnerTest` (nouveau) :

- `newLaunchCancelsPreviousOne` : le chargement de la position, plus lent,
  est annulé et seule la ville choisie s'affiche ;
- `childrenOfCancelledLaunchAreCancelled` : la qualité de l'air de
  l'ancienne ville n'est pas affichée ;
- `completedLaunchDoesNotAffectNextOne`.

Le ViewModel n'a pas de test direct : il dépend du `ServiceLocator` et du
fournisseur de position Google. Voir le backlog : « Injection de
dépendances ».

## Impact et risques

- Un pull-to-refresh lancé pendant un chargement l'annule et le remplace :
  même comportement qu'avant pour `loadWeatherForCity`, désormais étendu
  à la géolocalisation.
- L'enregistrement de l'historique (`recordHistory`) reste indépendant et
  n'est pas annulé : les données reçues sont valides.
