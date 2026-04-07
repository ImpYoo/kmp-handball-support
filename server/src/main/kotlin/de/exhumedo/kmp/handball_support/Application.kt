package de.exhumedo.kmp.handball_support

import de.exhumedo.kmp.handball_support.application.PerformanceEvaluationApplicationService
import de.exhumedo.kmp.handball_support.application.UuidEvaluationIdGenerator
import de.exhumedo.kmp.handball_support.api.configureHttp
import de.exhumedo.kmp.handball_support.api.configureRouting
import de.exhumedo.kmp.handball_support.domain.rating.repository.PerformanceEvaluationRepository
import de.exhumedo.kmp.handball_support.persistence.JsonFilePerformanceEvaluationRepository
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.time.Clock

/**
 * Server entry point.
 */
fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

/**
 * Configures the Ktor application with the file-backed repository and API routes.
 *
 * @param repository Repository adapter used by the API.
 * @param clock Clock used to derive creation timestamps for new evaluations.
 */
fun Application.module(
    repository: PerformanceEvaluationRepository = JsonFilePerformanceEvaluationRepository(defaultStoragePath()),
    clock: Clock = Clock.System,
) {
    val applicationService = PerformanceEvaluationApplicationService(
        repository = repository,
        idGenerator = UuidEvaluationIdGenerator(),
        clock = clock,
    )

    configureHttp()
    configureRouting(repository, applicationService)
}

private fun defaultStoragePath(): Path {
    val configured = System.getenv("PERFORMANCE_EVALUATIONS_FILE")
        ?: System.getProperty("performance.evaluations.file")
        ?: "server/data/performance-evaluations.json"

    return Paths.get(configured)
}
