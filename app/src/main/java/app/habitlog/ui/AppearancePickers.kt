package app.habitlog.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.habitlog.model.HabitColor
import app.habitlog.model.HabitIcon
import app.habitlog.model.IconGroup

@Composable
internal fun HabitAppearanceControls(
    color: HabitColor,
    icon: HabitIcon,
    onColor: (HabitColor) -> Unit,
    onPickIcon: () -> Unit,
) {
    var choosingColor by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        AppearanceOption("Color", color.label, Modifier.weight(1f), {
            focus.clearFocus(force = true)
            keyboard?.hide()
            choosingColor = true
        }) {
            Box(Modifier.size(22.dp).background(color.tint, CircleShape))
        }
        AppearanceOption("Icon", icon.label, Modifier.weight(1f), {
            focus.clearFocus(force = true)
            keyboard?.hide()
            onPickIcon()
        }) {
            Icon(icon.glyph, null, Modifier.size(22.dp), tint = habitInk(color.tint))
        }
    }
    if (choosingColor) {
        ColorPicker(color, { choosingColor = false }) {
            onColor(it)
            choosingColor = false
        }
    }
}

@Composable
private fun AppearanceOption(
    title: String,
    value: String,
    modifier: Modifier,
    onClick: () -> Unit,
    indicator: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = Ink.recessed,
        border = BorderStroke(1.dp, Ink.border),
    ) {
        Row(
            Modifier.heightIn(min = 58.dp).padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            indicator()
            Column(Modifier.weight(1f)) {
                Text(title, color = Ink.muted, style = MaterialTheme.typography.labelSmall)
                Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Outlined.ChevronRight, null, Modifier.size(15.dp), tint = Ink.dim)
        }
    }
}

@Composable
private fun ColorPicker(selected: HabitColor, onClose: () -> Unit, onColor: (HabitColor) -> Unit) {
    PickerDialog("Habit color", onClose) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(48.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 310.dp).selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(HabitColor.entries, key = HabitColor::id) { color ->
                val active = selected == color
                ChoiceTile(color.label, active, color.tint, { onColor(color) }) {
                    Box(Modifier.size(28.dp).background(color.tint, CircleShape), contentAlignment = Alignment.Center) {
                        if (active) Icon(Icons.Outlined.Check, null, Modifier.size(17.dp), tint = Color(0xFF171717))
                    }
                }
            }
        }
    }
}

@Composable
internal fun IconPicker(selected: HabitIcon, tint: Color, onClose: () -> Unit, onIcon: (HabitIcon) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val groups = remember(query) {
        val term = query.trim()
        HabitIcon.entries.filter { option ->
            option.label.contains(term, ignoreCase = true) || option.group.label.contains(term, ignoreCase = true)
        }.groupBy(HabitIcon::group)
    }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    fun leave() {
        focus.clearFocus(force = true)
        keyboard?.hide()
        onClose()
    }
    BackHandler(onBack = ::leave)
    Column(
        Modifier.fillMaxWidth().padding(start = 21.dp, end = 21.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        PickerHeader("Habit icon", ::leave)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search icons") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            singleLine = true,
        )
        if (groups.isEmpty()) {
            Text("No icons match your search", Modifier.padding(vertical = 14.dp), color = Ink.muted)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(48.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 325.dp).selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                IconGroup.entries.forEach { group ->
                    val choices = groups[group] ?: return@forEach
                    item(key = group.name, span = { GridItemSpan(maxLineSpan) }) {
                        Text(group.label, Modifier.padding(top = 5.dp, bottom = 2.dp),
                            color = Ink.muted, style = MaterialTheme.typography.labelLarge)
                    }
                    items(choices, key = HabitIcon::id) { icon ->
                        ChoiceTile(icon.label, selected == icon, tint, onClick = {
                            onIcon(icon)
                            leave()
                        }) {
                            Icon(icon.glyph, null, Modifier.size(23.dp), tint = if (selected == icon) tint else Ink.text)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceTile(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val shape = MaterialTheme.shapes.small
    Box(
        Modifier.fillMaxWidth().height(48.dp).clip(shape)
            .background(if (selected) accent.copy(alpha = 0.12f) else Ink.recessed)
            .border(1.dp, if (selected) accent else Ink.border, shape)
            .semantics { contentDescription = label }
            .selectable(selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
internal fun PickerDialog(title: String, onClose: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.padding(horizontal = 16.dp).widthIn(max = 440.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = Ink.panel,
            border = BorderStroke(1.dp, Ink.border),
        ) {
            Column(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                PickerHeader(title, onClose)
                content()
            }
        }
    }
}

@Composable
private fun PickerHeader(title: String, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        IconButton(onClick = onClose) {
            Icon(Icons.Outlined.Close, "Close", tint = Ink.muted)
        }
    }
}
