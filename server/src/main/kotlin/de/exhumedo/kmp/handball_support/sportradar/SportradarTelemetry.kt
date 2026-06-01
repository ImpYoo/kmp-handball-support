package de.exhumedo.kmp.handball_support.sportradar

import de.exhumedo.kmp.handball_support.sportradar.error.SportradarError

/**
 * Optional observability hook.
 * Wire metrics/logging in production by implementing this interface.
 */
interface SportradarTelemetry {
    fun onCacheHit(cacheKey: String) {}
    fun onCacheMiss(cacheKey: String) {}
    fun onCacheWrite(cacheKey: String, phaseCount: Int) {}

    fun onRequestStart(url: String, attempt: Int) {}
    fun onRequestRetry(url: String, attempt: Int, delayMillis: Long, reason: SportradarError) {}
    fun onRequestSuccess(url: String, attempt: Int) {}
    fun onRequestFailure(url: String, attempt: Int, error: SportradarError) {}
}

object NoOpSportradarTelemetry : SportradarTelemetry

