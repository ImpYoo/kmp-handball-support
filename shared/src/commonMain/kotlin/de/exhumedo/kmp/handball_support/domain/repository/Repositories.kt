package de.exhumedo.kmp.handball_support.domain.repository

import de.exhumedo.kmp.handball_support.domain.model.Phase
import de.exhumedo.kmp.handball_support.domain.model.VoteByReferees

interface PhaseRepository {
    fun getAllPhases(): List<Phase>
}

interface VoteByRefereesRepository {
    fun findById(id: String): VoteByReferees?
    fun findAll(): List<VoteByReferees>
    fun save(vote: VoteByReferees)
    fun deleteById(id: String): Boolean
}

