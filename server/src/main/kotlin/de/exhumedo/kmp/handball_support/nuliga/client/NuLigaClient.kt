package de.exhumedo.kmp.handball_support.nuliga.client

import de.exhumedo.kmp.handball_support.nuliga.NuLigaClient
import de.exhumedo.kmp.handball_support.nuliga.NuLigaClientOptions
import de.exhumedo.kmp.handball_support.nuliga.NuLigaError
import de.exhumedo.kmp.handball_support.nuliga.NuLigaMatchPerson
import de.exhumedo.kmp.handball_support.nuliga.NuLigaOfficial
import de.exhumedo.kmp.handball_support.nuliga.NuLigaPlayer
import de.exhumedo.kmp.handball_support.nuliga.NuLigaResolvedMatch
import de.exhumedo.kmp.handball_support.nuliga.NuLigaResolvedTeam
import de.exhumedo.kmp.handball_support.nuliga.NuLigaResult
import de.exhumedo.kmp.handball_support.nuliga.NuLigaUrls
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException

/** Default engine for outbound nuLiga calls: bounded timeouts, no retries (stateless reads). */
internal fun defaultNuLigaHttpClient(): HttpClient = HttpClient {
    expectSuccess = false
}

// ── nuLiga (HBDE live) HTTP client ────────────────────────────────────────────
// Interface separates the contract from the engine — swap for a mock in tests.
// Mirrors the SportradarClient pattern in sportradar.client.
// ─────────────────────────────────────────────────────────────────────────────

