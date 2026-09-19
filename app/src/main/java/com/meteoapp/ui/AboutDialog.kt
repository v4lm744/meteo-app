package com.meteoapp.ui

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.meteoapp.databinding.DialogAboutBinding
import androidx.core.net.toUri

/**
 * Boîte de dialogue "À propos" : informations légales obligatoires
 * (source des données, politique de confidentialité, permissions,
 * licences open-source, éditeur).
 */
class AboutDialog : DialogFragment() {

    private var _binding: DialogAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAboutBinding.inflate(layoutInflater)

        val versionName = try {
            val pkg = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            pkg.versionName ?: ""
        } catch (e: Exception) {
            ""
        }
        binding.aboutVersion.text = getString(com.meteoapp.R.string.about_version_format, versionName)

        binding.aboutOpenWeatherLink.setOnClickListener {
            openUrl("https://openweathermap.org")
        }
        binding.aboutLicensesLink.setOnClickListener {
            openUrl("https://github.com/v4lm744/meteo-app")
        }
        binding.aboutContactLink.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = "mailto:".toUri()
                putExtra(Intent.EXTRA_SUBJECT, getString(com.meteoapp.R.string.app_name))
            }
            try {
                startActivity(intent)
            } catch (_: Exception) {
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(com.meteoapp.R.string.action_about)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: Exception) {
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
