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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.exhumedo.kmp.handball_support.client.CoachingApiClient
import de.exhumedo.kmp.handball_support.client.CoachingEvaluationResponseDto
import de.exhumedo.kmp.handball_support.config.AppConfig
import de.exhumedo.kmp.handball_support.persistence.coachingCache
import de.exhumedo.kmp.handball_support.ui.theme.AppTheme
import de.exhumedo.kmp.handball_support.ui.theme.DhbButton
import de.exhumedo.kmp.handball_support.ui.theme.DhbDialog
import de.exhumedo.kmp.handball_support.ui.theme.DhbHeader
import de.exhumedo.kmp.handball_support.ui.theme.Dimens
import de.exhumedo.kmp.handball_support.ui.UserMenuButton
import kotlinx.coroutines.launch

private const val TAB_MINE = 0
private const val TAB_ALL = 1

/**
 * Lists coaching evaluations. Referee coaches see their own under "Meine Coachings"
 * and all others under "Alle Coachings". Admins see everything in both tabs and can
 * open or delete entries.
 *
 * On network failure, cached data from the last successful fetch is shown with an
 * "offline" banner so the list remains usable without a connection.
 */
@Composable
fun CoachingListScreen(
    token: String,
    username: String,
    role: String?,
    initialTab: String?,
    onOpenEvaluation: (String) -> Unit,
    onContinueEvaluation: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
    onNavigateHome: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val client = remember { CoachingApiClient() }
    val cache = remember { coachingCache() }
    var selectedTab by remember { mutableIntStateOf(if (initialTab == "all") TAB_ALL else TAB_MINE) }
    var myEvaluations by remember { mutableStateOf(cache.loadMyEvaluations() ?: emptyList()) }
    var allEvaluations by remember { mutableStateOf(cache.loadAllEvaluations() ?: emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isOffline by remember { mutableStateOf(false) }
    var evaluationToDelete by remember { mutableStateOf<CoachingEvaluationResponseDto?>(null) }

    fun isAdmin(): Boolean = role.equals("admin", ignoreCase = true) || role.equals("referee-coach-admin", ignoreCase = true)

    fun load() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val mine = client.listMyEvaluations(AppConfig.baseApiUrl, token)
                    .sortedByDescending { it.createdAt }
                val all = client.listAllEvaluations(AppConfig.baseApiUrl, token)
                    .sortedByDescending { it.createdAt }
                myEvaluations = mine
                allEvaluations = all
                cache.saveMyEvaluations(mine)
                cache.saveAllEvaluations(all)
                isOffline = false
            } catch (e: Throwable) {
                // Serve cached data if available; otherwise show the error.
                val cachedMine = cache.loadMyEvaluations()
                val cachedAll = cache.loadAllEvaluations()
                if (cachedMine != null) myEvaluations = cachedMine.sortedByDescending { it.createdAt }
                if (cachedAll != null) allEvaluations = cachedAll.sortedByDescending { it.createdAt }
                if (myEvaluations.isEmpty() && allEvaluations.isEmpty()) {
                    errorMessage = e.message ?: "Fehler beim Laden"
                    isOffline = false
                } else {
                    isOffline = true
                }
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(token) { load() }
    LaunchedEffect(Unit) { load() }

    AppTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            DhbHeader(
                title = "Coaching-Übersicht",
                subtitle = when (selectedTab) {
                    TAB_MINE -> "Meine Coachings"
                    else -> "Alle Coachings"
                },
                onLogoClick = onNavigateHome,
                actions = {
                    DhbButton(onClick = { load() }) { Text("Aktualisieren") }
                    Spacer(Modifier.width(Dimens.spaceSm))
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
                ) {
                    if (isOffline) {
                        Text(
                            text = "Offline — gespeicherte Daten werden angezeigt.",
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = Dimens.spaceSm),
                        )
                    }

                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == TAB_MINE,
                            onClick = { selectedTab = TAB_MINE },
                            text = { Text("Meine Coachings") },
                        )
                        Tab(
                            selected = selectedTab == TAB_ALL,
                            onClick = { selectedTab = TAB_ALL },
                            text = { Text("Alle Coachings") },
                        )
                    }
                    Spacer(Modifier.height(Dimens.spaceMd))

                    val evaluations = if (selectedTab == TAB_MINE) myEvaluations else allEvaluations

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

                        evaluations.isEmpty() -> Text(
                            text = "Keine Einträge vorhanden.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )

                        else -> LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
                        ) {
                            items(evaluations, key = { it.id }) { evaluation ->
                                CoachingEvaluationCard(
                                    evaluation = evaluation,
                                    canDelete = isAdmin() || evaluation.evaluatorUsername.equals(username, ignoreCase = true),
                                    onOpen = { onOpenEvaluation(evaluation.id) },
                                    onContinue = { onContinueEvaluation(evaluation.id) },
                                    onDelete = { evaluationToDelete = evaluation },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    evaluationToDelete?.let { evaluation ->
        DhbDialog(
            onDismissRequest = { evaluationToDelete = null },
            title = "Coaching löschen?",
            confirmText = "Löschen",
            dismissText = "Abbrechen",
            onConfirm = {
                scope.launch {
                    try {
                        client.deleteEvaluation(AppConfig.baseApiUrl, token, evaluation.id)
                        cache.removeReport(evaluation.id)
                        load()
                    } catch (e: Throwable) {
                        errorMessage = e.message ?: "Löschen fehlgeschlagen"
                    } finally {
                        evaluationToDelete = null
                    }
                }
            },
        ) {
            Text(
                text = "Das Coaching ${evaluation.game.homeTeam} vs ${evaluation.game.awayTeam} (${evaluation.game.matchDate}) wird unwiderruflich gelöscht.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CoachingEvaluationCard(
    evaluation: CoachingEvaluationResponseDto,
    canDelete: Boolean,
    onOpen: () -> Unit,
    onContinue: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cardCorner),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = Dimens.cardElevation,
    ) {
        Column(modifier = Modifier.padding(Dimens.spaceLg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${evaluation.game.homeTeam} vs ${evaluation.game.awayTeam}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(Dimens.spaceXs))
                    Text(
                        text = "Datum: ${evaluation.game.matchDate} · Bewertung: ${evaluation.totalScore}/${evaluation.maxTotalScore} (${evaluation.percentage}%)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Schiedsrichter: ${evaluation.firstReferee.firstName} ${evaluation.firstReferee.lastName} / " +
                            "${evaluation.secondReferee.firstName} ${evaluation.secondReferee.lastName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Coach: ${evaluation.evaluatorUsername} · ${evaluation.createdAt.take(16)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DhbButton(onClick = onOpen) { Text("Öffnen") }
                Spacer(Modifier.width(Dimens.spaceSm))
                DhbButton(onClick = onContinue) { Text("Fortsetzen") }
                if (canDelete) {
                    Spacer(Modifier.width(Dimens.spaceSm))
                    DhbButton(onClick = onDelete) { Text("Löschen") }
                }
            }
        }
    }
}

@Preview
@Composable
private fun CoachingListScreenPreview() {
    CoachingListScreen(
        token = "dummy",
        username = "coach",
        role = "referee-coach-admin",
        initialTab = null,
        onOpenEvaluation = {},
        onContinueEvaluation = {},
        onOpenSettings = {},
        onLogout = {},
        onNavigateHome = {},
    )
}