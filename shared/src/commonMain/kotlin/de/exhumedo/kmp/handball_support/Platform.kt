package de.exhumedo.kmp.handball_support

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform