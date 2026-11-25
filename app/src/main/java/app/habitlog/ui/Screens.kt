package app.habitlog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.habitlog.data.CheckIn
import app.habitlog.data.LogSnapshot
import app.habitlog.data.Task
import app.habitlog.model.HabitColor
import app.habitlog.model.HabitHistory
import app.habitlog.model.amountLabel
import app.habitlog.model.dayLabel
import app.habitlog.model.weekStart

internal val screenPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 7.dp, bottom = 24.dp)

@Composable
internal fun HabitDetails(
    history: HabitHistory,
    now: Long,
    contextLine: String,
    onDate: (Long) -> Unit,
    onDatePicker: () -> Unit,
) {
    val habit = history.habit
    val accent = HabitColor.fromId(habit.color).tint
    val streak = remember(history, now) { history.streaks(now) }
    var range by rememberSaveable(habit.id) { mutableIntStateOf(30) }
    var notesOnly by rememberSaveable(habit.id) { mutableStateOf(false) }
    val entries = remember(history, notesOnly, now) {
        history.entries.values.filter { it.day <= now && (!notesOnly || it.note.isNotEmpty()) }
            .sortedByDescending(CheckIn::day)
    }
    val from = now - range + 1
    val completion = remember(history, range, now) { history.completion(from, now, now) }
    val count = remember(history, range, now) { history.completedBetween(from, now, now) }
    val period = if (streak.weekly) "wk" else "d"
    LazyColumn(contentPadding = screenPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Panel {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        HabitGlyph(habit.icon, accent)
                        Text(contextLine, Modifier.weight(1f), color = Ink.muted, style = MaterialTheme.typography.bodyMedium)
                        ActionPill("Pick date", onDatePicker, icon = Icons.Outlined.CalendarToday)
                    }
                    Heatmap(now, { day -> habitCell(history.dayState(day, now), accent) }, { day ->
                        if (history.isActive(day)) onDate(day)
                    })
                    val next = history.schedules.firstOrNull { it.effectiveDay > now }
                    if (next != null && habit.archivedDay == null) {
                        Text("Schedule changes " + dayLabel(next.effectiveDay), color = Ink.muted,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            Panel {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("Progress", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        listOf(7, 30, 90).forEach { days -> ActionPill("${days}d", { range = days }, active = range == days) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ValueLabel(completion.percent?.let { "$it%" } ?: "—", "Completion rate", Modifier.weight(1f))
                        ValueLabel(count.toString(), "Check-ins", Modifier.weight(1f))
                    }
                    HorizontalDivider(color = Ink.border)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ValueLabel("${streak.current} $period", "Current streak", Modifier.weight(1f))
                        ValueLabel("${streak.best} $period", "Best streak", Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            SectionTitle("History", Icons.Outlined.CalendarToday) {
                ActionPill("Notes only", { notesOnly = !notesOnly }, active = notesOnly, icon = Icons.AutoMirrored.Outlined.StickyNote2)
            }
        }
        if (entries.isEmpty()) {
            item {
                Panel {
                    Text(if (notesOnly) "No notes yet" else "No entries yet", Modifier.padding(17.dp),
                        color = Ink.muted, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        items(entries, key = CheckIn::day) { entry ->
            val status = history.dayState(entry.day, now)
            val schedule = history.scheduleAt(entry.day)
            val amount = if (schedule?.amountGoal != null && entry.amount > 0) {
                " · " + amountLabel(entry.amount) + schedule.unit.takeIf(String::isNotBlank)?.let { " $it" }.orEmpty()
            } else ""
            Panel {
                DetailRow(dayLabel(entry.day, "EEE, MMM d, uuuu"), status.label + amount, entry.note,
                    icon = if (entry.note.isNotBlank()) Icons.AutoMirrored.Outlined.StickyNote2 else null,
                    onClick = { onDate(entry.day) })
            }
        }
    }
}

@Composable
internal fun Insights(snapshot: LogSnapshot, now: Long, onEntry: (Long, Long) -> Unit) {
    val currentWeek = weekStart(now)
    var offset by rememberSaveable { mutableIntStateOf(0) }
    val first = currentWeek - offset * 7L
    val last = minOf(now, first + 6)
    val histories = snapshot.histories
    val countByDay = remember(snapshot, now) {
        histories.asSequence().flatMap { history ->
            history.entries.keys.asSequence().filter { history.completedOn(it, now) }
        }.groupingBy { it }.eachCount()
    }
    val trend = remember(countByDay, currentWeek) {
        (7 downTo 0).map { ago ->
            val start = currentWeek - ago * 7L
            dayLabel(start, "d") to (start..start + 6).sumOf { countByDay[it] ?: 0 }
        }
    }
    val counts = (first..first + 6).map { dayLabel(it, "EEE").take(2) to (countByDay[it] ?: 0) }
    val notes = remember(snapshot, first, last) {
        histories.flatMap { history ->
            history.entries.values.filter { it.day in first..last && it.note.isNotBlank() }.map { history to it }
        }.sortedByDescending { it.second.day }
    }
    val earliest = histories.minOfOrNull { weekStart(it.habit.startDay) } ?: currentWeek
    LazyColumn(contentPadding = screenPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionTitle("Activity by week", Icons.Outlined.BarChart)
            Panel {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Text("Last eight weeks", color = Ink.muted, style = MaterialTheme.typography.bodySmall)
                    BarChart(trend, Ink.graph)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(dayLabel(currentWeek - 49, "MMM d"), color = Ink.dim, style = MaterialTheme.typography.labelSmall)
                        Text("This week", color = Ink.dim, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        item {
            SectionTitle("Weekly review", Icons.Outlined.CalendarToday)
            Panel {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { offset++ }, enabled = first > earliest) {
                            Icon(Icons.Outlined.ChevronLeft, "Previous week")
                        }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (offset == 0) "This week" else periodLabel(first, first + 6),
                                style = MaterialTheme.typography.titleMedium)
                            if (offset == 0) Text(periodLabel(first, first + 6), color = Ink.muted,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { offset-- }, enabled = offset > 0) {
                            Icon(Icons.Outlined.ChevronRight, "Next week")
                        }
                    }
                    BarChart(counts, Ink.primary)
                    HorizontalDivider(color = Ink.border)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        ValueLabel(counts.sumOf { it.second }.toString(), "Check-ins", Modifier.weight(1f))
                        ValueLabel(counts.count { it.second > 0 }.toString(), "Active days", Modifier.weight(1f))
                    }
                }
            }
        }
        item { SectionTitle("Notes this week".takeIf { offset == 0 } ?: "Weekly notes", Icons.AutoMirrored.Outlined.StickyNote2) }
        if (notes.isEmpty()) {
            item { Panel { Text("No notes for this week", Modifier.padding(17.dp), color = Ink.muted) } }
        }
        items(notes, key = { "${it.first.habit.id}:${it.second.day}" }) { (history, entry) ->
            Panel {
                DetailRow(history.habit.name, dayLabel(entry.day), entry.note, Icons.AutoMirrored.Outlined.StickyNote2,
                    onClick = { onEntry(history.habit.id, entry.day) })
            }
        }
    }
}

@Composable
private fun BarChart(bars: List<Pair<String, Int>>, color: Color) {
    val cap = bars.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        bars.forEach { (label, count) ->
            Column(
                Modifier.weight(1f).semantics { contentDescription = "$label, $count check-ins" },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(count.toString(), color = Ink.muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                Box(Modifier.height(68.dp).fillMaxWidth().clip(RoundedCornerShape(5.dp)).background(Ink.recessed),
                    contentAlignment = Alignment.BottomCenter) {
                    if (count > 0) Box(Modifier.fillMaxWidth().fillMaxHeight(count.toFloat() / cap)
                        .background(color, RoundedCornerShape(5.dp)))
                }
                Text(label, color = Ink.muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

@Composable
internal fun TasksScreen(
    tasks: List<Task>,
    now: Long,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onToggle: (Long) -> Unit,
) {
    val open = tasks.filter { it.doneDay == null }.sortedWith(compareBy({ it.dueDay ?: Long.MAX_VALUE }, Task::id))
    val finished = tasks.filter { it.doneDay != null }.sortedByDescending(Task::doneDay)
    LazyColumn(contentPadding = screenPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SectionTitle("To do", Icons.Outlined.Checklist) {
                ActionPill("New task", onAdd, icon = Icons.Outlined.Add, active = true)
            }
        }
        if (open.isEmpty()) {
            item {
                Panel {
                    Text(if (tasks.isEmpty()) "Add something you need to do once." else "You’re caught up.",
                        Modifier.padding(17.dp), color = Ink.muted, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        items(open, key = Task::id) { task -> TaskRow(task, now, { onEdit(task.id) }, { onToggle(task.id) }) }
        if (finished.isNotEmpty()) {
            item { SectionTitle("Completed", Icons.Outlined.Checklist) }
            items(finished, key = Task::id) { task -> TaskRow(task, now, { onEdit(task.id) }, { onToggle(task.id) }) }
        }
    }
}

@Composable
private fun TaskRow(task: Task, now: Long, onEdit: () -> Unit, onToggle: () -> Unit) {
    val done = task.doneDay != null
    val due = task.dueDay
    Panel {
        Row(Modifier.padding(start = 7.dp, end = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(done, { onToggle() }, modifier = Modifier.semantics { contentDescription = task.name })
            Column(Modifier.weight(1f).clickable(onClickLabel = "Edit task", onClick = onEdit).padding(6.dp)) {
                Text(task.name, style = MaterialTheme.typography.titleSmall,
                    color = if (done) Ink.muted else Ink.text,
                    textDecoration = if (done) TextDecoration.LineThrough else null)
                if (due != null) Text(
                    when {
                        due == now -> "Today"
                        due < now && !done -> "Overdue · " + dayLabel(due)
                        else -> dayLabel(due)
                    },
                    color = if (due < now && !done) Ink.danger else Ink.dim,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.ChevronRight, "Edit ${task.name}", tint = Ink.dim) }
        }
    }
}

@Composable
internal fun SettingsScreen(
    snapshot: LogSnapshot,
    themeLabel: String,
    onTheme: () -> Unit,
    onExport: () -> Unit,
    onRestore: () -> Unit,
    onWidget: () -> Unit,
    onNotifications: () -> Unit,
    onArchived: (Long) -> Unit,
) {
    val archived = snapshot.histories.filter { it.habit.archivedDay != null }
    LazyColumn(contentPadding = screenPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Panel {
                DetailRow("App theme", themeLabel, icon = Icons.Outlined.Palette, onClick = onTheme)
            }
        }
        item {
            Panel {
                Column {
                    DetailRow("Export backup", "Save a copy of your habits, entries, notes, and tasks.",
                        icon = Icons.Outlined.FileDownload, onClick = onExport)
                    HorizontalDivider(Modifier.padding(horizontal = 15.dp), color = Ink.border)
                    DetailRow("Restore backup", "Load a Pivot backup file.",
                        icon = Icons.Outlined.FileUpload, onClick = onRestore)
                }
            }
        }
        item {
            Panel {
                Column {
                    DetailRow("Home-screen widget", "Check in from your Android home screen.",
                        icon = Icons.Outlined.Widgets, onClick = onWidget)
                    HorizontalDivider(Modifier.padding(horizontal = 15.dp), color = Ink.border)
                    DetailRow("Notification settings", "Manage local habit reminders.",
                        icon = Icons.Outlined.NotificationsNone, onClick = onNotifications)
                }
            }
        }
        if (archived.isNotEmpty()) {
            item { SectionTitle("Archived habits", Icons.Outlined.Archive) }
            items(archived, key = { it.habit.id }) { history ->
                Panel {
                    DetailRow(history.habit.name, "Archived " + dayLabel(requireNotNull(history.habit.archivedDay)),
                        onClick = { onArchived(history.habit.id) })
                }
            }
        }
    }
}

@Composable
internal fun DayOverview(
    snapshot: LogSnapshot,
    day: Long,
    now: Long,
    onPick: () -> Unit,
    onEntry: (Long) -> Unit,
) {
    val entries = snapshot.histories.filter { it.isScheduled(day) || it.entry(day) != null }
    EditorContent(if (day == now) "Today’s activity" else "Day activity", dayLabel(day, "EEEE, MMM d, uuuu")) {
        ActionPill("Pick date", onPick, icon = Icons.Outlined.CalendarToday)
        if (entries.isEmpty()) Text("No habits scheduled on this day.", color = Ink.muted,
            style = MaterialTheme.typography.bodyMedium)
        entries.forEach { history ->
            Panel {
                DetailRow(history.habit.name, history.dayState(day, now).label, history.entry(day)?.note.orEmpty(),
                    onClick = { onEntry(history.habit.id) })
            }
        }
    }
}

@Composable
internal fun DetailRow(
    title: String,
    subtitle: String,
    note: String = "",
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 57.dp).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(21.dp), tint = Ink.primary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Ink.muted, style = MaterialTheme.typography.bodySmall)
            if (note.isNotBlank()) Text(note, maxLines = 3, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium)
        }
        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(17.dp), tint = Ink.dim)
    }
}
