package com.meteoapp.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.meteoapp.R
import com.meteoapp.data.model.DailyData
import com.meteoapp.databinding.ItemDailyBinding
import com.meteoapp.util.WeatherIcons
import com.meteoapp.util.WeatherUtils

/**
 * Prévisions sur 7 jours : barres de température proportionnelles aux plages
 * de la semaine. Basé sur ListAdapter/DiffUtil pour ne rebinder que les
 * éléments modifiés lors des rafraîchissements de la même ville.
 */
class DailyAdapter(
    context: Context,
    items: List<DailyData>,
    currentTemp: Double? = null,
    private val onItemClick: (DailyData) -> Unit = {}
) : ListAdapter<DailyData, DailyAdapter.ViewHolder>(DIFF) {

    private val context = context.applicationContext
    private var currentTemp = currentTemp

    init {
        submitList(items)
    }

    /**
     * Met à jour les données sans recréer l'adapter ni rejouer la cascade
     * d'animation d'entrée (les plages de la semaine sont recalculées).
     */
    fun submitItems(newItems: List<DailyData>, newCurrentTemp: Double?) {
        currentTemp = newCurrentTemp
        submitList(newItems)
    }

    class ViewHolder(val binding: ItemDailyBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDailyBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.setOnClickListener { onItemClick(item) }
        val weekMin = currentList.minOfOrNull { it.tempMin } ?: 0.0
        val weekMax = currentList.maxOfOrNull { it.tempMax } ?: 1.0
        with(holder.binding) {
            dayText.text = if (WeatherUtils.isToday(item.dt, item.timezoneOffset)) {
                context.getString(R.string.today_short)
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

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<DailyData>() {
            override fun areItemsTheSame(oldItem: DailyData, newItem: DailyData) =
                oldItem.dt == newItem.dt

            override fun areContentsTheSame(oldItem: DailyData, newItem: DailyData) =
                oldItem == newItem
        }
    }
}
