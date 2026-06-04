package de.exhumedo.kmp.handball_support.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/**
 * DHB brand font: **"DIN Next LT Pro"** with a generic `sans-serif` fallback.
 *
 * "DIN Next LT Pro" is a commercial font and is not bundled with the project,
 * so Compose cannot render it until the font files are added. To enable it:
 *
 * 1. Drop the font files into:
 *    `composeApp/src/commonMain/composeResources/font/`
 *    e.g. `din_next_lt_pro_regular.ttf`, `din_next_lt_pro_medium.ttf`,
 *         `din_next_lt_pro_bold.ttf`.
 * 2. Replace the body of [dhbFontFamily] with:
 *    ```
 *    FontFamily(
 *        Font(Res.font.din_next_lt_pro_regular, FontWeight.Normal),
 *        Font(Res.font.din_next_lt_pro_medium, FontWeight.Medium),
 *        Font(Res.font.din_next_lt_pro_bold, FontWeight.Bold),
 *    )
 *    ```
 *    (import `org.jetbrains.compose.resources.Font` and the generated `Res`).
 *
 * Until then it falls back to the platform's default sans-serif so layout and
 * weights stay correct. On the web the HTML shell additionally requests
 * "DIN Next LT Pro" via CSS (see `styles.css`).
 */
@Composable
fun dhbFontFamily(): FontFamily = FontFamily.SansSerif

