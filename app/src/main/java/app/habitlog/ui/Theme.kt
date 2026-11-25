package app.habitlog.ui

import android.content.Context
import android.view.ContextThemeWrapper
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.habitlog.R
import app.habitlog.model.AppAccent
import app.habitlog.model.appearanceContext
import app.habitlog.model.withAppearance

internal fun Context.pickerTheme(dark: Boolean, accent: AppAccent): Context {
    val base = if (dark) R.style.Theme_HabitLog_Dark else R.style.Theme_HabitLog_Light
    val configuration = resources.configuration.withAppearance(dark)
    return ContextThemeWrapper(this, base).apply {
        applyOverrideConfiguration(configuration)
        theme.applyStyle(accent.pickerStyle, true)
    }
}

internal fun pivotScheme(context: Context, dark: Boolean, accent: AppAccent): ColorScheme {
    val colors = context.appearanceContext(dark)
    fun color(id: Int) = Color(colors.getColor(id))
    val background = color(R.color.pivot_background)
    val surface = color(R.color.pivot_surface)
    val field = color(R.color.pivot_field)
    val raised = color(R.color.pivot_raised)
    val text = color(R.color.pivot_text)
    val muted = color(R.color.pivot_muted)
    val primary = color(accent.colorRes)
    val onPrimary = if (dark) Color(0xFF151515) else Color.White
    val container = lerp(surface, primary, if (dark) 0.17f else 0.10f)
    val onContainer = lerp(text, primary, 0.27f)
    val error = color(R.color.pivot_error)
    val base = if (dark) darkColorScheme() else lightColorScheme()

    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        inversePrimary = Color(context.appearanceContext(!dark).getColor(accent.colorRes)),
        secondary = primary,
        onSecondary = onPrimary,
        secondaryContainer = container,
        onSecondaryContainer = onContainer,
        tertiary = muted,
        onTertiary = onPrimary,
        tertiaryContainer = field,
        onTertiaryContainer = text,
        background = background,
        onBackground = text,
        surface = surface,
        onSurface = text,
        surfaceVariant = field,
        onSurfaceVariant = muted,
        surfaceTint = Color.Transparent,
        inverseSurface = if (dark) Color(0xFFF0F0F0) else Color(0xFF292929),
        inverseOnSurface = if (dark) Color(0xFF292929) else Color(0xFFF0F0F0),
        error = error,
        onError = onPrimary,
        errorContainer = lerp(surface, error, 0.15f),
        onErrorContainer = text,
        outline = color(R.color.pivot_outline),
        outlineVariant = color(R.color.pivot_divider),
        scrim = Color.Black,
        surfaceBright = if (dark) raised else surface,
        surfaceDim = background,
        surfaceContainerLowest = if (dark) background else surface,
        surfaceContainerLow = if (dark) surface else background,
        surfaceContainer = surface,
        surfaceContainerHigh = field,
        surfaceContainerHighest = raised,
    )
}

internal object Ink {
    val background: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.background
    val panel: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surface
    val recessed: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceVariant
    val border: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outlineVariant
    val text: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface
    val muted: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val dim: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outline
    val danger: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.error
    val primary: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
    val graph: Color @Composable @ReadOnlyComposable get() = lerp(background, primary, 0.87f)
    val selectedSurface: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primaryContainer
    val selectedBorder: Color @Composable @ReadOnlyComposable get() = lerp(border, primary, 0.58f)
}

@Composable
internal fun habitInk(swatch: Color): Color {
    val colors = MaterialTheme.colorScheme
    return if (colors.background.luminance() < 0.5f) swatch else lerp(swatch, colors.onSurface, 0.55f)
}

private fun type(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = size.sp,
    lineHeight = height.sp,
    fontWeight = weight,
)

private val pivotTypography = Typography(
    headlineMedium = type(27, 33, FontWeight.SemiBold).copy(letterSpacing = (-0.4).sp),
    headlineSmall = type(22, 29, FontWeight.SemiBold),
    titleLarge = type(20, 26, FontWeight.SemiBold),
    titleMedium = type(16, 22, FontWeight.SemiBold),
    titleSmall = type(14, 20, FontWeight.SemiBold),
    bodyLarge = type(16, 23),
    bodyMedium = type(14, 20),
    bodySmall = type(12, 17),
    labelLarge = type(14, 19, FontWeight.Medium),
    labelMedium = type(12, 16, FontWeight.Medium),
    labelSmall = type(11, 15, FontWeight.Medium),
)

private val pivotShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(18.dp),
)

@Composable
internal fun PivotTheme(dark: Boolean, accent: AppAccent, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scheme = remember(context, dark, accent) { pivotScheme(context, dark, accent) }
    MaterialTheme(colorScheme = scheme, typography = pivotTypography, shapes = pivotShapes, content = content)
}
