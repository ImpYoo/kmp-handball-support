package de.exhumedo.kmp.handball_support.persistence

import kotlinx.browser.window

actual fun drawingStorage(): DrawingStorage = object : DrawingStorage {
    override fun read(): String? {
        return window.localStorage.getItem(DRAWING_STORAGE_KEY)
    }

    override fun save(json: String) {
        window.localStorage.setItem(DRAWING_STORAGE_KEY, json)
    }

    override fun clear() {
        window.localStorage.removeItem(DRAWING_STORAGE_KEY)
    }
}
