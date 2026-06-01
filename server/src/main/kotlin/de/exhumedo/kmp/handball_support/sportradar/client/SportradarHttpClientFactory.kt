package de.exhumedo.kmp.handball_support.sportradar.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout

/**
 * Production-safe default client for JVM.
 *
 * KMP note: in commonMain you can mirror this factory with platform engines:
 * - Android: OkHttp
 * - iOS: Darwin
 * - JS: Js
 */
object SportradarHttpClientFactory {
    fun createJvmDefault(
        connectTimeoutMillis: Long = 3_000,
        requestTimeoutMillis: Long = 8_000,
        socketTimeoutMillis: Long = 8_000,
    ): HttpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            this.connectTimeoutMillis = connectTimeoutMillis
            this.requestTimeoutMillis = requestTimeoutMillis
            this.socketTimeoutMillis = socketTimeoutMillis
        }
    }
}

