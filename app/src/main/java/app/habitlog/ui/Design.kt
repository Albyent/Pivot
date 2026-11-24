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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object Ink {
    val background = Color(0xFF101520)
    val panel = Color(0xFF1A2433)
    val recessed = Color(0xFF141D2C)
    val border = Color(0xFF2D394D)
    val text = Color(0xFFF0F3FA)
    val muted = Color(0xFFADB9CF)
    val dim = Color(0xFF8594AE)
    val primary = Color(0xFFBAB0FF)
    val danger = Color(0xFFFF9B9E)
    val graph = Color(0xFF9587FC)
}

internal val habitColors = listOf(
    Color(0xFFAA9BFC), Color(0xFFF29498), Color(0xFF6DCAA8), Color(0xFF77B7FA),
    Color(0xFFF3C277), Color(0xFFF29ECF), Color(0xFF65C9DA), Color(0xFFB7CD79),
)

internal val habitIcons = listOf(
    Icons.Outlined.TrackChanges, Icons.Outlined.FitnessCenter, Icons.AutoMirrored.Outlined.MenuBook,
    Icons.Outlined.WaterDrop, Icons.Outlined.SelfImprovement, Icons.Outlined.Bedtime,
    Icons.AutoMirrored.Outlined.DirectionsWalk, Icons.Outlined.Timer,
)
internal val iconNames = listOf("Target", "Workout", "Reading", "Water", "Mindfulness", "Sleep", "Walking", "Timer")
internal val colorNames = listOf("Lilac", "Coral", "Mint", "Blue", "Amber", "Rose", "Aqua", "Olive")

private fun type(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontSize = size.sp, lineHeight = height.sp, fontWeight = weight)

@Composable
internal fun PivotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Ink.primary,
            onPrimary = Color(0xFF20164D),
            primaryContainer = Color(0xFF353057),
            onPrimaryContainer = Color(0xFFE5DEFF),
            secondary = Ink.muted,
            background = Ink.background,
            onBackground = Ink.text,
            surface = Ink.panel,
            onSurface = Ink.text,
            surfaceVariant = Ink.recessed,
            onSurfaceVariant = Ink.muted,
            surfaceContainer = Ink.panel,
            surfaceContainerHigh = Color(0xFF202B3E),
            outline = Ink.dim,
            outlineVariant = Ink.border,
            error = Ink.danger,
        ),
        typography = Typography(
            headlineMedium = type(29, 35, FontWeight.SemiBold),
            headlineSmall = type(24, 30, FontWeight.SemiBold),
            titleLarge = type(20, 26, FontWeight.SemiBold),
            titleMedium = type(17, 23, FontWeight.SemiBold),
            titleSmall = type(14, 19, FontWeight.SemiBold),
            bodyLarge = type(16, 23),
            bodyMedium = type(14, 20),
            bodySmall = type(12, 17),
            labelLarge = type(14, 19, FontWeight.Medium),
            labelMedium = type(12, 16, FontWeight.Medium),
            labelSmall = type(11, 15, FontWeight.Medium),
        ),
        content = content,
    )
}

@Composable
internal fun Panel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(21.dp),
        color = Ink.panel,
        border = BorderStroke(1.dp, Ink.border),
        content = content,
    )
}

@Composable
internal fun SectionTitle(
    title: String,
    icon: ImageVector,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(icon, null, Modifier.size(19.dp), tint = Ink.primary)
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        trailing()
    }
}

@Composable
internal fun HabitGlyph(icon: Int, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.size(40.dp).background(color.copy(alpha = 0.13f), RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(habitIcons.getOrElse(icon) { habitIcons.first() }, null, Modifier.size(22.dp), tint = color)
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
        shape = CircleShape,
        color = if (active) Color(0xFF37305A) else Ink.recessed,
        border = BorderStroke(1.dp, if (active) Color(0xFF50457A) else Ink.border),
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
internal fun CircleAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        color = Ink.panel,
        border = BorderStroke(1.dp, Ink.border),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, label, Modifier.size(20.dp), tint = Ink.muted)
        }
    }
}
