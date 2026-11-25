package app.habitlog.model

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.StyleRes
import androidx.core.content.edit
import app.habitlog.R

enum class AppAccent(val label: String, @ColorRes val colorRes: Int, @StyleRes val pickerStyle: Int) {
    LILAC("Lilac", R.color.accent_lilac, R.style.ThemeOverlay_Pivot_Lilac),
    OCEAN("Ocean", R.color.accent_ocean, R.style.ThemeOverlay_Pivot_Ocean),
    TEAL("Teal", R.color.accent_teal, R.style.ThemeOverlay_Pivot_Teal),
    FOREST("Forest", R.color.accent_forest, R.style.ThemeOverlay_Pivot_Forest),
    AMBER("Amber", R.color.accent_amber, R.style.ThemeOverlay_Pivot_Amber),
    CORAL("Coral", R.color.accent_coral, R.style.ThemeOverlay_Pivot_Coral),
    ROSE("Rose", R.color.accent_rose, R.style.ThemeOverlay_Pivot_Rose),
    SLATE("Slate", R.color.accent_slate, R.style.ThemeOverlay_Pivot_Slate);

    fun save(context: Context) {
        preferences(context).edit { putString(KEY, name) }
    }

    companion object {
        private const val KEY = "accent"

        fun load(context: Context): AppAccent {
            val name = preferences(context).getString(KEY, null)
            return entries.firstOrNull { it.name == name } ?: LILAC
        }

        private fun preferences(context: Context) =
            context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    }
}
