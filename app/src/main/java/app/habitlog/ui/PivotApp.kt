package app.habitlog.ui

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.ContextThemeWrapper
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.habitlog.LogViewModel
import app.habitlog.model.DayState
import app.habitlog.model.HabitColor
import app.habitlog.model.HabitHistory
import app.habitlog.model.MIN_DAY
import app.habitlog.model.asDate
import app.habitlog.model.dayLabel
import app.habitlog.system.HabitWidget
import kotlinx.coroutines.flow.collectLatest

private enum class Tab(val label: String, val icon: ImageVector) {
    Dashboard("Dashboard", Icons.Outlined.GridView),
    Insights("Insights", Icons.Outlined.BarChart),
    Tasks("Tasks", Icons.Outlined.Checklist),
}

private sealed interface Sheet {
    data class HabitForm(val id: Long?) : Sheet
    data class TaskForm(val id: Long?) : Sheet
    data class Entry(val id: Long, val day: Long) : Sheet
    data class Activity(val day: Long) : Sheet
}

private sealed interface Confirm {
    data class Archive(val id: Long, val name: String) : Confirm
    data class DeleteTask(val id: Long, val name: String) : Confirm
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PivotApp(model: LogViewModel) {
    val snapshot by model.state.collectAsStateWithLifecycle()
    val now by model.now.collectAsStateWithLifecycle()
    val requested by model.requestedHabit.collectAsStateWithLifecycle()
    val pendingBackup by model.pendingBackup.collectAsStateWithLifecycle()
    val accent by model.accent.collectAsStateWithLifecycle()
    val data = snapshot
    val context = LocalContext.current
    val pickerContext = remember(context, accent) { ContextThemeWrapper(context, accent.pickerStyle) }
    var tab by rememberSaveable { mutableStateOf(Tab.Dashboard) }
    var selectedHabit by rememberSaveable { mutableStateOf<Long?>(null) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var showingThemePicker by rememberSaveable { mutableStateOf(false) }
    var todayOnly by rememberSaveable { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var confirm by remember { mutableStateOf<Confirm?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val dashboardScroll = rememberLazyListState()
    val detail = data?.histories?.firstOrNull { it.habit.id == selectedHabit }

    fun openEntry(id: Long, day: Long) { sheet = Sheet.Entry(id, day) }
    fun pauseHabit(id: Long) {
        chooseDate(pickerContext, now + 7, now, now + 3650) { last ->
            model.write("Habit paused") { pause(id, last) }
        }
    }
    fun setSkip(id: Long, skip: Boolean) = model.write(if (skip) "Skipped for today" else "Skip cleared") {
        skipToday(id, skip)
    }
    fun resumeHabit(id: Long) = model.write("Habit resumed") { resume(id) }
    fun goBack() {
        when {
            selectedHabit != null -> selectedHabit = null
            settings -> settings = false
            else -> tab = Tab.Dashboard
        }
    }

    BackHandler(selectedHabit != null || settings || tab != Tab.Dashboard) { goBack() }
    LaunchedEffect(requested, data != null) {
        if (requested != null && data != null) {
            selectedHabit = data.histories.firstOrNull { it.habit.id == requested }?.habit?.id
            model.requestedHabit.value = null
        }
    }
    LaunchedEffect(model) {
        model.messages.collectLatest { feedback ->
            val result = snackbar.showSnackbar(
                feedback.text,
                actionLabel = if (feedback.habitId != null) "Add note" else null,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed && feedback.habitId != null && feedback.day != null) {
                openEntry(feedback.habitId, feedback.day)
            }
        }
    }

    val exportFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(model::export)
    }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(model::previewBackup)
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) model.refreshDevice() else model.message("Enable notifications in Android settings for reminders.")
    }

    PivotTheme(accent) {
        Scaffold(
            containerColor = Ink.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                val title = detail?.habit?.name ?: when {
                    settings -> "Settings"
                    tab == Tab.Dashboard -> "Pivot"
                    else -> tab.label
                }
                AppHeader(
                    title = title,
                    subtitle = if (detail == null && !settings && tab == Tab.Dashboard) dayLabel(now, "EEEE, MMM d") else null,
                    onBack = if (selectedHabit != null || settings) ({ goBack() }) else null,
                ) {
                    if (detail != null && detail.habit.archivedDay == null) {
                        val id = detail.habit.id
                        val active = data.histories.filter { it.habit.archivedDay == null }
                        val position = active.indexOf(detail)
                        HabitMenu(
                            detail, now, position > 0, position < active.lastIndex,
                            onEdit = { sheet = Sheet.HabitForm(id) },
                            onPause = { pauseHabit(id) },
                            onResume = { resumeHabit(id) },
                            onSkip = { setSkip(id, it) },
                            onMove = { delta -> model.write { moveHabit(id, delta) } },
                            onArchive = { confirm = Confirm.Archive(id, detail.habit.name) },
                        )
                    } else if (detail == null && !settings) {
                        CircleAction(Icons.Outlined.Settings, "Settings", { settings = true })
                    }
                }
            },
            bottomBar = {
                if (selectedHabit != null || settings) {
                    Spacer(Modifier.navigationBarsPadding())
                } else {
                    BottomNavigation(tab) { tab = it }
                }
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = 620.dp).fillMaxSize()) {
                    when {
                        data == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                        detail != null -> {
                            val id = detail.habit.id
                            val archived = detail.habit.archivedDay
                            val pause = detail.pauseOn(now)
                            val contextLine = when {
                                archived != null -> "Archived " + dayLabel(archived)
                                pause != null -> "Paused through " + dayLabel(pause.lastDay)
                                else -> scheduleLabel(detail.scheduleAt(now))
                            }
                            HabitDetails(detail, now, contextLine, { openEntry(id, it) }, {
                                val latest = minOf(now, archived ?: now)
                                if (latest >= detail.habit.startDay) {
                                    chooseDate(pickerContext, latest, detail.habit.startDay, latest) { openEntry(id, it) }
                                }
                            })
                        }
                        settings -> SettingsScreen(
                            data,
                            accent.label,
                            onTheme = { showingThemePicker = true },
                            onExport = { exportFile.launch("pivot-backup-${now.asDate()}.json") },
                            onRestore = { importFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                            onWidget = {
                                val manager = AppWidgetManager.getInstance(context)
                                if (manager.isRequestPinAppWidgetSupported) {
                                    manager.requestPinAppWidget(ComponentName(context, HabitWidget::class.java), null, null)
                                } else {
                                    model.message("Touch and hold your home screen, then choose Widgets → Pivot.")
                                }
                            },
                            onNotifications = {
                                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                            },
                            onArchived = { selectedHabit = it },
                        )
                        tab == Tab.Insights -> Insights(data, now, ::openEntry)
                        tab == Tab.Tasks -> TasksScreen(
                            data.tasks, now, { sheet = Sheet.TaskForm(null) }, { sheet = Sheet.TaskForm(it) },
                            { id -> model.write { toggleTask(id) } },
                        )
                        else -> Dashboard(
                            data, now, dashboardScroll, todayOnly,
                            { todayOnly = !todayOnly }, { sheet = Sheet.HabitForm(null) }, { selectedHabit = it },
                            { sheet = Sheet.Activity(it) }, ::openEntry,
                            { history -> model.checkIn(history.habit.id, history.habit.name, now) },
                            { id -> model.write("Check-in cleared") { undoCheckIn(id, now) } },
                            { id, skip -> setSkip(id, skip) },
                            ::pauseHabit, { resumeHabit(it) },
                        )
                    }
                }
            }
        }

