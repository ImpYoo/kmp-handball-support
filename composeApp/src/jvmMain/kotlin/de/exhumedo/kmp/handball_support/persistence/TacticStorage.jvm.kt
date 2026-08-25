package de.exhumedo.kmp.handball_support.persistence

import java.io.File

actual fun tacticStorage(): TacticStorage = FileTacticStorage()

private class FileTacticStorage : TacticStorage {
    private val file = File(System.getProperty("user.home"), ".handball_support/tactic.json")

    override fun read(): String? {
        return runCatching { file.takeIf { it.exists() }?.readText() }.getOrNull()
    }

    override fun save(json: String) {
        runCatching {
            file.parentFile.mkdirs()
            file.writeText(json)
        }
    }

    override fun clear() {
        runCatching { file.delete() }
    }
}
