package de.exhumedo.kmp.handball_support.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.exhumedo.kmp.handball_support.vote.VoteAppPresenter

@Composable
fun LoginScreen(
    presenter: VoteAppPresenter,
    onAction: (suspend () -> Unit) -> Unit,
    onBackToPhases: () -> Unit,
) {
    MaterialTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = "Handball Support",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Performance Evaluation Voting",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(24.dp))

            Section(title = "Sign In to Vote") {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = presenter.username,
                    onValueChange = { presenter.username = it },
                    label = { Text("Username") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = presenter.password,
                    onValueChange = { presenter.password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onAction { presenter.login() } },
                    enabled = !presenter.isBusy,
                ) {
                    Text("Sign In")
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = presenter.statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onBackToPhases
                ) {
                    Text("Back to Phases")
                }
            }
        }
    }
}

@Preview
@Composable
private fun LoginScreenPreview() {
    val presenter = previewPresenter()
    LoginScreen(
        presenter = presenter,
        onAction = {},
        onBackToPhases = {},
    )
}

@Preview
@Composable
private fun LoginScreenPreviewBusy() {
    val presenter = previewPresenter {
        isBusy = true
        statusMessage = "Signing in..."
    }
    LoginScreen(
        presenter = presenter,
        onAction = {},
        onBackToPhases = {},
    )
}

