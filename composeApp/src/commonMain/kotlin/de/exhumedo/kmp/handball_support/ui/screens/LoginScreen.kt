package de.exhumedo.kmp.handball_support.ui
import de.exhumedo.kmp.handball_support.ui.theme.DhbButton

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.exhumedo.kmp.handball_support.ui.theme.AppTheme
import de.exhumedo.kmp.handball_support.ui.theme.DhbHeader
import de.exhumedo.kmp.handball_support.ui.theme.Dimens
import de.exhumedo.kmp.handball_support.vote.VoteAppPresenter

@Composable
fun LoginScreen(
    presenter: VoteAppPresenter,
    onAction: (suspend () -> Unit) -> Unit,
    onBackToPhases: () -> Unit,
) {
    AppTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            DhbHeader(
                title = "Handball Support",
                subtitle = "Performance Evaluation Voting",
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimens.spaceLg),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 460.dp)
                        .fillMaxWidth(),
                ) {
                    Section(title = "Sign In to Vote") {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = presenter.username,
                            onValueChange = { presenter.username = it },
                            label = { Text("Username") },
                            singleLine = true,
                        )
                        Spacer(Modifier.height(Dimens.spaceSm))
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = presenter.password,
                            onValueChange = { presenter.password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                        )
                        Spacer(Modifier.height(Dimens.spaceLg))
                        DhbButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onAction { presenter.login() } },
                            enabled = !presenter.isBusy,
                        ) {
                            Text("Sign In")
                        }
                        Spacer(Modifier.height(Dimens.spaceSm))
                        Text(
                            text = presenter.statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(Dimens.spaceLg))
                        DhbButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onBackToPhases,
                        ) {
                            Text("Back to Phases")
                        }
                    }
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

