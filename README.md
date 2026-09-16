# Meteo App

Application Android (Kotlin) affichant la météo via l'API OpenWeatherMap,
avec un design Material Design moderne inspiré de l'application Météo France.

## Fonctionnalités

- Météo actuelle (température, ressenti, min/max, description)
- Prévisions par heure (24 h)
- Prévisions sur 7 jours
- **Minimap de la région** de la ville sélectionnée (osmdroid / OpenStreetMap) avec
  les principales villes voisines, leur icône météo et leur température ;
  tap sur une ville voisine charge sa météo, et le pull-to-refresh rafraîchit les marqueurs
- Recherche de ville (géocoding OpenWeather)
- Géolocalisation (position courante)
- **Widget d'accueil** : résumé météo du jour (icône, température, description, min/max)
  pour une ville choisie à l'ajout du widget (écran de configuration avec recherche) ;
  fond en dégradé dynamique selon la météo et l'heure (jour/nuit, soleil, nuages, pluie, neige…)
- **Synchronisation automatique des widgets** : mise à jour régulière de la météo des
  widgets en arrière-plan (WorkManager), même quand l'application est fermée ;
  l'intervalle (15 min, 30 min, 1 h, 3 h ou désactivé) se règle dans le menu
  « Paramètres ». Sans cela, les widgets ne se rafraîchissaient qu'à la fréquence
  minimale imposée par le système (≈30 min) et de façon peu fiable.
- Rafraîchissement par « pull-to-refresh »
- Affichage détaillé : humidité, vent, pression, visibilité, lever/coucher du soleil
- **Fond dynamique de l'app** : dégradé vertical en haut selon la météo et l'heure
  (jour/nuit, soleil, nuages, pluie, neige…), se fondant vers le bleu de l'app
  à mesure qu'on descend

## Configuration de la clé API

La clé OpenWeatherMap est saisie directement dans l'application (menu
« Clé API ») puis stockée de façon persistante et privée sur l'appareil
(via `SharedPreferences`). Elle n'est jamais intégrée au binaire ni
versionnée.

1. Créez un compte gratuit sur https://openweathermap.org/api
2. Récupérez votre clé API (endpoint gratuit `/data/2.5/*`)
3. Lancez l'app, ouvrez le menu « Clé API » et collez votre clé

## Endpoints OpenWeather utilisés (tier gratuit)

L'application n'utilise **pas** l'API One Call (payante) mais les endpoints gratuits :

- `GET /data/2.5/weather` — météo actuelle
- `GET /data/2.5/forecast` — prévisions 5 jours / 3 heures
- `GET /data/2.5/find` — villes proches (alimente la minimap région)
- `GET /geo/1.0/direct` — géocoding (recherche de ville)
- `GET /geo/1.0/reverse` — géocoding inverse (position → ville)

Les prévisions sur 7 jours sont reconstruites en regroupant par jour les données 3h.

> ⚠️ Une clé OpenWeather nouvellement créée peut mettre **jusqu'à 1–2h** à s'activer.
> Avant cela, l'API renvoie `401 Invalid API key` même sur les endpoints gratuits.

## Build

Ouvrez le projet dans Android Studio (Giraffe+ / Hedgehog+) puis lancez
l'application sur un émulateur ou un appareil (API 26 minimum).

```bash
./gradlew assembleDebug
```

## Architecture

- `data/` : modèles, API Retrofit, repository
- `location/` : géolocalisation (FusedLocationProvider)
- `widget/` : `WeatherWidgetProvider`, `WeatherWidgetConfigureActivity`, `WidgetPrefs`,
  `SyncPrefs`, `WidgetSyncScheduler`, `WidgetSyncWorker` (synchro auto WorkManager)
- `ui/` : MainActivity, ViewModel, adapters, dialog de recherche
- `util/` : formatage des dates/températures

Technologies : Kotlin, Coroutines, WorkManager (synchro widget), Retrofit + Moshi, Glide, Material 3,
ViewBinding, LiveData, FusedLocationProvider, osmdroid (cartes OpenStreetMap).
