package de.exhumedo.kmp.handball_support.auth

actual fun tokenStorage(): TokenStorage = InMemoryTokenStorage()

