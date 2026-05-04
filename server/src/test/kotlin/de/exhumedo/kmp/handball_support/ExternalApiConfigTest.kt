package de.exhumedo.kmp.handball_support

import de.exhumedo.kmp.handball_support.config.ExternalApiConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExternalApiConfigTest {

    @Test
    fun defaultConfigIsDisabled() {
        val config = ExternalApiConfig()
        assertEquals(false, config.enabled)
        assertEquals("https://hbl.fmp.sportradar.com", config.baseUrl)
        assertEquals("en", config.language)
        assertEquals("Europe:Berlin", config.timeZone)
        assertEquals("gismo", config.product)
        assertEquals(5_000L, config.requestTimeoutMillis)
        assertEquals(3_000L, config.connectTimeoutMillis)
    }

    @Test
    fun customConfigWithBaseUrl() {
        val config = ExternalApiConfig(
            enabled = true,
            baseUrl = "https://custom.sportradar.api",
            language = "de",
            timeZone = "Europe:Paris",
            product = "gismo-premium",
            apiKey = "my-secret-key",
            apiKeyQueryParamName = "auth_token",
            sendApiKeyAsQueryParam = true,
            apiKeyHeaderName = "Authorization",
            requestTimeoutMillis = 10_000,
            connectTimeoutMillis = 5_000,
        )
        assertEquals(true, config.enabled)
        assertEquals("https://custom.sportradar.api", config.baseUrl)
        assertEquals("de", config.language)
        assertEquals("Europe:Paris", config.timeZone)
        assertEquals("gismo-premium", config.product)
        assertEquals("my-secret-key", config.apiKey)
        assertEquals("auth_token", config.apiKeyQueryParamName)
        assertEquals(true, config.sendApiKeyAsQueryParam)
        assertEquals("Authorization", config.apiKeyHeaderName)
        assertEquals(10_000L, config.requestTimeoutMillis)
        assertEquals(5_000L, config.connectTimeoutMillis)
    }

    @Test
    fun configValidatesRequiredBaseUrlWhenEnabled() {
        val config = ExternalApiConfig(
            enabled = true,
            baseUrl = "",  // Empty base URL
        )
        val error = assertFailsWith<IllegalArgumentException> {
            validateExternalApiConfig(config)
        }
        assertEquals(true, error.message?.contains("EXTERNAL_API_BASE_URL is required") ?: false)
    }

    @Test
    fun configValidatesBaseUrlHttpsPrefix() {
        val config = ExternalApiConfig(
            enabled = true,
            baseUrl = "ftp://invalid-protocol.com",  // Invalid protocol
        )
        val error = assertFailsWith<IllegalArgumentException> {
            validateExternalApiConfig(config)
        }
        assertEquals(true, error.message?.contains("http:// or https://"))
    }

    @Test
    fun configValidatesPositiveTimeouts() {
        val config = ExternalApiConfig(
            enabled = true,
            baseUrl = "https://api.example.com",
            requestTimeoutMillis = -1,  // Invalid
        )
        val error = assertFailsWith<IllegalArgumentException> {
            validateExternalApiConfig(config)
        }
        assertEquals(true, error.message?.contains("greater than zero"))
    }

    @Test
    fun configAllowsDisabledWithoutBaseUrl() {
        val config = ExternalApiConfig(
            enabled = false,
            baseUrl = "",  // Can be empty when disabled
        )
        // Should not throw
        validateExternalApiConfig(config)
    }

    private fun validateExternalApiConfig(config: ExternalApiConfig) {
        require(config.apiKeyHeaderName.isNotBlank()) {
            "EXTERNAL_API_KEY_HEADER must not be blank."
        }
        require(config.requestTimeoutMillis > 0) {
            "EXTERNAL_API_REQUEST_TIMEOUT_MS must be greater than zero."
        }
        require(config.connectTimeoutMillis > 0) {
            "EXTERNAL_API_CONNECT_TIMEOUT_MS must be greater than zero."
        }
        if (config.enabled) {
            require(config.baseUrl.isNotBlank()) {
                "EXTERNAL_API_BASE_URL is required when EXTERNAL_API_ENABLED=true."
            }
            require(
                config.baseUrl.startsWith("http://") ||
                    config.baseUrl.startsWith("https://"),
            ) {
                "EXTERNAL_API_BASE_URL must start with http:// or https://."
            }
        }
    }
}



