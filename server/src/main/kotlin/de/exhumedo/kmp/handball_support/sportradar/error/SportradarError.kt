package de.exhumedo.kmp.handball_support.sportradar.error

// ── Typed errors propagated from Sportradar client ───────────────────────────
// Callers handle via SportradarResult; nothing is silently swallowed.
// ─────────────────────────────────────────────────────────────────────────────

sealed interface SportradarError {
    /** Successful HTTP response but payload was empty or structurally invalid */
    data object EmptyResponse : SportradarError

    /** HTTP layer returned a non-2xx status */
    data class HttpError(val statusCode: Int) : SportradarError

    /** Network-level failure (DNS, timeout, connection refused, …) */
    data class NetworkError(val cause: Throwable) : SportradarError

    /** JSON could be fetched but failed to deserialize */
    data class ParseError(val cause: Throwable) : SportradarError
}

sealed interface SportradarResult<out T> {
    data class Success<T>(val value: T) : SportradarResult<T>
    data class Failure(val error: SportradarError) : SportradarResult<Nothing>
}

