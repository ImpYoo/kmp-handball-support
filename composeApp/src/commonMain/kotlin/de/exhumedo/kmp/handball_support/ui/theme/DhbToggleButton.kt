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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * DHB-style toggle button used to pick between mutually exclusive options.
 *
 * - **Selected:** yellow fill, black text.
 * - **Unselected:** white fill, black text.
 * - **Hover:** black fill, white text.
 * - **Pressed:** red fill, white text.
 *
 * Always has a 1 dp black border and rounded corners, matching [DhbButton].
 */
@Composable
fun DhbToggleButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()

    val containerColor = when {
        pressed -> DhbRed
        hovered -> DhbBlack
        selected -> DhbYellow
        else -> DhbWhite
    }
    val contentColor = if (pressed || hovered) DhbWhite else DhbBlack

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.semantics {
            this.role = Role.RadioButton
            this.selected = selected
        },
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(2.dp, if (enabled) DhbBlack else DhbBlack.copy(alpha = 0.38f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = if (selected) DhbYellow else DhbWhite,
            disabledContentColor = DhbBlack.copy(alpha = 0.38f),
        ),
        content = content,
    )
}





