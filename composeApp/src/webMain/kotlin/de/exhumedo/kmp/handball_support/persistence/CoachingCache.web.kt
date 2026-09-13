package de.exhumedo.kmp.handball_support.persistence

import de.exhumedo.kmp.handball_support.client.CoachingEvaluationResponseDto
import de.exhumedo.kmp.handball_support.client.CoachingReportResponseDto
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.browser.window

actual fun coachingCache(): CoachingCache = LocalStorageCoachingCache

private val json = Json { ignoreUnknownKeys = true }
private val evalSerializer = ListSerializer(CoachingEvaluationResponseDto.serializer())
private val reportSerializer = CoachingReportResponseDto.serializer()

private const val KEY_MY = "handball.cache.my_evaluations"
private const val KEY_ALL = "handball.cache.all_evaluations"
private const val KEY_REPORT_PREFIX = "handball.cache.report."

private object LocalStorageCoachingCache : CoachingCache {
    override fun saveMyEvaluations(evaluations: List<CoachingEvaluationResponseDto>) {
        runCatching { window.localStorage.setItem(KEY_MY, json.encodeToString(evalSerializer, evaluations)) }
    }

    override fun loadMyEvaluations(): List<CoachingEvaluationResponseDto>? {
        val raw = runCatching { window.localStorage.getItem(KEY_MY) }.getOrNull() ?: return null
        return runCatching { json.decodeFromString(evalSerializer, raw) }.getOrNull()
    }

    override fun saveAllEvaluations(evaluations: List<CoachingEvaluationResponseDto>) {
        runCatching { window.localStorage.setItem(KEY_ALL, json.encodeToString(evalSerializer, evaluations)) }
    }

    override fun loadAllEvaluations(): List<CoachingEvaluationResponseDto>? {
        val raw = runCatching { window.localStorage.getItem(KEY_ALL) }.getOrNull() ?: return null
        return runCatching { json.decodeFromString(evalSerializer, raw) }.getOrNull()
    }

    override fun saveReport(evaluationId: String, report: CoachingReportResponseDto) {
        runCatching { window.localStorage.setItem(KEY_REPORT_PREFIX + evaluationId, json.encodeToString(reportSerializer, report)) }
    }

    override fun loadReport(evaluationId: String): CoachingReportResponseDto? {
        val raw = runCatching { window.localStorage.getItem(KEY_REPORT_PREFIX + evaluationId) }.getOrNull() ?: return null
        return runCatching { json.decodeFromString(reportSerializer, raw) }.getOrNull()
    }

    override fun removeReport(evaluationId: String) {
        runCatching { window.localStorage.removeItem(KEY_REPORT_PREFIX + evaluationId) }
    }

    override fun clear() {
        runCatching {
            window.localStorage.removeItem(KEY_MY)
            window.localStorage.removeItem(KEY_ALL)
        }
    }
}