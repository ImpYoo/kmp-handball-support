package de.exhumedo.kmp.handball_support.domain.rating.model

/**
 * Person known to the domain by explicit caller-provided identity.
 *
 * The domain does not generate identifiers automatically. This keeps the
 * common domain layer free from target-specific UUID generation concerns and
 * makes identity creation the responsibility of the application layer.
 *
 * @property id Unique person identifier supplied by the caller.
 * @property firstName First name used for display and identification.
 * @property lastName Last name used for display and identification.
 */
class Person(
    val id: String,
    val firstName: String,
    val lastName: String,
) {
    init {
        require(id.isNotBlank()) { "Person id must not be blank" }
        require(firstName.isNotBlank()) { "Person firstName must not be blank" }
        require(lastName.isNotBlank()) { "Person lastName must not be blank" }
    }

    /**
     * Full name assembled for display purposes.
     */
    val fullName: String
        get() = "$firstName $lastName"

    /**
     * Identity equality based solely on the stable person identifier.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Person) return false
        return id == other.id
    }

    /**
     * Hash code derived solely from the person identifier.
     */
    override fun hashCode(): Int = id.hashCode()

    /**
     * Human-readable summary suitable for logs and debugging.
     */
    override fun toString(): String = "Person(id='$id', name='$fullName')"
}
