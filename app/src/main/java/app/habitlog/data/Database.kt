package app.habitlog.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import app.habitlog.model.HabitHistory

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int,
    val icon: Int,
    val startDay: Long,
    val position: Int,
    val archivedDay: Long? = null,
    val reminderMinute: Int? = null,
)

@Entity(
    tableName = "schedules",
    primaryKeys = ["habitId", "effectiveDay"],
    foreignKeys = [ForeignKey(entity = Habit::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)],
)
data class Schedule(
    val habitId: Long = 0,
    val effectiveDay: Long = 0,
    val weekdays: Int = 127,
    val weeklyTarget: Int? = null,
    val amountGoal: Double? = null,
    val unit: String = "",
    val quickAmount: Double? = null,
) {
    fun settings() = copy(habitId = 0, effectiveDay = 0)
}

@Entity(
    tableName = "check_ins",
    primaryKeys = ["habitId", "day"],
    foreignKeys = [ForeignKey(entity = Habit::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)],
)
data class CheckIn(
    val habitId: Long,
    val day: Long,
    val amount: Double = 0.0,
    val skipped: Boolean = false,
    val note: String = "",
)

@Entity(
    tableName = "pauses",
    primaryKeys = ["habitId", "firstDay"],
    foreignKeys = [ForeignKey(entity = Habit::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)],
)
data class Pause(val habitId: Long, val firstDay: Long, val lastDay: Long)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dueDay: Long? = null,
    val doneDay: Long? = null,
)

data class LogSnapshot(
    val habits: List<Habit> = emptyList(),
    val schedules: List<Schedule> = emptyList(),
    val checkIns: List<CheckIn> = emptyList(),
    val pauses: List<Pause> = emptyList(),
    val tasks: List<Task> = emptyList(),
) {
    val histories: List<HabitHistory> by lazy {
        val bySchedule = schedules.groupBy(Schedule::habitId)
        val byCheckIn = checkIns.groupBy(CheckIn::habitId)
        val byPause = pauses.groupBy(Pause::habitId)
        habits.sortedWith(compareBy(Habit::position, Habit::id)).map { habit ->
            HabitHistory(
                habit,
                bySchedule[habit.id].orEmpty(),
                byCheckIn[habit.id].orEmpty(),
                byPause[habit.id].orEmpty(),
            )
        }
    }
}

@Dao
interface LogDao {
    @Query("SELECT * FROM habits") suspend fun habits(): List<Habit>
    @Query("SELECT * FROM schedules") suspend fun schedules(): List<Schedule>
    @Query("SELECT * FROM check_ins") suspend fun checkIns(): List<CheckIn>
    @Query("SELECT * FROM pauses") suspend fun pauses(): List<Pause>
    @Query("SELECT * FROM tasks") suspend fun tasks(): List<Task>

    @Insert suspend fun insertHabit(habit: Habit): Long
    @Upsert suspend fun putHabit(habit: Habit)
    @Upsert suspend fun putSchedule(schedule: Schedule)
    @Upsert suspend fun putCheckIn(checkIn: CheckIn)
    @Upsert suspend fun putPause(pause: Pause)
    @Upsert suspend fun putTask(task: Task)

    @Query("DELETE FROM schedules WHERE habitId = :habitId AND effectiveDay > :day")
    suspend fun deleteFutureSchedules(habitId: Long, day: Long)

    @Query("DELETE FROM check_ins WHERE habitId = :habitId AND day = :day")
    suspend fun deleteCheckIn(habitId: Long, day: Long)

    @Query("DELETE FROM pauses WHERE habitId = :habitId AND firstDay = :firstDay")
    suspend fun deletePause(habitId: Long, firstDay: Long)

    @Query("DELETE FROM tasks WHERE id = :id") suspend fun deleteTask(id: Long)
    @Query("DELETE FROM habits") suspend fun deleteHabits()
    @Query("DELETE FROM tasks") suspend fun deleteTasks()

    @Insert suspend fun insertHabits(habits: List<Habit>)
    @Insert suspend fun insertSchedules(schedules: List<Schedule>)
    @Insert suspend fun insertCheckIns(checkIns: List<CheckIn>)
    @Insert suspend fun insertPauses(pauses: List<Pause>)
    @Insert suspend fun insertTasks(tasks: List<Task>)
}

@Database(
    entities = [Habit::class, Schedule::class, CheckIn::class, Pause::class, Task::class],
    version = 1,
)
abstract class LogDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao

    companion object {
        @Volatile private var instance: LogDatabase? = null

        fun get(context: Context): LogDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                LogDatabase::class.java,
                "habit-log.db",
            ).build().also { instance = it }
        }
    }
}
