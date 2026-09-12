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
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import de.exhumedo.kmp.handball_support.ui.theme.DhbBlack
import de.exhumedo.kmp.handball_support.ui.theme.DhbHeader
import de.exhumedo.kmp.handball_support.ui.theme.DhbRed
import de.exhumedo.kmp.handball_support.ui.theme.Dimens

/**
 * Account hub: groups all user-scoped actions in one place.
 *
 * - Profile summary (username + role)
 * - Self-service password change
 * - Logout
 * - Admin users get a shortcut to the administration area
 */
@Composable
fun SettingsScreen(
    username: String,
    role: String?,
    isLoggedIn: Boolean,
    onOpenChangePassword: () -> Unit,
    onOpenAdmin: () -> Unit,
    onLogout: () -> Unit,
    onLogin: () -> Unit,
    onNavigateHome: () -> Unit,
) {
    val isAdmin = role.equals("admin", ignoreCase = true) || role.equals("referee-coach-admin", ignoreCase = true)

    AppTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            DhbHeader(
                title = "Einstellungen",
                subtitle = if (isLoggedIn) "Angemeldet als $username" else "Nicht angemeldet",
                onLogoClick = onNavigateHome,
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
                    if (isLoggedIn) {
                        ProfileCard(username = username, role = role)
                        SettingsTile(
                            icon = Icons.Default.Lock,
                            title = "Passwort ändern",
                            description = "Aktuelles Passwort eingeben und neues Passwort setzen.",
                            accent = DhbBlack,
                            onClick = onOpenChangePassword,
                        )
                        if (isAdmin) {
                            SettingsTile(
                                icon = Icons.Default.AdminPanelSettings,
                                title = "Administration",
                                description = "Benutzer, Rollen und Berechtigungen verwalten.",
                                accent = DhbRed,
                                onClick = onOpenAdmin,
                            )
                        }
                        SettingsTile(
                            icon = Icons.Default.PowerSettingsNew,
                            title = "Abmelden",
                            description = "Aktuelle Sitzung beenden.",
                            accent = Color(0xFF616161),
                            onClick = onLogout,
                        )
                    } else {
                        SettingsTile(
                            icon = Icons.Default.Person,
                            title = "Anmelden",
                            description = "Mit bestehendem Konto anmelden, um Coachings zu synchronisieren.",
                            accent = DhbBlack,
                            onClick = onLogin,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(
    username: String,
    role: String?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cardCorner),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = Dimens.cardElevation,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.spaceLg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.width(Dimens.spaceMd))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = username,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(Dimens.spaceXs))
                Text(
                    text = role?.replaceFirstChar { it.uppercase() } ?: "Benutzer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
private fun SettingsScreenPreview() {
    SettingsScreen(
        username = "coach",
        role = "referee-coach-admin",
        isLoggedIn = true,
        onOpenChangePassword = {},
        onOpenAdmin = {},
        onLogout = {},
        onLogin = {},
        onNavigateHome = {},
    )
}

@Preview
@Composable
private fun SettingsScreenPreviewLoggedOut() {
    SettingsScreen(
        username = "",
        role = null,
        isLoggedIn = false,
        onOpenChangePassword = {},
        onOpenAdmin = {},
        onLogout = {},
        onLogin = {},
        onNavigateHome = {},
    )
}
