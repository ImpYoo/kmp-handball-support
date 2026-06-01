package de.exhumedo.kmp.handball_support

import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SportradarRoutingTest {

    @Test
    fun refreshRequiresAuthentication() = testApplication {
        application {
            module(
                appConfig = createTestAppConfig(),
                authUserStore = createTestAuthUserStore(),
            )
        }

        val response = client.post("/api/sportradar/refresh") {
            header(HttpHeaders.Accept, ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(response.bodyAsText().contains("Unauthorized"))
    }

    @Test
    fun refreshReturnsDisabledWhenExternalApiIsOff() = testApplication {
        application {
            module(
                appConfig = createTestAppConfig(),
                authUserStore = createTestAuthUserStore(),
            )
        }

        val token = issueToken(username = "admin", password = "AdminPass123!")
        val response = client.post("/api/sportradar/refresh") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Accept, ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertTrue(response.bodyAsText().contains("DISABLED"))
    }
}


