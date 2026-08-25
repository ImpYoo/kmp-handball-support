package de.exhumedo.kmp.handball_support.persistence

import kotlinx.browser.localStorage
import org.w3c.dom.get
import org.w3c.dom.set

actual fun tacticStorage(): TacticStorage = LocalStorageTacticStorage

private object LocalStorageTacticStorage : TacticStorage {
    override fun read(): String? {
        return runCatching { localStorage[TACTIC_STORAGE_KEY] }.getOrNull()
    }

    override fun save(json: String) {
        runCatching { localStorage[TACTIC_STORAGE_KEY] = json }
    }

    override fun clear() {
        runCatching { localStorage.removeItem(TACTIC_STORAGE_KEY) }
    }
}
