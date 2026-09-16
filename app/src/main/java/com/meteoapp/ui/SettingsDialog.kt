package com.meteoapp.ui

import android.app.Dialog
import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.meteoapp.R
import com.meteoapp.databinding.DialogSettingsBinding
import com.meteoapp.widget.SyncPrefs
import com.meteoapp.widget.WidgetSyncScheduler

/**
 * Dialogue « Paramètres » : choix de l'intervalle de synchronisation automatique
 * des widgets d'accueil. La sélection est persistée dans [SyncPrefs] et le travail
 * périodique est (re)planifié immédiatement via [WidgetSyncScheduler].
 */
class SettingsDialog : DialogFragment() {

    private var _binding: DialogSettingsBinding? = null
    private val binding get() = _binding!!

    private var selectedMinutes: Int = SyncPrefs.SYNC_DISABLED

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogSettingsBinding.inflate(layoutInflater)

        val context = requireContext()
        selectedMinutes = SyncPrefs.getIntervalMinutes(context)
        buildOptions(selectedMinutes)

        return MaterialAlertDialogBuilder(context)
            .setTitle(R.string.action_settings)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                SyncPrefs.setIntervalMinutes(context, selectedMinutes)
                WidgetSyncScheduler.schedule(context, selectedMinutes)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    private fun buildOptions(current: Int) {
        val group: RadioGroup = binding.syncIntervalGroup
        group.removeAllViews()

        val options = mutableListOf<Int>()
        options.add(SyncPrefs.SYNC_DISABLED)
        options.addAll(SyncPrefs.INTERVAL_OPTIONS_MINUTES)

        for (minutes in options) {
            val radio = RadioButton(requireContext()).apply {
                text = labelFor(minutes)
                id = minutes
                isChecked = minutes == current
            }
            group.addView(radio)
        }

        group.setOnCheckedChangeListener { _, checkedId ->
            selectedMinutes = checkedId
        }
    }

    private fun labelFor(minutes: Int): String = when (minutes) {
        SyncPrefs.SYNC_DISABLED -> getString(R.string.settings_sync_disabled)
        60 -> getString(R.string.settings_sync_hourly)
        180 -> getString(R.string.settings_sync_3h)
        else -> getString(R.string.settings_sync_minutes, minutes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