internal class KtorNuLigaClient(
    private val http: HttpClient = defaultNuLigaHttpClient(),
    private val options: NuLigaClientOptions = NuLigaClientOptions(),
    /** Injectable so tests are deterministic; production uses the wall clock. */
    private val clock: () -> Instant = Instant::now,
) : NuLigaClient {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun resolveMeeting(meetingId: String): NuLigaResult<NuLigaResolvedMatch> {
        val bucket = currentBucket()

        val meetingResponse = get<NuMeeting>("${options.baseUrl}/meeting/$meetingId/time/$bucket")
            ?: return NuLigaResult.Failure(NuLigaError.NetworkError(IllegalStateException("no response")))
        return when (meetingResponse) {
            is NuLigaResult.Failure -> meetingResponse
            is NuLigaResult.Success -> {
                val meeting = meetingResponse.value
                val version = meeting.versionUid
                    ?: return NuLigaResult.Failure(NuLigaError.MissingVersion)

                // The three person lists all hang off the same version, so once it is known
                // they are read side by side; a failure in one cancels the others.
                coroutineScope {
                    val players = async { get<NuPersonList>("${options.baseUrl}/players/$meetingId/versions/$version") }
                    val officials = async { get<NuPersonList>("${options.baseUrl}/officials/$meetingId/versions/$version") }
                    val federationPeople = async { get<NuPersonList>("${options.baseUrl}/fedOfficials/$meetingId/versions/$version") }

                    val playerList = (players.await() as? NuLigaResult.Success)?.value?.meetingPersons
                        ?: return@coroutineScope NuLigaResult.Failure(NuLigaError.MissingVersion)
                    val officialList = (officials.await() as? NuLigaResult.Success)?.value?.meetingPersons
                        ?: return@coroutineScope NuLigaResult.Failure(NuLigaError.MissingVersion)
                    val federationList = (federationPeople.await() as? NuLigaResult.Success)?.value?.meetingPersons
                        ?: return@coroutineScope NuLigaResult.Failure(NuLigaError.MissingVersion)

                    NuLigaResult.Success(map(meetingId, meeting, playerList, officialList, federationList))
                }
            }
        }
    }

    /** Current epoch second floored to the 15-second bucket the nuLiga backend caches in. */
    private fun currentBucket(): Long = clock().epochSecond.let { it - (it % BUCKET_SECONDS) }

    private suspend inline fun <reified T> get(url: String): NuLigaResult<T>? {
        val response = try {
            http.get(url) {
                header("Accept", "application/json, text/plain, */*")
                header("Referer", options.referer)
            }
        } catch (e: HttpRequestTimeoutException) {
            return NuLigaResult.Failure(NuLigaError.NetworkError(e))
        } catch (e: IOException) {
            return NuLigaResult.Failure(NuLigaError.NetworkError(e))
        }
        if (!response.status.isSuccess()) {
            return NuLigaResult.Failure(NuLigaError.HttpError(response.status.value))
        }
        val body = response.bodyAsText()
        val parsed = try {
            json.decodeFromString<T>(body)
        } catch (e: Exception) {
            return NuLigaResult.Failure(NuLigaError.ParseError(e))
        }
        return NuLigaResult.Success(parsed)
    }

    // ── Mapping: upstream shape -> resolved match ─────────────────────────────

    private fun map(
        meetingId: String,
        meeting: NuMeeting,
        players: List<NuPerson>,
        officials: List<NuPerson>,
        federationPeople: List<NuPerson>,
    ): NuLigaResolvedMatch {
        val homePlayers = players.filter { it.teamHome == true }
        val awayPlayers = players.filter { it.teamHome == false }
        val homeOfficials = officials.filter { it.teamHome == true }
        val awayOfficials = officials.filter { it.teamHome == false }

        val homeName = meeting.teamHome ?: "Heim"
        val awayName = meeting.teamGuest ?: "Gast"

        // personType is nuLiga's own discriminator, transcribed from the nuScoreLive client's
        // enum. 3 is a referee, 5 the timekeeper, 6 the secretary; the remaining federation
        // functions are kept in `supervisors` instead of being folded into the referee or
        // table lists, which would misreport them. An unknown value stays UNKNOWN rather than
        // being silently classified as a referee.
        val referees = federationPeople.filter { it.roleCode() == ROLE_REFEREE }
            .mapIndexed { index, person -> person.toPerson(ROLE_REFEREE, index + 1) }
        val tableOfficials = federationPeople
            .filter { it.roleCode() == ROLE_TIMEKEEPER || it.roleCode() == ROLE_SECRETARY }
            .sortedBy { it.personType }
            .mapIndexed { index, person -> person.toPerson(person.roleCode(), index + 1) }
        val supervisors = federationPeople
            .filter { it.roleCode() in SUPERVISOR_ROLES }
            .sortedBy { it.personType }
            .mapIndexed { index, person -> person.toPerson(person.roleCode(), index + 1) }

        return NuLigaResolvedMatch(
            sourceMatchId = meetingId,
            scheduledAt = meeting.scheduled?.toInstantOrNull(),
            isLive = meeting.live == true,
            periods = DEFAULT_PERIODS,
            // Published per match; 30 is the IHF default if the field is missing.
            minutesPerPeriod = meeting.halftimeLength ?: DEFAULT_MINUTES_PER_PERIOD,
            competitionName = meeting.groupID?.let { "Gruppe $it" },
            venueName = meeting.courtHallName?.trim()?.takeIf { it.isNotEmpty() },
            home = NuLigaResolvedTeam(
                name = homeName,
                logoUrl = meeting.homeImgUrl,
                players = homePlayers.map { it.toPlayer() },
                officials = homeOfficials.map { it.toOfficial() },
            ),
            away = NuLigaResolvedTeam(
                name = awayName,
                logoUrl = meeting.guestImgUrl,
                players = awayPlayers.map { it.toPlayer() },
                officials = awayOfficials.map { it.toOfficial() },
            ),
            referees = referees,
            tableOfficials = tableOfficials,
            supervisors = supervisors,
        )
    }

    private fun NuPerson.toPlayer() = NuLigaPlayer(
        id = (personId ?: id)?.toString().orEmpty(),
        firstName = firstname.orEmpty(),
        lastName = lastname.orEmpty(),
        number = nr?.toIntOrNull(),
    )

    private fun NuPerson.toOfficial() = NuLigaOfficial(
        id = (personId ?: id)?.toString().orEmpty(),
        firstName = firstname.orEmpty(),
        lastName = lastname.orEmpty(),
        // nuLiga puts the IHF letter in `nr` for officials.
        badge = nr?.takeIf { it.isNotBlank() },
    )

    private fun NuPerson.toPerson(roleCode: String, position: Int) = NuLigaMatchPerson(
        id = (personId ?: id)?.toString().orEmpty(),
        firstName = firstname.orEmpty(),
        lastName = lastname.orEmpty(),
        roleCode = roleCode,
        position = position,
    )

    /** Maps the source discriminator onto a normalized role code, never guessing. */
    private fun NuPerson.roleCode(): String = when (personType) {
        PERSON_TYPE_REFEREE -> ROLE_REFEREE
        PERSON_TYPE_TIMEKEEPER -> ROLE_TIMEKEEPER
        PERSON_TYPE_SECRETARY -> ROLE_SECRETARY
        PERSON_TYPE_REFEREE_OBSERVER -> ROLE_REFEREE_OBSERVER
        PERSON_TYPE_SUPERVISOR -> ROLE_SUPERVISOR
        PERSON_TYPE_TECHNICAL_DELEGATE -> ROLE_TECHNICAL_DELEGATE
        else -> ROLE_UNKNOWN
    }

    /**
     * nuLiga returns `2026-09-13T14:00:00.000+0000`, which is not valid RFC 3339 (missing the
     * colon in the offset) and cannot be parsed by [OffsetDateTime]. Normalized here so the
     * API can emit a proper instant.
         */
    private fun String.toInstantOrNull(): Instant? = runCatching {
        val normalized = replace(Regex("([+-]\\d{2})(\\d{2})$"), "$1:$2")
        OffsetDateTime.parse(normalized).toInstant()
    }.getOrNull()

    private companion object {
        const val BUCKET_SECONDS = 15L

        /** nuLiga's person discriminator, transcribed from the `PersonType` enum in the
         *  nuScoreLive client bundle. */
        const val PERSON_TYPE_REFEREE = 3
        const val PERSON_TYPE_REFEREE_OBSERVER = 4
        const val PERSON_TYPE_TIMEKEEPER = 5
        const val PERSON_TYPE_SECRETARY = 6
        const val PERSON_TYPE_SUPERVISOR = 7
        const val PERSON_TYPE_TECHNICAL_DELEGATE = 8

        /** Normalized role codes, UPPER_SNAKE_CASE. */
        const val ROLE_REFEREE = "REFEREE"
        const val ROLE_TIMEKEEPER = "TIMEKEEPER"
        const val ROLE_SECRETARY = "SECRETARY"
        const val ROLE_REFEREE_OBSERVER = "REFEREE_OBSERVER"
        const val ROLE_SUPERVISOR = "SUPERVISOR"
        const val ROLE_TECHNICAL_DELEGATE = "TECHNICAL_DELEGATE"
        const val ROLE_UNKNOWN = "UNKNOWN"

        const val DEFAULT_PERIODS = 2
        const val DEFAULT_MINUTES_PER_PERIOD = 30

        /** Federation functions that are neither referees nor table officials. */
        val SUPERVISOR_ROLES = setOf(ROLE_REFEREE_OBSERVER, ROLE_SUPERVISOR, ROLE_TECHNICAL_DELEGATE)
    }
}

/** Wire models for the nuLiga REST backend. Nullable throughout: the payload is assembled
 *  from a live scoring system and any field may be absent before the match starts. */
@Serializable
internal data class NuMeeting(
    val meetingID: String? = null,
    val meetingUuid: String? = null,
    val teamHome: String? = null,
    val teamGuest: String? = null,
    val scheduled: String? = null,
    val groupID: String? = null,
    val courtHallName: String? = null,
    val homeImgUrl: String? = null,
    val guestImgUrl: String? = null,
    val live: Boolean? = null,
    val versionUid: String? = null,
    val halftimeLength: Int? = null,
)

@Serializable
internal data class NuPersonList(val meetingPersons: List<NuPerson> = emptyList())

@Serializable
internal data class NuPerson(
    val personId: Int? = null,
    val firstname: String? = null,
    val lastname: String? = null,
    /** Shirt number for players; the IHF letter A–E for officials. */
    val nr: String? = null,
    /** 0 player, 2 team official, 3/4 referee, 5 timekeeper, 6 secretary. */
    val personType: Int? = null,
    val teamHome: Boolean? = null,
    val id: Int? = null,
)