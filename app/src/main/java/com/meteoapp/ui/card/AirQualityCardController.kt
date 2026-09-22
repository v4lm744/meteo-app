package com.meteoapp.ui.card

import android.content.Context
import android.view.View
import com.meteoapp.R
import com.meteoapp.data.model.AirPollutionItem
import com.meteoapp.data.model.WeatherData
import com.meteoapp.databinding.ViewAirQualityCardBinding
import com.meteoapp.util.AirQualityUtils
import com.meteoapp.util.UvIndexEstimator
import java.util.Locale

/**
 * Collaborateur de [com.meteoapp.ui.MainActivity] pour la carte
 * « Qualité de l'air » : indice AQI OpenWeather, PM2.5, conseils et
 * indice UV (valeur réelle de l'API Open-Meteo, estimation en repli).
 */
class AirQualityCardController(
    private val binding: ViewAirQualityCardBinding
) {
    fun bind(weather: WeatherData, airQuality: AirPollutionItem?) {
        val context: Context = binding.root.context
        val aqi = airQuality?.main?.aqi
        if (aqi != null) {
            binding.airQualityValue.text = AirQualityUtils.label(context, aqi)
            binding.airQualityPm.text = airQuality.components?.let {
                context.getString(
                    R.string.pollutant_unit_ugm3,
                    AirQualityUtils.formatPm25(it.pm25)
                )
            } ?: ""
            binding.airQualityAdvice.text = AirQualityUtils.recommendation(context, aqi)
        } else {
            binding.airQualityValue.text = context.getString(R.string.comparison_unavailable)
            binding.airQualityPm.text = ""
            binding.airQualityAdvice.text = ""
        }
        val uv = weather.current.uvIndex
            ?: weather.daily.firstOrNull()?.uvIndexMax
            ?: UvIndexEstimator.estimate(
                weather.lat,
                weather.lon,
                weather.current.dt,
                weather.timezoneOffset,
                weather.current.cloudiness
            )
        binding.uvIndexValue.text = String.format(Locale.getDefault(), "%.1f", uv)
        binding.uvIndexLabel.text = UvIndexEstimator.label(context, uv)
        binding.uvAdvice.text = UvIndexEstimator.recommendation(context, uv)
        binding.root.visibility = View.VISIBLE
    }

    fun hide() {
        binding.root.visibility = View.GONE
    }
}
