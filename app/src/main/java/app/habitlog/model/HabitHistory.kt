package app.habitlog.model

import app.habitlog.data.CheckIn
import app.habitlog.data.Habit
import app.habitlog.data.Pause
import app.habitlog.data.Schedule
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min

val MIN_DAY: Long = LocalDate.of(1900, 1, 1).toEpochDay()
val MAX_DAY: Long = LocalDate.of(9999, 12, 31).toEpochDay()

fun today(): Long = LocalDate.now().toEpochDay()
fun Long.asDate(): LocalDate = LocalDate.ofEpochDay(this)
fun weekStart(day: Long): Long = day - (day.asDate().dayOfWeek.value - 1)
fun dayLabel(day: Long, pattern: String = "EEE, MMM d"): String =
    day.asDate().format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))

fun amountLabel(amount: Double): String = NumberFormat.getNumberInstance().apply {
    maximumFractionDigits = 3
}.format(amount)

fun parseAmount(input: String): Double? =
    input.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

enum class DayState(val label: String) {
    INACTIVE("Outside habit dates"),
    FUTURE("Upcoming"),
    REST("Rest day"),
    UNLOGGED("No check-in"),
    PENDING("Due"),
    MISSED("Missed"),
    SKIPPED("Skipped"),
    PARTIAL("In progress"),
    DONE("Done"),
}

data class WeeklyProgress(val daysDone: Int, val target: Int) {
    val met get() = target > 0 && daysDone >= target
}

data class Streaks(val current: Int, val best: Int, val weekly: Boolean)

data class Completion(val done: Int, val opportunities: Int) {
    val percent get() = if (opportunities > 0) (100 * done + opportunities / 2) / opportunities else null
}

class HabitHistory(
    val habit: Habit,
    schedules: List<Schedule>,
    checkIns: List<CheckIn>,
    val pauses: List<Pause>,
) {
    val schedules = schedules.sortedBy(Schedule::effectiveDay)
    val entries = checkIns.associateBy(CheckIn::day)

    fun scheduleAt(day: Long): Schedule? = schedules.lastOrNull { it.effectiveDay <= day }
    fun entry(day: Long): CheckIn? = entries[day]
    fun pauseOn(day: Long): Pause? = pauses.firstOrNull { day in it.firstDay..it.lastDay }
    fun isActive(day: Long) = day >= habit.startDay && (habit.archivedDay == null || day <= habit.archivedDay)

    fun isScheduled(day: Long): Boolean {
        if (!isActive(day)) return false
        val schedule = scheduleAt(day) ?: return false
        val bit = 1 shl (day.asDate().dayOfWeek.value - 1)
        return schedule.weeklyTarget != null || schedule.weekdays and bit != 0
    }

    fun dayState(day: Long, now: Long): DayState {
        if (!isActive(day)) return DayState.INACTIVE
        if (day > now) return DayState.FUTURE
        if (!isScheduled(day)) return DayState.REST
        val schedule = scheduleAt(day) ?: return DayState.REST
        val entry = entries[day]
        val amount = entry?.amount ?: 0.0
        val goal = schedule.amountGoal ?: 1.0
        return when {
            entry?.skipped != true && amount + 1e-9 >= goal -> DayState.DONE
            excused(day) -> DayState.SKIPPED
            amount > 0 -> DayState.PARTIAL
            day == now -> DayState.PENDING
            schedule.weeklyTarget != null -> DayState.UNLOGGED
            else -> DayState.MISSED
        }
    }

    // An archived habit can retain a completion from the last day it was active.
    private fun excused(day: Long) =
        entries[day]?.skipped == true || pauseOn(day) != null || day == habit.archivedDay

    private fun isComplete(day: Long): Boolean {
        if (!isScheduled(day)) return false
        val entry = entries[day] ?: return false
        val goal = scheduleAt(day)?.amountGoal ?: 1.0
        return !entry.skipped && entry.amount + 1e-9 >= goal
    }

    fun weeklyProgress(day: Long, asOf: Long): WeeklyProgress? {
        val start = weekStart(day)
        val scheduled = (start..start + 6).filter { isActive(it) && scheduleAt(it)?.weeklyTarget != null }
        val target = scheduled.firstNotNullOfOrNull { scheduleAt(it)?.weeklyTarget } ?: return null
        val available = scheduled.count { !excused(it) || isComplete(it) }
        val done = scheduled.count { it <= asOf && isComplete(it) }
        return WeeklyProgress(done, min(target, available))
    }

    fun isDue(day: Long): Boolean {
        if (habit.archivedDay != null) return false
        if (dayState(day, day) !in setOf(DayState.PENDING, DayState.PARTIAL)) return false
        return weeklyProgress(day, day)?.met != true
    }

    fun completedOn(day: Long, now: Long): Boolean = day <= now && isComplete(day)

    fun completedBetween(first: Long, last: Long, now: Long): Int =
        entries.keys.count { it in first..last && completedOn(it, now) }

    fun completion(first: Long, last: Long, now: Long): Completion {
        val outcomes = outcomes(first, last, now).toList()
        return Completion(outcomes.count(Outcome::done), outcomes.size)
    }

    fun streaks(now: Long): Streaks {
        val last = min(now, habit.archivedDay ?: now)
        val weekly = scheduleAt(last)?.weeklyTarget != null
        var current = 0
        var best = 0
        for (outcome in outcomes(habit.startDay, last, now)) {
            if (outcome.weekly != weekly || !outcome.done) {
                current = 0
            } else {
                current++
                best = maxOf(current, best)
            }
        }
        return Streaks(current, best, weekly)
    }

    private fun outcomes(first: Long, last: Long, now: Long): Sequence<Outcome> = sequence {
        val end = min(last, min(now, (habit.archivedDay ?: now) + 6))
        val start = maxOf(first, habit.startDay)
        if (start > end) return@sequence
        for (day in start..end) {
            val schedule = scheduleAt(day) ?: continue
            val windowEnd = day == weekStart(day) + 6 || day == end
            if (schedule.weeklyTarget != null) {
                if (!windowEnd) continue
                val progress = weeklyProgress(day, day) ?: continue
                if (progress.target == 0) continue
                val weekIsOver = weekStart(day) + 6 < now
                if (progress.met || weekIsOver) yield(Outcome(weekly = true, done = progress.met))
            } else {
                val state = dayState(day, now)
                if (state == DayState.DONE) yield(Outcome(weekly = false, done = true))
                if (state == DayState.MISSED || state == DayState.PARTIAL && day < now) {
                    yield(Outcome(weekly = false, done = false))
                }
            }
        }
    }

    private data class Outcome(val weekly: Boolean, val done: Boolean)
}
