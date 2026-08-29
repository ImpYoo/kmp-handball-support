package de.exhumedo.kmp.handball_support.api

import de.exhumedo.kmp.handball_support.auth.AuthRoleDto
import de.exhumedo.kmp.handball_support.createTestAppConfig
import de.exhumedo.kmp.handball_support.createTestAuthUserStore
import de.exhumedo.kmp.handball_support.fixedClock
import de.exhumedo.kmp.handball_support.issueToken
import de.exhumedo.kmp.handball_support.module
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthAdminApiTest {

    @Test
    fun refereeCoachAdminCanCreateAndManageCoachRole() = testApplication {
        val authUserStore = createTestAuthUserStore(clock = fixedClock("2026-09-15T10:00:00Z"))

        application {
            module(
                appConfig = createTestAppConfig(),
                authUserStore = authUserStore,
                clock = fixedClock("2026-09-15T10:00:00Z"),
            )
        }

        val adminToken = issueToken("referee-coach-admin", "RefereeCoachAdminPass123!")

        // List users
        val listResponse = client.get("/api/auth/users") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(HttpStatusCode.OK, listResponse.status)
        assertTrue(listResponse.bodyAsText().contains("referee-coach-admin"))

        // Create a new coach
        val createBody = Json.encodeToString(
            JsonObject(
                mapOf(
                    "username" to JsonPrimitive("new-coach"),
                    "password" to JsonPrimitive("NewCoachPass123!"),
                    "role" to JsonPrimitive("COACH"),
                    "enabled" to JsonPrimitive(true),
                )
            )
        )
        val createResponse = client.post("/api/auth/users") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            setBody(createBody)
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        val createdText = createResponse.bodyAsText()
        assertTrue(createdText.contains("\"role\":\"COACH\""))

        // Update role back to viewer
        val updateBody = Json.encodeToString(
            JsonObject(
                mapOf(
                    "role" to JsonPrimitive("VIEWER"),
                    "enabled" to JsonPrimitive(true),
                )
            )
        )
        val updateResponse = client.put("/api/auth/users/new-coach") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            setBody(updateBody)
        }
        assertEquals(HttpStatusCode.OK, updateResponse.status)
        assertTrue(updateResponse.bodyAsText().contains("\"role\":\"VIEWER\""))

        // Delete user
        val deleteResponse = client.delete("/api/auth/users/new-coach") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)
    }

    @Test
    fun refereeCoachCannotAccessUserAdmin() = testApplication {
        val authUserStore = createTestAuthUserStore(clock = fixedClock("2026-09-15T10:00:00Z"))

        application {
            module(
                appConfig = createTestAppConfig(),
                authUserStore = authUserStore,
                clock = fixedClock("2026-09-15T10:00:00Z"),
            )
        }

        val token = issueToken("referee-coach", "RefereeCoachPass123!")
        val response = client.get("/api/auth/users") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun selfServicePasswordChangeWorksForRefereeCoach() = testApplication {
        val authUserStore = createTestAuthUserStore(clock = fixedClock("2026-09-15T10:00:00Z"))

        application {
            module(
                appConfig = createTestAppConfig(),
                authUserStore = authUserStore,
                clock = fixedClock("2026-09-15T10:00:00Z"),
            )
        }

        val token = issueToken("referee-coach", "RefereeCoachPass123!")
        val body = Json.encodeToString(
            JsonObject(
                mapOf(
                    "currentPassword" to JsonPrimitive("RefereeCoachPass123!"),
                    "newPassword" to JsonPrimitive("UpdatedRefereeCoachPass123!"),
                )
            )
        )
        val response = client.post("/api/auth/users/me/change-password") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(body)
        }
        assertEquals(HttpStatusCode.OK, response.status)

        val newToken = issueToken("referee-coach", "UpdatedRefereeCoachPass123!")
        assertTrue(newToken.isNotBlank())
    }
}
