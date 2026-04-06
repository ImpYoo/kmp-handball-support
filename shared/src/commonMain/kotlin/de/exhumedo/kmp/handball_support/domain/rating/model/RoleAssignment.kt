package de.exhumedo.kmp.handball_support.domain.rating.model

/**
 * Assignment of one person to one official role within a specific game.
 *
 * This object is only meaningful inside a game context because roles are
 * contextual rather than inherent properties of a person.
 *
 * @property person The assigned person.
 * @property role The role the person fulfills for the game.
 */
data class RoleAssignment(
    val person: Person,
    val role: OfficialRole,
)
