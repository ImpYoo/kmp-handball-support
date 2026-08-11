package de.exhumedo.kmp.handball_support.referee_coaching.domain.usecase

import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.Criterion
import de.exhumedo.kmp.handball_support.referee_coaching.domain.repository.CriterionCatalogRepository

/**
 * Loads the criteria catalog as a fresh, ready-to-score template.
 */
class LoadCriteriaUseCase(
    private val repository: CriterionCatalogRepository,
) {
    operator fun invoke(): List<Criterion> = repository.loadCriteria()
}

