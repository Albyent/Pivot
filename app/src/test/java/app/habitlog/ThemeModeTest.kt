package app.habitlog

import app.habitlog.model.AppAccent
import app.habitlog.model.AppAppearance
import app.habitlog.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {
    @Test
    fun systemModeFollowsTheDevice() {
        assertFalse(ThemeMode.SYSTEM.isDark(false))
        assertTrue(ThemeMode.SYSTEM.isDark(true))
    }

    @Test
    fun manualModesOverrideTheDevice() {
        assertFalse(ThemeMode.LIGHT.isDark(true))
        assertTrue(ThemeMode.DARK.isDark(false))
    }

    @Test
    fun newInstallDefaultsToDeviceModeAndNeutralAccent() {
        assertEquals(AppAppearance(ThemeMode.SYSTEM, AppAccent.SLATE), AppAppearance())
    }
}
