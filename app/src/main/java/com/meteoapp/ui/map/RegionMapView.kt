package com.meteoapp.ui.map

import android.content.Context
import android.graphics.ColorFilter
import android.graphics.PorterDuffColorFilter
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.LinearLayout
import com.meteoapp.R
import com.meteoapp.data.model.RegionCity
import com.meteoapp.util.WeatherIcons
import com.meteoapp.util.WeatherUtils
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView as OsmdroidMapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow

/**
 * Minimap régionale avec mode « Carte météo » : superposition du radar de
 * précipitations RainViewer (public, sans clé) au-dessus du fond OSM, et
 * marqueurs de villes. La bascule se fait via [setRadarEnabled].
 */
class RegionMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val mapView: ClickableMapView
    private val markersHolder = mutableListOf<Marker>()

    private var radarOverlay: TilesOverlay? = null
    private var radarEnabled = false

    var onCitySelected: ((RegionCity) -> Unit)? = null

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_region_map, this, true)
        mapView = findViewById(R.id.regionMap)
        setupMap()
        findViewById<com.google.android.material.button.MaterialButton>(R.id.radarToggle)
            ?.setOnClickListener {
                setRadarEnabled(!radarEnabled)
                updateRadarToggleLabel()
            }
    }

    private fun updateRadarToggleLabel() {
        findViewById<com.google.android.material.button.MaterialButton>(R.id.radarToggle)
            ?.setText(
                if (radarEnabled) R.string.radar_toggle_on else R.string.radar_toggle_off
            )
        findViewById<android.widget.TextView>(R.id.radarSourceNote)?.visibility =
            if (radarEnabled) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.isHorizontalMapRepetitionEnabled = false
        mapView.isVerticalMapRepetitionEnabled = false
        mapView.controller.setZoom(9.0)
        mapView.overlays.clear()

        mapView.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_POINTER_DOWN -> {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_POINTER_UP,
                MotionEvent.ACTION_CANCEL -> {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    view.performClick()
                }
            }
            false
        }
    }

    /**
     * Active/désactive la couche radar de pluie RainViewer.
     * Les tuiles proviennent du cache public v2 (chemin 2/1_1 : couleur,
     * lissé, neige en bleu).
     */
    fun setRadarEnabled(enabled: Boolean) {
        if (radarEnabled == enabled) return
        radarEnabled = enabled
        updateRadarToggleLabel()
        val overlay = radarOverlay ?: createRadarOverlay().also { radarOverlay = it }
        val idx = mapView.overlays.indexOf(overlay)
        if (enabled && idx < 0) {
            mapView.overlays.add(0, overlay)
        } else if (!enabled && idx >= 0) {
            mapView.overlays.remove(overlay)
        }
        mapView.invalidate()
    }

    fun isRadarEnabled(): Boolean = radarEnabled

    private fun createRadarOverlay(): TilesOverlay {
        val tileSource = object : OnlineTileSourceBase(
            "RainViewerRadar",
            0,
            12,
            256,
            ".png",
            arrayOf("https://tilecache.rainviewer.com")
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val zoom = org.osmdroid.util.MapTileIndex.getZoom(pMapTileIndex)
                val x = org.osmdroid.util.MapTileIndex.getX(pMapTileIndex)
                val y = org.osmdroid.util.MapTileIndex.getY(pMapTileIndex)
                return baseUrl + "/v2/radar/nowcast_300/" +
                    "256/" + zoom + "/" + x + "/" + y + "/2/1_1.png"
            }
        }
        val provider = MapTileProviderBasic(context, tileSource)
        return TilesOverlay(provider, context).apply {
            setColorFilter(ALPHA_FILTER)
            setLoadingBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
    }

    fun showCities(cities: List<RegionCity>, centerLat: Double, centerLon: Double) {
        refreshMarkersOverlay()
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

    /**
     * Retire uniquement les marqueurs (en conservant la couche radar),
     * puis les remet en fin de pile d'overlays.
     */
    private fun refreshMarkersOverlay() {
        val radar = radarOverlay
        if (radar != null && radarEnabled) {
            mapView.overlays.remove(radar)
            markersHolder.forEach { mapView.overlays.remove(it) }
            mapView.overlays.add(0, radar)
        } else {
            markersHolder.forEach { mapView.overlays.remove(it) }
        }
    }

    fun clear() {
        markersHolder.forEach { mapView.overlays.remove(it) }
        markersHolder.clear()
        mapView.invalidate()
    }

    fun onResume() {
        mapView.onResume()
    }

    fun onPause() {
        mapView.onPause()
    }

    companion object {
        private val ALPHA_FILTER: ColorFilter =
            PorterDuffColorFilter(
                android.graphics.Color.argb(200, 255, 255, 255),
                android.graphics.PorterDuff.Mode.SRC_ATOP
            )
    }

    private class WeatherMarker(
        private val mapView: OsmdroidMapView,
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
                        val iconRes = WeatherIcons.forIconCode(city.weatherIcon)
                        if (iconRes != null) {
                            val drawable = androidx.core.content.ContextCompat.getDrawable(
                                mapView.context, iconRes
                            )
                            if (drawable != null) {
                                icon.setImageDrawable(drawable)
                                (drawable as? android.graphics.drawable.Animatable)?.start()
                            }
                        }
                    }
                    temp.text = WeatherUtils.formatTemp(mapView.context, city.temp)
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

class ClickableMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : OsmdroidMapView(context, attrs) {

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
