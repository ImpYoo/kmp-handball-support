package de.exhumedo.kmp.handball_support.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.Composable
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
import de.exhumedo.kmp.handball_support.client.ChangePasswordRequestDto
import de.exhumedo.kmp.handball_support.client.VoteApiClient
import de.exhumedo.kmp.handball_support.config.AppConfig
import de.exhumedo.kmp.handball_support.ui.theme.AppTheme
import de.exhumedo.kmp.handball_support.ui.theme.DhbButton
import de.exhumedo.kmp.handball_support.ui.theme.DhbHeader
import de.exhumedo.kmp.handball_support.ui.theme.Dimens
import de.exhumedo.kmp.handball_support.ui.UserMenuButton
import kotlinx.coroutines.launch

/**
 * Self-service password change screen.
 */
@Composable
fun ChangePasswordScreen(
    token: String,
    username: String,
    onPasswordChanged: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
    onNavigateHome: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val client = remember { VoteApiClient() }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    fun submit() {
        if (newPassword != confirmPassword) {
            message = "Die neuen Passwörter stimmen nicht überein."
            isError = true
            return
        }
        if (newPassword.isBlank()) {
            message = "Neues Passwort darf nicht leer sein."
            isError = true
            return
        }
        scope.launch {
            try {
                client.changePassword(
                    AppConfig.baseApiUrl,
                    token,
                    ChangePasswordRequestDto(currentPassword = currentPassword, newPassword = newPassword),
                )
                message = "Passwort geändert."
                isError = false
                currentPassword = ""
                newPassword = ""
                confirmPassword = ""
                onPasswordChanged()
            } catch (e: Throwable) {
                message = e.message ?: "Fehler"
                isError = true
            }
        }
    }

    AppTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            DhbHeader(
                title = "Passwort ändern",
                subtitle = username,
                onLogoClick = onNavigateHome,
                actions = {
                    UserMenuButton(
                        isLoggedIn = true,
                        username = username,
                        onSettings = onOpenSettings,
                        onLogin = { },
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
                    Text(
                        text = "Passwort ändern",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Aktuelles Passwort") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Neues Passwort") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Neues Passwort wiederholen") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (message.isNotBlank()) {
                        Text(
                            text = message,
                            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Spacer(Modifier.height(Dimens.spaceSm))
                    DhbButton(onClick = { submit() }) { Text("Passwort ändern") }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ChangePasswordScreenPreview() {
    ChangePasswordScreen(
        token = "dummy",
        username = "coach",
        onPasswordChanged = {},
        onOpenSettings = {},
        onLogout = {},
        onNavigateHome = {},
    )
}
