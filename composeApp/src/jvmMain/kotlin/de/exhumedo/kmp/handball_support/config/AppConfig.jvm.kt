package de.exhumedo.kmp.handball_support.config

internal actual fun defaultBaseApiUrl(): String =
    System.getenv("API_BASE_URL")
        ?: System.getProperty("api.base.url")
        ?: ""

