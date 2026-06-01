package de.exhumedo.kmp.handball_support.sportradar

import de.exhumedo.kmp.handball_support.sportradar.error.SportradarError
import org.slf4j.LoggerFactory

/**
 * Production [SportradarTelemetry] implementation that writes structured log lines
 * to SLF4J at appropriate levels:
 *  - DEBUG: cache hits / request start
 *  - INFO:  successful fetch, cache writes
 *  - WARN:  retries, cache misses on first attempt
 *  - ERROR: final failures
 */
class Slf4jSportradarTelemetry : SportradarTelemetry {

    private val log = LoggerFactory.getLogger(Slf4jSportradarTelemetry::class.java)

    override fun onCacheHit(cacheKey: String) {
        log.debug("Sportradar cache HIT  key={}", cacheKey)
    }

    override fun onCacheMiss(cacheKey: String) {
        log.debug("Sportradar cache MISS key={}", cacheKey)
    }

    override fun onCacheWrite(cacheKey: String, phaseCount: Int) {
        log.info("Sportradar cache WRITE key={} phases={}", cacheKey, phaseCount)
    }

    override fun onRequestStart(url: String, attempt: Int) {
        log.debug("Sportradar fetch START attempt={} url={}", attempt, url)
    }

    override fun onRequestRetry(url: String, attempt: Int, delayMillis: Long, reason: SportradarError) {
        log.warn(
            "Sportradar fetch RETRY attempt={} delayMs={} reason={} url={}",
            attempt, delayMillis, reason::class.simpleName, url,
        )
    }

    override fun onRequestSuccess(url: String, attempt: Int) {
        log.info("Sportradar fetch OK attempt={} url={}", attempt, url)
    }

    override fun onRequestFailure(url: String, attempt: Int, error: SportradarError) {
        val cause = when (error) {
            is SportradarError.NetworkError -> error.cause.message
            is SportradarError.ParseError   -> error.cause.message
            is SportradarError.HttpError    -> "HTTP ${error.statusCode}"
            is SportradarError.EmptyResponse -> "empty response"
        }
        log.error(
            "Sportradar fetch FAILED attempt={} error={} cause={} url={}",
            attempt, error::class.simpleName, cause, url,
        )
    }
}

