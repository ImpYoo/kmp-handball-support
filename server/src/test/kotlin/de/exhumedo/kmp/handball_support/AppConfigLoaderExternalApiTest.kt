package de.exhumedo.kmp.handball_support

import de.exhumedo.kmp.handball_support.config.AppConfigLoader
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class AppConfigLoaderExternalApiTest {

    @Test
    fun enabledExternalApiRequiresApiKey() {
        val env = Files.createTempFile("app-config", ".env")
        Files.writeString(
            env,
            """
            APP_DEVELOPMENT=true
            EXTERNAL_API_ENABLED=true
            EXTERNAL_API_BASE_URL=https://hbl.fmp.sportradar.com
            EXTERNAL_API_TOURNAMENTS_FILE=server/data/tournaments.json
            EXTERNAL_API_KEY=
            """.trimIndent(),
        )

        val error = assertFailsWith<IllegalArgumentException> {
            AppConfigLoader.load(env)
        }
        assertTrue(error.message.orEmpty().contains("EXTERNAL_API_KEY is required"))
    }

    @Test
    fun enabledExternalApiRejectsPlaceholderApiKey() {
        val env = Files.createTempFile("app-config", ".env")
        Files.writeString(
            env,
            """
            APP_DEVELOPMENT=true
            EXTERNAL_API_ENABLED=true
            EXTERNAL_API_BASE_URL=https://hbl.fmp.sportradar.com
            EXTERNAL_API_TOURNAMENTS_FILE=server/data/tournaments.json
            EXTERNAL_API_KEY=YOUR_API_KEY_HERE
            """.trimIndent(),
        )

        val error = assertFailsWith<IllegalArgumentException> {
            AppConfigLoader.load(env)
        }
        assertTrue(error.message.orEmpty().contains("placeholder"), error.message.orEmpty())
    }
}

