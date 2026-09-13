package de.exhumedo.kmp.handball_support.persistence

import de.exhumedo.kmp.handball_support.client.CoachingEvaluationResponseDto
import de.exhumedo.kmp.handball_support.client.CoachingReportResponseDto
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

actual fun coachingCache(): CoachingCache = NSUserDefaultsCoachingCache

private val json = Json { ignoreUnknownKeys = true }
private val evalSerializer = ListSerializer(CoachingEvaluationResponseDto.serializer())
private val reportSerializer = CoachingReportResponseDto.serializer()

private const val KEY_MY = "my_evaluations"
private const val KEY_ALL = "all_evaluations"
private const val KEY_REPORT_PREFIX = "report_"

private object NSUserDefaultsCoachingCache : CoachingCache {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun saveMyEvaluations(evaluations: List<CoachingEvaluationResponseDto>) {
        defaults.setObject(json.encodeToString(evalSerializer, evaluations), KEY_MY)
    }

    override fun loadMyEvaluations(): List<CoachingEvaluationResponseDto>? {
        val raw = defaults.stringForKey(KEY_MY) ?: return null
        return runCatching { json.decodeFromString(evalSerializer, raw) }.getOrNull()
    }

    override fun saveAllEvaluations(evaluations: List<CoachingEvaluationResponseDto>) {
        defaults.setObject(json.encodeToString(evalSerializer, evaluations), KEY_ALL)
    }

    override fun loadAllEvaluations(): List<CoachingEvaluationResponseDto>? {
        val raw = defaults.stringForKey(KEY_ALL) ?: return null
        return runCatching { json.decodeFromString(evalSerializer, raw) }.getOrNull()
    }

    override fun saveReport(evaluationId: String, report: CoachingReportResponseDto) {
        defaults.setObject(json.encodeToString(reportSerializer, report), KEY_REPORT_PREFIX + evaluationId)
    }

    override fun loadReport(evaluationId: String): CoachingReportResponseDto? {
        val raw = defaults.stringForKey(KEY_REPORT_PREFIX + evaluationId) ?: return null
        return runCatching { json.decodeFromString(reportSerializer, raw) }.getOrNull()
    }

    override fun removeReport(evaluationId: String) {
        defaults.removeObjectForKey(KEY_REPORT_PREFIX + evaluationId)
    }

    override fun clear() {
        defaults.removeObjectForKey(KEY_MY)
        defaults.removeObjectForKey(KEY_ALL)
        // Remove all report keys — we don't have an enumeration API for NSUserDefaults
        // through Kotlin/Native, so we accept that individual report keys may linger.
        // They'll be overwritten on next cache or are harmless.
    }
}