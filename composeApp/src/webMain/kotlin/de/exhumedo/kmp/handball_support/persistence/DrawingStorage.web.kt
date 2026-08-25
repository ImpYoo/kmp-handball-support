package de.exhumedo.kmp.handball_support.persistence

import kotlinx.browser.localStorage
import org.w3c.dom.get
import org.w3c.dom.set

actual fun drawingStorage(): DrawingStorage = LocalStorageDrawingStorage

private object LocalStorageDrawingStorage : DrawingStorage {
    override fun read(): String? {
        return runCatching { localStorage[DRAWING_STORAGE_KEY] }.getOrNull()
    }

    override fun save(json: String) {
        runCatching { localStorage[DRAWING_STORAGE_KEY] = json }
    }

    override fun clear() {
        runCatching { localStorage.removeItem(DRAWING_STORAGE_KEY) }
    }
}
