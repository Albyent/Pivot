package app.habitlog.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.habitlog.data.LogSnapshot
import app.habitlog.data.Schedule
import app.habitlog.model.DayState
import app.habitlog.model.HabitColor
import app.habitlog.model.HabitHistory
import app.habitlog.model.amountLabel
import app.habitlog.model.dayLabel
import app.habitlog.model.weekStart
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun Dashboard(
    snapshot: LogSnapshot,
    now: Long,
    listState: LazyListState,
    todayOnly: Boolean,
    onFilter: () -> Unit,
    onAdd: () -> Unit,
    onHabit: (Long) -> Unit,
    onDay: (Long) -> Unit,
    onEntry: (Long, Long) -> Unit,
    onCheck: (HabitHistory) -> Unit,
    onUndo: (Long) -> Unit,
    onSkip: (Long, Boolean) -> Unit,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
) {
    val active = remember(snapshot) { snapshot.histories.filter { it.habit.archivedDay == null } }
    val due = remember(active, now) { active.filter { it.isDue(now) } }
    val visible = if (todayOnly) due else active
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "activity") {
            ActivityCard(snapshot, now, onDay)
        }
        item(key = "habit-controls") {
            SectionTitle("Habits") {
                if (active.isNotEmpty()) {
                    ActionPill(
                        if (todayOnly) "Due today" else "${due.size} due",
                        onFilter,
                        icon = Icons.Outlined.FilterList,
                        active = todayOnly,
                    )
                }
                ToolbarAction(Icons.Outlined.Add, "New habit", onAdd)
            }
        }
        if (active.isEmpty()) {
            item(key = "empty-habits") {
                Panel {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("No habits yet", style = MaterialTheme.typography.titleMedium)
                        Button(onClick = onAdd, shape = MaterialTheme.shapes.small) { Text("Create a habit") }
                    }
                }
            }
        } else if (visible.isEmpty()) {
            item(key = "empty-today") {
                Panel {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("You’re all set for today", style = MaterialTheme.typography.titleMedium)
                        ActionPill("Show all habits", onFilter, modifier = Modifier.align(Alignment.Start))
                    }
                }
            }
        }
        items(visible, key = { it.habit.id }) { history ->
            val id = history.habit.id
            HabitCard(history, now, onDetails = { onHabit(id) },
                onEntry = { onEntry(id, it) }, onCheck = { onCheck(history) },
                onUndo = { onUndo(id) }, onSkip = { onSkip(id, it) },
                onPause = { onPause(id) }, onResume = { onResume(id) })
        }
    }
}

