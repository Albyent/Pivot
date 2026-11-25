package app.habitlog.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.habitlog.data.HabitDraft
import app.habitlog.data.Schedule
import app.habitlog.data.Task
import app.habitlog.model.HabitColor
import app.habitlog.model.HabitHistory
import app.habitlog.model.HabitIcon
import app.habitlog.model.MIN_DAY
import app.habitlog.model.asDate
import app.habitlog.model.dayLabel
import app.habitlog.model.parseAmount
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun EditorContent(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 21.dp, end = 21.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Ink.muted)
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HabitEditor(
    history: HabitHistory?,
    now: Long,
    suggestedColor: Int,
    pickerContext: Context,
    onSave: (HabitDraft) -> Unit,
) {
    val habit = history?.habit
    val initial = history?.schedules?.lastOrNull() ?: Schedule()
    var name by rememberSaveable { mutableStateOf(habit?.name.orEmpty()) }
    var color by rememberSaveable { mutableStateOf(habit?.color ?: suggestedColor) }
    var icon by rememberSaveable { mutableStateOf(habit?.icon ?: 0) }
    var pickingIcon by rememberSaveable { mutableStateOf(false) }
    var start by rememberSaveable { mutableStateOf(habit?.startDay ?: now) }
    var mode by rememberSaveable {
        mutableStateOf(if (initial.weeklyTarget != null) 2 else if (initial.weekdays == 127) 0 else 1)
    }
    var weekdays by rememberSaveable { mutableStateOf(if (initial.weekdays == 127) 31 else initial.weekdays) }
    var weekly by rememberSaveable { mutableStateOf(initial.weeklyTarget ?: 3) }
    var numeric by rememberSaveable { mutableStateOf(initial.amountGoal != null) }
    var goal by rememberSaveable { mutableStateOf(initial.amountGoal?.inputAmount().orEmpty()) }
    var unit by rememberSaveable { mutableStateOf(initial.unit) }
    var quick by rememberSaveable { mutableStateOf(initial.quickAmount?.inputAmount() ?: "1") }
    var reminder by rememberSaveable { mutableStateOf(habit?.reminderMinute) }
    val parsedGoal = parseAmount(goal)
    val parsedQuick = parseAmount(quick)
    val validNumeric = !numeric || parsedGoal != null && parsedGoal in 0.01..1_000_000.0 &&
        parsedQuick != null && parsedQuick in 0.01..1_000_000.0
    val validSchedule = mode != 1 || weekdays != 0
    if (pickingIcon) {
        IconPicker(HabitIcon.fromId(icon), habitInk(HabitColor.fromId(color).tint), onClose = { pickingIcon = false }) {
            icon = it.id
        }
        return
    }
    EditorContent(if (habit == null) "New habit" else "Edit habit") {
        OutlinedTextField(name, { if (it.length <= 80) name = it }, Modifier.fillMaxWidth(),
            label = { Text("Habit name") }, singleLine = true)
        Text("Schedule", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Daily", "Selected days", "Weekly goal").forEachIndexed { index, label ->
                ActionPill(label, { mode = index }, active = mode == index)
            }
        }
        if (mode == 1) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DayOfWeek.entries.forEach { day ->
                    val bit = 1 shl (day.value - 1)
                    val selected = weekdays and bit != 0
                    val full = day.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    val short = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2)
                    Box(Modifier.weight(1f).height(44.dp).clip(CircleShape)
                        .background(if (selected) Ink.primary.copy(alpha = 0.19f) else Ink.recessed)
                        .border(1.dp, if (selected) Ink.primary.copy(alpha = 0.65f) else Ink.border, CircleShape)
                        .semantics { contentDescription = full }
                        .selectable(selected, role = Role.Checkbox, onClick = { weekdays = weekdays xor bit }),
                        contentAlignment = Alignment.Center) {
                        Text(short, color = if (selected) Ink.primary else Ink.muted, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        if (mode == 2) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                ToolbarAction(Icons.Outlined.Remove, "Decrease weekly target", { weekly = (weekly - 1).coerceAtLeast(1) })
                Text("$weekly days per week", style = MaterialTheme.typography.titleSmall)
                ToolbarAction(Icons.Outlined.Add, "Increase weekly target", { weekly = (weekly + 1).coerceAtMost(7) })
            }
        }
        if (habit == null) {
            ActionPill("Starts " + dayLabel(start, "MMM d"), {
                chooseDate(pickerContext, start, MIN_DAY, now) { start = it }
            }, icon = Icons.Outlined.CalendarToday)
        }
        SwitchRow("Track an amount", numeric) { numeric = it }
        if (numeric) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(goal, { if (it.length <= 11) goal = it }, Modifier.weight(1f),
                    label = { Text("Goal per day") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(unit, { if (it.length <= 16) unit = it }, Modifier.weight(1f),
                    label = { Text("Unit (optional)") }, singleLine = true)
            }
            OutlinedTextField(quick, { if (it.length <= 11) quick = it }, Modifier.fillMaxWidth(),
                label = { Text("Quick add amount") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        }
        SwitchRow("Reminder", reminder != null) { reminder = if (it) 8 * 60 else null }
        reminder?.let { minute ->
            ActionPill(timeLabel(pickerContext, minute), {
                TimePickerDialog(pickerContext, { _, hour, mins -> reminder = hour * 60 + mins },
                    minute / 60, minute % 60, DateFormat.is24HourFormat(pickerContext)).show()
            }, icon = Icons.Outlined.NotificationsNone)
        }
        HabitAppearanceControls(
            HabitColor.fromId(color), HabitIcon.fromId(icon),
            onColor = { color = it.id },
            onPickIcon = { pickingIcon = true },
        )
        Button(
            onClick = {
                val schedule = Schedule(
                    weekdays = if (mode == 1) weekdays else 127,
                    weeklyTarget = weekly.takeIf { mode == 2 },
                    amountGoal = parsedGoal.takeIf { numeric },
                    quickAmount = parsedQuick.takeIf { numeric },
                    unit = if (numeric) unit.trim() else "",
                )
                onSave(HabitDraft(name, color, icon, start, schedule, reminder))
            },
            enabled = name.isNotBlank() && validNumeric && validSchedule,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text(if (habit == null) "Create habit" else "Save changes") }
    }
}

@Composable
internal fun EntryEditor(
    history: HabitHistory,
    day: Long,
    now: Long,
    onSave: (Double, Boolean, String) -> Unit,
) {
    val entry = history.entry(day)
    val schedule = history.scheduleAt(day)
    val numeric = schedule?.amountGoal != null
    val label = history.dayState(day, now).label
    var done by rememberSaveable { mutableStateOf((entry?.amount ?: 0.0) > 0.0) }
    var amount by rememberSaveable { mutableStateOf(entry?.amount?.takeIf { it > 0.0 }?.inputAmount().orEmpty()) }
    var note by rememberSaveable { mutableStateOf(entry?.note.orEmpty()) }
    var skipped by rememberSaveable { mutableStateOf(entry?.skipped == true) }
    val canCheck = history.isScheduled(day) && history.pauseOn(day) == null
    val parsed = if (amount.isBlank()) 0.0 else parseAmount(amount)
    val valid = skipped || !numeric || parsed != null && parsed in 0.0..1_000_000.0
    EditorContent(history.habit.name, dayLabel(day, "EEEE, MMM d, uuuu") + " · " + label) {
        if (canCheck) {
            if (numeric && !skipped) {
                val unit = schedule?.unit.orEmpty()
                OutlinedTextField(amount, { if (it.length <= 11) amount = it }, Modifier.fillMaxWidth(),
                    label = { Text(if (unit.isBlank()) "Amount logged" else "Amount logged ($unit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            } else if (!skipped) {
                SwitchRow("Completed", done) { done = it }
            }
            SwitchRow("Skip this day", skipped) { skipped = it }
        }
        OutlinedTextField(
            note, { if (it.length <= 4000) note = it }, Modifier.fillMaxWidth(),
            label = { Text("Note (optional)") }, minLines = 3, maxLines = 6,
        )
        Button(
            onClick = {
                val value = when {
                    !canCheck -> entry?.amount ?: 0.0
                    numeric -> parsed ?: 0.0
                    done -> 1.0
                    else -> 0.0
                }
                onSave(value, skipped, note)
            },
            enabled = valid,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text("Save entry") }
    }
}

@Composable
internal fun TaskEditor(
    task: Task?,
    now: Long,
    pickerContext: Context,
    onSave: (String, Long?) -> Unit,
    onDelete: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(task?.name.orEmpty()) }
    var due by rememberSaveable { mutableStateOf(task?.dueDay) }
    EditorContent(if (task == null) "New task" else "Edit task") {
        OutlinedTextField(name, { if (it.length <= 160) name = it }, Modifier.fillMaxWidth(),
            label = { Text("Task name") }, singleLine = true)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            ActionPill(due?.let { dayLabel(it) } ?: "Add due date", {
                chooseDate(pickerContext, due ?: now, MIN_DAY, now + 36_525) { due = it }
            }, icon = Icons.Outlined.CalendarToday)
            if (due != null) TextButton(onClick = { due = null }) { Text("Clear date") }
        }
        Button(onClick = { onSave(name, due) }, enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (task == null) "Create task" else "Save changes")
        }
        if (task != null) TextButton(onClick = onDelete) { Text("Delete task", color = Ink.danger) }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked, onChecked, modifier = Modifier.semantics { contentDescription = label })
    }
}

internal fun chooseDate(
    context: Context,
    initial: Long,
    minimum: Long,
    maximum: Long,
    onSelect: (Long) -> Unit,
) {
    val date = initial.coerceIn(minimum, maximum).asDate()
    val zone = ZoneId.systemDefault()
    DatePickerDialog(context, { _, year, month, day ->
        val selected = LocalDate.of(year, month + 1, day).toEpochDay()
        onSelect(selected.coerceIn(minimum, maximum))
    }, date.year, date.monthValue - 1, date.dayOfMonth).apply {
        datePicker.minDate = minimum.asDate().atStartOfDay(zone).toInstant().toEpochMilli()
        datePicker.maxDate = maximum.asDate().atStartOfDay(zone).toInstant().toEpochMilli()
    }.show()
}

internal fun timeLabel(context: Context, minute: Int): String {
    val time = LocalTime.of(minute / 60, minute % 60)
    val formatter = if (DateFormat.is24HourFormat(context)) DateTimeFormatter.ofPattern("HH:mm")
    else DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    return time.format(formatter)
}

private fun Double.inputAmount(): String = BigDecimal.valueOf(this).stripTrailingZeros().toPlainString()
