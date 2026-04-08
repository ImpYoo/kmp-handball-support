package de.exhumedo.kmp.handball_support

import de.exhumedo.kmp.handball_support.persistence.JsonFilePerformanceEvaluationRepository
import io.ktor.client.request.delete
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

class AuthApiTest {

    @Test
    fun issuesJwtTokenForValidCredentials() = testApplication {
        application {
            module(
                appConfig = createTestAppConfig(),
                repository = JsonFilePerformanceEvaluationRepository(Files.createTempFile("performance-evaluations", ".json")),
                authUserStore = createTestAuthUserStore(),
                clock = fixedClock("2026-04-09T10:30:00Z"),
            )
        }

        val response = client.post("/api/auth/token") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"username":"referee","password":"RefereePass123!"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("accessToken"))
        assertTrue(body.contains("Bearer"))
        assertTrue(body.contains("referee"))
    }

    @Test
    fun rejectsInvalidCredentials() = testApplication {
        application {
            module(
                appConfig = createTestAppConfig(),
                repository = JsonFilePerformanceEvaluationRepository(Files.createTempFile("performance-evaluations", ".json")),
                authUserStore = createTestAuthUserStore(),
                clock = fixedClock("2026-04-09T10:30:00Z"),
            )
        }

        val response = client.post("/api/auth/token") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"username":"referee","password":"wrong"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(response.bodyAsText().contains("Invalid username or password"))
    }

    @Test
    fun rejectsProtectedRouteWithoutToken() = testApplication {
        application {
            module(
                appConfig = createTestAppConfig(),
                repository = JsonFilePerformanceEvaluationRepository(Files.createTempFile("performance-evaluations", ".json")),
                authUserStore = createTestAuthUserStore(),
                clock = fixedClock("2026-04-09T10:30:00Z"),
            )
        }

        val response = client.get("/api/performance-evaluations")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(response.bodyAsText().contains("Token is missing, invalid, or expired"))
    }

    @Test
    fun rejectsProtectedRouteWithInvalidToken() = testApplication {
        application {
            module(
                appConfig = createTestAppConfig(),
                repository = JsonFilePerformanceEvaluationRepository(Files.createTempFile("performance-evaluations", ".json")),
                authUserStore = createTestAuthUserStore(),
                clock = fixedClock("2026-04-09T10:30:00Z"),
            )
        }

        val response = client.get("/api/performance-evaluations") {
            header(HttpHeaders.Authorization, "Bearer not-a-valid-jwt")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(response.bodyAsText().contains("Token is missing, invalid, or expired"))
    }

    @Test
    fun rejectsWriteRouteForViewerRole() = testApplication {
        application {
            module(
                appConfig = createTestAppConfig(),
                repository = JsonFilePerformanceEvaluationRepository(Files.createTempFile("performance-evaluations", ".json")),
                authUserStore = createTestAuthUserStore(),
                clock = fixedClock("2026-04-09T10:30:00Z"),
            )
        }

        val token = issueToken("viewer", "ViewerPass123!")

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
                      "role": "FIRST_REFEREE"
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

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(response.bodyAsText().contains("not allowed"))
    }

    @Test
    fun adminCanCrudAuthUsersAndPasswordsAreStoredHashed() = testApplication {
        val authUsersFile = Files.createTempFile("auth-users", ".json")
        application {
            module(
                appConfig = createTestAppConfig(authUsersFile = authUsersFile),
                repository = JsonFilePerformanceEvaluationRepository(Files.createTempFile("performance-evaluations", ".json")),
                authUserStore = de.exhumedo.kmp.handball_support.persistence.auth.JsonFileAuthUserStore(
                    storagePath = authUsersFile,
                    clock = fixedClock("2026-04-09T10:30:00Z"),
                    bootstrapAdmin = de.exhumedo.kmp.handball_support.auth.BootstrapAdmin(
                        username = "admin",
                        password = "AdminPass123!",
                    ),
                ),
                clock = fixedClock("2026-04-09T10:30:00Z"),
            )
        }

        val adminToken = issueToken("admin", "AdminPass123!")

        val createResponse = client.post("/api/auth/users") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                """
                {
                  "username": "new.viewer",
                  "password": "ViewerPass123!",
                  "role": "VIEWER",
                  "enabled": true
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)
        val createdBody = createResponse.bodyAsText()
        assertTrue(createdBody.contains("new.viewer"))
        assertTrue(!createdBody.contains("passwordHash"))

        val storedJson = Files.readString(authUsersFile)
        assertTrue(storedJson.contains("pbkdf2_sha256"))
        assertTrue(!storedJson.contains("ViewerPass123!"))

        val listResponse = client.get("/api/auth/users") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(HttpStatusCode.OK, listResponse.status)
        assertTrue(listResponse.bodyAsText().contains("new.viewer"))

        val getResponse = client.get("/api/auth/users/new.viewer") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(HttpStatusCode.OK, getResponse.status)
        assertTrue(getResponse.bodyAsText().contains("VIEWER"))

        val updateResponse = client.put("/api/auth/users/new.viewer") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                """
                {
                  "password": "RefereePass123!",
                  "role": "REFEREE",
                  "enabled": true
                }
                """.trimIndent(),
            )
        }
        assertEquals(HttpStatusCode.OK, updateResponse.status)
        assertTrue(updateResponse.bodyAsText().contains("REFEREE"))

        val loginResponse = client.post("/api/auth/token") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"username":"new.viewer","password":"RefereePass123!"}""")
        }
        assertEquals(HttpStatusCode.OK, loginResponse.status)

        val deleteResponse = client.delete("/api/auth/users/new.viewer") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)
    }

    @Test
    fun rejectsRemovingLastEnabledAdmin() = testApplication {
        application {
            module(
                appConfig = createTestAppConfig(),
                repository = JsonFilePerformanceEvaluationRepository(Files.createTempFile("performance-evaluations", ".json")),
                authUserStore = de.exhumedo.kmp.handball_support.persistence.auth.JsonFileAuthUserStore(
                    storagePath = Files.createTempFile("auth-users", ".json"),
                    clock = fixedClock("2026-04-09T10:30:00Z"),
                    bootstrapAdmin = de.exhumedo.kmp.handball_support.auth.BootstrapAdmin(
                        username = "admin",
                        password = "AdminPass123!",
                    ),
                ),
                clock = fixedClock("2026-04-09T10:30:00Z"),
            )
        }

        val adminToken = issueToken("admin", "AdminPass123!")

        val response = client.delete("/api/auth/users/admin") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertTrue(response.bodyAsText().contains("last enabled admin"))
    }

    @Test
    fun throttlesRepeatedFailedLogins() = testApplication {
        application {
            module(
                appConfig = createTestAppConfig(),
                repository = JsonFilePerformanceEvaluationRepository(Files.createTempFile("performance-evaluations", ".json")),
                authUserStore = createTestAuthUserStore(),
                clock = fixedClock("2026-04-09T10:30:00Z"),
            )
        }

        repeat(5) {
            val response = client.post("/api/auth/token") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("""{"username":"referee","password":"wrong"}""")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        val throttledResponse = client.post("/api/auth/token") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"username":"referee","password":"wrong"}""")
        }

        assertEquals(HttpStatusCode.TooManyRequests, throttledResponse.status)
        assertEquals("900", throttledResponse.headers[HttpHeaders.RetryAfter])
        assertTrue(throttledResponse.bodyAsText().contains("temporarily throttled"))
    }

    @Test
    fun adminCanRevokeAnotherUsersTokens() = testApplication {
        val authUserStore = createTestAuthUserStore()
        application {
            module(
                appConfig = createTestAppConfig(),
                repository = JsonFilePerformanceEvaluationRepository(Files.createTempFile("performance-evaluations", ".json")),
                authUserStore = authUserStore,
                clock = fixedClock("2026-04-09T10:30:00Z"),
            )
        }

        val adminToken = issueToken("admin", "AdminPass123!")
        val viewerToken = issueToken("viewer", "ViewerPass123!")

        val revokeResponse = client.post("/api/auth/users/viewer/revoke-tokens") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(HttpStatusCode.OK, revokeResponse.status)
        assertTrue(revokeResponse.bodyAsText().contains("tokenInvalidBefore"))

        val protectedResponse = client.get("/api/performance-evaluations") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }
        assertEquals(HttpStatusCode.Unauthorized, protectedResponse.status)
        assertTrue(protectedResponse.bodyAsText().contains("Token is missing, invalid, or expired"))
    }
}
