package app.habitlog.system

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.edit
import androidx.core.net.toUri
import app.habitlog.MainActivity
import app.habitlog.R
import app.habitlog.data.LogRepository
import app.habitlog.data.LogSnapshot
import app.habitlog.model.AppAppearance
import app.habitlog.model.appearanceContext
import app.habitlog.model.isSystemDark
import app.habitlog.model.DayState
import app.habitlog.model.HabitHistory
import app.habitlog.model.amountLabel
import app.habitlog.model.asDate
import app.habitlog.model.today
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.ZonedDateTime

private const val HABIT_ID = "habit_id"
private const val DAY = "day"
private const val CHECK = "app.habitlog.WIDGET_CHECK"
private const val REMIND = "app.habitlog.REMIND"
private const val UNDO = "undo"
private const val CHANNEL = "habit-reminders"
private const val NOTIFICATION_ID = 1

object DeviceFeatures {
    private val lock = Mutex()

    suspend fun refresh(context: Context) = withContext(Dispatchers.IO) {
        lock.withLock {
            val snapshot = LogRepository(context).snapshot()
            updateWidgets(context, snapshot)
            updateReminders(context, snapshot)
        }
    }

    private fun updateReminders(context: Context, snapshot: LogSnapshot) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Habit reminders", NotificationManager.IMPORTANCE_DEFAULT))
        val alarms = context.getSystemService(AlarmManager::class.java)
        val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
        val nextIds = mutableSetOf<String>()
        val now = ZonedDateTime.now()
        val today = now.toLocalDate().toEpochDay()
        for (history in snapshot.histories) {
            val id = history.habit.id
            if (!history.isDue(today)) manager.cancel(id.toString(), NOTIFICATION_ID)
            if (!notificationsEnabled(context) || history.habit.archivedDay != null) continue
            val minute = history.habit.reminderMinute ?: continue
            val next = nextReminder(history, minute, now) ?: continue
            val intent = reminderIntent(context, id).putExtra(DAY, next.toLocalDate().toEpochDay())
            val pending = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), pending)
            nextIds.add(id.toString())
        }
        val previousIds = prefs.getStringSet("scheduled", emptySet()).orEmpty()
        for (id in previousIds - nextIds) {
            val number = id.toLongOrNull() ?: continue
            val pending = PendingIntent.getBroadcast(context, 0, reminderIntent(context, number),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
            if (pending != null) {
                alarms.cancel(pending)
                pending.cancel()
            }
            if (snapshot.habits.none { it.id == number }) manager.cancel(id, NOTIFICATION_ID)
        }
        prefs.edit { putStringSet("scheduled", nextIds) }
    }

    private fun nextReminder(history: HabitHistory, minute: Int, now: ZonedDateTime): ZonedDateTime? {
        var day = maxOf(now.toLocalDate().toEpochDay(), history.habit.startDay)
        val time = LocalTime.of(minute / 60, minute % 60)
        repeat(15) {
            val pause = history.pauseOn(day)
            if (pause != null) day = pause.lastDay + 1
            val candidate = ZonedDateTime.of(day.asDate(), time, now.zone)
            if (candidate > now && history.isDue(day)) return candidate
            day++
        }
        return null
    }

    private fun updateWidgets(context: Context, snapshot: LogSnapshot) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, HabitWidget::class.java))
        if (ids.isEmpty()) return
        val date = today()
        val rowIds = intArrayOf(R.id.widget_row_1, R.id.widget_row_2, R.id.widget_row_3)
        val names = intArrayOf(R.id.widget_name_1, R.id.widget_name_2, R.id.widget_name_3)
        val checks = intArrayOf(R.id.widget_check_1, R.id.widget_check_2, R.id.widget_check_3)
        val appearance = AppAppearance.load(context)
        val dark = appearance.mode.isDark(context.isSystemDark())
        val theme = context.appearanceContext(dark)
        val accent = theme.getColor(appearance.accent.colorRes)
        val foreground = theme.getColor(R.color.pivot_text)
        val muted = theme.getColor(R.color.pivot_muted)
        val active = snapshot.histories.filter { it.habit.archivedDay == null }
        val visible = (active.filter { it.isDue(date) } +
            active.filter { it.dayState(date, date) == DayState.DONE }).take(rowIds.size)
        val views = RemoteViews(context.packageName, R.layout.habit_widget)
        views.setInt(R.id.widget_container, "setBackgroundResource",
            if (dark) R.drawable.widget_surface else R.drawable.widget_surface_light)
        views.setTextColor(R.id.widget_title, foreground)
        views.setTextColor(R.id.widget_empty, muted)
        views.setOnClickPendingIntent(R.id.widget_title, openApp(context, null))
        views.setViewVisibility(R.id.widget_empty, if (visible.isEmpty()) View.VISIBLE else View.GONE)
        for (index in rowIds.indices) {
            val history = visible.getOrNull(index)
            views.setViewVisibility(rowIds[index], if (history == null) View.GONE else View.VISIBLE)
            if (history == null) continue
            val habit = history.habit
            val done = history.dayState(date, date) == DayState.DONE
            val schedule = history.scheduleAt(date)
            val label = when {
                done -> "✓"
                schedule?.amountGoal != null -> "+" + amountLabel(schedule.quickAmount ?: 1.0)
                else -> "○"
            }
            views.setTextViewText(names[index], habit.name)
            views.setTextColor(names[index], foreground)
            views.setInt(checks[index], "setBackgroundResource",
                if (dark) R.drawable.widget_check else R.drawable.widget_check_light)
            views.setOnClickPendingIntent(names[index], openApp(context, habit.id))
            views.setTextViewText(checks[index], label)
            views.setTextColor(checks[index], accent)
            views.setContentDescription(checks[index], if (done) "Undo ${habit.name}" else "Log ${habit.name}")
            val intent = Intent(context, HabitWidget::class.java).setAction(CHECK)
                .setData("pivot://widget/${habit.id}/$date/$done".toUri())
                .putExtra(HABIT_ID, habit.id).putExtra(DAY, date).putExtra(UNDO, done)
            views.setOnClickPendingIntent(checks[index], PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        }
        manager.updateAppWidget(ids, views)
    }
}

class HabitWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        inBackground { DeviceFeatures.refresh(context) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != CHECK) {
            super.onReceive(context, intent)
            return
        }
        val id = intent.getLongExtra(HABIT_ID, 0)
        val day = intent.getLongExtra(DAY, Long.MIN_VALUE)
        val undo = intent.getBooleanExtra(UNDO, false)
        inBackground {
            if (id > 0 && day == today()) {
                val repository = LogRepository(context)
                if (undo) repository.undoCheckIn(id, day) else repository.quickCheckIn(id, day)
            }
            DeviceFeatures.refresh(context)
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(HABIT_ID, 0)
        val day = today()
        val isReminder = intent.action == REMIND && intent.getLongExtra(DAY, Long.MIN_VALUE) == day
        inBackground {
            if (id > 0 && isReminder && notificationsEnabled(context)) {
                val habit = LogRepository(context).snapshot().histories.find { it.habit.id == id }
                if (habit != null && habit.habit.reminderMinute != null && habit.isDue(day)) {
                    val manager = context.getSystemService(NotificationManager::class.java)
                    val notification = Notification.Builder(context, CHANNEL)
                        .setSmallIcon(R.drawable.ic_reminder)
                        .setContentTitle(habit.habit.name)
                        .setContentText("Ready for today’s check-in?")
                        .setContentIntent(openApp(context, id))
                        .setAutoCancel(true)
                        .build()
                    manager.notify(id.toString(), NOTIFICATION_ID, notification)
                }
            }
            DeviceFeatures.refresh(context)
        }
    }
}

fun notificationsEnabled(context: Context): Boolean {
    val granted = Build.VERSION.SDK_INT < 33 ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    return granted && context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
}

private fun BroadcastReceiver.inBackground(task: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            task()
        } catch (exception: Exception) {
            Log.e("Pivot", "Unable to update Android reminders or widget", exception)
        } finally {
            pending.finish()
        }
    }
}

private fun reminderIntent(context: Context, id: Long) =
    Intent(context, ReminderReceiver::class.java).setAction(REMIND)
        .setData("pivot://reminder/$id".toUri()).putExtra(HABIT_ID, id)

private fun openApp(context: Context, id: Long?): PendingIntent {
    val intent = Intent(context, MainActivity::class.java)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    if (id != null) intent
        .setData("pivot://habit/$id".toUri())
        .putExtra(MainActivity.OPEN_HABIT, id)
    return PendingIntent.getActivity(context, 0, intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
}
