package com.meteoapp.data.api

import com.meteoapp.data.model.WeatherCondition

/**
 * Conversion des codes météo WMO 2 (Open-Meteo) vers les conditions
 * OpenWeatherMap (identifiant + icône) afin de conserver le rendu existant
 * (icônes, dégradés, évaluateur d'alertes) sans double système visuel.
 *
 * Les descriptions sont localisées côté interface via [descriptionFor].
 */
object WmoWeatherCodeMapper {

    /**
     * @param isDay true le jour (icône « d »), false la nuit (icône « n »).
     */
    fun toCondition(code: Long, isDay: Boolean): WeatherCondition {
        val (id, main, icon) = when (code.toInt()) {
            0 -> Triple(800L, "Clear", if (isDay) "01d" else "01n")
            1 -> Triple(801L, "Clouds", if (isDay) "02d" else "02n")
            2 -> Triple(802L, "Clouds", if (isDay) "03d" else "03n")
            3 -> Triple(803L, "Clouds", if (isDay) "03d" else "03n")
            45, 48 -> Triple(741L, "Fog", if (isDay) "50d" else "50n")
            51, 53, 55 -> Triple(300L, "Drizzle", if (isDay) "09d" else "09n")
            56, 57 -> Triple(511L, "Drizzle", if (isDay) "09d" else "09n")
            61 -> Triple(500L, "Rain", if (isDay) "10d" else "10n")
            63 -> Triple(501L, "Rain", if (isDay) "10d" else "10n")
            65 -> Triple(502L, "Rain", if (isDay) "10d" else "10n")
            66, 67 -> Triple(511L, "Rain", if (isDay) "10d" else "10n")
            71, 73, 75 -> Triple(601L, "Snow", if (isDay) "13d" else "13n")
            77 -> Triple(611L, "Snow", if (isDay) "13d" else "13n")
            80 -> Triple(500L, "Rain", if (isDay) "10d" else "10n")
            81 -> Triple(501L, "Rain", if (isDay) "10d" else "10n")
            82 -> Triple(502L, "Rain", if (isDay) "10d" else "10n")
            85, 86 -> Triple(601L, "Snow", if (isDay) "13d" else "13n")
            95 -> Triple(211L, "Thunderstorm", if (isDay) "11d" else "11n")
            96, 99 -> Triple(212L, "Thunderstorm", if (isDay) "11d" else "11n")
            else -> Triple(804L, "Clouds", if (isDay) "04d" else "04n")
        }
        return WeatherCondition(
            id = id,
            main = main,
            description = descriptionFor(code),
            icon = icon
        )
    }

    /**
     * Description localisée du code WMO : français et anglais fournis par
     * la librairie, les autres langues retombent sur le français.
     */
    fun descriptionFor(code: Long, lang: String = "fr"): String {
        val en = lang == "en"
        return when (code.toInt()) {
            0 -> if (en) "Clear sky" else "Ciel dégagé"
            1 -> if (en) "Mainly clear" else "Peu nuageux"
            2 -> if (en) "Partly cloudy" else "Partiellement nuageux"
            3 -> if (en) "Overcast" else "Couvert"
            45 -> if (en) "Fog" else "Brouillard"
            48 -> if (en) "Depositing rime fog" else "Brouillard givrant"
            51 -> if (en) "Light drizzle" else "Bruine légère"
            53 -> if (en) "Moderate drizzle" else "Bruine modérée"
            55 -> if (en) "Dense drizzle" else "Bruine dense"
            56 -> if (en) "Light freezing drizzle" else "Bruine verglaçante légère"
            57 -> if (en) "Dense freezing drizzle" else "Bruine verglaçante dense"
            61 -> if (en) "Slight rain" else "Pluie légère"
            63 -> if (en) "Moderate rain" else "Pluie modérée"
            65 -> if (en) "Heavy rain" else "Fortes pluies"
            66 -> if (en) "Light freezing rain" else "Pluie verglaçante légère"
            67 -> if (en) "Heavy freezing rain" else "Pluie verglaçante forte"
            71 -> if (en) "Slight snowfall" else "Neige légère"
            73 -> if (en) "Moderate snowfall" else "Neige modérée"
            75 -> if (en) "Heavy snowfall" else "Fortes chutes de neige"
            77 -> if (en) "Snow grains" else "Grains de neige"
            80 -> if (en) "Slight rain showers" else "Averses légères"
            81 -> if (en) "Moderate rain showers" else "Averses modérées"
            82 -> if (en) "Violent rain showers" else "Averses violentes"
            85 -> if (en) "Slight snow showers" else "Averses de neige légères"
            86 -> if (en) "Heavy snow showers" else "Averses de neige fortes"
            95 -> if (en) "Thunderstorm" else "Orage"
            96 -> if (en) "Thunderstorm with slight hail" else "Orage avec grêle légère"
            99 -> if (en) "Thunderstorm with heavy hail" else "Orage avec forte grêle"
            else -> if (en) "Clouds" else "Nuages"
        }
    }
}
