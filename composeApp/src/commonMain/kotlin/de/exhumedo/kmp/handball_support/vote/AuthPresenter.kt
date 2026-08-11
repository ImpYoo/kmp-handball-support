package de.exhumedo.kmp.handball_support.vote

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.exhumedo.kmp.handball_support.auth.StoredAuth
import de.exhumedo.kmp.handball_support.auth.TokenStorage
import de.exhumedo.kmp.handball_support.auth.tokenStorage
import de.exhumedo.kmp.handball_support.client.VoteApiClient
import de.exhumedo.kmp.handball_support.client.VoteApiException
import de.exhumedo.kmp.handball_support.config.AppConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Holds authentication state and login/logout logic.
 * Emits [UiEvent]s for session expiry and errors.
 */
class AuthPresenter(
    private val api: VoteApiClient = VoteApiClient(),
    private val authStorage: TokenStorage = tokenStorage(),
    private val events: MutableSharedFlow<UiEvent> = MutableSharedFlow(extraBufferCapacity = 16),
) {
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var token by mutableStateOf<String?>(null)
    var role by mutableStateOf<String?>(null)
    var isBusy by mutableStateOf(false)
    var statusMessage by mutableStateOf("Ready")

    val eventsFlow: SharedFlow<UiEvent> = events.asSharedFlow()

    init {
        authStorage.read()?.let {
            token = it.token
            role = it.role
        }
    }

    suspend fun login(
        onPendingVoteResume: (suspend (phaseId: Int, matchId: Int) -> Unit)? = null,
        pendingPhaseId: Int? = null,
        pendingMatchId: Int? = null,
    ) {
        isBusy = true
        statusMessage = "Signing in..."
        try {
            val response = api.login(baseUrl = AppConfig.baseApiUrl, username = username, password = password)
            token = response.accessToken
            role = response.role
            authStorage.save(StoredAuth(token = response.accessToken, role = response.role))
            password = ""
            statusMessage = "Logged in as $username (${response.role})"

            if (pendingPhaseId != null && pendingMatchId != null) {
                onPendingVoteResume?.let { it(pendingPhaseId, pendingMatchId) }
            }
        } catch (e: Throwable) {
            handleError(e, "Login failed")
        } finally {
            isBusy = false
        }
    }

    fun logout() {
        clearAuthState()
        statusMessage = "Logged out"
    }

    fun clearAuthState() {
        token = null
        role = null
        password = ""
        authStorage.clear()
    }

    fun handleError(e: Throwable, prefix: String) {
        val message = e.toUiMessage(prefix)
        statusMessage = message
        if (e is VoteApiException && e.statusCode == 401) {
            clearAuthState()
            events.tryEmit(UiEvent.SessionExpired)
        } else {
            events.tryEmit(UiEvent.Error(message))
        }
    }

    fun emitInfo(message: String) {
        events.tryEmit(UiEvent.Info(message))
    }
}

private fun Throwable.toUiMessage(prefix: String): String = when (this) {
    is VoteApiException -> "$prefix (HTTP $statusCode): ${message.orEmpty()}"
    else -> "$prefix: ${message.orEmpty()}"
}