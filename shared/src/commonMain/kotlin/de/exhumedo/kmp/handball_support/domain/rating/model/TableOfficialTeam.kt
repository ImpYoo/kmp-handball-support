package de.exhumedo.kmp.handball_support.domain.rating.model

data class TableOfficialTeam(
    val timeKeeper: RoleAssignment,
    val scoreKeeper: RoleAssignment,
    val delegate: RoleAssignment? = null,
)
