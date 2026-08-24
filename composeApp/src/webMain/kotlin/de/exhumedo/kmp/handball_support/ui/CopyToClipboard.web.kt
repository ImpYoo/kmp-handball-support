package de.exhumedo.kmp.handball_support.ui

import kotlinx.browser.window
import org.w3c.dom.HTMLTextAreaElement

actual fun copyToClipboard(text: String) {
    val document = window.document
    val textarea = document.createElement("textarea") as HTMLTextAreaElement
    textarea.value = text
    textarea.style.position = "fixed"
    textarea.style.top = "0"
    textarea.style.left = "0"
    textarea.style.opacity = "0"
    document.body?.appendChild(textarea)
    textarea.focus()
    textarea.select()
    try {
        document.execCommand("copy")
    } catch (e: Throwable) {
        // ignore
    }
    document.body?.removeChild(textarea)
}