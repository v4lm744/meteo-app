package com.meteoapp.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.meteoapp.data.api.ApiClient
import com.meteoapp.data.model.DailyData
import com.meteoapp.databinding.ActivityDayDetailBinding
import com.meteoapp.util.WeatherColors
import com.meteoapp.util.WeatherUtils

class DayDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DAILY_JSON = "com.meteoapp.EXTRA_DAILY_JSON"
    }

    private lateinit var binding: ActivityDayDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDayDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Plein écran : le fond dégradé remplit l'écran sous la barre d'état
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)

        binding.dayDetailToolbar.setNavigationOnClickListener { finish() }

        val json = intent.getStringExtra(EXTRA_DAILY_JSON)
        if (json.isNullOrBlank()) {
            finish()
            return
        }

        val day: DailyData = try {
            val parsed = ApiClient.moshi.adapter(DailyData::class.java).fromJson(json)
            if (parsed == null) {
                finish()
                return
            }
            parsed
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
        binding.dayTempMax.text = WeatherUtils.formatTemp(this, day.tempMax)
        binding.dayTempMin.text = WeatherUtils.formatTemp(this, day.tempMin)
        binding.dayDescription.text =
            day.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: ""

        val iconCode = day.weather.firstOrNull()?.icon
        if (!iconCode.isNullOrEmpty()) {
            Glide.with(this)
                .load(WeatherUtils.iconUrl(iconCode))
                .into(binding.dayIcon)
        }

        binding.morningTemp.text = WeatherUtils.formatTemp(this, day.morningTemp)
        binding.dayTemp.text = WeatherUtils.formatTemp(this, day.dayTemp)
        binding.eveningTemp.text = WeatherUtils.formatTemp(this, day.eveningTemp)
        binding.nightTemp.text = WeatherUtils.formatTemp(this, day.nightTemp)
        binding.feelsLike.text = WeatherUtils.formatTemp(this, day.feelsLikeDay)

        binding.humidityValue.text = "${day.humidity} %"
        binding.windValue.text =
            "${WeatherUtils.formatWindSpeed(this, day.windSpeed)} ${WeatherUtils.windDirection(day.windDeg)}"
        binding.pressureValue.text = WeatherUtils.formatPressure(this, day.pressure)
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
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars =
            com.meteoapp.util.WeatherColors.contrastColor(grad.top) == 0xFF000000.toInt()
    }
}
