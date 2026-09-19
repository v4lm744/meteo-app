package com.meteoapp.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.meteoapp.data.model.HourlyData
import com.meteoapp.databinding.ItemHourlyBinding
import com.meteoapp.util.WeatherUtils

class HourlyAdapter(
    private val context: Context,
    private val items: List<HourlyData>,
    private val onItemClick: (HourlyData) -> Unit = {}
) : RecyclerView.Adapter<HourlyAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemHourlyBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHourlyBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding).apply {
            itemView.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) onItemClick(items[pos])
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            hourText.text = WeatherUtils.formatHour(context, item.dt, item.timezoneOffset)
            hourTemp.text = WeatherUtils.formatTemp(context, item.temp)
            val iconCode = item.weather.firstOrNull()?.icon
            if (!iconCode.isNullOrEmpty()) {
                Glide.with(context)
                    .load(WeatherUtils.iconUrl(iconCode))
                    .into(hourIcon)
            }
            val pop = item.pop ?: 0.0
            if (pop >= 0.05) {
                hourPop.visibility = android.view.View.VISIBLE
                hourPop.text = "${(pop * 100).toInt()}%"
            } else {
                hourPop.visibility = android.view.View.GONE
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
