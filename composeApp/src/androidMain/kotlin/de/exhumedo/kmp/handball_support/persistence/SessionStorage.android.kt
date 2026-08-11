package de.exhumedo.kmp.handball_support.persistence

actual fun sessionStorage(): SessionStorage = InMemorySessionStorage()

