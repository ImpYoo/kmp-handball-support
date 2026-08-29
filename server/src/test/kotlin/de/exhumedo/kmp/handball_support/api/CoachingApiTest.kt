package de.exhumedo.kmp.handball_support.api

import de.exhumedo.kmp.handball_support.createTestAppConfig
import de.exhumedo.kmp.handball_support.createTestAuthUserStore
import de.exhumedo.kmp.handball_support.fixedClock
import de.exhumedo.kmp.handball_support.issueToken
import de.exhumedo.kmp.handball_support.module
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoachingApiTest {

    @Test
    fun catalogIsPublicAndCreateRetrieveReportFlowWorks() = testApplication {
        val coachingDb = Files.createTempFile("coaching-evaluations", ".sqlite")
        val authUsersFile = Files.createTempFile("auth-users", ".json")
        val authUserStore = createTestAuthUserStore(clock = fixedClock("2026-09-15T10:00:00Z"))

        application {
            module(
                appConfig = createTestAppConfig(
                    authUsersFile = authUsersFile,
                    coachingDb = coachingDb,
                ),
                authUserStore = authUserStore,
                clock = fixedClock("2026-09-15T10:00:00Z"),
            )
        }

        // Public catalog
        val catalogResponse = client.get("/api/coaching/catalog")
        assertEquals(HttpStatusCode.OK, catalogResponse.status)
        val catalogBody = catalogResponse.bodyAsText()
        assertTrue(catalogBody.contains("a1-spielgedanke-vorteil"))

        val token = issueToken("coach", "CoachPass123!")

        // Create
        val createResponse = client.post("/api/coaching/evaluations") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(sampleEvaluationRequest())
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        val createdBody = createResponse.bodyAsText()
        assertTrue(createdBody.contains("totalScore"))
        assertTrue(createdBody.contains("percentage"))

        val savedId = """"id"\s*:\s*"([^"]+)"""".toRegex()
            .find(createdBody)
            ?.groupValues
            ?.get(1)
            ?: error("No id found in response body: $createdBody")

        // Get
        val getResponse = client.get("/api/coaching/evaluations/$savedId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, getResponse.status)
        assertTrue(getResponse.bodyAsText().contains("G-COACH-001"))

        // Report
        val reportResponse = client.get("/api/coaching/evaluations/$savedId/report") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, reportResponse.status)
        val reportBody = reportResponse.bodyAsText()
        assertTrue(reportBody.contains("evaluationId"))
        assertTrue(reportBody.contains("rows"))
    }

    @Test
    fun coachCannotCreateEvaluationForAnotherEvaluator() = testApplication {
        val coachingDb = Files.createTempFile("coaching-evaluations", ".sqlite")
        val authUserStore = createTestAuthUserStore(clock = fixedClock("2026-09-15T10:00:00Z"))

        application {
            module(
                appConfig = createTestAppConfig(coachingDb = coachingDb),
                authUserStore = authUserStore,
                clock = fixedClock("2026-09-15T10:00:00Z"),
            )
        }

        val token = issueToken("coach", "CoachPass123!")

        val response = client.post("/api/coaching/evaluations") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(
                sampleEvaluationRequest()
                    .replace("\"evaluatorUsername\": \"coach\"", "\"evaluatorUsername\": \"someone-else\"")
            )
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun viewerCannotCreateEvaluation() = testApplication {
        val coachingDb = Files.createTempFile("coaching-evaluations", ".sqlite")
        val authUserStore = createTestAuthUserStore(clock = fixedClock("2026-09-15T10:00:00Z"))

        application {
            module(
                appConfig = createTestAppConfig(coachingDb = coachingDb),
                authUserStore = authUserStore,
                clock = fixedClock("2026-09-15T10:00:00Z"),
            )
        }

        val token = issueToken("viewer", "ViewerPass123!")

        val response = client.post("/api/coaching/evaluations") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(sampleEvaluationRequest())
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun updateReplacesExistingEvaluation() = testApplication {
        val coachingDb = Files.createTempFile("coaching-evaluations", ".sqlite")
        val authUserStore = createTestAuthUserStore(clock = fixedClock("2026-09-15T10:00:00Z"))

        application {
            module(
                appConfig = createTestAppConfig(coachingDb = coachingDb),
                authUserStore = authUserStore,
                clock = fixedClock("2026-09-15T10:00:00Z"),
            )
        }

        val token = issueToken("coach", "CoachPass123!")

        val createResponse = client.post("/api/coaching/evaluations") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(sampleEvaluationRequest())
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        val savedId = """"id"\s*:\s*"([^"]+)"""".toRegex()
            .find(createResponse.bodyAsText())
            ?.groupValues
            ?.get(1)
            ?: error("No id found")

        val updateResponse = client.put("/api/coaching/evaluations/$savedId") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(sampleEvaluationRequest(comment = "Updated comment"))
        }
        assertEquals(HttpStatusCode.OK, updateResponse.status)
        assertTrue(updateResponse.bodyAsText().contains("Updated comment"))
    }

    private fun sampleEvaluationRequest(comment: String = "Initial comment"): String = """
        {
          "game": {
            "gameId": "G-COACH-001",
            "matchDate": "2026-09-15",
            "homeTeam": "THW Kiel",
            "awayTeam": "SC Magdeburg"
          },
          "evaluatorUsername": "coach",
          "firstReferee": { "personId": "R1", "firstName": "Max", "lastName": "Mustermann" },
          "secondReferee": { "personId": "R2", "firstName": "Anna", "lastName": "Schmidt" },
          "rootCauseCounts": {
            "a1-spielgedanke-vorteil": {
              "a1-spielverstaendnis": { "a1-schneller-anwurf": 3 }
            }
          },
          "comment": "$comment"
        }
    """.trimIndent()
}
