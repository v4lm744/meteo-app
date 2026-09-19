package com.meteoapp.ui

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.meteoapp.data.ApiKeyStore
import com.meteoapp.databinding.DialogApiKeyBinding
import androidx.core.net.toUri

/**
 * Dialogue de saisie de la clé API OpenWeatherMap.
 * La clé est persistée via [ApiKeyStore] : elle survit aux redémarrages
 * de l'application et du téléphone.
 */
class ApiKeyDialog(
    private val onSaved: () -> Unit = {}
) : DialogFragment() {

    private var _binding: DialogApiKeyBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogApiKeyBinding.inflate(layoutInflater)

        // Pré-remplir avec la clé déjà enregistrée
        binding.apiKeyInput.setText(ApiKeyStore.getApiKey(requireContext()))

        binding.getKeyButton.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                "https://home.openweathermap.org/api_keys".toUri()
            )
            startActivity(intent)
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(com.meteoapp.R.string.action_api_key)
            .setView(binding.root)
            .setPositiveButton(com.meteoapp.R.string.action_save) { _, _ ->
                val key = binding.apiKeyInput.text?.toString().orEmpty()
                ApiKeyStore.setApiKey(requireContext(), key)
                onSaved()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
