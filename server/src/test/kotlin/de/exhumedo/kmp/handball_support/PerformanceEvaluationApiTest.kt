package de.exhumedo.kmp.handball_support

import de.exhumedo.kmp.handball_support.domain.rating.repository.PerformanceEvaluationRepository
import de.exhumedo.kmp.handball_support.persistence.JsonFilePerformanceEvaluationRepository
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
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

class PerformanceEvaluationApiTest {

    @Test
    fun healthEndpointRespondsSuccessfully() = testApplication {
        application {
            module(
                appConfig = createTestAppConfig(),
                repository = JsonFilePerformanceEvaluationRepository(Files.createTempFile("performance-evaluations", ".json")),
                authUserStore = createTestAuthUserStore(),
                clock = fixedClock("2026-04-09T10:30:00Z"),
            )
        }

        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("UP"))
    }

    @Test
    fun createsAndRetrievesEvaluation() = testApplication {
        val storageFile = Files.createTempFile("performance-evaluations", ".json")
        val repository = JsonFilePerformanceEvaluationRepository(storageFile)

        application {
            module(
                appConfig = createTestAppConfig(evaluationsFile = storageFile),
                repository = repository,
                authUserStore = createTestAuthUserStore(),
                clock = fixedClock("2026-04-09T10:30:00Z"),
            )
        }

        val token = issueToken("referee", "RefereePass123!")

        val response = client.post("/api/performance-evaluations") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(baseEvaluationRequest())
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val createdBody = response.bodyAsText()
        assertTrue(createdBody.contains("2026-04-09T10:30:00Z"))
        assertTrue(createdBody.contains("weightedTotalScore"))
        assertTrue(createdBody.contains("24"))

        val savedId = """"id"\s*:\s*"([^"]+)"""".toRegex()
            .find(createdBody)
            ?.groupValues
            ?.get(1)
            ?: error("No id found in response body: $createdBody")

        val getResponse = client.get("/api/performance-evaluations/$savedId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, getResponse.status)
        assertTrue(getResponse.bodyAsText().contains("G-001"))
    }

    @Test
    fun listsAllEvaluationsAndFiltersByGameAndRefereePair() = testApplication {
        val storageFile = Files.createTempFile("performance-evaluations", ".json")
        val repository = JsonFilePerformanceEvaluationRepository(storageFile)

        application {
            module(
                appConfig = createTestAppConfig(evaluationsFile = storageFile),
                repository = repository,
                authUserStore = createTestAuthUserStore(),
                clock = fixedClock("2026-04-09T10:30:00Z"),
            )
        }

        val token = issueToken("admin", "AdminPass123!")

        val firstCreate = client.post("/api/performance-evaluations") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(createEvaluationRequest(gameId = "G-001", firstRefereeId = "R1", secondRefereeId = "R2"))
        }
        assertEquals(HttpStatusCode.Created, firstCreate.status)

        val secondCreate = client.post("/api/performance-evaluations") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(createEvaluationRequest(gameId = "G-002", firstRefereeId = "R3", secondRefereeId = "R4"))
        }
        assertEquals(HttpStatusCode.Created, secondCreate.status)

        val allResponse = client.get("/api/performance-evaluations") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, allResponse.status)
        assertTrue(allResponse.bodyAsText().contains("G-001"))
        assertTrue(allResponse.bodyAsText().contains("G-002"))

        val byGameResponse = client.get("/api/performance-evaluations?gameId=G-002") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, byGameResponse.status)
        assertTrue(byGameResponse.bodyAsText().contains("G-002"))

        val byRefPairResponse = client.get("/api/performance-evaluations?firstRefereeId=R1&secondRefereeId=R2") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, byRefPairResponse.status)
        val byRefPairBody = byRefPairResponse.bodyAsText()
        assertTrue(byRefPairBody.contains("G-001"))
        assertTrue(!byRefPairBody.contains("G-002"))
    }

    @Test
    fun returnsNotFoundForMissingEvaluation() = testApplication {
        application {
            module(
                appConfig = createTestAppConfig(),
                repository = JsonFilePerformanceEvaluationRepository(Files.createTempFile("performance-evaluations", ".json")),
                authUserStore = createTestAuthUserStore(),
                clock = fixedClock("2026-04-09T10:30:00Z"),
            )
        }

        val token = issueToken("viewer", "ViewerPass123!")

        val byIdResponse = client.get("/api/performance-evaluations/missing-id") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.NotFound, byIdResponse.status)
        assertTrue(byIdResponse.bodyAsText().contains("Not Found"))

        val byGameResponse = client.get("/api/performance-evaluations?gameId=missing-game") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.NotFound, byGameResponse.status)
        assertTrue(byGameResponse.bodyAsText().contains("No evaluation found for game"))
    }

    @Test
    fun rejectsIncompleteRefereePairFilterRequest() = testApplication {
        application {
            module(
                appConfig = createTestAppConfig(),
                repository = JsonFilePerformanceEvaluationRepository(Files.createTempFile("performance-evaluations", ".json")),
                authUserStore = createTestAuthUserStore(),
                clock = fixedClock("2026-04-09T10:30:00Z"),
            )
        }

        val token = issueToken("admin", "AdminPass123!")

        val response = client.get("/api/performance-evaluations?firstRefereeId=R1") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Both firstRefereeId and secondRefereeId are required together."))
    }

    @Test
    fun rejectsInvalidDomainRequestWithProblemResponse() = testApplication {
        application {
            module(
                appConfig = createTestAppConfig(),
                repository = JsonFilePerformanceEvaluationRepository(Files.createTempFile("performance-evaluations", ".json")),
                authUserStore = createTestAuthUserStore(),
                clock = fixedClock("2026-04-09T10:30:00Z"),
            )
        }

        val token = issueToken("referee", "RefereePass123!")

        val response = client.post("/api/performance-evaluations") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(
                """
                {
                  "game": {
                    "gameId": "G-001",
                    "date": "2026-04-06",
                    "homeTeam": "THW Kiel",
                    "awayTeam": "SG Flensburg",
                    "venue": "Sparkassen-Arena"
                  },
                  "refereePair": {
                    "firstReferee": {
                      "person": { "id": "R1", "firstName": "Max", "lastName": "Mueller" },
                      "role": "SECOND_REFEREE"
                    },
                    "secondReferee": {
                      "person": { "id": "R2", "firstName": "Anna", "lastName": "Schmidt" },
                      "role": "SECOND_REFEREE"
                    }
                  },
                  "tableOfficialTeam": {
                    "timeKeeper": {
                      "person": { "id": "T1", "firstName": "Jan", "lastName": "Weber" },
                      "role": "TIME_KEEPER"
                    },
                    "scoreKeeper": {
                      "person": { "id": "T2", "firstName": "Lisa", "lastName": "Koch" },
                      "role": "SCORE_KEEPER"
                    }
                  },
                  "score": {
                    "appearance": 8,
                    "influence": 7,
                    "teamwork": 9
                  }
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("Domain Validation Failed") || body.contains("Expected role FirstReferee"))
        assertTrue(body.contains("Expected role FirstReferee"))
    }

    @Test
    fun rejectsSecondEvaluationForSameGame() = testApplication {
        val storageFile = Files.createTempFile("performance-evaluations", ".json")
        val repository: PerformanceEvaluationRepository = JsonFilePerformanceEvaluationRepository(storageFile)

        application {
            module(
                appConfig = createTestAppConfig(evaluationsFile = storageFile),
                repository = repository,
                authUserStore = createTestAuthUserStore(),
                clock = fixedClock("2026-04-09T10:30:00Z"),
            )
        }

        val token = issueToken("admin", "AdminPass123!")
        val body = baseEvaluationRequest()

        val first = client.post("/api/performance-evaluations") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(body)
        }
        assertEquals(HttpStatusCode.Created, first.status)

        val second = client.post("/api/performance-evaluations") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(body)
        }
        assertEquals(HttpStatusCode.Conflict, second.status)
        assertTrue(second.bodyAsText().contains("Conflict"))
    }

    private fun baseEvaluationRequest(): String {
        return createEvaluationRequest(
            gameId = "G-001",
            firstRefereeId = "R1",
            secondRefereeId = "R2",
        )
    }

    private fun createEvaluationRequest(
        gameId: String,
        firstRefereeId: String,
        secondRefereeId: String,
    ): String {
        return """
            {
              "game": {
                "gameId": "$gameId",
                "date": "2026-04-06",
                "homeTeam": "THW Kiel",
                "awayTeam": "SG Flensburg",
                "venue": "Sparkassen-Arena"
              },
              "refereePair": {
                "firstReferee": {
                  "person": { "id": "$firstRefereeId", "firstName": "Max", "lastName": "Mueller" },
                  "role": "FIRST_REFEREE"
                },
                "secondReferee": {
                  "person": { "id": "$secondRefereeId", "firstName": "Anna", "lastName": "Schmidt" },
                  "role": "SECOND_REFEREE"
                }
              },
              "tableOfficialTeam": {
                "timeKeeper": {
                  "person": { "id": "T1-$gameId", "firstName": "Jan", "lastName": "Weber" },
                  "role": "TIME_KEEPER"
                },
                "scoreKeeper": {
                  "person": { "id": "T2-$gameId", "firstName": "Lisa", "lastName": "Koch" },
                  "role": "SCORE_KEEPER"
                }
              },
              "score": {
                "appearance": 8,
                "influence": 7,
                "teamwork": 9
              },
              "comment": "Solid performance"
            }
        """.trimIndent()
    }
}
