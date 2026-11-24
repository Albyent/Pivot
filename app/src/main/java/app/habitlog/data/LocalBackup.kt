package app.habitlog.data

import android.content.Context
import android.net.Uri
import app.habitlog.model.MAX_DAY
import app.habitlog.model.MIN_DAY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object LocalBackup {
    private const val MAX_BYTES = 16 * 1024 * 1024

    suspend fun write(context: Context, uri: Uri, snapshot: LogSnapshot) = withContext(Dispatchers.IO) {
        val document = JSONObject().apply {
            put("format", "pivot-habits")
            put("version", 1)
            put("habits", array(snapshot.habits) {
                fields("id" to it.id, "name" to it.name, "color" to it.color, "icon" to it.icon,
                    "startDay" to it.startDay, "position" to it.position, "archivedDay" to it.archivedDay,
                    "reminderMinute" to it.reminderMinute)
            })
            put("schedules", array(snapshot.schedules) {
                fields("habitId" to it.habitId, "effectiveDay" to it.effectiveDay, "weekdays" to it.weekdays,
                    "weeklyTarget" to it.weeklyTarget, "amountGoal" to it.amountGoal, "unit" to it.unit,
                    "quickAmount" to it.quickAmount)
            })
            put("checkIns", array(snapshot.checkIns) {
                fields("habitId" to it.habitId, "day" to it.day, "amount" to it.amount,
                    "skipped" to it.skipped, "note" to it.note)
            })
            put("pauses", array(snapshot.pauses) {
                fields("habitId" to it.habitId, "firstDay" to it.firstDay, "lastDay" to it.lastDay)
            })
            put("tasks", array(snapshot.tasks) {
                fields("id" to it.id, "name" to it.name, "dueDay" to it.dueDay, "doneDay" to it.doneDay)
            })
        }
        val bytes = document.toString(2).toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_BYTES)
        checkNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { it.write(bytes) }
    }

    suspend fun read(context: Context, uri: Uri): LogSnapshot = withContext(Dispatchers.IO) {
        val document = checkNotNull(context.contentResolver.openInputStream(uri)).use { stream ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var count = stream.read(buffer)
            while (count >= 0) {
                require(output.size() + count <= MAX_BYTES)
                output.write(buffer, 0, count)
                count = stream.read(buffer)
            }
            JSONObject(output.toString(Charsets.UTF_8.name()))
        }
        require(document.getString("format") == "pivot-habits" && document.getInt("version") == 1)
        val habits = document.list("habits") {
            Habit(getLong("id").checked { it > 0 }, getString("name").checked { it.isNotBlank() && it.length <= 80 },
                getInt("color").checked { it in 0..7 }, getInt("icon").checked { it in 0..7 },
                getLong("startDay").day(), getInt("position").checked { it in 0..1_000_000 },
                longOrNull("archivedDay")?.day(), intOrNull("reminderMinute")?.checked { it in 0..1439 })
        }
        val byId = habits.associateBy(Habit::id)
        require(byId.size == habits.size)
        habits.forEach { require(it.archivedDay == null || it.archivedDay >= it.startDay) }

        val schedules = document.list("schedules") {
            Schedule(getLong("habitId"), getLong("effectiveDay").day(), getInt("weekdays").checked { it in 1..127 },
                intOrNull("weeklyTarget")?.checked { it in 1..7 },
                doubleOrNull("amountGoal")?.checked { it in 0.01..1_000_000.0 },
                getString("unit").checked { it.length <= 16 },
                doubleOrNull("quickAmount")?.checked { it in 0.01..1_000_000.0 })
        }
        require(schedules.distinctBy { it.habitId to it.effectiveDay }.size == schedules.size)
        schedules.forEach { require(it.effectiveDay >= requireNotNull(byId[it.habitId]).startDay) }
        val firstSchedules = schedules.groupBy(Schedule::habitId)
        habits.forEach { habit -> require(firstSchedules[habit.id]?.minOf(Schedule::effectiveDay) == habit.startDay) }

        val checkIns = document.list("checkIns") {
            CheckIn(getLong("habitId"), getLong("day").day(), getDouble("amount").checked { it in 0.0..1_000_000.0 },
                getBoolean("skipped"), getString("note").checked { it.length <= 4000 })
        }
        require(checkIns.distinctBy { it.habitId to it.day }.size == checkIns.size)
        checkIns.forEach { entry ->
            val habit = requireNotNull(byId[entry.habitId])
            require(entry.day in habit.startDay..(habit.archivedDay ?: MAX_DAY))
            require(!entry.skipped || entry.amount == 0.0)
        }

        val pauses = document.list("pauses") {
            Pause(getLong("habitId"), getLong("firstDay").day(), getLong("lastDay").day())
        }
        pauses.groupBy(Pause::habitId).forEach { (id, periods) ->
            val habit = requireNotNull(byId[id])
            periods.forEach { require(it.firstDay >= habit.startDay && it.lastDay >= it.firstDay) }
            periods.sortedBy(Pause::firstDay).zipWithNext().forEach { (a, b) ->
                require(a.lastDay < b.firstDay)
            }
        }
        val tasks = document.list("tasks") {
            Task(getLong("id").checked { it > 0 }, getString("name").checked { it.isNotBlank() && it.length <= 160 },
                longOrNull("dueDay")?.day(), longOrNull("doneDay")?.day())
        }
        require(tasks.distinctBy(Task::id).size == tasks.size)
        LogSnapshot(habits, schedules, checkIns, pauses, tasks)
    }

    private fun fields(vararg entries: Pair<String, Any?>): JSONObject = JSONObject().apply {
        entries.forEach { (key, value) -> put(key, value ?: JSONObject.NULL) }
    }

    private fun <T> array(items: List<T>, encode: (T) -> JSONObject) =
        JSONArray().apply { items.forEach { put(encode(it)) } }

    private fun <T> JSONObject.list(key: String, decode: JSONObject.() -> T): List<T> {
        val values = getJSONArray(key)
        return List(values.length()) { values.getJSONObject(it).decode() }
    }

    private fun JSONObject.longOrNull(key: String) = if (isNull(key)) null else getLong(key)
    private fun JSONObject.intOrNull(key: String) = if (isNull(key)) null else getInt(key)
    private fun JSONObject.doubleOrNull(key: String) = if (isNull(key)) null else getDouble(key)
    private fun Long.day() = checked { it in MIN_DAY..MAX_DAY }

    private inline fun <T> T.checked(test: (T) -> Boolean): T {
        require(test(this))
        return this
    }
}
