package de.exhumedo.kmp.handball_support.navigation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * All top-level destinations of the app. Serializable so they can be used with
 * type-safe Navigation destinations and parsed back from URLs.
 */
@Serializable
sealed interface AppRoute {
    @Serializable
    @SerialName("home")
    data object Home : AppRoute

    @Serializable
    @SerialName("phases")
    data class Phases(
        val day: Int? = null,
        val month: Int? = null,
        val year: Int? = null,
        val showFilter: Boolean = false,
    ) : AppRoute

    @Serializable
    @SerialName("login")
    data class Login(
        val phaseId: Int? = null,
        val matchId: Int? = null,
        val day: Int? = null,
        val month: Int? = null,
        val year: Int? = null,
    ) : AppRoute

    @Serializable
    @SerialName("phase")
    data class PhaseDetail(
        val phaseId: Int,
        val day: Int? = null,
        val month: Int? = null,
        val year: Int? = null,
    ) : AppRoute

    @Serializable
    @SerialName("vote")
    data class Vote(
        val phaseId: Int,
        val matchId: Int,
        val day: Int? = null,
        val month: Int? = null,
        val year: Int? = null,
    ) : AppRoute

    @Serializable
    @SerialName("coaching")
    data object RefereeCoaching : AppRoute

    @Serializable
    @SerialName("session")
    data class CoachingSession(
        val evaluationId: String? = null,
    ) : AppRoute

    @Serializable
    @SerialName("sheet")
    data object CoachingSheet : AppRoute

    @Serializable
    @SerialName("clock")
    data object MatchConsole : AppRoute

    @Serializable
    @SerialName("roster")
    data object Roster : AppRoute

    @Serializable
    @SerialName("setup")
    data object MatchSetup : AppRoute

    @Serializable
    @SerialName("drawing")
    data object DrawingPad : AppRoute

    @Serializable
    @SerialName("tactic")
    data class TacticBoard(
        val debug: Boolean = false,
    ) : AppRoute
}
