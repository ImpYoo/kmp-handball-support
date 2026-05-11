package de.exhumedo.kmp.handball_support.domain.policy

data class AuthPayload(
    val username: String,
    val applications: Set<String>,
    val phaseIds: Set<Int>,
    val hasAccessToAllApplications: Boolean,
    val hasAccessToAllPhases: Boolean,
    val hasAccessToEvaluation: Boolean,
    val isSuperUser: Boolean,
)

class AccessPolicy {
    fun hasAccessToApplication(payload: AuthPayload, application: String): Boolean =
        payload.applications.contains(application) ||
            payload.hasAccessToAllApplications ||
            payload.isSuperUser

    fun hasAccessToPhase(payload: AuthPayload, phaseId: Int): Boolean =
        payload.phaseIds.contains(phaseId) ||
            payload.hasAccessToAllPhases ||
            payload.isSuperUser

    fun hasAccessToEvaluation(payload: AuthPayload): Boolean =
        payload.hasAccessToEvaluation || payload.isSuperUser
}

