package de.exhumedo.kmp.handball_support.config

object AppConfig {
    val baseApiUrl: String by lazy {
        defaultBaseApiUrl().trim().trimEnd('/').ifBlank { "http://localhost:8080" }
    }
}

/**
 * Per-target API base URL, injected at deploy/build time:
 *  - web: `<meta name="api-base-url">` or `window.APP_API_BASE_URL`, falling back to the page origin.
 *  - android: `BuildConfig.API_BASE_URL` (set via Gradle `apiBaseUrl` property).
 *  - ios: `Info.plist` key `ApiBaseUrl`.
 *  - jvm: env `API_BASE_URL` or system property `api.base.url`.
 */
internal expect fun defaultBaseApiUrl(): String

