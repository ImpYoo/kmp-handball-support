package de.exhumedo.kmp.handball_support.nuliga

/**
 * Reads a match from **nuLiga** (HBDE live), e.g.
 * `https://hbde-live.liga.nu/nuScoreLive/#/groups/489670/meetings/8278820`.
 *
 * See [KtorNuLigaClient][de.exhumedo.kmp.handball_support.nuliga.client.KtorNuLigaClient] for
 * the upstream semantics (15-second buckets, version-pinned roster reads, personType roles).
 */
interface NuLigaClient {
    /** Resolves one meeting by its id. The service extracts the id from a pasted URL. */
    suspend fun resolveMeeting(meetingId: String): NuLigaResult<NuLigaResolvedMatch>

    companion object {
        const val SOURCE_ID = "nuliga"
        const val SAMPLE_URL = "https://hbde-live.liga.nu/nuScoreLive/#/groups/489670/meetings/8278820"
    }
}

/** URL building for external HTTP calls. */
data class NuLigaClientOptions(
    val baseUrl: String = "https://hbde-live.liga.nu/nuScoreLiveRestBackend/api/1",
    /** The public site the backend expects as Referer; helps identify the API client upstream. */
    val referer: String = "https://hbde-live.liga.nu/nuScoreLive/",
)