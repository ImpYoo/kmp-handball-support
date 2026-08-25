package de.exhumedo.kmp.handball_support.persistence

import kotlinx.serialization.json.Json
import java.io.File

actual fun drawingStorage(): DrawingStorage = FileDrawingStorage()

private class FileDrawingStorage : DrawingStorage {
    private val file = File(System.getProperty("user.home"), ".handball_support/drawing.json")

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
