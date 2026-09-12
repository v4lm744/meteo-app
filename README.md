# Meteo App

Application Android (Kotlin) affichant la météo via l'API OpenWeatherMap,
avec un design Material Design moderne inspiré de l'application Météo France.

## Fonctionnalités

- Météo actuelle (température, ressenti, min/max, description)
- Prévisions par heure (24 h)
- Prévisions sur 7 jours
- Recherche de ville (géocoding OpenWeather)
- Géolocalisation (position courante)
- Rafraîchissement par « pull-to-refresh »
- Affichage détaillé : humidité, vent, pression, visibilité, lever/coucher du soleil

## Configuration de la clé API

La clé OpenWeatherMap est chargée via `BuildConfig` depuis le fichier
`local.properties` (non versionné) pour ne jamais être commitée.

1. Créez un compte gratuit sur https://openweathermap.org/api
2. Récupérez votre clé API (One Call API 3.0 ou API classique)
3. À la racine du projet, créez/éditez `local.properties` :

```properties
sdk.dir=/chemin/vers/Android/Sdk
OPEN_WEATHER_API_KEY=votre_cle_api_ici
```

> ⚠️ L'API One Call 2.5 (utilisée ici) nécessite une clé valide.
> N'ajoutez jamais `local.properties` à git (il est dans `.gitignore`).

## Endpoints OpenWeather utilisés (tier gratuit)

L'application n'utilise **pas** l'API One Call (payante) mais les endpoints gratuits :

- `GET /data/2.5/weather` — météo actuelle
- `GET /data/2.5/forecast` — prévisions 5 jours / 3 heures
- `GET /geo/1.0/direct` — géocoding (recherche de ville)
- `GET /geo/1.0/reverse` — géocoding inverse (position → ville)

Les prévisions sur 7 jours sont reconstruites en regroupant par jour les données 3h.

> ⚠️ Une clé OpenWeather nouvellement créée peut mettre **jusqu'à 1–2h** à s'activer.
> Avant cela, l'API renvoie `401 Invalid API key` même sur les endpoints gratuits.

## Build

Ouvrez le projet dans Android Studio (Giraffe+ / Hedgehog+) puis lancez
l'application sur un émulateur ou un appareil (API 24 minimum).

```bash
./gradlew assembleDebug
```

## Architecture

- `data/` : modèles, API Retrofit, repository
- `location/` : géolocalisation (FusedLocationProvider)
- `ui/` : MainActivity, ViewModel, adapters, dialog de recherche
- `util/` : formatage des dates/températures

Technologies : Kotlin, Coroutines, Retrofit + Moshi, Glide, Material 3,
ViewBinding, LiveData, FusedLocationProvider.
