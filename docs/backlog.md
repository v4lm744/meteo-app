# Backlog produit

Éléments identifiés lors de l'audit du 2026-10-20, pas encore planifiés.
Priorité : ⭐⭐⭐ haute, ⭐⭐ moyenne, ⭐ basse.

## Anomalies mineures

| Priorité | Élément | Détail |
|---|---|---|
| ⭐⭐ | Icônes jour/nuit des prévisions horaires | `OpenMeteoMapper` considère qu'il fait jour de 7 h à 19 h au lieu d'utiliser le lever et le coucher du soleil |
| ⭐ | Clignotement des cartes au changement de ville | `loadWeatherForCity` recrée un `UiState` vide : qualité de l'air et minimap disparaissent puis réapparaissent |

## Améliorations techniques

| Priorité | Élément | Détail |
|---|---|---|
| ⭐⭐⭐ | Clé OpenWeatherMap facultative | Recherche via l'API de géocodage Open-Meteo, géocodage inverse via `android.location.Geocoder` |
| ⭐⭐ | Découpage de `MainActivity` | 668 lignes : poursuivre l'extraction en contrôleurs (`ui/card/`) |
| ⭐⭐ | Injection de dépendances | Remplacer le `ServiceLocator` pour tester le ViewModel |
| ⭐ | Clé de signature de release dédiée | Le keystore partagé utilise le mot de passe public « android » |
| ⭐ | Vérification des traductions en CI | Garantir que `values/` et `values-en/` gardent les mêmes chaînes |

## Fonctionnalités

| Priorité | Élément | Détail |
|---|---|---|
| ⭐⭐⭐ | Vigilances officielles | Météo-France ou flux CAP/MeteoAlarm |
| ⭐⭐⭐ | Rafales de vent | `wind_gusts_10m` est déjà récupéré mais pas affiché |
| ⭐⭐⭐ | Prévisions sur 14 à 16 jours | Paramètre `forecast_days` d'Open-Meteo |
| ⭐⭐ | Radar des précipitations | Tuiles RainViewer sur la minimap osmdroid |
| ⭐⭐ | Pollens | API qualité de l'air Open-Meteo (Europe) |
| ⭐⭐ | Point de rosée, indice de confort, heure dorée | Données disponibles ou calculables |
| ⭐⭐ | Historique « il y a un an » | API Archive Open-Meteo |
| ⭐⭐ | Notification « pluie dans 15 min » | Données minute par minute déjà récupérées |
| ⭐ | Conseils dans le résumé quotidien | « Prenez un parapluie », « Crème solaire conseillée » |
| ⭐ | Météo pour les activités | Course, vélo, séchage du linge |
| ⭐ | Tuile Réglages rapides, complication Wear OS | |
| ⭐ | Cartes personnalisables | Ordre et visibilité des cartes |
