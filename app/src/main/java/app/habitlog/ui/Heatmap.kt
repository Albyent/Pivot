package app.habitlog.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.habitlog.model.DayState
import app.habitlog.model.asDate
import app.habitlog.model.dayLabel
import app.habitlog.model.weekStart
import kotlin.math.min
import kotlin.math.roundToInt

internal data class Cell(val color: Color, val skipped: Boolean = false, val outlined: Boolean = false)

internal fun ColorScheme.habitGraphColor(swatch: Color): Color =
    if (background.luminance() < 0.5f) swatch else lerp(swatch, onSurface, 0.28f)

internal fun habitCell(state: DayState, color: Color, colors: ColorScheme): Cell = when (state) {
    DayState.DONE -> Cell(lerp(colors.surface, color, 0.87f))
    DayState.PARTIAL -> Cell(lerp(colors.surfaceVariant, color, 0.47f))
    DayState.MISSED -> Cell(lerp(colors.surfaceVariant, colors.outline, 0.35f))
    DayState.PENDING -> Cell(colors.surfaceVariant, outlined = true)
    DayState.SKIPPED -> Cell(lerp(colors.surfaceVariant, colors.outline, 0.16f), skipped = true)
    DayState.UNLOGGED -> Cell(lerp(colors.surfaceVariant, colors.outline, 0.13f))
    DayState.REST -> Cell(colors.surfaceVariant)
    DayState.INACTIVE, DayState.FUTURE -> Cell(lerp(colors.surface, colors.surfaceVariant, 0.54f))
}

internal fun activityCell(count: Int, day: Long, now: Long, accent: Color, colors: ColorScheme): Cell {
    if (day > now) return Cell(lerp(colors.surface, colors.surfaceVariant, 0.54f))
    val level = when (count) {
        0 -> 0f
        1 -> 0.39f
        2 -> 0.60f
        3 -> 0.81f
        else -> 1f
    }
    return Cell(lerp(colors.surfaceVariant, accent, level), outlined = day == now)
}

@Composable
internal fun Heatmap(
    now: Long,
    cell: (Long) -> Cell,
    onDay: (Long) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onVisibleDates: ((Long, Long) -> Unit)? = null,
) {
    val outlineColor = Ink.primary.copy(alpha = 0.8f)
    val skipColor = Ink.dim
    val lastWeek = weekStart(now)
    val columns = 53
    val first = lastWeek - (columns - 1) * 7
    val scroll = rememberScrollState(Int.MAX_VALUE)
    val selectDay by rememberUpdatedState(onDay)
    val visibleDates by rememberUpdatedState(onVisibleDates)
    val marks = remember(first) {
        (0 until columns).mapNotNull { column ->
            val week = first + 7L * column
            val date = week.asDate()
            val nextMonth = date.withDayOfMonth(1).plusMonths(1)
            when {
                date.dayOfMonth == 1 -> column to dayLabel(week, "MMM")
                nextMonth.toEpochDay() < week + 7 -> column to dayLabel(nextMonth.toEpochDay(), "MMM")
                else -> null
            }
        }
    }
    val cells = (0 until columns * 7).map { cell(first + it) }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val rowPitch = if (compact) 15.dp else 17.dp
        val visibleColumns = (maxWidth / rowPitch).roundToInt().coerceIn(12, 30)
        val pitch = maxWidth / visibleColumns
        val density = LocalDensity.current
        val columnPx = with(density) { pitch.toPx() }
        val rowPx = with(density) { rowPitch.toPx() }
        val radius = with(density) { 2.dp.toPx() }
        val gap = with(density) { 3.dp.toPx() }
        val fullWidth = pitch * columns
        LaunchedEffect(first, now, maxWidth) {
            snapshotFlow { scroll.value }.collect { offset ->
                val column = (offset / columnPx).roundToInt().coerceIn(0, columns - visibleColumns)
                val left = first + 7L * column
                visibleDates?.invoke(left, min(now, left + visibleColumns * 7L - 1))
            }
        }
        Column(
            Modifier.fillMaxWidth()
                .semantics { contentDescription = "Activity heatmap. Swipe sideways for earlier weeks." }
                .horizontalScroll(scroll),
        ) {
            Box(Modifier.width(fullWidth).height(21.dp)) {
                for ((index, label) in marks) {
                    Text(
                        label,
                        Modifier.absoluteOffset(x = minOf(pitch * index + 1.dp, fullWidth - 29.dp)),
                        color = Ink.muted,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
            Canvas(
                Modifier.width(fullWidth).height(rowPitch * 7)
                    .pointerInput(first, now, columnPx, rowPx) {
                        detectTapGestures { point ->
                            val x = (point.x / columnPx).toInt()
                            val y = (point.y / rowPx).toInt()
                            val day = first + x * 7L + y
                            if (x in 0 until columns && y in 0..6 && day <= now) selectDay(day)
                        }
                    },
            ) {
                val tile = min(columnPx, rowPx) - gap
                for (column in 0 until columns) {
                    for (row in 0..6) {
                        val mark = cells[column * 7 + row]
                        val x = column * columnPx + (columnPx - tile) / 2
                        val y = row * rowPx + (rowPx - tile) / 2
                        val point = Offset(x, y)
                        val square = Size(tile, tile)
                        val corners = CornerRadius(radius)
                        drawRoundRect(mark.color, point, square, corners)
                        if (mark.outlined) {
                            drawRoundRect(outlineColor, point, square, corners, style = Stroke(1.2.dp.toPx()))
                        }
                        if (mark.skipped) {
                            drawLine(skipColor, Offset(x + tile * 0.31f, y + tile * 0.69f),
                                Offset(x + tile * 0.69f, y + tile * 0.31f), strokeWidth = 1.dp.toPx())
                        }
                    }
                }
            }
        }
    }
}
