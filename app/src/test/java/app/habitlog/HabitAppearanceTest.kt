package app.habitlog

import app.habitlog.model.HabitColor
import app.habitlog.model.HabitIcon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitAppearanceTest {
    @Test
    fun versionOneAppearanceIdsStillResolveToTheirOriginalChoices() {
        val previousColors = listOf(
            0xFFAA9BFCL, 0xFFF29498L, 0xFF6DCAA8L, 0xFF77B7FAL,
            0xFFF3C277L, 0xFFF29ECFL, 0xFF65C9DAL, 0xFFB7CD79L,
        )
        assertEquals(previousColors, (0..7).map { HabitColor.fromId(it).argb })

        val previousIcons = listOf(
            HabitIcon.TARGET, HabitIcon.WORKOUT, HabitIcon.READING, HabitIcon.WATER,
            HabitIcon.MINDFULNESS, HabitIcon.SLEEP, HabitIcon.WALKING, HabitIcon.TIMER,
        )
        assertEquals(previousIcons, (0..7).map(HabitIcon.Companion::fromId))
    }

    @Test
    fun everyPickerChoiceHasAUniqueIdAcceptedByBackups() {
        assertEquals(HabitColor.entries.size, HabitColor.entries.map(HabitColor::id).distinct().size)
        assertEquals(HabitIcon.entries.size, HabitIcon.entries.map(HabitIcon::id).distinct().size)
        HabitColor.entries.forEach { color -> assertTrue(HabitColor.containsId(color.id)) }
        HabitIcon.entries.forEach { icon -> assertTrue(HabitIcon.containsId(icon.id)) }
    }
}
