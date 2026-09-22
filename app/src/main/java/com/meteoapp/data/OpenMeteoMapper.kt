package com.meteoapp.data

import com.meteoapp.data.api.WmoWeatherCodeMapper
import com.meteoapp.data.model.CurrentData
import com.meteoapp.data.model.DailyData
import com.meteoapp.data.model.HourlyData
import com.meteoapp.data.model.OpenMeteoResponse
import com.meteoapp.data.model.WeatherData
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Conversion d'une réponse Open-Meteo vers le modèle de domaine [WeatherData] :
 * prévisions horaires réelles (pas de 1 h), 7 jours réels avec lever et
 * coucher par jour, indice UV fourni par l'API (remplace l'estimateur local).
 *
 * Les températures du matin / jour / soir / nuit sont dérivées des données
 * horaires (le daily Open-Meteo ne fournit pas d'agrégats par moment du jour).
 */
object OpenMeteoMapper {

    private val ISO_LOCAL_DATE_TIME: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    private fun parseLocalDateTime(value: String): LocalDateTime =
        LocalDateTime.parse(value, ISO_LOCAL_DATE_TIME)

    private fun epochSeconds(local: LocalDateTime, utcOffsetSeconds: Long): Long =
        local.toEpochSecond(ZoneOffset.UTC) - utcOffsetSeconds

    fun toWeatherData(
        response: OpenMeteoResponse,
        lang: String
    ): WeatherData {
        val offset = response.utcOffsetSeconds
        val current = response.current
        val isDay = current.isDay == 1L

        val hourly = mapHourly(response.hourly, offset, lang)
        val daily = mapDaily(response.daily, hourly, offset, lang)

        val today = daily.firstOrNull()
        val currentData = CurrentData(
            dt = epochSeconds(parseLocalDateTime(current.time), offset),
            sunrise = today?.sunrise,
            sunset = today?.sunset,
            temp = current.temperature2m,
            feelsLike = current.apparentTemperature,
            pressure = current.pressureMsl.toLong(),
            humidity = current.relativeHumidity2m,
            visibility = null,
            windSpeed = current.windSpeed10m,
            windDeg = current.windDirection10m,
            weather = listOf(
                WmoWeatherCodeMapper.toCondition(current.weatherCode, isDay).copy(
                    description = WmoWeatherCodeMapper.descriptionFor(current.weatherCode, lang)
                )
            ),
            cloudiness = current.cloudCover,
            timezoneOffset = offset,
            uvIndex = hourly.firstOrNull()?.uvIndex
        )

        return WeatherData(
            lat = response.latitude,
            lon = response.longitude,
            timezone = response.timezone,
            timezoneOffset = offset,
            current = currentData,
            hourly = hourly,
            daily = daily
        )
    }

    /**
     * Heures passées exclues : seules les heures à partir de l'heure courante
     * (locale au lieu) sont conservées.
     */
    private fun mapHourly(
        hourly: com.meteoapp.data.model.OpenMeteoHourly,
        offset: Long,
        lang: String
    ): List<HourlyData> {
        val nowLocal = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(offset)
        val result = mutableListOf<HourlyData>()
        for (index in hourly.time.indices) {
            val local = parseLocalDateTime(hourly.time[index])
            if (local < nowLocal) continue
            val condition = WmoWeatherCodeMapper.toCondition(
                hourly.weatherCode[index],
                isDay = local.hour in 7..19
            ).copy(
                description = WmoWeatherCodeMapper.descriptionFor(hourly.weatherCode[index], lang)
            )
            result.add(
                HourlyData(
                    dt = epochSeconds(local, offset),
                    temp = hourly.temperature2m[index],
                    feelsLike = hourly.apparentTemperature[index],
                    humidity = hourly.relativeHumidity2m[index],
                    windSpeed = hourly.windSpeed10m[index],
                    windDeg = hourly.windDirection10m[index],
                    weather = listOf(condition),
                    pop = hourly.precipitationProbability[index]?.let { it / 100.0 },
                    timezoneOffset = offset,
                    pressure = hourly.pressureMsl[index].toLong(),
                    cloudiness = hourly.cloudCover[index],
                    visibility = hourly.visibility[index],
                    rainVolume = hourly.precipitation[index],
                    snowVolume = 0.0,
                    uvIndex = hourly.uvIndex[index]
                )
            )
        }
        return result
    }

    private fun mapDaily(
        daily: com.meteoapp.data.model.OpenMeteoDaily,
        hourly: List<HourlyData>,
        offset: Long,
        lang: String
    ): List<DailyData> {
        val result = mutableListOf<DailyData>()
        for (index in daily.time.indices) {
            val date = LocalDate.parse(daily.time[index])
            val condition = WmoWeatherCodeMapper.toCondition(
                daily.weatherCode[index],
                isDay = true
            ).copy(
                description = WmoWeatherCodeMapper.descriptionFor(daily.weatherCode[index], lang)
            )
            val sunriseLocal = parseLocalDateTime(daily.sunrise[index])
            val sunsetLocal = parseLocalDateTime(daily.sunset[index])
            val dayStart = epochSeconds(date.atStartOfDay(), offset)
            val dayKey = dayStartKey(dayStart, offset)
            val dayHours = hourly.filter { hour -> dayStartKey(hour.dt, offset) == dayKey }
            val byMoment = DayMoments.fromHours(dayHours)
            result.add(
                DailyData(
                    dt = dayStart,
                    sunrise = epochSeconds(sunriseLocal, offset),
                    sunset = epochSeconds(sunsetLocal, offset),
                    tempMin = daily.temperature2mMin[index],
                    tempMax = daily.temperature2mMax[index],
                    morningTemp = byMoment.morning,
                    dayTemp = byMoment.day,
                    eveningTemp = byMoment.evening,
                    nightTemp = byMoment.night,
                    feelsLikeDay = byMoment.feelsLikeDay,
                    pressure = byMoment.pressure,
                    humidity = byMoment.humidity,
                    windSpeed = byMoment.windSpeed,
                    windDeg = byMoment.windDeg,
                    weather = listOf(condition),
                    pop = daily.precipitationProbabilityMax[index]?.let { it / 100.0 },
                    timezoneOffset = offset,
                    uvIndexMax = daily.uvIndexMax[index]
                )
            )
        }
        return result
    }

    private fun dayStartKey(timestampSeconds: Long, timezoneOffsetSeconds: Long): Long =
        Instant.ofEpochSecond(timestampSeconds + timezoneOffsetSeconds)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .atStartOfDay(ZoneOffset.UTC)
            .toEpochSecond()

    private data class DayMoments(
        val morning: Double,
        val day: Double,
        val evening: Double,
        val night: Double,
        val feelsLikeDay: Double,
        val pressure: Long,
        val humidity: Long,
        val windSpeed: Double,
        val windDeg: Long
    ) {
        companion object {
            fun fromHours(hours: List<HourlyData>): DayMoments {
                val fallback = hours.firstOrNull()
                fun closest(hour: Int): Double =
                    hours.minByOrNull {
                        kotlin.math.abs(hourOfDay(it.dt, it.timezoneOffset) - hour)
                    }?.temp ?: fallback?.temp ?: 0.0
                return DayMoments(
                    morning = closest(9),
                    day = closest(15),
                    evening = closest(21),
                    night = closest(3),
                    feelsLikeDay = hours.maxByOrNull { it.feelsLike }?.feelsLike
                        ?: fallback?.feelsLike ?: 0.0,
                    pressure = hours.firstOrNull()?.pressure ?: 0L,
                    humidity = hours.map { it.humidity }.average().toLong(),
                    windSpeed = hours.maxByOrNull { it.windSpeed }?.windSpeed ?: 0.0,
                    windDeg = hours.firstOrNull()?.windDeg ?: 0L
                )
            }

            private fun hourOfDay(dt: Long, timezoneOffset: Long): Int =
                Instant.ofEpochSecond(dt + timezoneOffset).atZone(ZoneOffset.UTC).hour
        }
    }
}
