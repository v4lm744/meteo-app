package com.meteoapp.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.meteoapp.R
import com.meteoapp.data.model.HourlyData
import com.meteoapp.databinding.ItemHourlyBinding
import com.meteoapp.util.WeatherIcons
import com.meteoapp.util.WeatherUtils

/**
 * Prévisions horaires : liste horizontale des 48 prochaines heures. Basé sur
 * ListAdapter/DiffUtil : les rafraîchissements de la même ville (qualité de
 * l'air, minimap) mettent à jour uniquement les éléments modifiés et
 * conservent la position de défilement.
 */
class HourlyAdapter(
    context: Context,
    items: List<HourlyData>,
    private val onItemClick: (HourlyData) -> Unit = {}
) : ListAdapter<HourlyData, HourlyAdapter.ViewHolder>(DIFF) {

    private val context = context.applicationContext

    init {
        submitList(items)
    }

    class ViewHolder(val binding: ItemHourlyBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHourlyBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding).apply {
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onItemClick(getItem(pos))
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            hourText.text = WeatherUtils.formatHour(context, item.dt, item.timezoneOffset)
            hourTemp.text = WeatherUtils.formatTemp(context, item.temp)
            val cond = item.weather.firstOrNull()
            if (cond != null) {
                WeatherIcons.bind(
                    hourIcon,
                    cond.id,
                    isDay = !cond.icon.endsWith("n")
                )
            }
            val pop = item.pop ?: 0.0
            if (pop >= 0.05) {
                hourPop.visibility = View.VISIBLE
                hourPop.text = context.getString(R.string.format_percent, (pop * 100).toInt())
            } else {
                hourPop.visibility = View.GONE
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<HourlyData>() {
            override fun areItemsTheSame(oldItem: HourlyData, newItem: HourlyData) =
                oldItem.dt == newItem.dt

            override fun areContentsTheSame(oldItem: HourlyData, newItem: HourlyData) =
                oldItem == newItem
        }
    }
}
