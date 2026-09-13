package de.exhumedo.kmp.handball_support.persistence

import de.exhumedo.kmp.handball_support.AndroidContext
import de.exhumedo.kmp.handball_support.client.CoachingEvaluationResponseDto
import de.exhumedo.kmp.handball_support.client.CoachingReportResponseDto
import android.content.Context
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

actual fun coachingCache(): CoachingCache {
    val ctx = AndroidContext.appContext
        ?: error("AndroidContext.appContext is not set.")
    return SharedPreferencesCoachingCache(ctx)
}

private class SharedPreferencesCoachingCache(
    context: Context,
) : CoachingCache {
    private val prefs = context.getSharedPreferences("handball_coaching_cache", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val evalSerializer = ListSerializer(CoachingEvaluationResponseDto.serializer())
    private val reportSerializer = CoachingReportResponseDto.serializer()

    override fun saveMyEvaluations(evaluations: List<CoachingEvaluationResponseDto>) {
        prefs.edit().putString(KEY_MY, json.encodeToString(evalSerializer, evaluations)).apply()
    }

    override fun loadMyEvaluations(): List<CoachingEvaluationResponseDto>? {
        val raw = prefs.getString(KEY_MY, null) ?: return null
        return runCatching { json.decodeFromString(evalSerializer, raw) }.getOrNull()
    }

    override fun saveAllEvaluations(evaluations: List<CoachingEvaluationResponseDto>) {
        prefs.edit().putString(KEY_ALL, json.encodeToString(evalSerializer, evaluations)).apply()
    }

    override fun loadAllEvaluations(): List<CoachingEvaluationResponseDto>? {
        val raw = prefs.getString(KEY_ALL, null) ?: return null
        return runCatching { json.decodeFromString(evalSerializer, raw) }.getOrNull()
    }

    override fun saveReport(evaluationId: String, report: CoachingReportResponseDto) {
        prefs.edit().putString(reportKey(evaluationId), json.encodeToString(reportSerializer, report)).apply()
    }

    override fun loadReport(evaluationId: String): CoachingReportResponseDto? {
        val raw = prefs.getString(reportKey(evaluationId), null) ?: return null
        return runCatching { json.decodeFromString(reportSerializer, raw) }.getOrNull()
    }

    override fun removeReport(evaluationId: String) {
        prefs.edit().remove(reportKey(evaluationId)).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private fun reportKey(id: String) = "report_$id"

    companion object {
        private const val KEY_MY = "my_evaluations"
        private const val KEY_ALL = "all_evaluations"
    }
}