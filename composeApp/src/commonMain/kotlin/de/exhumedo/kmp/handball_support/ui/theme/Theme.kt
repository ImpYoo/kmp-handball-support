package de.exhumedo.kmp.handball_support.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DhbLightColors = lightColorScheme(
    primary = DhbBlack,
    onPrimary = DhbWhite,
    primaryContainer = DhbSurfaceVariant,
    onPrimaryContainer = DhbBlack,
    secondary = DhbBlack,
    onSecondary = DhbWhite,
    secondaryContainer = DhbSurfaceVariant,
    onSecondaryContainer = DhbBlack,
    tertiary = DhbBlack,
    onTertiary = DhbWhite,
    background = DhbWhite,
    onBackground = DhbBlack,
    surface = DhbWhite,
    onSurface = DhbBlack,
    surfaceVariant = DhbSurfaceVariant,
    onSurfaceVariant = DhbOnSurfaceVariant,
    outline = DhbOutline,
    outlineVariant = DhbOutlineVariant,
    error = DhbRedDark,
    onError = DhbWhite,
)

private val DhbDarkColors = darkColorScheme(
    primary = DhbRed,
    onPrimary = DhbWhite,
    primaryContainer = DhbRedDark,
    onPrimaryContainer = DhbWhite,
    secondary = DhbDarkOnSurfaceVariant,
    onSecondary = DhbAnthracite,
    secondaryContainer = DhbDarkSurfaceVariant,
    onSecondaryContainer = DhbDarkOnSurface,
    tertiary = DhbDarkOnSurfaceVariant,
    onTertiary = DhbAnthracite,
    background = DhbDarkBackground,
    onBackground = DhbDarkOnSurface,
    surface = DhbDarkSurface,
    onSurface = DhbDarkOnSurface,
    surfaceVariant = DhbDarkSurfaceVariant,
    onSurfaceVariant = DhbDarkOnSurfaceVariant,
    outline = DhbDarkOutline,
    outlineVariant = DhbDarkSurfaceVariant,
    error = DhbRed,
    onError = DhbWhite,
)

/**
 * Typography tuned for a sporty, editorial feel like dhb.de: bold, tight
 * headlines and clear, legible body text, set in the DHB brand font
 * ("DIN Next LT Pro" with sans-serif fallback — see [dhbFontFamily]).
 */
@Composable
private fun dhbTypography(): Typography {
    val family = dhbFontFamily()
    return Typography().run {
        copy(
            displayLarge = displayLarge.copy(fontFamily = family),
            displayMedium = displayMedium.copy(fontFamily = family),
            displaySmall = displaySmall.copy(fontFamily = family),
            headlineLarge = headlineLarge.copy(fontFamily = family, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
            headlineMedium = headlineMedium.copy(fontFamily = family, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.25).sp),
            headlineSmall = headlineSmall.copy(fontFamily = family, fontWeight = FontWeight.Bold),
            titleLarge = titleLarge.copy(fontFamily = family, fontWeight = FontWeight.Bold),
            titleMedium = titleMedium.copy(fontFamily = family, fontWeight = FontWeight.SemiBold),
            titleSmall = titleSmall.copy(fontFamily = family),
            bodyLarge = bodyLarge.copy(fontFamily = family),
            bodyMedium = bodyMedium.copy(fontFamily = family),
            bodySmall = bodySmall.copy(fontFamily = family),
            labelLarge = labelLarge.copy(fontFamily = family, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
            labelMedium = labelMedium.copy(fontFamily = family),
            labelSmall = labelSmall.copy(fontFamily = family),
        )
    }
}

/** Squared-off, modern shapes matching the DHB tile/card aesthetic. */
private val DhbShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

/**
 * App-wide theme styled after dhb.de. Wrap every screen/preview in this instead
 * of using a bare [MaterialTheme] so brand colors, typography and shapes apply
 * consistently.
 *
 * dhb.de is a light, high-contrast white site, so the theme defaults to the
 * light color scheme and does NOT follow the system dark mode. Pass
 * `useDarkTheme = true` explicitly only if a dark variant is ever needed.
 */
@Composable
fun AppTheme(
    useDarkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (useDarkTheme) DhbDarkColors else DhbLightColors,
        typography = dhbTypography(),
        shapes = DhbShapes,
        content = content,
    )
}




