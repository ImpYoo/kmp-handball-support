package de.exhumedo.kmp.handball_support

import de.exhumedo.kmp.handball_support.vote.VotePayloadFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class ComposeAppCommonTest {

    @Test
    fun splitNameHandlesFirstAndLastName() {
        val (first, last) = VotePayloadFactory.splitName("Jane Doe")
        assertEquals("Jane", first)
        assertEquals("Doe", last)
    }

    @Test
    fun splitNameAddsFallbackLastNameForSingleToken() {
        val (first, last) = VotePayloadFactory.splitName("Ref1")
        assertEquals("Ref1", first)
        assertEquals("Official", last)
    }
}
