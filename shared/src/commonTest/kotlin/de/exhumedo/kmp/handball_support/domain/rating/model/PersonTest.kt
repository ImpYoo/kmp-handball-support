package de.exhumedo.kmp.handball_support.domain.rating.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PersonTest {

    @Test
    fun rejectsBlankNamesAndId() {
        assertFailsWith<IllegalArgumentException> {
            Person(id = "", firstName = "Jane", lastName = "Doe")
        }

        assertFailsWith<IllegalArgumentException> {
            Person(id = "p1", firstName = " ", lastName = "Doe")
        }

        assertFailsWith<IllegalArgumentException> {
            Person(id = "p1", firstName = "Jane", lastName = "")
        }
    }

    @Test
    fun usesIdentityEquality() {
        val first = Person(id = "p1", firstName = "Jane", lastName = "Doe")
        val second = Person(id = "p1", firstName = "Janet", lastName = "Smith")

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }
}
