package de.exhumedo.kmp.handball_support.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.exhumedo.kmp.handball_support.client.AuthUserResponseDto
import de.exhumedo.kmp.handball_support.client.CreateAuthUserRequestDto
import de.exhumedo.kmp.handball_support.client.UpdateAuthUserRequestDto
import de.exhumedo.kmp.handball_support.client.VoteApiClient
import de.exhumedo.kmp.handball_support.config.AppConfig
import de.exhumedo.kmp.handball_support.ui.theme.AppTheme
import de.exhumedo.kmp.handball_support.ui.theme.DhbButton
import de.exhumedo.kmp.handball_support.ui.theme.DhbDialog
import de.exhumedo.kmp.handball_support.ui.theme.DhbHeader
import de.exhumedo.kmp.handball_support.ui.theme.Dimens
import kotlinx.coroutines.launch

private val MANAGEABLE_ROLES = listOf("admin", "referee", "coach", "viewer", "referee-coach", "referee-coach-admin")

/**
 * Admin UI for managing auth users. Only users with `admin` or `referee-coach-admin`
 * roles can reach this screen. Supports create, role toggle, enable/disable and delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAdminScreen(
    token: String,
    onNavigateHome: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val client = remember { VoteApiClient() }
    var users by remember { mutableStateOf(listOf<AuthUserResponseDto>()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<AuthUserResponseDto?>(null) }

    fun load() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                users = client.listUsers(AppConfig.baseApiUrl, token).sortedBy { it.username }
            } catch (e: Throwable) {
                errorMessage = e.message ?: "Fehler beim Laden"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(token) { load() }

    AppTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            DhbHeader(
                title = "Benutzerverwaltung",
                subtitle = "Rollen verwalten",
                onLogoClick = onNavigateHome,
                actions = {
                    DhbButton(onClick = { load() }) { Text("Aktualisieren") }
                    Spacer(Modifier.width(Dimens.spaceSm))
                    DhbButton(onClick = { showCreateDialog = true }) { Text("Benutzer anlegen") }
                    Spacer(Modifier.width(Dimens.spaceSm))
                    DhbButton(onClick = onNavigateHome) { Text("Menü") }
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
                ) {
                    when {
                        isLoading -> Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("Laden...") }

                        errorMessage != null -> Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )

                        users.isEmpty() -> Text(
                            text = "Keine Benutzer gefunden.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )

                        else -> LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
                        ) {
                            items(users, key = { it.username }) { user ->
                                UserAdminCard(
                                    user = user,
                                    onRoleChange = { newRole ->
                                        scope.launch {
                                            try {
                                                client.updateUser(
                                                    AppConfig.baseApiUrl,
                                                    token,
                                                    user.username,
                                                    UpdateAuthUserRequestDto(role = newRole),
                                                )
                                                load()
                                            } catch (e: Throwable) {
                                                errorMessage = e.message ?: "Speichern fehlgeschlagen"
                                            }
                                        }
                                    },
                                    onEnabledChange = { enabled ->
                                        scope.launch {
                                            try {
                                                client.updateUser(
                                                    AppConfig.baseApiUrl,
                                                    token,
                                                    user.username,
                                                    UpdateAuthUserRequestDto(enabled = enabled),
                                                )
                                                load()
                                            } catch (e: Throwable) {
                                                errorMessage = e.message ?: "Speichern fehlgeschlagen"
                                            }
                                        }
                                    },
                                    onDelete = { userToDelete = user },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateUserDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { username, password, role ->
                scope.launch {
                    try {
                        client.createUser(
                            AppConfig.baseApiUrl,
                            token,
                            CreateAuthUserRequestDto(username = username, password = password, role = role),
                        )
                        load()
                        showCreateDialog = false
                    } catch (e: Throwable) {
                        errorMessage = e.message ?: "Anlegen fehlgeschlagen"
                    }
                }
            },
        )
    }

    userToDelete?.let { user ->
        DhbDialog(
            onDismissRequest = { userToDelete = null },
            title = "Benutzer löschen?",
            confirmText = "Löschen",
            dismissText = "Abbrechen",
            onConfirm = {
                scope.launch {
                    try {
                        client.deleteUser(AppConfig.baseApiUrl, token, user.username)
                        load()
                    } catch (e: Throwable) {
                        errorMessage = e.message ?: "Löschen fehlgeschlagen"
                    } finally {
                        userToDelete = null
                    }
                }
            },
        ) {
            Text(
                text = "Der Benutzer '${user.username}' wird unwiderruflich gelöscht.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserAdminCard(
    user: AuthUserResponseDto,
    onRoleChange: (String) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cardCorner),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = Dimens.cardElevation,
    ) {
        Column(modifier = Modifier.padding(Dimens.spaceLg)) {
            Text(
                text = user.username,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(Dimens.spaceSm))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                OutlinedTextField(
                    value = user.role,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Rolle") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    MANAGEABLE_ROLES.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role) },
                            onClick = {
                                expanded = false
                                onRoleChange(role)
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(Dimens.spaceSm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Aktiviert", modifier = Modifier.weight(1f))
                Switch(checked = user.enabled, onCheckedChange = onEnabledChange)
            }
            Spacer(Modifier.height(Dimens.spaceSm))
            DhbButton(onClick = onDelete) { Text("Löschen") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateUserDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("coach") }
    var expanded by remember { mutableStateOf(false) }

    DhbDialog(
        onDismissRequest = onDismiss,
        title = "Benutzer anlegen",
        confirmText = "Anlegen",
        dismissText = "Abbrechen",
        onConfirm = { onCreate(username, password, role) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Benutzername") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Passwort") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                OutlinedTextField(
                    value = role,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Rolle") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    MANAGEABLE_ROLES.forEach { r ->
                        DropdownMenuItem(
                            text = { Text(r) },
                            onClick = {
                                role = r
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun UserAdminScreenPreview() {
    UserAdminScreen(
        token = "dummy",
        onNavigateHome = {},
    )
}
