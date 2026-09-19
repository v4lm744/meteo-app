package com.meteoapp.ui

import android.app.Dialog
import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.meteoapp.R
import com.meteoapp.databinding.DialogSettingsBinding
import com.meteoapp.util.UnitPrefs
import com.meteoapp.widget.SyncPrefs
import com.meteoapp.widget.WidgetSyncScheduler

/**
 * Dialogue « Paramètres » : unités d'affichage (température, vent, pression,
 * format de l'heure) et intervalle de synchronisation automatique des widgets
 * d'accueil. Les choix sont persistés dans [UnitPrefs] et [SyncPrefs] ; le
 * travail périodique est (re)planifié immédiatement via [WidgetSyncScheduler].
 */
class SettingsDialog(
    private val onSettingsApplied: () -> Unit = {}
) : DialogFragment() {

    private var _binding: DialogSettingsBinding? = null
    private val binding get() = _binding!!

    private var selectedMinutes: Int = SyncPrefs.SYNC_DISABLED

    private var selectedTempUnit: UnitPrefs.TempUnit = UnitPrefs.TempUnit.CELSIUS
    private var selectedWindUnit: UnitPrefs.WindUnit = UnitPrefs.WindUnit.KMH
    private var selectedPressureUnit: UnitPrefs.PressureUnit = UnitPrefs.PressureUnit.HPA
    private var selectedTimeFormat: UnitPrefs.TimeFormat = UnitPrefs.TimeFormat.FORMAT_24H

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogSettingsBinding.inflate(layoutInflater)

        val context = requireContext()
        selectedMinutes = SyncPrefs.getIntervalMinutes(context)
        buildSyncOptions(selectedMinutes)

        selectedTempUnit = UnitPrefs.getTempUnit(context)
        selectedWindUnit = UnitPrefs.getWindUnit(context)
        selectedPressureUnit = UnitPrefs.getPressureUnit(context)
        selectedTimeFormat = UnitPrefs.getTimeFormat(context)
        buildUnitOptions(context)

        return MaterialAlertDialogBuilder(context)
            .setTitle(R.string.action_settings)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                UnitPrefs.setTempUnit(context, selectedTempUnit)
                UnitPrefs.setWindUnit(context, selectedWindUnit)
                UnitPrefs.setPressureUnit(context, selectedPressureUnit)
                UnitPrefs.setTimeFormat(context, selectedTimeFormat)
                SyncPrefs.setIntervalMinutes(context, selectedMinutes)
                WidgetSyncScheduler.schedule(context, selectedMinutes)
                onSettingsApplied()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    private fun buildSyncOptions(current: Int) {
        val group: RadioGroup = binding.syncIntervalGroup
        group.removeAllViews()

        val options = mutableListOf<Int>()
        options.add(SyncPrefs.SYNC_DISABLED)
        options.addAll(SyncPrefs.INTERVAL_OPTIONS_MINUTES)

        for (minutes in options) {
            val radio = RadioButton(requireContext()).apply {
                text = syncLabelFor(minutes)
                id = minutes
                isChecked = minutes == current
            }
            group.addView(radio)
        }

        group.setOnCheckedChangeListener { _, checkedId ->
            selectedMinutes = checkedId
        }
    }

    private fun buildUnitOptions(context: android.content.Context) {
        val tempGroup: RadioGroup = binding.tempUnitGroup
        val windGroup: RadioGroup = binding.windUnitGroup
        val pressureGroup: RadioGroup = binding.pressureUnitGroup
        val timeGroup: RadioGroup = binding.timeFormatGroup
        tempGroup.removeAllViews()
        windGroup.removeAllViews()
        pressureGroup.removeAllViews()
        timeGroup.removeAllViews()

        UnitPrefs.TempUnit.entries.forEach { unit ->
            val radio = RadioButton(context).apply {
                text = getString(tempLabelFor(unit))
                id = unit.ordinal
                isChecked = unit == selectedTempUnit
            }
            tempGroup.addView(radio)
        }
        tempGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedTempUnit = UnitPrefs.TempUnit.entries[checkedId]
        }

        UnitPrefs.WindUnit.entries.forEach { unit ->
            val radio = RadioButton(context).apply {
                text = getString(windLabelFor(unit))
                id = unit.ordinal
                isChecked = unit == selectedWindUnit
            }
            windGroup.addView(radio)
        }
        windGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedWindUnit = UnitPrefs.WindUnit.entries[checkedId]
        }

        UnitPrefs.PressureUnit.entries.forEach { unit ->
            val radio = RadioButton(context).apply {
                text = getString(pressureLabelFor(unit))
                id = unit.ordinal
                isChecked = unit == selectedPressureUnit
            }
            pressureGroup.addView(radio)
        }
        pressureGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedPressureUnit = UnitPrefs.PressureUnit.entries[checkedId]
        }

        UnitPrefs.TimeFormat.entries.forEach { format ->
            val radio = RadioButton(context).apply {
                text = getString(timeLabelFor(format))
                id = format.ordinal
                isChecked = format == selectedTimeFormat
            }
            timeGroup.addView(radio)
        }
        timeGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedTimeFormat = UnitPrefs.TimeFormat.entries[checkedId]
        }
    }

    private fun syncLabelFor(minutes: Int): String = when (minutes) {
        SyncPrefs.SYNC_DISABLED -> getString(R.string.settings_sync_disabled)
        60 -> getString(R.string.settings_sync_hourly)
        180 -> getString(R.string.settings_sync_3h)
        else -> getString(R.string.settings_sync_minutes, minutes)
    }

    private fun tempLabelFor(unit: UnitPrefs.TempUnit): Int = when (unit) {
        UnitPrefs.TempUnit.CELSIUS -> R.string.settings_unit_temp_celsius
        UnitPrefs.TempUnit.FAHRENHEIT -> R.string.settings_unit_temp_fahrenheit
    }

    private fun windLabelFor(unit: UnitPrefs.WindUnit): Int = when (unit) {
        UnitPrefs.WindUnit.KMH -> R.string.settings_unit_wind_kmh
        UnitPrefs.WindUnit.MPH -> R.string.settings_unit_wind_mph
    }

    private fun pressureLabelFor(unit: UnitPrefs.PressureUnit): Int = when (unit) {
        UnitPrefs.PressureUnit.HPA -> R.string.settings_unit_pressure_hpa
        UnitPrefs.PressureUnit.INHG -> R.string.settings_unit_pressure_inhg
    }

    private fun timeLabelFor(format: UnitPrefs.TimeFormat): Int = when (format) {
        UnitPrefs.TimeFormat.FORMAT_24H -> R.string.settings_unit_time_24h
        UnitPrefs.TimeFormat.FORMAT_12H -> R.string.settings_unit_time_12h
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
