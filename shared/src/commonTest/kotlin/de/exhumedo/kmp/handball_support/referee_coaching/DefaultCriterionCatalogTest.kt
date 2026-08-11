package de.exhumedo.kmp.handball_support.referee_coaching

import de.exhumedo.kmp.handball_support.referee_coaching.data.DefaultCriterionCatalog
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.CriterionCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultCriterionCatalogTest {

    private val catalog = DefaultCriterionCatalog()

    @Test
    fun catalogContainsTwelveCriteriaFromTheSheet() {
        assertEquals(12, catalog.loadCriteria().size)
    }

    @Test
    fun sectionsMatchTheObservationSheet() {
        val criteria = catalog.loadCriteria()
        val rules = criteria.filter { it.category == CriterionCategory.RULES_OF_THE_GAME }
        val personal = criteria.filter { it.category == CriterionCategory.PERSONAL_IMPRESSION }

        assertEquals(8, rules.size, "Section A (Spielregeln) should have 8 criteria")
        assertEquals(4, personal.size, "Section B (Persönlicher Eindruck) should have 4 criteria")
    }

    @Test
    fun everyCriterionHasGroupsAndCauses() {
        catalog.loadCriteria().forEach { criterion ->
            assertTrue(criterion.defectGroups.isNotEmpty(), "criterion ${criterion.id} has no groups")
            criterion.defectGroups.forEach { group ->
                assertTrue(group.rootCauses.isNotEmpty(), "group ${group.id} has no causes")
            }
        }
    }

    @Test
    fun catalogStartsAtDefaultScoreWithZeroCounts() {
        catalog.loadCriteria().forEach { criterion ->
            assertEquals(6, criterion.score, "criterion ${criterion.id} not at default score")
            criterion.defectGroups.forEach { group ->
                group.rootCauses.forEach { cause ->
                    assertEquals(0, cause.count, "cause ${cause.id} not zeroed")
                }
            }
        }
    }

    @Test
    fun allIdsAreGloballyUnique() {
        val criteria = catalog.loadCriteria()

        val criterionIds = criteria.map { it.id }
        assertEquals(criterionIds.size, criterionIds.toSet().size, "duplicate criterion ids")

        val groupIds = criteria.flatMap { it.defectGroups }.map { it.id }
        assertEquals(groupIds.size, groupIds.toSet().size, "duplicate group ids across catalog")

        val causeIds = criteria.flatMap { it.defectGroups }.flatMap { it.rootCauses }.map { it.id }
        assertEquals(causeIds.size, causeIds.toSet().size, "duplicate root cause ids across catalog")
    }

    @Test
    fun namesAreNonBlank() {
        catalog.loadCriteria().forEach { criterion ->
            assertTrue(criterion.name.isNotBlank())
            criterion.defectGroups.forEach { group ->
                assertTrue(group.name.isNotBlank())
                group.rootCauses.forEach { cause -> assertTrue(cause.name.isNotBlank()) }
            }
        }
    }
}


