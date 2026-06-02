package de.exhumedo.kmp.handball_support.config

import platform.Foundation.NSBundle

internal actual fun defaultBaseApiUrl(): String {
    val value = NSBundle.mainBundle.objectForInfoDictionaryKey("ApiBaseUrl") as? String
    return value?.takeIf { it.isNotBlank() } ?: ""
}

