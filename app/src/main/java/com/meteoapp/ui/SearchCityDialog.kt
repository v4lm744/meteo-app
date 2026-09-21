package com.meteoapp.ui

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.meteoapp.R
import com.meteoapp.city.CitySearchController
import com.meteoapp.data.WeatherRepository
import com.meteoapp.data.model.GeoLocation
import com.meteoapp.databinding.DialogSearchBinding

/**
 * Boîte de dialogue de recherche de ville : la logique de saisie, d'anti-rebond
 * et de rendu des résultats est déléguée à [CitySearchController], partagée avec
 * l'écran de configuration du widget.
 */
class SearchCityDialog(
    private val onCitySelected: (GeoLocation) -> Unit
) : DialogFragment() {

    private var _binding: DialogSearchBinding? = null
    private val binding get() = _binding!!

    private var searchController: CitySearchController? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogSearchBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_search)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .create()
        searchController = CitySearchController(
            binding = binding,
            lifecycleOwner = this,
            repository = WeatherRepository(requireContext()),
            onCitySelected = { city ->
                onCitySelected(city)
                dismiss()
            }
        )
        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
