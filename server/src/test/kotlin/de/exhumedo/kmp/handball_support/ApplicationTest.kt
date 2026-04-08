package de.exhumedo.kmp.handball_support

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        application {
            module(
                appConfig = createTestAppConfig(),
                authUserStore = createTestAuthUserStore(),
            )
        }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Ktor: ${Greeting().greet()}", response.bodyAsText())
    }

    @Test
    fun corsPreflightAllowsConfiguredOrigin() = testApplication {
        application {
            module(
                appConfig = createTestAppConfig().copy(
                    http = de.exhumedo.kmp.handball_support.config.HttpConfig(
                        corsAllowedOrigins = setOf("https://app.example.com"),
                    ),
                ),
                authUserStore = createTestAuthUserStore(),
            )
        }

        val response = client.options("/api/performance-evaluations") {
            header(HttpHeaders.Origin, "https://app.example.com")
            header(HttpHeaders.AccessControlRequestMethod, "GET")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("https://app.example.com", response.headers[HttpHeaders.AccessControlAllowOrigin])
    }
}
