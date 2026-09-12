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
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import de.exhumedo.kmp.handball_support.ui.UserMenuButton

/**
 * Administration hub: a single entry point for admin-scoped actions.
 *
 * Currently only user management is available, but the hub layout keeps room
 * for future admin modules (audit logs, system config, etc.).
 */
@Composable
fun AdminScreen(
    token: String,
    username: String,
    role: String?,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
    onLogin: () -> Unit,
    onNavigateHome: () -> Unit,
) {
    var showUserAdmin by remember { mutableStateOf(false) }

    AppTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            if (showUserAdmin) {
                UserAdminScreen(
                    token = token,
                    onNavigateHome = onNavigateHome,
                    onBackToHub = { showUserAdmin = false },
                )
            } else {
                DhbHeader(
                    title = "Administration",
                    subtitle = "Systemverwaltung",
                    onLogoClick = onNavigateHome,
                    actions = {
                        UserMenuButton(
                            isLoggedIn = true,
                            username = username,
                            onSettings = onOpenSettings,
                            onLogin = onLogin,
                            onLogout = onLogout,
                        )
                    },
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
                        AdminTile(
                            icon = Icons.Default.People,
                            title = "Benutzerverwaltung",
                            description = "Benutzer anlegen, Rollen zuweisen, Konten aktivieren oder deaktivieren.",
                            accent = DhbRed,
                            onClick = { showUserAdmin = true },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminTile(
    icon: ImageVector,
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
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

@Preview
@Composable
private fun AdminScreenPreview() {
    AdminScreen(
        token = "dummy",
        username = "admin",
        role = "admin",
        onOpenSettings = {},
        onLogout = {},
        onLogin = {},
        onNavigateHome = {},
    )
}
