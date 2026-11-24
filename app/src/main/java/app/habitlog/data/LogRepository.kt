package app.habitlog.data

import android.content.Context
import androidx.room.withTransaction
import app.habitlog.model.DayState
import app.habitlog.model.HabitHistory
import app.habitlog.model.MIN_DAY
import app.habitlog.model.today
import app.habitlog.model.weekStart
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

data class HabitDraft(
    val name: String,
    val color: Int,
    val icon: Int,
    val startDay: Long,
    val schedule: Schedule,
    val reminderMinute: Int?,
)

class LogRepository(context: Context) {
    private val db = LogDatabase.get(context)
    private val dao = db.logDao()

    val snapshots = db.invalidationTracker.createFlow("habits", "schedules", "check_ins", "pauses", "tasks")
        .map { snapshot() }
        .distinctUntilChanged()

    suspend fun snapshot(): LogSnapshot = db.withTransaction { readSnapshot() }

    private suspend fun readSnapshot() = LogSnapshot(
        dao.habits(), dao.schedules(), dao.checkIns(), dao.pauses(), dao.tasks(),
    )

    suspend fun saveHabit(id: Long?, draft: HabitDraft) = db.withTransaction {
        val day = today()
        val snapshot = readSnapshot()
        val previous = snapshot.histories.find { it.habit.id == id }
        val name = draft.name.trim()
        require(name.isNotEmpty() && name.length <= 80)
        require(draft.startDay in MIN_DAY..day)
        val validSchedule = draft.schedule.weeklyTarget?.let { it in 1..7 } ?: (draft.schedule.weekdays in 1..127)
        require(validSchedule)
        require(draft.schedule.amountGoal == null || draft.schedule.amountGoal in 0.01..1_000_000.0)
        require(draft.schedule.quickAmount == null || draft.schedule.quickAmount in 0.01..1_000_000.0)
        require(draft.reminderMinute == null || draft.reminderMinute in 0..1439)

        if (previous == null) {
            check(id == null)
            val nextPosition = (snapshot.habits.maxOfOrNull(Habit::position) ?: -1) + 1
            val newId = dao.insertHabit(Habit(name = name, color = draft.color, icon = draft.icon,
                startDay = draft.startDay, position = nextPosition, reminderMinute = draft.reminderMinute))
            dao.putSchedule(draft.schedule.copy(habitId = newId, effectiveDay = draft.startDay))
        } else {
            dao.putHabit(previous.habit.copy(name = name, icon = draft.icon, color = draft.color,
                reminderMinute = draft.reminderMinute))
            val current = previous.scheduleAt(day)
            if (previous.schedules.lastOrNull()?.settings() != draft.schedule.settings()) {
                dao.deleteFutureSchedules(previous.habit.id, day)
                if (current?.settings() != draft.schedule.settings()) {
                    val hasHistory = previous.habit.startDay < day || previous.entry(day) != null
                    val effective = when {
                        hasHistory && (current?.weeklyTarget != null || draft.schedule.weeklyTarget != null) ->
                            weekStart(day) + 7
                        previous.entry(day)?.let { it.amount > 0 || it.skipped } == true -> day + 1
                        else -> day
                    }
                    dao.putSchedule(draft.schedule.copy(habitId = previous.habit.id, effectiveDay = effective))
                }
            }
        }
    }

    suspend fun quickCheckIn(id: Long, day: Long) = db.withTransaction {
        val history = history(id) ?: return@withTransaction false
        val now = today()
        val state = history.dayState(day, now)
        if (state !in listOf(DayState.PENDING, DayState.PARTIAL, DayState.MISSED, DayState.UNLOGGED)) return@withTransaction false
        val schedule = history.scheduleAt(day) ?: return@withTransaction false
        val current = history.entry(day) ?: CheckIn(id, day)
        val amount = if (schedule.amountGoal == null) 1.0 else current.amount + (schedule.quickAmount ?: 1.0)
        val saved = amount.coerceAtMost(1_000_000.0)
        putEntry(current.copy(amount = saved, skipped = false))
        saved + 1e-9 >= (schedule.amountGoal ?: 1.0)
    }

