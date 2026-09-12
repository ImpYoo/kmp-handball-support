package de.exhumedo.kmp.handball_support.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import de.exhumedo.kmp.handball_support.client.CoachingApiClient
import de.exhumedo.kmp.handball_support.client.CoachingReportResponseDto
import de.exhumedo.kmp.handball_support.config.AppConfig
import de.exhumedo.kmp.handball_support.coaching.RefereeCoachingPresenter
import de.exhumedo.kmp.handball_support.ui.CoachingSheetScreen
import de.exhumedo.kmp.handball_support.ui.restoreCoachingFromReport
import de.exhumedo.kmp.handball_support.ui.theme.AppTheme
import de.exhumedo.kmp.handball_support.ui.theme.DhbHeader
import de.exhumedo.kmp.handball_support.ui.theme.Dimens
import kotlinx.coroutines.launch

@Composable
fun CoachingEvaluationReadOnlyScreen(
    token: String,
    username: String,
    evaluationId: String,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
    onNavigateHome: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val client = remember { CoachingApiClient() }
    val viewPresenter = remember { RefereeCoachingPresenter() }
    var report by remember { mutableStateOf<CoachingReportResponseDto?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val loaded = client.getReport(AppConfig.baseApiUrl, token, evaluationId)
                restoreCoachingFromReport(viewPresenter, loaded)
                report = loaded
            } catch (e: Throwable) {
                errorMessage = e.message ?: "Fehler beim Laden"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(evaluationId) { load() }

    if (report != null) {
        CoachingSheetScreen(
            coaching = viewPresenter,
            readOnly = true,
            onNavigateHome = onNavigateHome,
        )
        return
    }

    AppTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            DhbHeader(
                title = "Coaching ansehen",
                subtitle = "",
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
                    }
                }
            }
        }
    }
}
