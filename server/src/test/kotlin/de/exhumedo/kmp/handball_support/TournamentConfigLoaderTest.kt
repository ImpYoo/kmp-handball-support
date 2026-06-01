package de.exhumedo.kmp.handball_support

import de.exhumedo.kmp.handball_support.sportradar.config.TournamentConfigLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TournamentConfigLoaderTest {

    @Test
    fun rejectsDuplicateTournamentSeasonPairs() {
        val json = """
            [
              {"tournamentId": 921, "seasonId": 33765, "isHbl": true},
              {"tournamentId": 921, "seasonId": 33765, "isHbl": true}
            ]
        """.trimIndent()

        val error = assertFailsWith<IllegalArgumentException> {
            TournamentConfigLoader.fromJson(json)
        }

        assertTrue(error.message.orEmpty().contains("duplicate"))
    }

    @Test
    fun rejectsNonPositiveIds() {
        val json = """
            [
              {"tournamentId": 0, "seasonId": 33765, "isHbl": true}
            ]
        """.trimIndent()

        val error = assertFailsWith<IllegalArgumentException> {
            TournamentConfigLoader.fromJson(json)
        }

        assertTrue(error.message.orEmpty().contains("tournamentId must be > 0"))
    }

    @Test
    fun parsesValidTournamentConfigs() {
        val json = """
            [
              {"tournamentId": 921, "seasonId": 33765, "isHbl": true, "label": "HBL"},
              {"tournamentId": 16059, "seasonId": 35106, "isHbl": true, "label": "3. Liga"}
            ]
        """.trimIndent()

        val configs = TournamentConfigLoader.fromJson(json)

        assertEquals(2, configs.size)
        assertEquals(921, configs.first().tournamentId)
        assertEquals(35106, configs.last().seasonId)
    }
}