    suspend fun undoCheckIn(id: Long, day: Long) = db.withTransaction {
        val entry = history(id)?.entry(day) ?: return@withTransaction
        putEntry(entry.copy(amount = 0.0, skipped = false))
    }

    suspend fun saveCheckIn(id: Long, day: Long, amount: Double, skipped: Boolean, note: String) = db.withTransaction {
        val history = history(id) ?: return@withTransaction
        check(day <= today() && history.isActive(day))
        require(amount in 0.0..1_000_000.0 && note.length <= 4000)
        check(amount == 0.0 || history.isScheduled(day) &&
            (history.pauseOn(day) == null || history.entry(day)?.amount == amount))
        val finalAmount = if (history.scheduleAt(day)?.amountGoal == null && amount > 0) 1.0 else amount
        putEntry(CheckIn(id, day, if (skipped) 0.0 else finalAmount, skipped, note.trim()))
    }

    suspend fun skipToday(id: Long, skip: Boolean) = db.withTransaction {
        val day = today()
        val history = history(id) ?: return@withTransaction
        if (history.habit.archivedDay != null || !history.isScheduled(day)) return@withTransaction
        val entry = history.entry(day) ?: CheckIn(id, day)
        if (skip && history.dayState(day, day) == DayState.DONE) return@withTransaction
        putEntry(entry.copy(amount = if (skip) 0.0 else entry.amount, skipped = skip))
    }

    suspend fun pause(id: Long, lastDay: Long) = db.withTransaction {
        val day = today()
        require(lastDay in day..day + 3650)
        val history = history(id) ?: return@withTransaction
        if (history.habit.archivedDay == null) {
            val current = history.pauseOn(day)
            dao.putPause(Pause(id, current?.firstDay ?: day, lastDay))
        }
    }

    suspend fun resume(id: Long) = db.withTransaction {
        val day = today()
        val pause = history(id)?.pauseOn(day) ?: return@withTransaction
        if (pause.firstDay == day) dao.deletePause(id, day)
        else dao.putPause(pause.copy(lastDay = day - 1))
    }

    suspend fun moveHabit(id: Long, delta: Int) = db.withTransaction {
        val active = readSnapshot().histories.map(HabitHistory::habit).filter { it.archivedDay == null }.toMutableList()
        val index = active.indexOfFirst { it.id == id }
        val destination = index + delta
        if (index >= 0 && destination in active.indices) {
            val moved = active.removeAt(index)
            active.add(destination, moved)
            active.forEachIndexed { position, habit -> dao.putHabit(habit.copy(position = position)) }
        }
    }

    suspend fun archiveHabit(id: Long) = db.withTransaction {
        val habit = history(id)?.habit ?: return@withTransaction
        if (habit.archivedDay == null) dao.putHabit(habit.copy(archivedDay = today()))
    }

    suspend fun saveTask(id: Long?, name: String, dueDay: Long?) = db.withTransaction {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty() && trimmed.length <= 160)
        val existing = dao.tasks().find { it.id == id }
        check(id == null || existing != null)
        dao.putTask(Task(id ?: 0, trimmed, dueDay, existing?.doneDay))
    }

    suspend fun toggleTask(id: Long) = db.withTransaction {
        val task = dao.tasks().find { it.id == id } ?: return@withTransaction
        dao.putTask(task.copy(doneDay = if (task.doneDay == null) today() else null))
    }

    suspend fun deleteTask(id: Long) = db.withTransaction { dao.deleteTask(id) }

    suspend fun restore(snapshot: LogSnapshot) = db.withTransaction {
        dao.deleteHabits()
        dao.deleteTasks()
        dao.insertHabits(snapshot.habits)
        dao.insertSchedules(snapshot.schedules)
        dao.insertCheckIns(snapshot.checkIns)
        dao.insertPauses(snapshot.pauses)
        dao.insertTasks(snapshot.tasks)
    }

    private suspend fun history(id: Long) = readSnapshot().histories.find { it.habit.id == id }

    private suspend fun putEntry(entry: CheckIn) {
        if (entry.amount == 0.0 && !entry.skipped && entry.note.isEmpty()) {
            dao.deleteCheckIn(entry.habitId, entry.day)
        } else {
            dao.putCheckIn(entry)
        }
    }
}
