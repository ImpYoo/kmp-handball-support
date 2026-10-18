package de.exhumedo.kmp.handball_support.nuliga

// ── Typed errors propagated from the nuLiga client ───────────────────────────
// Callers handle via NuLigaResult; nothing is silently swallowed.
// Mirrors the SportradarError/SportradarResult pair in sportradar.error.
// ─────────────────────────────────────────────────────────────────────────────

sealed interface NuLigaError {
    /** nuLiga answered, but without the versionUid that pins all rosters to one revision. */
    data object MissingVersion : NuLigaError

    /** HTTP layer returned a non-2xx status. */
    data class HttpError(val statusCode: Int) : NuLigaError

    /** Network-level failure (DNS, timeout, connection refused, …) */
    data class NetworkError(val cause: Throwable) : NuLigaError

    /** JSON could be fetched but failed to deserialize. */
    data class ParseError(val cause: Throwable) : NuLigaError
}

sealed interface NuLigaResult<out T> {
    data class Success<T>(val value: T) : NuLigaResult<T>
    data class Failure(val error: NuLigaError) : NuLigaResult<Nothing>
}