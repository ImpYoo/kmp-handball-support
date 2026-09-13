package de.exhumedo.kmp.handball_support.persistence

import de.exhumedo.kmp.handball_support.client.CoachingEvaluationResponseDto
import de.exhumedo.kmp.handball_support.client.CoachingReportResponseDto
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

actual fun coachingCache(): CoachingCache = FileCoachingCache()

private class FileCoachingCache : CoachingCache {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val evalSerializer = ListSerializer(CoachingEvaluationResponseDto.serializer())
    private val reportSerializer = CoachingReportResponseDto.serializer()
    private val dir: File =
        File(System.getProperty("user.home"), ".handball-support").resolve("coaching-cache")

    private val myFile get() = dir.resolve("my-evaluations.json")
    private val allFile get() = dir.resolve("all-evaluations.json")
    private fun reportFile(id: String) = dir.resolve("reports").resolve("$id.json")

    override fun saveMyEvaluations(evaluations: List<CoachingEvaluationResponseDto>) {
        dir.mkdirs()
        myFile.writeText(json.encodeToString(evalSerializer, evaluations))
    }

    override fun loadMyEvaluations(): List<CoachingEvaluationResponseDto>? {
        if (!myFile.exists()) return null
        return runCatching { json.decodeFromString(evalSerializer, myFile.readText()) }.getOrNull()
    }

    override fun saveAllEvaluations(evaluations: List<CoachingEvaluationResponseDto>) {
        dir.mkdirs()
        allFile.writeText(json.encodeToString(evalSerializer, evaluations))
    }

    override fun loadAllEvaluations(): List<CoachingEvaluationResponseDto>? {
        if (!allFile.exists()) return null
        return runCatching { json.decodeFromString(evalSerializer, allFile.readText()) }.getOrNull()
    }

    override fun saveReport(evaluationId: String, report: CoachingReportResponseDto) {
        reportFile(evaluationId).apply { parentFile?.mkdirs() }
            .writeText(json.encodeToString(reportSerializer, report))
    }

    override fun loadReport(evaluationId: String): CoachingReportResponseDto? {
        val file = reportFile(evaluationId)
        if (!file.exists()) return null
        return runCatching { json.decodeFromString(reportSerializer, file.readText()) }.getOrNull()
    }

    override fun removeReport(evaluationId: String) {
        reportFile(evaluationId).delete()
    }

    override fun clear() {
        runCatching { dir.deleteRecursively() }
    }
}