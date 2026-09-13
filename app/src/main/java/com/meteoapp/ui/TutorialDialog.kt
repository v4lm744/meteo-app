package com.meteoapp.ui

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.meteoapp.databinding.DialogTutorialBinding

@Suppress("unused")
class TutorialDialog : DialogFragment() {

    private var _binding: DialogTutorialBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogTutorialBinding.inflate(layoutInflater)

        binding.tutorialOpenSite.setOnClickListener { openUrl("https://openweathermap.org") }
        binding.tutorialOpenKeys.setOnClickListener { openUrl("https://home.openweathermap.org/api_keys") }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(com.meteoapp.R.string.tutorial_title)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
