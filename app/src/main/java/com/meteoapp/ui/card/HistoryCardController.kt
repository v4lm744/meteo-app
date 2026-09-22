package com.meteoapp.ui.card

import android.content.Context
import android.view.View
import com.meteoapp.R
import com.meteoapp.data.model.WeatherData
import com.meteoapp.databinding.ViewHistoryCardBinding
import com.meteoapp.stats.WeatherHistoryStore
import com.meteoapp.util.WeatherUtils
import java.util.Locale

/**
 * Collaborateur de [com.meteoapp.ui.MainActivity] pour la carte
 * « Historique » : graphe des 7 derniers jours, records absolus et
 * comparaison avec la semaine précédente.
 */
class HistoryCardController(
    private val binding: ViewHistoryCardBinding
) {
    fun bind(weather: WeatherData) {
        val context: Context = binding.root.context
        val records = WeatherHistoryStore.last7Days(context, weather.lat, weather.lon)
        if (records.size < 2) {
            binding.historyChart.submit(emptyList())
            binding.historyRecords.text = context.getString(R.string.history_empty)
            binding.historyWeekCompare.visibility = View.GONE
            binding.root.visibility = View.VISIBLE
            return
        }
        binding.historyChart.submit(records)
        val recordsText = buildString {
            WeatherHistoryStore.allTimeMin(context, weather.lat, weather.lon)?.let {
                append(
                    context.getString(
                        R.string.history_record_min,
                        WeatherUtils.formatTemp(context, it.tempMin),
                        WeatherUtils.formatDate(it.dayKey, 0)
                    )
                )
                append('\n')
            }
            WeatherHistoryStore.allTimeMax(context, weather.lat, weather.lon)?.let {
                append(
                    context.getString(
                        R.string.history_record_max,
                        WeatherUtils.formatTemp(context, it.tempMax),
                        WeatherUtils.formatDate(it.dayKey, 0)
                    )
                )
            }
        }
        binding.historyRecords.text = recordsText.trim()
        WeatherHistoryStore.weekComparison(context, weather.lat, weather.lon)?.let { (current, previous) ->
            val delta = String.format(Locale.getDefault(), "%.1f°C", kotlin.math.abs(current - previous))
            binding.historyWeekCompare.text = context.getString(
                if (current >= previous) R.string.history_week_compare_warmer
                else R.string.history_week_compare_cooler,
                delta
            )
            binding.historyWeekCompare.visibility = View.VISIBLE
        } ?: run {
            binding.historyWeekCompare.visibility = View.GONE
        }
        binding.root.visibility = View.VISIBLE
    }

    fun hide() {
        binding.root.visibility = View.GONE
    }
}
