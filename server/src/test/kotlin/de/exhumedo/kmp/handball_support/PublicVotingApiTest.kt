package de.exhumedo.kmp.handball_support

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

class PublicVotingApiTest {

    @Test
    fun phasesMatchesAndVoteSubmissionWorkWithoutToken() = testApplication {
        val storageFile = Files.createTempFile("public-votes", ".json")
        application {
            module(
                appConfig = createTestAppConfig(evaluationsFile = storageFile),
                repository = JsonFilePerformanceEvaluationRepository(storageFile),
                authUserStore = createTestAuthUserStore(),
                clock = fixedClock("2026-05-10T10:30:00Z"),
            )
        }

        val phasesResponse = client.get("/api/phases")
        assertEquals(HttpStatusCode.OK, phasesResponse.status)
        assertTrue(phasesResponse.bodyAsText().startsWith("["))

        val matchesResponse = client.get("/api/phases/1/matches")
        assertEquals(HttpStatusCode.OK, matchesResponse.status)
        assertTrue(matchesResponse.bodyAsText().startsWith("["))

        val matchResponse = client.get("/api/phases/1/matches/3001")
        assertEquals(HttpStatusCode.Unauthorized, matchResponse.status)

        val voteResponse = client.post("/api/performance-evaluations") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                """
                {
                  "game": {
                    "gameId": "G-PUBLIC-001",
                    "date": "2026-04-06",
                    "homeTeam": "THW Kiel",
                    "awayTeam": "SG Flensburg",
                    "venue": "Sparkassen-Arena"
                  },
                  "evaluator": {
                    "type": "REFEREE_TEAM",
                    "refereePair": {
                      "firstReferee": {
                        "person": { "id": "R1", "firstName": "Max", "lastName": "Mueller" },
                        "role": "FIRST_REFEREE"
                      },
                      "secondReferee": {
                        "person": { "id": "R2", "firstName": "Anna", "lastName": "Schmidt" },
                        "role": "SECOND_REFEREE"
                      }
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
                    "appearance": 4,
                    "influence": 5,
                    "teamwork": 3
                  },
                  "comment": "Public vote"
                }
                """.trimIndent(),
            )
        }
        assertEquals(HttpStatusCode.Created, voteResponse.status)
        assertTrue(voteResponse.bodyAsText().contains("G-PUBLIC-001"))
    }
}


