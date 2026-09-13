package com.meteoapp.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.meteoapp.data.model.DailyData
import com.meteoapp.databinding.ActivityDayDetailBinding
import com.meteoapp.util.WeatherColors
import com.meteoapp.util.WeatherUtils
import com.squareup.moshi.Moshi

@Suppress("unused")
class DayDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DAILY_JSON = "com.meteoapp.EXTRA_DAILY_JSON"
    }

    private lateinit var binding: ActivityDayDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDayDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.dayDetailToolbar.setNavigationOnClickListener { finish() }

        val json = intent.getStringExtra(EXTRA_DAILY_JSON)
        if (json.isNullOrBlank()) {
            finish()
            return
        }

        val day: DailyData = try {
            Moshi.Builder().build().adapter(DailyData::class.java).fromJson(json)!!
        } catch (e: Exception) {
            finish()
            return
        }

        render(day)
    }

    private fun render(day: DailyData) {
        binding.dayDetailToolbar.title = WeatherUtils.formatFullDayName(day.dt, day.timezoneOffset)

        binding.dayTitle.text = WeatherUtils.formatFullDayName(day.dt, day.timezoneOffset)
        binding.dayDate.text = WeatherUtils.formatDate(day.dt, day.timezoneOffset)
        binding.dayTempMax.text = "${WeatherUtils.roundToInt(day.tempMax)}\u00b0"
        binding.dayTempMin.text = "${WeatherUtils.roundToInt(day.tempMin)}\u00b0"
        binding.dayDescription.text =
            day.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: ""

        val iconCode = day.weather.firstOrNull()?.icon
        if (!iconCode.isNullOrEmpty()) {
            Glide.with(this)
                .load(WeatherUtils.iconUrl(iconCode))
                .into(binding.dayIcon)
        }

        binding.morningTemp.text = "${WeatherUtils.roundToInt(day.morningTemp)}\u00b0"
        binding.dayTemp.text = "${WeatherUtils.roundToInt(day.dayTemp)}\u00b0"
        binding.eveningTemp.text = "${WeatherUtils.roundToInt(day.eveningTemp)}\u00b0"
        binding.nightTemp.text = "${WeatherUtils.roundToInt(day.nightTemp)}\u00b0"
        binding.feelsLike.text = "${WeatherUtils.roundToInt(day.feelsLikeDay)}\u00b0"

        binding.humidityValue.text = "${day.humidity} %"
        binding.windValue.text =
            "${WeatherUtils.kmh(day.windSpeed)} km/h ${WeatherUtils.windDirection(day.windDeg)}"
        binding.pressureValue.text = "${day.pressure} hPa"
        val pop = day.pop ?: 0.0
        binding.popValue.text = "${(pop * 100).toInt()} %"

        applyDynamicBackground(day)
    }

    private fun applyDynamicBackground(day: DailyData) {
        val grad = WeatherColors.gradient(day)
        val drawable = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(grad.top, grad.bottom)
        )
        binding.dayDetailRoot.background = drawable
        window.statusBarColor = grad.top
    }
}
