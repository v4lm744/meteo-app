package com.meteoapp.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.meteoapp.R
import com.meteoapp.data.api.ApiClient
import com.meteoapp.data.model.HourlyData
import com.meteoapp.databinding.ActivityHourDetailBinding
import com.meteoapp.util.WeatherColors
import com.meteoapp.util.WeatherUtils
import com.meteoapp.util.WeatherIcons

class HourDetailActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_HOURLY_JSON = "com.meteoapp.EXTRA_HOURLY_JSON"
    }

    private lateinit var binding: ActivityHourDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHourDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        binding.hourDetailToolbar.setNavigationOnClickListener { finish() }

        val json = intent.getStringExtra(EXTRA_HOURLY_JSON)
        if (json.isNullOrBlank()) {
            finish()
            return
        }
        val hour: HourlyData = try {
            val parsed = ApiClient.moshi.adapter(HourlyData::class.java).fromJson(json)
            if (parsed == null) {
                finish()
                return
            }
            parsed
        } catch (e: Exception) {
            finish()
            return
        }
        render(hour)
    }

    private fun render(hour: HourlyData) {
        binding.hourDetailToolbar.title = getString(R.string.hour_detail_title)
        binding.hourTitle.text = WeatherUtils.formatFullDayNameTime(this, hour.dt, hour.timezoneOffset)
        binding.hourDate.text = WeatherUtils.formatDate(hour.dt, hour.timezoneOffset)
        binding.hourTemp.text = WeatherUtils.formatTemp(this, hour.temp)
        binding.hourFeelsLike.text = WeatherUtils.formatTemp(this, hour.feelsLike)
        binding.hourDescription.text =
            hour.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: ""
        hour.weather.firstOrNull()?.let { cond ->
            WeatherIcons.bind(binding.hourIcon, cond.id, isDay = !cond.icon.endsWith("n"))
        }
        binding.feelsLikeValue.text = WeatherUtils.formatTemp(this, hour.feelsLike)
        binding.humidityValue.text = "${hour.humidity} %"
        binding.windValue.text =
            "${WeatherUtils.formatWindSpeed(this, hour.windSpeed)} ${WeatherUtils.windDirection(hour.windDeg)}"
        binding.pressureValue.text = WeatherUtils.formatPressure(this, hour.pressure)
        val pop = hour.pop ?: 0.0
        binding.popValue.text = "${(pop * 100).toInt()} %"
        binding.cloudinessValue.text = "${hour.cloudiness} %"
        val precip = hour.rainVolume + hour.snowVolume
        binding.precipValue.text = getString(R.string.mm, precip)
        applyDynamicBackground(hour)
    }

    private fun applyDynamicBackground(hour: HourlyData) {
        val grad = WeatherColors.gradient(hour)
        val drawable = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(grad.top, grad.bottom)
        )
        binding.hourDetailRoot.background = drawable
        window.statusBarColor = grad.top
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars =
            WeatherColors.contrastColor(grad.top) == 0xFF000000.toInt()
    }
}
