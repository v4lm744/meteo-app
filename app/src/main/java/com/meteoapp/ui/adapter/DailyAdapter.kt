package com.meteoapp.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meteoapp.data.model.DailyData
import com.meteoapp.databinding.ItemDailyBinding
import com.meteoapp.util.WeatherUtils
import com.meteoapp.util.WeatherIcons

class DailyAdapter(
    private val context: Context,
    private val items: List<DailyData>,
    private val onItemClick: (DailyData) -> Unit = {}
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
        holder.itemView.setOnClickListener { onItemClick(item) }
        with(holder.binding) {
            dayText.text = if (WeatherUtils.isToday(item.dt, item.timezoneOffset)) {
                context.getString(com.meteoapp.R.string.today_short)
            } else {
                WeatherUtils.formatDayName(item.dt, item.timezoneOffset)
            }
            dayMin.text = WeatherUtils.formatTemp(context, item.tempMin)
            dayMax.text = WeatherUtils.formatTemp(context, item.tempMax)
            dayDesc.text = item.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() }
                ?: ""
            val cond = item.weather.firstOrNull()
            if (cond != null) {
                WeatherIcons.bind(dayIcon, cond.id, isDaytime = true)
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