@Composable
private fun ActivityCard(snapshot: LogSnapshot, now: Long, onDay: (Long) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val accent = Ink.graph
    val activity = remember(snapshot, now) {
        snapshot.histories.asSequence().flatMap { history ->
            history.entries.keys.asSequence().filter { history.completedOn(it, now) }
        }.groupingBy { it }.eachCount()
    }
    var first by remember(now) { mutableStateOf(weekStart(now) - 77) }
    var last by remember(now) { mutableStateOf(now) }
    val period = remember(activity, first, last) { activity.filterKeys { it in first..last } }
    Panel {
        Column(Modifier.padding(start = 15.dp, end = 15.dp, top = 13.dp, bottom = 13.dp)) {
            SectionTitle("Activity") {
                ActionPill("Today", { onDay(now) }, icon = Icons.Outlined.CalendarToday)
            }
            Spacer(Modifier.height(7.dp))
            Heatmap(now, { day -> activityCell(activity[day] ?: 0, day, now, accent, colors) }, onDay,
                onVisibleDates = { from, through -> first = from; last = through })
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Ink.border)
            Row(Modifier.padding(top = 11.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                ValueLabel(period.values.sum().toString(), "Check-ins")
                ValueLabel(period.size.toString(), "Active days")
                Text(
                    periodLabel(first, last), color = Ink.dim, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
                    maxLines = 2,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitCard(
    history: HabitHistory,
    now: Long,
    onDetails: () -> Unit,
    onEntry: (Long) -> Unit,
    onCheck: () -> Unit,
    onUndo: () -> Unit,
    onSkip: (Boolean) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
) {
    val habit = history.habit
    val swatch = HabitColor.fromId(habit.color).tint
    val color = habitInk(swatch)
    val colors = MaterialTheme.colorScheme
    val chartColor = colors.habitGraphColor(swatch)
    val checkShape = MaterialTheme.shapes.small
    val schedule = history.scheduleAt(now)
    val progress = remember(history, now) { history.weeklyProgress(now, now) }
    val streak = remember(history, now) { history.streaks(now) }
    val state = history.dayState(now, now)
    val pause = history.pauseOn(now)
    val amount = history.entry(now)?.amount ?: 0.0
    val done = state == DayState.DONE
    val canCheck = state == DayState.PENDING || state == DayState.PARTIAL
    val numeric = schedule?.amountGoal != null
    var menu by rememberSaveable { mutableStateOf(false) }
    val interval = if (streak.weekly) "wk" else "d"

    Panel {
        Column(Modifier.padding(start = 15.dp, end = 15.dp, top = 13.dp, bottom = 13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).heightIn(min = 49.dp).clip(RoundedCornerShape(8.dp))
                        .clickable(onClickLabel = "Open habit", onClick = onDetails),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HabitGlyph(habit.icon, swatch)
                    Column(Modifier.weight(1f)) {
                        Text(habit.name, style = MaterialTheme.typography.titleMedium,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            when {
                                pause != null -> "Paused through " + dayLabel(pause.lastDay, "MMM d")
                                state == DayState.SKIPPED -> "Skipped today"
                                progress != null && progress.target > 0 ->
                                    "${minOf(progress.daysDone, progress.target)} of ${progress.target} this week"
                                else -> scheduleLabel(schedule)
                            },
                            color = if (progress?.met == true) color else Ink.muted,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(Icons.Outlined.ChevronRight, null, Modifier.size(15.dp), tint = Ink.dim)
                }
                Box(Modifier.padding(start = 10.dp)) {
                    val description = when {
                        done -> "${habit.name} checked in. Open today’s entry."
                        canCheck && numeric -> "Add ${amountLabel(schedule?.quickAmount ?: 1.0)} ${schedule?.unit.orEmpty()}"
                        canCheck -> "Check in for ${habit.name}"
                        else -> "${habit.name}. ${state.label}. Open today’s entry."
                    }
                    Box(
                        Modifier.size(48.dp).clip(checkShape)
                            .background(if (done) lerp(Ink.panel, swatch, 0.18f) else Ink.panel)
                            .border(1.dp, if (done) lerp(Ink.border, color, 0.65f) else Ink.border, checkShape)
                            .semantics { contentDescription = description }
                            .combinedClickable(
                                role = Role.Button,
                                onClick = { if (canCheck) onCheck() else onEntry(now) },
                                onLongClick = { if (done) onUndo() else menu = true },
                                onLongClickLabel = if (done) "Undo check-in" else "Check-in options",
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            canCheck && numeric -> Text(
                                "+${amountLabel(schedule?.quickAmount ?: 1.0)}",
                                Modifier.padding(horizontal = 4.dp), color = color,
                                style = MaterialTheme.typography.labelSmall, maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            canCheck || done -> Icon(
                                Icons.Outlined.Check, null, Modifier.size(22.dp),
                                tint = color,
                            )
                            else -> Icon(Icons.Outlined.MoreHoriz, null, Modifier.size(20.dp), tint = Ink.dim)
                        }
                    }
                    DropdownMenu(menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Edit today’s entry") }, onClick = {
                            menu = false
                            onEntry(now)
                        })
                        if (pause == null && history.isScheduled(now)) {
                            val skipped = state == DayState.SKIPPED
                            DropdownMenuItem(text = { Text(if (skipped) "Undo skip" else "Skip today") }, onClick = {
                                menu = false
                                onSkip(!skipped)
                            })
                        }
                        DropdownMenuItem(text = { Text(if (pause == null) "Pause habit" else "Resume habit") }, onClick = {
                            menu = false
                            if (pause == null) onPause() else onResume()
                        })
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SmallMetric(Icons.Outlined.LocalFireDepartment, "${streak.current} $interval streak", color)
                SmallMetric(Icons.Outlined.EmojiEvents, "${streak.best} best", Ink.muted)
                if (numeric) {
                    val units = schedule?.unit.orEmpty().let { if (it.isEmpty()) "" else " $it" }
                    Text(
                        "${amountLabel(amount)} / ${amountLabel(requireNotNull(schedule?.amountGoal))}$units",
                        Modifier.weight(1f), color = color, style = MaterialTheme.typography.labelSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                } else if (history.entry(now)?.note?.isNotBlank() == true) {
                    SmallMetric(Icons.AutoMirrored.Outlined.StickyNote2, "Note")
                }
            }
            Heatmap(now, { day -> habitCell(history.dayState(day, now), chartColor, colors) }, { day ->
                if (history.isActive(day)) onEntry(day)
            }, compact = true)
        }
    }
}

internal fun scheduleLabel(schedule: Schedule?): String {
    if (schedule == null) return "Upcoming"
    val target = schedule.weeklyTarget
    if (target != null) return "$target days a week"
    if (schedule.weekdays == 127) return "Daily"
    return DayOfWeek.entries.filter { day -> schedule.weekdays and (1 shl (day.value - 1)) != 0 }
        .joinToString { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
}

internal fun periodLabel(first: Long, last: Long): String =
    dayLabel(first, "MMM d") + " – " + dayLabel(last, "MMM d")
