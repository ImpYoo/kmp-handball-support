package de.exhumedo.kmp.handball_support

import de.exhumedo.kmp.handball_support.persistence.JsonFilePerformanceEvaluationRepository
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhaseRoutingAuthTest {

    @Test
    fun phasesAndMatchListArePublicButMatchDetailRequiresToken() = testApplication {
        application {
            module(
                appConfig = createTestAppConfig(),
                repository = JsonFilePerformanceEvaluationRepository(Files.createTempFile("performance-evaluations", ".json")),
                authUserStore = createTestAuthUserStore(),
                clock = fixedClock("2026-05-10T10:30:00Z"),
            )
        }

        val phases = client.get("/api/phases")
        assertEquals(HttpStatusCode.OK, phases.status)
        assertTrue(phases.bodyAsText().startsWith("["))

        val matches = client.get("/api/phases/1/matches")
        assertEquals(HttpStatusCode.OK, matches.status)
        assertTrue(matches.bodyAsText().startsWith("["))

        val detailWithoutToken = client.get("/api/phases/1/matches/3001")
        assertEquals(HttpStatusCode.Unauthorized, detailWithoutToken.status)

        val token = issueToken("viewer", "ViewerPass123!")
        val detailWithToken = client.get("/api/phases/1/matches/3001") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, detailWithToken.status)
        assertTrue(detailWithToken.bodyAsText().contains("THW Kiel"))
    }
}