        val openSheet = sheet
        if (openSheet != null && data != null) {
            key(openSheet) {
                ModalBottomSheet(
                    onDismissRequest = { sheet = null },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = Ink.panel,
                    scrimColor = Color(0xCC080D17),
                ) {
                    when (openSheet) {
                        is Sheet.HabitForm -> {
                            val history = data.histories.find { it.habit.id == openSheet.id }
                            val palette = HabitColor.suggestedOrder
                            val suggestedColor = palette[data.habits.size % palette.size].id
                            HabitEditor(history, now, suggestedColor, pickerContext) { draft ->
                                model.write { saveHabit(openSheet.id, draft) }
                                sheet = null
                                if (draft.reminderMinute != null && Build.VERSION.SDK_INT >= 33 &&
                                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        }
                        is Sheet.TaskForm -> {
                            val task = data.tasks.find { it.id == openSheet.id }
                            TaskEditor(task, now, pickerContext, onSave = { name, date ->
                                model.write { saveTask(openSheet.id, name, date) }
                                sheet = null
                            }, onDelete = {
                                if (task != null) confirm = Confirm.DeleteTask(task.id, task.name)
                            })
                        }
                        is Sheet.Entry -> {
                            val history = data.histories.find { it.habit.id == openSheet.id }
                            if (history != null && history.isActive(openSheet.day)) {
                                EntryEditor(history, openSheet.day, now) { amount, skipped, note ->
                                    model.write { saveCheckIn(openSheet.id, openSheet.day, amount, skipped, note) }
                                    sheet = null
                                }
                            } else {
                                EditorContent("Entry unavailable") { Text("Choose a date within this habit’s history.") }
                            }
                        }
                        is Sheet.Activity -> DayOverview(data, openSheet.day, now, onPick = {
                            chooseDate(pickerContext, openSheet.day, MIN_DAY, now) { sheet = Sheet.Activity(it) }
                        }) { id -> openEntry(id, openSheet.day) }
                    }
                }
            }
        }

        if (showingThemePicker) ThemePicker(accent, model::selectAccent) { showingThemePicker = false }

        confirm?.let { action ->
            val title: String
            val message: String
            val button: String
            when (action) {
                is Confirm.Archive -> {
                    title = "Archive habit?"
                    message = "${action.name} will move to Archived habits in Settings."
                    button = "Archive"
                }
                is Confirm.DeleteTask -> {
                    title = "Delete task?"
                    message = "Remove ${action.name} from Tasks."
                    button = "Delete"
                }
            }
            ConfirmDialog(title, message, button, onDismiss = { confirm = null }) {
                confirm = null
                when (action) {
                    is Confirm.Archive -> {
                        model.write("Habit archived") { archiveHabit(action.id) }
                        selectedHabit = null
                    }
                    is Confirm.DeleteTask -> {
                        model.write("Task deleted") { deleteTask(action.id) }
                        sheet = null
                    }
                }
            }
        }
        pendingBackup?.let { backup ->
            ConfirmDialog(
                "Restore this backup?",
                "This replaces your current data with ${backup.habits.size} habits and ${backup.tasks.size} tasks from the file.",
                "Restore",
                onDismiss = { model.pendingBackup.value = null },
            ) {
                selectedHabit = null
                model.restore()
            }
        }
    }
}

@Composable
private fun AppHeader(
    title: String,
    subtitle: String?,
    onBack: (() -> Unit)?,
    trailing: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxWidth().background(Ink.background).statusBarsPadding(), contentAlignment = Alignment.Center) {
        Row(
            Modifier.widthIn(max = 620.dp).fillMaxWidth().heightIn(min = 66.dp)
                .padding(start = 18.dp, end = 18.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (onBack != null) CircleAction(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) Text(subtitle, color = Ink.muted, style = MaterialTheme.typography.bodySmall)
            }
            trailing()
        }
    }
}

