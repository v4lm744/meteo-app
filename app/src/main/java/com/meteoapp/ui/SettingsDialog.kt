package com.meteoapp.ui

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.meteoapp.R
import com.meteoapp.databinding.DialogSettingsBinding
import com.meteoapp.notifications.NotificationPrefs
import com.meteoapp.notifications.WeatherNotificationScheduler
import com.meteoapp.util.ThemePrefs
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

    private val notifHourOptions = listOf(6, 7, 8, 9, 12, 18)

    private var _binding: DialogSettingsBinding? = null
    private val binding get() = _binding!!

    private var selectedMinutes: Int = SyncPrefs.SYNC_DISABLED
    private var notificationsEnabled: Boolean = false
    private var notificationHour: Int = NotificationPrefs.DEFAULT_HOUR
    private var selectedThemeMode: ThemePrefs.ThemeMode = ThemePrefs.ThemeMode.FOLLOW_SYSTEM
    private var dynamicColorsEnabled: Boolean = false

    private var selectedTempUnit: UnitPrefs.TempUnit = UnitPrefs.TempUnit.CELSIUS
    private var selectedWindUnit: UnitPrefs.WindUnit = UnitPrefs.WindUnit.KMH
    private var selectedPressureUnit: UnitPrefs.PressureUnit = UnitPrefs.PressureUnit.HPA
    private var selectedTimeFormat: UnitPrefs.TimeFormat = UnitPrefs.TimeFormat.FORMAT_24H
    private var selectedPrecision: UnitPrefs.ValuePrecision = UnitPrefs.ValuePrecision.WHOLE

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogSettingsBinding.inflate(layoutInflater)

        val context = requireContext()
        selectedMinutes = SyncPrefs.getIntervalMinutes(context)
        buildSyncOptions(selectedMinutes)

        notificationsEnabled = NotificationPrefs.isEnabled(context)
        notificationHour = NotificationPrefs.getHour(context)
        buildNotificationOptions()

        selectedThemeMode = ThemePrefs.getMode(context)
        dynamicColorsEnabled = ThemePrefs.isDynamicColorsEnabled(context)
        buildAppearanceOptions(context)

        selectedTempUnit = UnitPrefs.getTempUnit(context)
        selectedWindUnit = UnitPrefs.getWindUnit(context)
        selectedPressureUnit = UnitPrefs.getPressureUnit(context)
        selectedTimeFormat = UnitPrefs.getTimeFormat(context)
        selectedPrecision = UnitPrefs.getValuePrecision(context)
        buildUnitOptions(context)

        return MaterialAlertDialogBuilder(context)
            .setTitle(R.string.action_settings)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                UnitPrefs.setTempUnit(context, selectedTempUnit)
                UnitPrefs.setWindUnit(context, selectedWindUnit)
                UnitPrefs.setPressureUnit(context, selectedPressureUnit)
                UnitPrefs.setTimeFormat(context, selectedTimeFormat)
                UnitPrefs.setValuePrecision(context, selectedPrecision)
                SyncPrefs.setIntervalMinutes(context, selectedMinutes)
                WidgetSyncScheduler.schedule(context, selectedMinutes)
                NotificationPrefs.setEnabled(context, notificationsEnabled)
                NotificationPrefs.setHour(context, notificationHour)
                WeatherNotificationScheduler.reschedule(context)
                ThemePrefs.setMode(context, selectedThemeMode)
                ThemePrefs.setDynamicColorsEnabled(context, dynamicColorsEnabled)
                ThemePrefs.applyMode(context)
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
        val precisionGroup: RadioGroup = binding.precisionGroup
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

        precisionGroup.removeAllViews()
        UnitPrefs.ValuePrecision.entries.forEach { precision ->
            val radio = RadioButton(context).apply {
                text = getString(precisionLabelFor(precision))
                id = precision.ordinal
                isChecked = precision == selectedPrecision
            }
            precisionGroup.addView(radio)
        }
        precisionGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedPrecision = UnitPrefs.ValuePrecision.entries[checkedId]
        }
    }

    private fun buildAppearanceOptions(context: android.content.Context) {
        val modeGroup: RadioGroup = binding.themeModeGroup
        modeGroup.removeAllViews()
        ThemePrefs.ThemeMode.entries.forEach { mode ->
            val radio = RadioButton(context).apply {
                text = getString(themeModeLabelFor(mode))
                id = mode.ordinal
                isChecked = mode == selectedThemeMode
            }
            modeGroup.addView(radio)
        }
        modeGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedThemeMode = ThemePrefs.ThemeMode.entries[checkedId]
        }

        val dynamicSwitch: com.google.android.material.materialswitch.MaterialSwitch =
            binding.dynamicColorsSwitch
        val supported = ThemePrefs.isDynamicColorsSupported(context)
        dynamicSwitch.isEnabled = supported
        dynamicSwitch.isChecked = supported && dynamicColorsEnabled
        dynamicSwitch.setOnCheckedChangeListener { _, checked ->
            dynamicColorsEnabled = checked
        }
        binding.dynamicColorsHint.setText(
            if (supported) R.string.settings_dynamic_colors_hint
            else R.string.settings_dynamic_colors_unavailable
        )
    }

    private fun themeModeLabelFor(mode: ThemePrefs.ThemeMode): Int = when (mode) {
        ThemePrefs.ThemeMode.FOLLOW_SYSTEM -> R.string.settings_theme_mode_system
        ThemePrefs.ThemeMode.LIGHT -> R.string.settings_theme_mode_light
        ThemePrefs.ThemeMode.DARK -> R.string.settings_theme_mode_dark
    }

    private fun buildNotificationOptions() {
        val switch: MaterialSwitch = binding.notificationSwitch
        switch.isChecked = notificationsEnabled
        switch.setOnCheckedChangeListener { _, checked ->
            if (checked) requestNotificationPermissionIfNeeded()
            notificationsEnabled = checked
            updateNotificationSectionVisibility()
        }
        updateNotificationSectionVisibility()
        binding.notificationHourGroup.visibility =
            if (notificationsEnabled) android.view.View.VISIBLE else android.view.View.GONE
        binding.notificationHourLabel.visibility =
            if (notificationsEnabled) android.view.View.VISIBLE else android.view.View.GONE

        val group: RadioGroup = binding.notificationHourGroup
        group.removeAllViews()
        for (hour in notifHourOptions) {
            val radio = RadioButton(requireContext()).apply {
                text = getString(R.string.settings_notifications_hour_format, hour)
                id = hour
                isChecked = hour == notificationHour
            }
            group.addView(radio)
        }
        group.setOnCheckedChangeListener { _, checkedId ->
            notificationHour = checkedId
        }
    }

    private fun updateNotificationSectionVisibility() {
        val visibility =
            if (notificationsEnabled) android.view.View.VISIBLE else android.view.View.GONE
        binding.notificationHourGroup.visibility = visibility
        binding.notificationHourLabel.visibility = visibility
        binding.notificationPermHint.visibility =
            if (notificationsEnabled && !hasNotificationPermission())
                android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private fun syncLabelFor(minutes: Int): String = when (minutes) {
        SyncPrefs.SYNC_DISABLED -> getString(R.string.settings_sync_disabled)
        60 -> getString(R.string.settings_sync_hourly)
        180 -> getString(R.string.settings_sync_3h)
        else -> resources.getQuantityString(R.plurals.settings_sync_minutes, minutes, minutes)
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

    private fun precisionLabelFor(precision: UnitPrefs.ValuePrecision): Int = when (precision) {
        UnitPrefs.ValuePrecision.WHOLE -> R.string.settings_unit_precision_whole
        UnitPrefs.ValuePrecision.ONE_DECIMAL -> R.string.settings_unit_precision_decimal
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
