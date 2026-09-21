package com.meteoapp.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meteoapp.data.model.DailyData
import com.meteoapp.R
import com.meteoapp.databinding.ItemDailyBinding
import com.meteoapp.util.WeatherUtils
import com.meteoapp.util.WeatherIcons

class DailyAdapter(
    context: Context,
    items: List<DailyData>,
    currentTemp: Double? = null,
    private val onItemClick: (DailyData) -> Unit = {}
) : RecyclerView.Adapter<DailyAdapter.ViewHolder>() {

    private val context = context.applicationContext
    private var items = items
    private var currentTemp = currentTemp

    /**
     * Met à jour les données sans recréer l'adapter ni rejouer la cascade
     * d'animation d'entrée (les plages de la semaine sont recalculées).
     */
    fun submitItems(newItems: List<DailyData>, newCurrentTemp: Double?) {
        items = newItems
        currentTemp = newCurrentTemp
        weekMinBacking = items.minOfOrNull { it.tempMin } ?: 0.0
        weekMaxBacking = items.maxOfOrNull { it.tempMax } ?: 1.0
        notifyDataSetChanged()
    }

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
                WeatherIcons.bind(dayIcon, cond.id, isDay = true)
            }
            val pop = item.pop ?: 0.0
            if (pop >= 0.05) {
                dayPop.visibility = android.view.View.VISIBLE
                dayPop.text = context.getString(R.string.format_percent, (pop * 100).toInt())
            } else {
                dayPop.visibility = android.view.View.GONE
            }
            val isToday = WeatherUtils.isToday(item.dt, item.timezoneOffset)
            dayTempBar.update(
                valueMin = item.tempMin,
                valueMax = item.tempMax,
                weekMin = weekMin,
                weekMax = weekMax,
                currentTemp = if (isToday) currentTemp else null
            )
        }
    }

    override fun getItemCount(): Int = items.size

    private var weekMinBacking: Double = items.minOfOrNull { it.tempMin } ?: 0.0
    private var weekMaxBacking: Double = items.maxOfOrNull { it.tempMax } ?: 1.0
    private val weekMin: Double get() = weekMinBacking
    private val weekMax: Double get() = weekMaxBacking
}