@Composable
private fun BottomNavigation(current: Tab, onTab: (Tab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color(0xFF151D2B))) {
        HorizontalDivider(color = Ink.border)
        Box(Modifier.fillMaxWidth().navigationBarsPadding(), contentAlignment = Alignment.Center) {
            Row(Modifier.widthIn(max = 620.dp).fillMaxWidth().selectableGroup().padding(horizontal = 12.dp, vertical = 5.dp)) {
                Tab.entries.forEach { tab ->
                    val active = current == tab
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .selectable(active, role = Role.Tab, onClick = { onTab(tab) })
                            .heightIn(min = 52.dp).padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(Modifier.size(42.dp, 26.dp).background(
                            if (active) Ink.selectedSurface else Color.Transparent, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center) {
                            Icon(tab.icon, null, Modifier.size(19.dp), tint = if (active) Ink.primary else Ink.muted)
                        }
                        Text(tab.label, color = if (active) Ink.primary else Ink.muted, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitMenu(
    history: HabitHistory,
    now: Long,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: (Boolean) -> Unit,
    onMove: (Int) -> Unit,
    onArchive: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val paused = history.pauseOn(now) != null
    val state = history.dayState(now, now)
    Box {
        CircleAction(Icons.Outlined.MoreVert, "Habit options", { expanded = true })
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Edit habit") }, onClick = { expanded = false; onEdit() })
            if (!paused && history.isScheduled(now) && state != DayState.DONE) {
                val skipped = state == DayState.SKIPPED
                DropdownMenuItem(text = { Text(if (skipped) "Undo skip" else "Skip today") },
                    onClick = { expanded = false; onSkip(!skipped) })
            }
            DropdownMenuItem(text = { Text(if (paused) "Resume habit" else "Pause habit") }, onClick = {
                expanded = false
                if (paused) onResume() else onPause()
            })
            if (canMoveUp) DropdownMenuItem(text = { Text("Move up") }, onClick = { expanded = false; onMove(-1) })
            if (canMoveDown) DropdownMenuItem(text = { Text("Move down") }, onClick = { expanded = false; onMove(1) })
            DropdownMenuItem(text = { Text("Archive habit") }, onClick = { expanded = false; onArchive() })
        }
    }
}

@Composable
private fun ConfirmDialog(title: String, message: String, button: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = { TextButton(onClick = onConfirm) { Text(button) } },
    )
}
