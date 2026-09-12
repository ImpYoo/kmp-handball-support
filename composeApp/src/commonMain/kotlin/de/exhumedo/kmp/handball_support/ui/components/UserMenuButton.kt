package de.exhumedo.kmp.handball_support.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.exhumedo.kmp.handball_support.ui.theme.Dimens

/**
 * Header account menu: a single icon that exposes user-scoped actions.
 *
 * - Signed in: shows username, Settings, Logout.
 * - Signed out: shows Login.
 */
@Composable
fun UserMenuButton(
    isLoggedIn: Boolean,
    username: String,
    onSettings: () -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.padding(end = Dimens.spaceSm),
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Kontomenü",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.height(28.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (isLoggedIn) {
                DropdownMenuItem(
                    text = { Text("Angemeldet als $username") },
                    onClick = { },
                    enabled = false,
                )
            }
            DropdownMenuItem(
                text = { Text("Einstellungen") },
                onClick = {
                    expanded = false
                    onSettings()
                },
            )
            if (isLoggedIn) {
                DropdownMenuItem(
                    text = { Text("Abmelden") },
                    onClick = {
                        expanded = false
                        onLogout()
                    },
                )
            } else {
                DropdownMenuItem(
                    text = { Text("Anmelden") },
                    onClick = {
                        expanded = false
                        onLogin()
                    },
                )
            }
        }
    }
}
