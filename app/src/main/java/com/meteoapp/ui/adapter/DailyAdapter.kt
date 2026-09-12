package com.meteoapp.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.meteoapp.data.model.DailyData
import com.meteoapp.databinding.ItemDailyBinding
import com.meteoapp.util.WeatherUtils

class DailyAdapter(
    private val context: Context,
    private val items: List<DailyData>
) : RecyclerView.Adapter<DailyAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDailyBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDailyBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            dayText.text = if (WeatherUtils.isToday(item.dt, item.timezoneOffset)) {
                "Auj."
            } else {
                WeatherUtils.formatDayName(item.dt, item.timezoneOffset)
            }
            dayMin.text = "${WeatherUtils.roundToInt(item.tempMin)}°"
            dayMax.text = "${WeatherUtils.roundToInt(item.tempMax)}°"
            dayDesc.text = item.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() }
                ?: ""
            val iconCode = item.weather.firstOrNull()?.icon
            if (!iconCode.isNullOrEmpty()) {
                Glide.with(context)
                    .load(WeatherUtils.iconUrl(iconCode))
                    .into(dayIcon)
            }
            val pop = item.pop ?: 0.0
            if (pop >= 0.05) {
                dayPop.visibility = android.view.View.VISIBLE
                dayPop.text = "${(pop * 100).toInt()}%"
            } else {
                dayPop.visibility = android.view.View.GONE
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
