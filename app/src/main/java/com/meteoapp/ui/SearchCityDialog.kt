package com.meteoapp.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.meteoapp.data.WeatherResult
import com.meteoapp.data.WeatherRepository
import com.meteoapp.data.model.GeoLocation
import com.meteoapp.databinding.DialogSearchBinding
import com.meteoapp.databinding.ItemSearchResultBinding
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchCityDialog(
    private val onCitySelected: (GeoLocation) -> Unit
) : DialogFragment() {

    private var _binding: DialogSearchBinding? = null
    private val binding get() = _binding!!

    private val repository by lazy { WeatherRepository(requireContext()) }
    private var searchJob: Job? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogSearchBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(com.meteoapp.R.string.action_search)
            .setView(binding.root)
            .setNegativeButton(com.meteoapp.R.string.cancel, null)
            .create()
        setupSearch()
        return dialog
    }

    private fun setupSearch() {
        binding.resultsRecycler.layoutManager = LinearLayoutManager(requireContext())
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
        lifecycleScope.launch {
            when (val result = repository.searchCity(query)) {
                is WeatherResult.Success -> {
                    binding.searchProgress.visibility = View.GONE
                    val results = result.data.deduplicate()
                    if (results.isEmpty()) {
                        binding.searchHint.visibility = View.VISIBLE
                        binding.searchHint.text = getString(com.meteoapp.R.string.no_result)
                    } else {
                        binding.searchHint.visibility = View.GONE
                    }
                    binding.resultsRecycler.adapter = ResultAdapter(results) { city ->
                        onCitySelected(city)
                        dismiss()
                    }
                }
                is WeatherResult.Error -> {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
