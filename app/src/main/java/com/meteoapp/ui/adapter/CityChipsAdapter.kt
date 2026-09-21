package com.meteoapp.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.meteoapp.R
import com.meteoapp.data.model.GeoLocation

/**
 * Puces horizontales des villes favorites : la ville active est mise en
 * évidence (fond plus opaque), un tap charge la ville correspondante,
 * un appui long la retire des favoris.
 */
class CityChipsAdapter(
    private val onCityClick: (GeoLocation) -> Unit,
    private val onCityLongClick: (GeoLocation) -> Unit
) : ListAdapter<GeoLocation, CityChipsAdapter.ChipViewHolder>(DIFF) {

    private var activeLat: Double = Double.NaN
    private var activeLon: Double = Double.NaN

    fun submitList(cities: List<GeoLocation>, active: GeoLocation?) {
        activeLat = active?.lat ?: Double.NaN
        activeLon = active?.lon ?: Double.NaN
        submitList(cities)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_city_chip, parent, false) as TextView
        return ChipViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        val city = getItem(position)
        holder.bind(city, city.lat == activeLat && city.lon == activeLon)
    }

    inner class ChipViewHolder(private val view: TextView) : RecyclerView.ViewHolder(view) {
        fun bind(city: GeoLocation, isActive: Boolean) {
            view.text = city.displayName(view.context)
            view.background.setTint(
                if (isActive) ContextCompat.getColor(view.context, R.color.md_white)
                else ContextCompat.getColor(view.context, android.R.color.transparent)
            )
            view.background.alpha = if (isActive) 230 else 120
            view.setTextColor(
                ContextCompat.getColor(view.context, R.color.md_text_dark)
            )
            view.setOnClickListener { onCityClick(city) }
            view.setOnLongClickListener {
                onCityLongClick(city)
                true
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<GeoLocation>() {
            override fun areItemsTheSame(oldItem: GeoLocation, newItem: GeoLocation): Boolean =
                oldItem.lat == newItem.lat && oldItem.lon == newItem.lon

            override fun areContentsTheSame(oldItem: GeoLocation, newItem: GeoLocation): Boolean =
                oldItem == newItem
        }
    }
}
