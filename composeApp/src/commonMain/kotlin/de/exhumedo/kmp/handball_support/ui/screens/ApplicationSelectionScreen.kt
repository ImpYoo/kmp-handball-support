package de.exhumedo.kmp.handball_support.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.exhumedo.kmp.handball_support.ui.theme.AppTheme
import de.exhumedo.kmp.handball_support.ui.theme.DhbHeader
import de.exhumedo.kmp.handball_support.ui.theme.DhbRed
import de.exhumedo.kmp.handball_support.ui.theme.Dimens

/**
 * Landing screen: lets the user choose which application to use. Each option is
 * rendered as a large, tappable tile. New applications can be added by appending
 * another [ApplicationTile].
 */
@Composable
fun ApplicationSelectionScreen(
    onOpenPhases: () -> Unit,
    onOpenCoaching: () -> Unit,
    onOpenCoachingSheet: () -> Unit,
    onOpenMatchConsole: () -> Unit,
    onOpenRoster: () -> Unit,
    onOpenMatchSetup: () -> Unit,
    onOpenDrawingPad: () -> Unit,
    onOpenTacticBoard: () -> Unit,
) {
    AppTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            DhbHeader(
                title = "Handball Support",
                subtitle = "Anwendung auswählen",
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = Dimens.contentMaxWidth)
                        .fillMaxWidth()
                        .padding(Dimens.spaceLg),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
                ) {
                    ApplicationTile(
                        title = "Spielbewertung",
                        description = "Phasen und Spiele auswählen und Schiedsrichter bewerten.",
                        accent = DhbRed,
                        onClick = onOpenPhases,
                    )
                    ApplicationTile(
                        title = "Schiedsrichter-Coaching",
                        description = "Coaching-Sitzung mit Bogen, Spieluhr und Anzeigetafel.",
                        accent = CoachingAccent,
                        onClick = onOpenCoaching,
                    )
                    ApplicationTile(
                        title = "Coaching-Bogen",
                        description = "Nur den HVNB-Beobachterbogen ausfüllen, ohne Spieluhr.",
                        accent = CoachingSheetAccent,
                        onClick = onOpenCoachingSheet,
                    )
                    ApplicationTile(
                        title = "Spieluhr & Anzeigetafel",
                        description = "Zeit stoppen und den Spielstand von Heim und Gast führen.",
                        accent = MatchConsoleAccent,
                        onClick = onOpenMatchConsole,
                    )
                    ApplicationTile(
                        title = "Aufstellungen",
                        description = "Kader für Heim- und Gastmannschaft anlegen.",
                        accent = RosterAccent,
                        onClick = onOpenRoster,
                    )
                    ApplicationTile(
                        title = "Spieldaten",
                        description = "Mannschaften (mit Kürzel) und Schiedsrichter festlegen.",
                        accent = MatchSetupAccent,
                        onClick = onOpenMatchSetup,
                    )
                    ApplicationTile(
                        title = "Notizblock",
                        description = "Freihand zeichnen — z.B. taktische Skizzen.",
                        accent = DrawingAccent,
                        onClick = onOpenDrawingPad,
                    )
                    ApplicationTile(
                        title = "Taktiktafel",
                        description = "Spielfeld mit verschiebbaren Spieler-Tokens.",
                        accent = TacticAccent,
                        onClick = onOpenTacticBoard,
                    )
                }
            }
        }
    }
}

@Composable
private fun ApplicationTile(
    title: String,
    description: String,
    accent: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Button }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(Dimens.cardCorner),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (hovered) 2.dp else 1.dp,
            color = if (hovered) accent else MaterialTheme.colorScheme.outlineVariant,
        ),
        shadowElevation = if (hovered) Dimens.cardElevationRaised else Dimens.cardElevation,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.spaceLg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Vertical brand accent bar.
            Box(
                modifier = Modifier
                    .size(width = Dimens.accentBarWidth, height = Dimens.accentBarHeight)
                    .clip(RoundedCornerShape(Dimens.accentBarWidth))
                    .background(accent),
            )
            Spacer(Modifier.width(Dimens.spaceLg))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(Dimens.spaceXs))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(Dimens.spaceMd))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val CoachingAccent = Color(0xFF2E7D32)
private val CoachingSheetAccent = Color(0xFF6A1B9A)
private val MatchConsoleAccent = Color(0xFF1565C0)
private val RosterAccent = Color(0xFFEF6C00)
private val MatchSetupAccent = Color(0xFF00838F)
private val DrawingAccent = Color(0xFF6D4C41)
private val TacticAccent  = Color(0xFF00695C)

@Preview
@Composable
private fun ApplicationSelectionScreenPreview() {
    ApplicationSelectionScreen(
        onOpenPhases = {},
        onOpenCoaching = {},
        onOpenCoachingSheet = {},
        onOpenMatchConsole = {},
        onOpenRoster = {},
        onOpenMatchSetup = {},
        onOpenDrawingPad = {},
        onOpenTacticBoard = {},
    )
}












