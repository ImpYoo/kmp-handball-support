package de.exhumedo.kmp.handball_support.domain

import de.exhumedo.kmp.handball_support.domain.policy.AccessPolicy
import de.exhumedo.kmp.handball_support.domain.policy.AuthPayload
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccessPolicyTest {

    private val policy = AccessPolicy()

    private fun payload(
        applications: Set<String> = emptySet(),
        phaseIds: Set<Int> = emptySet(),
        hasAccessToAllApplications: Boolean = false,
        hasAccessToAllPhases: Boolean = false,
        hasAccessToEvaluation: Boolean = false,
        isSuperUser: Boolean = false,
    ) = AuthPayload(
        username = "user",
        applications = applications,
        phaseIds = phaseIds,
        hasAccessToAllApplications = hasAccessToAllApplications,
        hasAccessToAllPhases = hasAccessToAllPhases,
        hasAccessToEvaluation = hasAccessToEvaluation,
        isSuperUser = isSuperUser,
    )

    @Test
    fun allowsApplicationWhenExplicitlyGranted() {
        assertTrue(policy.hasAccessToApplication(payload(applications = setOf("scoring")), "scoring"))
    }

    @Test
    fun deniesApplicationWhenNotGranted() {
        assertFalse(policy.hasAccessToApplication(payload(applications = setOf("other")), "scoring"))
    }

    @Test
    fun allowsApplicationWhenHasAccessToAllApplications() {
        assertTrue(policy.hasAccessToApplication(payload(hasAccessToAllApplications = true), "any-app"))
    }

    @Test
    fun allowsApplicationWhenSuperUser() {
        assertTrue(policy.hasAccessToApplication(payload(isSuperUser = true), "any-app"))
    }

    @Test
    fun deniesApplicationForUserWithNoGrants() {
        assertFalse(policy.hasAccessToApplication(payload(), "scoring"))
    }

    @Test
    fun allowsPhaseWhenExplicitlyGranted() {
        assertTrue(policy.hasAccessToPhase(payload(phaseIds = setOf(42)), 42))
    }

    @Test
    fun deniesPhaseWhenNotGranted() {
        assertFalse(policy.hasAccessToPhase(payload(phaseIds = setOf(1)), 42))
    }

    @Test
    fun allowsPhaseWhenHasAccessToAllPhases() {
        assertTrue(policy.hasAccessToPhase(payload(hasAccessToAllPhases = true), 42))
    }

    @Test
    fun allowsPhaseWhenSuperUser() {
        assertTrue(policy.hasAccessToPhase(payload(isSuperUser = true), 42))
    }

    @Test
    fun deniesPhaseForUserWithNoGrants() {
        assertFalse(policy.hasAccessToPhase(payload(), 42))
    }

    @Test
    fun allowsEvaluationWhenFlagIsSet() {
        assertTrue(policy.hasAccessToEvaluation(payload(hasAccessToEvaluation = true)))
    }

    @Test
    fun allowsEvaluationWhenSuperUser() {
        assertTrue(policy.hasAccessToEvaluation(payload(isSuperUser = true)))
    }

    @Test
    fun deniesEvaluationWhenFlagIsFalseAndNotSuperUser() {
        assertFalse(policy.hasAccessToEvaluation(payload()))
    }

    @Test
    fun superUserAlwaysHasFullAccess() {
        val su = payload(isSuperUser = true)
        assertTrue(policy.hasAccessToApplication(su, "any"))
        assertTrue(policy.hasAccessToPhase(su, 999))
        assertTrue(policy.hasAccessToEvaluation(su))
    }
}
