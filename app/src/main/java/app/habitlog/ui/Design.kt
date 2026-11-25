package app.habitlog.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.habitlog.model.HabitIcon

@Composable
internal fun Panel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = Ink.panel,
        border = BorderStroke(1.dp, Ink.border),
        content = content,
    )
}

@Composable
internal fun SectionTitle(
    title: String,
    icon: ImageVector? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(18.dp), tint = Ink.muted)
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        trailing()
    }
}

@Composable
internal fun HabitGlyph(icon: Int, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.size(36.dp).background(color.copy(alpha = 0.15f), MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        Icon(HabitIcon.fromId(icon).glyph, null, Modifier.size(21.dp), tint = habitInk(color))
    }
}

@Composable
internal fun ActionPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    active: Boolean = false,
) {
    Surface(
        modifier = modifier.heightIn(min = 36.dp),
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = if (active) Ink.selectedSurface else Ink.panel,
        border = BorderStroke(1.dp, if (active) Ink.selectedBorder else Ink.border),
        contentColor = if (active) Ink.primary else Ink.muted,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (icon != null) Icon(icon, null, Modifier.size(15.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun ValueLabel(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, color = Ink.muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun SmallMetric(icon: ImageVector, text: String, color: Color = Ink.muted) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(icon, null, Modifier.size(15.dp), tint = color)
        Text(text, color = Ink.muted, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
internal fun ToolbarAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(42.dp),
        shape = MaterialTheme.shapes.small,
        color = Ink.panel,
        border = BorderStroke(1.dp, Ink.border),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, label, Modifier.size(20.dp), tint = Ink.muted)
        }
    }
}
