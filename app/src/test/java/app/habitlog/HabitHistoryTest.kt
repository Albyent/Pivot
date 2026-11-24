package app.habitlog

import app.habitlog.data.CheckIn
import app.habitlog.data.Habit
import app.habitlog.data.Pause
import app.habitlog.data.Schedule
import app.habitlog.model.DayState
import app.habitlog.model.HabitHistory
import app.habitlog.model.WeeklyProgress
import app.habitlog.model.weekStart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitHistoryTest {
    private val monday = LocalDate.of(2026, 11, 23).toEpochDay()

    @Test
    fun completedDatesAppearInWeekAndAnUnfinishedWeekKeepsExistingStreak() {
        val history = history(
            start = monday - 7,
            schedule = Schedule(weeklyTarget = 3),
            dates = listOf(monday - 7, monday - 5, monday - 1, monday, monday + 3),
        )
        assertEquals(monday, weekStart(monday + 6))
        assertEquals(WeeklyProgress(2, 3), history.weeklyProgress(monday + 3, monday + 3))
        assertEquals(1, history.streaks(monday + 3).current)
        assertEquals(0, history.streaks(monday + 7).current)
        assertEquals(1, history.streaks(monday + 7).best)
    }

    @Test
    fun threeCompletedDaysMeetTheWeeklyGoalAndTheCounterRestarts() {
        val history = history(Schedule(weeklyTarget = 3), listOf(monday, monday + 3, monday + 5))
        assertTrue(history.weeklyProgress(monday + 5, monday + 5)!!.met)
        assertEquals(WeeklyProgress(0, 3), history.weeklyProgress(monday + 7, monday + 7))
        assertEquals(1, history.streaks(monday + 7).current)
        assertFalse(history.isDue(monday + 6))
    }

    @Test
    fun daysOffAndSkipsAreNeutralAndAnOverdueScheduledDayBreaksAStreak() {
        val schedule = Schedule(weekdays = 1 or 4 or 16)
        val history = history(schedule, listOf(monday), entries = listOf(CheckIn(1, monday + 2, skipped = true)))
        assertEquals(DayState.REST, history.dayState(monday + 1, monday + 4))
        assertEquals(DayState.SKIPPED, history.dayState(monday + 2, monday + 4))
        assertEquals(1, history.streaks(monday + 4).current)
        assertEquals(0, history.streaks(monday + 5).current)
        assertEquals(50, history.completion(monday, monday + 5, monday + 5).percent)
    }

    @Test
    fun aPauseCanCoverAWholeWeekWithoutBreakingTheStreak() {
        val history = history(
            start = monday - 7,
            schedule = Schedule(weeklyTarget = 3),
            dates = listOf(monday - 7, monday - 5, monday - 2),
            pauses = listOf(Pause(1, monday, monday + 6)),
        )
        assertEquals(DayState.SKIPPED, history.dayState(monday, monday))
        assertEquals(WeeklyProgress(0, 0), history.weeklyProgress(monday, monday + 6))
        assertEquals(1, history.streaks(monday + 7).current)
        assertEquals(100, history.completion(monday - 7, monday + 7, monday + 7).percent)
    }

    @Test
    fun newScheduleUsesItsEffectiveDateAndPreservesOldNumericGoal() {
        val history = history(
            schedule = Schedule(weekdays = 1, amountGoal = 20.0),
            entries = listOf(CheckIn(1, monday, amount = 20.0), CheckIn(1, monday + 1, amount = 20.0)),
            revisions = listOf(Schedule(habitId = 1, effectiveDay = monday + 1, weekdays = 2, amountGoal = 30.0)),
        )
        assertEquals(DayState.DONE, history.dayState(monday, monday + 2))
        assertEquals(DayState.PARTIAL, history.dayState(monday + 1, monday + 2))
        assertEquals(1, history.completedBetween(monday, monday + 2, monday + 2))
        assertEquals(50, history.completion(monday, monday + 2, monday + 2).percent)
    }

    @Test
    fun aSuccessfulDayBeforePausingKeepsItsCheckIn() {
        val history = history(Schedule(), listOf(monday), pauses = listOf(Pause(1, monday, monday + 3)))
        assertEquals(DayState.DONE, history.dayState(monday, monday + 1))
        assertEquals(DayState.SKIPPED, history.dayState(monday + 1, monday + 1))
        assertEquals(1, history.streaks(monday + 2).current)
    }

    @Test
    fun amountThresholdUsesToleranceAndFutureDaysStayOutOfActivity() {
        val history = history(
            schedule = Schedule(amountGoal = 0.3),
            entries = listOf(CheckIn(1, monday, amount = 0.1 + 0.2), CheckIn(1, monday + 1, amount = 0.3)),
        )
        assertEquals(DayState.DONE, history.dayState(monday, monday))
        assertEquals(DayState.FUTURE, history.dayState(monday + 1, monday))
        assertEquals(1, history.completedBetween(monday, monday + 5, monday))
    }

    private fun history(
        schedule: Schedule,
        dates: List<Long> = emptyList(),
        start: Long = monday,
        entries: List<CheckIn> = emptyList(),
        pauses: List<Pause> = emptyList(),
        revisions: List<Schedule> = emptyList(),
    ) = HabitHistory(
        Habit(id = 1, name = "Work out", color = 0, icon = 0, startDay = start, position = 0),
        listOf(schedule.copy(habitId = 1, effectiveDay = start)) + revisions,
        dates.map { CheckIn(1, it, amount = 1.0) } + entries,
        pauses,
    )
}
