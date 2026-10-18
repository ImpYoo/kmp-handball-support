package de.exhumedo.kmp.handball_support.nuliga

/**
 * Extracts a nuLiga meeting id from a pasted URL and resolves the match through the client.
 *
 * The public nuScoreLive URL is a hash route
 * (`https://hbde-live.liga.nu/nuScoreLive/#/groups/489670/meetings/8278820`), so the meeting id
 * usually lives in the fragment; a hand-written API-style URL with the id in the path works
 * too. Only URLs on liga.nu (or a subdomain) are accepted.
 */
class NuLigaApplicationService(private val client: NuLigaClient) {

    /** Thrown when the URL cannot yield a meeting id; mapped to a 400 problem by the route. */
    class InvalidMatchUrlException(message: String) : Exception(message)

    /** Whether the URL belongs to nuLiga at all. */
    fun accepts(url: String): Boolean {
        val uri = NuLigaUrls.parseOrNull(url) ?: return false
        val host = NuLigaUrls.normalizedHost(uri)
        return host == "liga.nu" || host.endsWith(".liga.nu")
    }

    /** The canonical meeting id, or a [InvalidMatchUrlException] the caller can show verbatim. */
    fun meetingIdFrom(url: String): String {
        if (url.isBlank()) {
            throw InvalidMatchUrlException("A url is required.")
        }
        val uri = NuLigaUrls.parseOrNull(url)
            ?: throw InvalidMatchUrlException("Only absolute http and https URLs can be resolved, and the url needs a host.")
        if (!accepts(url)) {
            throw InvalidMatchUrlException(
                "This is not a nuLiga URL. Pass a live match URL such as ${NuLigaClient.SAMPLE_URL}.",
            )
        }
        val segments = NuLigaUrls.pathSegments(uri) + NuLigaUrls.fragmentSegments(uri)
        val id = NuLigaUrls.segmentAfter(segments, "meetings")
            ?: NuLigaUrls.segmentAfter(segments, "meeting")
            ?: throw InvalidMatchUrlException(
                "Could not determine a meeting id from $url. Expected a URL such as ${NuLigaClient.SAMPLE_URL}.",
            )
        if (!id.matches(Regex("[0-9]+"))) {
            throw InvalidMatchUrlException(
                "nuLiga meeting ids are numeric; '$id' is not. Pass a live match URL such as ${NuLigaClient.SAMPLE_URL}.",
            )
        }
        return id
    }

    suspend fun resolve(url: String): NuLigaResult<NuLigaResolvedMatch> =
        client.resolveMeeting(meetingIdFrom(url))
}