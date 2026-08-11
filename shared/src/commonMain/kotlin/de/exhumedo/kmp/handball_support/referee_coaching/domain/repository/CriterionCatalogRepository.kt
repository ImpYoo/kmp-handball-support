package de.exhumedo.kmp.handball_support.referee_coaching.domain.repository

import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.Criterion

/**
 * Provides the catalog of referee-coaching criteria (with their defect groups
 * and root causes).
 *
 * Implementations return a *fresh template*: every returned [Criterion] starts
 * at its default score with all root-cause counts at zero. Live scoring is then
 * applied per coaching session via the scoring service.
 */
interface CriterionCatalogRepository {
    /** Returns the full list of criteria templates with counts reset to zero. */
    fun loadCriteria(): List<Criterion>
}

