package de.exhumedo.kmp.handball_support.persistence

import de.exhumedo.kmp.handball_support.client.CoachingEvaluationResponseDto
import de.exhumedo.kmp.handball_support.client.CoachingReportResponseDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Local cache for coaching evaluation lists and reports so they remain
 * available when the device is offline.
 *
 * The cache is best-effort: it stores the last successfully fetched data
 * and serves it on failure. It does not guarantee freshness.
 */
interface CoachingCache {
    /** Store the user's own evaluations list. */
    fun saveMyEvaluations(evaluations: List<CoachingEvaluationResponseDto>)

    /** Load the user's own evaluations list, or null if never cached. */
    fun loadMyEvaluations(): List<CoachingEvaluationResponseDto>?

    /** Store the all-evaluations list (admin view). */
    fun saveAllEvaluations(evaluations: List<CoachingEvaluationResponseDto>)

    /** Load the all-evaluations list, or null if never cached. */
    fun loadAllEvaluations(): List<CoachingEvaluationResponseDto>?

    /** Store a single report by evaluation id. */
    fun saveReport(evaluationId: String, report: CoachingReportResponseDto)

    /** Load a cached report by evaluation id, or null if not cached. */
    fun loadReport(evaluationId: String): CoachingReportResponseDto?

    /** Remove a cached report (e.g. after deletion). */
    fun removeReport(evaluationId: String)

    /** Clear all cached data. */
    fun clear()
}

/**
 * Returns the platform-specific [CoachingCache].
 */
expect fun coachingCache(): CoachingCache

/**
 * In-memory fallback used when no platform store is wired.
 */
class InMemoryCoachingCache : CoachingCache {
    private var myEvals: List<CoachingEvaluationResponseDto>? = null
    private var allEvals: List<CoachingEvaluationResponseDto>? = null
    private val reports = mutableMapOf<String, CoachingReportResponseDto>()

    override fun saveMyEvaluations(evaluations: List<CoachingEvaluationResponseDto>) { myEvals = evaluations }
    override fun loadMyEvaluations(): List<CoachingEvaluationResponseDto>? = myEvals
    override fun saveAllEvaluations(evaluations: List<CoachingEvaluationResponseDto>) { allEvals = evaluations }
    override fun loadAllEvaluations(): List<CoachingEvaluationResponseDto>? = allEvals
    override fun saveReport(evaluationId: String, report: CoachingReportResponseDto) { reports[evaluationId] = report }
    override fun loadReport(evaluationId: String): CoachingReportResponseDto? = reports[evaluationId]
    override fun removeReport(evaluationId: String) { reports.remove(evaluationId) }
    override fun clear() {
        myEvals = null
        allEvals = null
        reports.clear()
    }
}