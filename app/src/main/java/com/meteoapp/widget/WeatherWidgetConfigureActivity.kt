package com.meteoapp.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.meteoapp.R
import com.meteoapp.data.WeatherResult
import com.meteoapp.data.WeatherRepository
import com.meteoapp.data.model.GeoLocation
import com.meteoapp.databinding.DialogSearchBinding
import com.meteoapp.databinding.ItemSearchResultBinding
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WeatherWidgetConfigureActivity : AppCompatActivity() {

    private lateinit var binding: DialogSearchBinding
    private val repository by lazy { WeatherRepository(this) }
    private var searchJob: Job? = null
    private var searchSequence = 0
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var widgetTheme: WidgetThemePrefs.Theme = WidgetThemePrefs.Theme.WEATHER_GRADIENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        binding = DialogSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setTitle(R.string.widget_title)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        binding.resultsRecycler.layoutManager = LinearLayoutManager(this)

        binding.cityInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(binding.cityInput.text.toString())
                true
            } else false
        }
        binding.cityInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(450)
                    performSearch(s?.toString() ?: "")
                }
            }
        })

        binding.cityInputLayout.requestFocus()

        binding.widgetThemeLabel.visibility = View.VISIBLE
        binding.widgetThemeGroup.visibility = View.VISIBLE
        binding.widgetThemeGroup.setOnCheckedChangeListener { _, checkedId ->
            widgetTheme = if (checkedId == R.id.themeDarkRadio) {
                WidgetThemePrefs.Theme.DARK
            } else {
                WidgetThemePrefs.Theme.WEATHER_GRADIENT
            }
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) {
            binding.searchProgress.visibility = View.GONE
            binding.resultsRecycler.adapter = null
            binding.searchHint.visibility = View.GONE
            return
        }
        binding.searchProgress.visibility = View.VISIBLE
        binding.searchHint.visibility = View.GONE
        val searchId = ++searchSequence
        lifecycleScope.launch {
            when (val result = repository.searchCity(query)) {
                is WeatherResult.Success -> {
                    if (searchId != searchSequence) return@launch
                    binding.searchProgress.visibility = View.GONE
                    val results = deduplicate(result.data)
                    if (results.isEmpty()) {
                        binding.searchHint.visibility = View.VISIBLE
                        binding.searchHint.text = getString(R.string.no_result)
                    } else {
                        binding.searchHint.visibility = View.GONE
                    }
                    binding.resultsRecycler.adapter = ResultAdapter(results) { city ->
                        onCityChosen(city)
                    }
                }
                is WeatherResult.Error -> {
                    if (searchId != searchSequence) return@launch
                    binding.searchProgress.visibility = View.GONE
                    binding.searchHint.visibility = View.VISIBLE
                    binding.searchHint.text = result.message
                }
                WeatherResult.Loading -> {}
            }
        }
    }

    private fun deduplicate(items: List<GeoLocation>): List<GeoLocation> {
        val seen = mutableSetOf<String>()
        return items.filter { city ->
            val key = listOf(
                city.name.trim().lowercase(),
                city.country?.trim()?.lowercase().orEmpty(),
                city.state?.trim()?.lowercase().orEmpty()
            ).joinToString("|")
            seen.add(key)
        }
    }

    private fun onCityChosen(city: GeoLocation) {
        WidgetPrefs.saveCity(this, appWidgetId, city)
        WidgetThemePrefs.setTheme(this, appWidgetId, widgetTheme)

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultValue)
        finish()

        WeatherWidgetProvider.triggerUpdate(this)
    }

    private class ResultAdapter(
        private val items: List<GeoLocation>,
        private val onClick: (GeoLocation) -> Unit
    ) : RecyclerView.Adapter<ResultAdapter.VH>() {

        class VH(val b: ItemSearchResultBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val b = ItemSearchResultBinding.inflate(
                android.view.LayoutInflater.from(parent.context), parent, false
            )
            return VH(b)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            val displayName = item.localNames?.fr ?: item.name
            holder.b.resultCityName.text = displayName
            val detail = buildString {
                if (!item.country.isNullOrBlank()) append(item.country)
                if (!item.state.isNullOrBlank()) {
                    if (isNotEmpty()) append(", ")
                    append(item.state)
                }
            }
            holder.b.resultCityDetail.text = detail
            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount(): Int = items.size
    }
}
