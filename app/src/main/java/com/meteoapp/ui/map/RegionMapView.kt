package com.meteoapp.ui.map

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.bumptech.glide.Glide
import com.meteoapp.R
import com.meteoapp.data.model.RegionCity
import com.meteoapp.util.WeatherUtils
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow

class RegionMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val mapView: MapView
    private val markersHolder = mutableListOf<Marker>()

    var onCitySelected: ((RegionCity) -> Unit)? = null

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_region_map, this, true)
        mapView = findViewById(R.id.regionMap)
        setupMap()
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.isHorizontalMapRepetitionEnabled = false
        mapView.isVerticalMapRepetitionEnabled = false
        mapView.controller.setZoom(9.0)
        mapView.overlays.clear()
    }

    fun showCities(cities: List<RegionCity>, centerLat: Double, centerLon: Double) {
        mapView.overlays.clear()
        markersHolder.clear()

        for (city in cities) {
            val point = GeoPoint(city.lat, city.lon)
            val marker = WeatherMarker(mapView, city) { selected ->
                onCitySelected?.invoke(selected)
            }
            marker.position = point
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = city.name
            mapView.overlays.add(marker)
            markersHolder.add(marker)
        }

        mapView.controller.setCenter(GeoPoint(centerLat, centerLon))
        mapView.controller.setZoom(9.0)
        mapView.invalidate()
    }

    fun clear() {
        mapView.overlays.clear()
        markersHolder.clear()
        mapView.invalidate()
    }

    fun onResume() {
        mapView.onResume()
    }

    fun onPause() {
        mapView.onPause()
    }

    private class WeatherMarker(
        private val mapView: MapView,
        private val city: RegionCity,
        private val onSelect: (RegionCity) -> Unit
    ) : Marker(mapView) {

        init {
            infoWindow = object : MarkerInfoWindow(R.layout.marker_weather, mapView) {
                override fun onOpen(item: Any?) {
                    val view = mView ?: return
                    val icon = view.findViewById<android.widget.ImageView>(R.id.markerIcon)
                    val temp = view.findViewById<android.widget.TextView>(R.id.markerTemp)
                    val name = view.findViewById<android.widget.TextView>(R.id.markerName)
                    if (city.weatherIcon.isNotEmpty()) {
                        Glide.with(mapView.context)
                            .load(WeatherUtils.iconUrl(city.weatherIcon))
                            .into(icon)
                    }
                    temp.text = "${WeatherUtils.roundToInt(city.temp)}°"
                    name.text = city.name
                }

                override fun onClose() {}
            }
            setOnMarkerClickListener { m, _ ->
                showInfoWindow()
                mapView.controller.animateTo(m.position)
                onSelect(city)
                true
            }
        }
    }
}
