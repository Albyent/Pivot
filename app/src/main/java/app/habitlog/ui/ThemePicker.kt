package app.habitlog.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.habitlog.model.AppAccent
import app.habitlog.model.AppAppearance
import app.habitlog.model.ThemeMode
import app.habitlog.model.appearanceContext

@Composable
internal fun ThemePicker(
    appearance: AppAppearance,
    dark: Boolean,
    onMode: (ThemeMode) -> Unit,
    onAccent: (AppAccent) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val colors = remember(context, dark) { context.appearanceContext(dark) }
    val lightPreview = remember(context, appearance.accent) { pivotScheme(context, false, appearance.accent) }
    val darkPreview = remember(context, appearance.accent) { pivotScheme(context, true, appearance.accent) }
    PickerDialog("App theme", onClose) {
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            ThemeMode.entries.forEach { mode ->
                val selected = mode == appearance.mode
                val shape = MaterialTheme.shapes.small
                Surface(
                    modifier = Modifier.weight(1f).clip(shape)
                        .selectable(selected, role = Role.RadioButton, onClick = { onMode(mode) }),
                    shape = shape,
                    color = if (selected) Ink.selectedSurface else Ink.panel,
                    border = BorderStroke(1.dp, if (selected) Ink.primary else Ink.border),
                ) {
                    Column(
                        Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))) {
                            if (mode != ThemeMode.DARK) ModePreview(lightPreview, Modifier.weight(1f))
                            if (mode != ThemeMode.LIGHT) ModePreview(darkPreview, Modifier.weight(1f))
                        }
                        Text(mode.label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        Text("Accent", Modifier.padding(top = 5.dp), style = MaterialTheme.typography.titleSmall)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth().heightIn(max = 241.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            items(AppAccent.entries, key = AppAccent::name) { option ->
                val selected = option == appearance.accent
                val tint = Color(colors.getColor(option.colorRes))
                val shape = MaterialTheme.shapes.small
                Surface(
                    modifier = Modifier.clip(shape)
                        .selectable(selected, role = Role.RadioButton, onClick = { onAccent(option) }),
                    shape = shape,
                    color = if (selected) Ink.selectedSurface else Ink.panel,
                    border = BorderStroke(1.dp, if (selected) Ink.primary else Ink.border),
                ) {
                    Row(
                        Modifier.heightIn(min = 48.dp).padding(horizontal = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Box(Modifier.size(15.dp).background(tint, RoundedCornerShape(3.dp)))
                        Text(option.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                        if (selected) Icon(Icons.Outlined.Check, null, Modifier.size(16.dp), tint = Ink.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModePreview(colors: ColorScheme, modifier: Modifier = Modifier) {
    Column(
        modifier.height(31.dp).background(colors.background).padding(horizontal = 5.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.fillMaxWidth(0.55f).height(2.dp).background(colors.onSurfaceVariant))
        Row(
            Modifier.fillMaxWidth().height(14.dp).background(colors.surface).padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            listOf(0.15f, 0.45f, 0.68f, 1f).forEach { intensity ->
                Box(Modifier.weight(1f).height(6.dp).background(lerp(colors.surfaceVariant, colors.primary, intensity)))
            }
        }
    }
}
