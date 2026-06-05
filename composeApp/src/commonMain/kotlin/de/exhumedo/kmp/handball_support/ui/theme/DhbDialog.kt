package de.exhumedo.kmp.handball_support.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * DHB-styled dialog matching the app's card language: a white surface with a
 * hairline border, soft elevation, the short red accent bar + bold title used
 * by the section headers, consistent [Dimens] spacing, and [DhbButton] actions
 * aligned to the end.
 *
 * Use this instead of a bare Material `AlertDialog` so dialogs feel native to
 * the rest of the UI.
 */
@Composable
fun DhbDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String = "Cancel",
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .padding(Dimens.spaceXl),
            shape = RoundedCornerShape(Dimens.cardCorner),
            color = DhbWhite,
            border = BorderStroke(1.dp, DhbOutlineVariant),
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(Dimens.spaceXl)) {
                // Red brand accent bar + bold title (matches Section headers).
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(width = Dimens.accentBarWidth, height = Dimens.accentBarHeight)
                            .clip(RoundedCornerShape(Dimens.accentBarWidth))
                            .background(DhbRed),
                    )
                    Spacer(Modifier.width(Dimens.spaceMd))
                    Text(
                        text = title,
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = DhbBlack,
                    )
                }

                Spacer(Modifier.height(Dimens.spaceLg))

                Column(content = content)

                Spacer(Modifier.height(Dimens.spaceXl))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm, Alignment.End),
                ) {
                    DhbButton(onClick = onDismissRequest) { Text(dismissText) }
                    DhbButton(onClick = onConfirm) { Text(confirmText) }
                }
            }
        }
    }
}


