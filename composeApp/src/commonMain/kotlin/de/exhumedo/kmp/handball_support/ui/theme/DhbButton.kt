package de.exhumedo.kmp.handball_support.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * DHB-style button matching dhb.de:
 *
 * - Always has a 1 dp **black border** and **rounded corners**.
 * - **Default (no hover):** white fill, black text.
 * - **Hover / press:** black fill, white text.
 *
 * Hover is detected via the [MutableInteractionSource] (works on desktop & web);
 * the pressed state mirrors the hover look so touch devices still get clear
 * feedback.
 */
@Composable
fun DhbButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val active = hovered || pressed

    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (enabled) DhbBlack else DhbBlack.copy(alpha = 0.38f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (active) DhbBlack else DhbWhite,
            contentColor = if (active) DhbWhite else DhbBlack,
            disabledContainerColor = DhbWhite,
            disabledContentColor = DhbBlack.copy(alpha = 0.38f),
        ),
        content = content,
    )
}

