package com.meteoapp.city

import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.meteoapp.R
import com.meteoapp.data.WeatherRepository
import com.meteoapp.data.WeatherResult
import com.meteoapp.data.model.GeoLocation
import com.meteoapp.databinding.DialogSearchBinding
import com.meteoapp.databinding.ItemSearchResultBinding
import android.view.LayoutInflater
import android.view.ViewGroup
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Logique de recherche de ville partagée entre la boîte de dialogue de
 * recherche du tableau de bord et l'écran de configuration du widget :
 * saisie avec anti-rebond (450 ms), annulation des réponses obsolètes via
 * un compteur de séquence, déduplication des résultats et affichage des
 * suggestions (nom local, pays, région).
 */
class CitySearchController(
    private val binding: DialogSearchBinding,
    lifecycleOwner: LifecycleOwner,
    private val repository: WeatherRepository,
    private val onCitySelected: (GeoLocation) -> Unit
) {

    private val lifecycleScope = lifecycleOwner.lifecycleScope

    private var searchJob: Job? = null
    private var searchSequence = 0

    init {
        binding.resultsRecycler.layoutManager =
            LinearLayoutManager(binding.root.context, LinearLayoutManager.VERTICAL, false)
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
                    delay(SEARCH_DEBOUNCE_MS)
                    performSearch(s?.toString() ?: "")
                }
            }
        })
    }

    fun performSearch(query: String) {
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
                    val results = result.data.deduplicate()
                    if (results.isEmpty()) {
                        binding.searchHint.visibility = View.VISIBLE
                        binding.searchHint.setText(R.string.no_result)
                    } else {
                        binding.searchHint.visibility = View.GONE
                    }
                    binding.resultsRecycler.adapter = ResultAdapter(results, onCitySelected)
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

    private fun List<GeoLocation>.deduplicate(): List<GeoLocation> {
        val seen = mutableSetOf<String>()
        return filter { city ->
            val key = listOf(
                city.name.trim().lowercase(),
                city.country?.trim()?.lowercase().orEmpty(),
                city.state?.trim()?.lowercase().orEmpty()
            ).joinToString("|")
            seen.add(key)
        }
    }

    private class ResultAdapter(
        private val items: List<GeoLocation>,
        private val onClick: (GeoLocation) -> Unit
    ) : RecyclerView.Adapter<ResultAdapter.VH>() {

        class VH(val b: ItemSearchResultBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = ItemSearchResultBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return VH(b)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            val displayName = item.displayName(holder.b.root.context)
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

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 450L
    }
}
