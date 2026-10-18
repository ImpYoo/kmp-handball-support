package de.exhumedo.kmp.handball_support.nuliga

import java.net.URI

/**
 * Small URL helpers for the nuLiga match source.
 *
 * Pasted URLs arrive with fragments (`#/groups/489670/meetings/8278820`), tracking query
 * parameters, trailing slashes and mixed case hostnames. Normalizing in one place keeps the
 * source focused on its payload shape. Ported from the predecessor's SourceUrls.
 */
internal object NuLigaUrls {

    /** Parses a user-supplied URL; returns null when it is not an absolute http(s) URL with a host. */
    fun parseOrNull(raw: String): URI? {
        val candidate = raw.trim()
        if (candidate.isEmpty()) return null
        val uri = runCatching { URI(candidate) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return null
        if (uri.host.isNullOrBlank()) return null
        return uri
    }

    /** Lower-case hostname without a leading `www.`, for hostname comparison. */
    fun normalizedHost(uri: URI): String = (uri.host ?: "").lowercase().removePrefix("www.")

    /** Path segments with empty segments removed. */
    fun pathSegments(uri: URI): List<String> = (uri.path ?: "").split('/').filter { it.isNotBlank() }

    /**
     * The fragment as path segments. liga.nu addresses a match as
     * `#/groups/489670/meetings/8278820`, so the fragment *is* the route there.
     */
    fun fragmentSegments(uri: URI): List<String> = (uri.fragment ?: "").split('/').filter { it.isNotBlank() }

    /** The segment immediately following [marker], or null when the marker is absent. */
    fun segmentAfter(segments: List<String>, marker: String): String? {
        val index = segments.indexOfFirst { it.equals(marker, ignoreCase = true) }
        if (index < 0) return null
        return segments.getOrNull(index + 1)?.takeIf { it.isNotBlank() }
    }
}