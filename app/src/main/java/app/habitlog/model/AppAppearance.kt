package app.habitlog.model

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }
}

data class AppAppearance(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AppAccent = AppAccent.SLATE,
) {
    fun save(context: Context) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit {
            putString("mode", mode.name)
            putString("accent", accent.name)
        }
    }

    companion object {
        private const val PREFERENCES = "appearance"

        fun load(context: Context): AppAppearance {
            val defaults = AppAppearance()
            val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            val mode = preferences.getString("mode", null)
            val accent = preferences.getString("accent", null)
            return AppAppearance(
                mode = ThemeMode.entries.firstOrNull { it.name == mode } ?: defaults.mode,
                accent = AppAccent.entries.firstOrNull { it.name == accent } ?: defaults.accent,
            )
        }
    }
}

internal fun Configuration.withAppearance(dark: Boolean): Configuration = Configuration(this).apply {
    val night = if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
}

internal fun Context.appearanceContext(dark: Boolean): Context =
    createConfigurationContext(resources.configuration.withAppearance(dark))

internal fun Context.isSystemDark(): Boolean =
    resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
