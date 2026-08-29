package de.exhumedo.kmp.handball_support.persistence.coaching

import de.exhumedo.kmp.handball_support.referee_coaching.data.DefaultCriterionCatalog
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.CoachingGame
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.CoachingPerson
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.Criterion
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.RefereeCoachingEvaluation
import de.exhumedo.kmp.handball_support.referee_coaching.domain.scoring.CriterionScoringService
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.util.UUID

/**
 * SQLite-backed implementation of [CoachingEvaluationRepository].
 *
 * Stores evaluation metadata and per-root-cause counts. On load, counts are
 * replayed against the fresh catalog template via [CriterionScoringService] so
 * the stored scores can never diverge from the domain rules.
 */
class SqliteCoachingEvaluationRepository(
    private val dbPath: Path,
) : CoachingEvaluationRepository {

    private val catalog by lazy { DefaultCriterionCatalog().loadCriteria() }
    private val scoring = CriterionScoringService()

    init {
        require(dbPath.toString().isNotBlank()) { "dbPath must not be blank" }
        Files.createDirectories(dbPath.parent)
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
            connection.createStatement().use { statement ->
                CREATE_SCHEMA.split(";").map { it.trim() }.filter { it.isNotBlank() }.forEach { sql ->
                    statement.execute(sql)
                }
            }
        }
    }

    override fun save(evaluation: RefereeCoachingEvaluation): RefereeCoachingEvaluation {
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
            connection.autoCommit = false
            try {
                // Upsert evaluation metadata
                connection.prepareStatement(UPSERT_EVALUATION).use { ps ->
                    ps.setString(1, evaluation.id)
                    ps.setString(2, evaluation.game.gameId)
                    ps.setString(3, evaluation.game.matchDate)
                    ps.setString(4, evaluation.game.homeTeam)
                    ps.setString(5, evaluation.game.awayTeam)
                    ps.setString(6, evaluation.evaluatorUsername)
                    ps.setString(7, evaluation.firstReferee.personId)
                    ps.setString(8, evaluation.firstReferee.firstName)
                    ps.setString(9, evaluation.firstReferee.lastName)
                    ps.setString(10, evaluation.secondReferee.personId)
                    ps.setString(11, evaluation.secondReferee.firstName)
                    ps.setString(12, evaluation.secondReferee.lastName)
                    ps.setString(13, evaluation.comment)
                    ps.setString(14, evaluation.createdAt)
                    ps.setString(15, evaluation.updatedAt)
                    ps.setInt(16, evaluation.totalScore)
                    ps.setInt(17, evaluation.maxTotalScore)
                    ps.executeUpdate()
                }

                // Delete old counts, then insert current counts
                connection.prepareStatement(DELETE_COUNTS).use { ps ->
                    ps.setString(1, evaluation.id)
                    ps.executeUpdate()
                }

                connection.prepareStatement(INSERT_COUNT).use { ps ->
                    evaluation.criteria.forEach { criterion ->
                        criterion.defectGroups.forEach { group ->
                            group.rootCauses.forEach { rootCause ->
                                if (rootCause.count != 0) {
                                    ps.setString(1, UUID.randomUUID().toString())
                                    ps.setString(2, evaluation.id)
                                    ps.setString(3, criterion.id)
                                    ps.setString(4, group.id)
                                    ps.setString(5, rootCause.id)
                                    ps.setInt(6, rootCause.count)
                                    ps.addBatch()
                                }
                            }
                        }
                    }
                    ps.executeBatch()
                }

                connection.commit()
            } catch (e: Exception) {
                connection.rollback()
                throw e
            } finally {
                connection.autoCommit = true
            }
        }
        return evaluation
    }

    override fun findById(id: String): RefereeCoachingEvaluation? {
        return DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
            connection.prepareStatement(SELECT_EVALUATION_BY_ID).use { ps ->
                ps.setString(1, id)
                ps.executeQuery().use { rs ->
                    if (!rs.next()) return@use null
                    buildEvaluation(connection, rs)
                }
            }
        }
    }

    override fun findAll(filter: CoachingEvaluationFilter): List<RefereeCoachingEvaluation> {
        val sql = buildString {
            append(SELECT_EVALUATIONS)
            val conditions = mutableListOf<String>()
            filter.gameId?.let { conditions += "e.game_id = ?" }
            filter.evaluatorUsername?.let { conditions += "e.evaluator_username = ?" }
            filter.from?.let { conditions += "e.match_date >= ?" }
            filter.to?.let { conditions += "e.match_date <= ?" }
            if (conditions.isNotEmpty()) {
                append(" WHERE ")
                append(conditions.joinToString(" AND "))
            }
            append(" ORDER BY e.updated_at DESC")
        }

        return DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
            connection.prepareStatement(sql).use { ps ->
                var idx = 1
                filter.gameId?.let { ps.setString(idx++, it) }
                filter.evaluatorUsername?.let { ps.setString(idx++, it) }
                filter.from?.let { ps.setString(idx++, it) }
                filter.to?.let { ps.setString(idx++, it) }

                ps.executeQuery().use { rs ->
                    val evaluations = mutableListOf<RefereeCoachingEvaluation>()
                    while (rs.next()) {
                        evaluations += buildEvaluation(connection, rs)
                    }
                    evaluations
                }
            }
        }
    }

    override fun deleteById(id: String): Boolean {
        return DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
            connection.prepareStatement(DELETE_EVALUATION).use { ps ->
                ps.setString(1, id)
                ps.executeUpdate() > 0
            }
        }
    }

    private fun buildEvaluation(connection: Connection, rs: ResultSet): RefereeCoachingEvaluation {
        val id = rs.getString("id")
        val criteria = catalog.map { criterion -> replayCounts(connection, id, criterion) }
        return RefereeCoachingEvaluation(
            id = id,
            game = CoachingGame(
                gameId = rs.getString("game_id"),
                matchDate = rs.getString("match_date"),
                homeTeam = rs.getString("home_team"),
                awayTeam = rs.getString("away_team"),
            ),
            evaluatorUsername = rs.getString("evaluator_username"),
            firstReferee = CoachingPerson(
                personId = rs.getString("first_referee_id"),
                firstName = rs.getString("first_referee_first_name"),
                lastName = rs.getString("first_referee_last_name"),
            ),
            secondReferee = CoachingPerson(
                personId = rs.getString("second_referee_id"),
                firstName = rs.getString("second_referee_first_name"),
                lastName = rs.getString("second_referee_last_name"),
            ),
            criteria = criteria,
            comment = rs.getString("comment"),
            createdAt = rs.getString("created_at"),
            updatedAt = rs.getString("updated_at"),
        )
    }

    private fun replayCounts(connection: Connection, evaluationId: String, template: Criterion): Criterion {
        connection.prepareStatement(SELECT_COUNTS).use { ps ->
            ps.setString(1, evaluationId)
            ps.setString(2, template.id)
            ps.executeQuery().use { rs ->
                var criterion = template
                while (rs.next()) {
                    val groupId = rs.getString("group_id")
                    val rootCauseId = rs.getString("root_cause_id")
                    val count = rs.getInt("count")
                    repeat(count.coerceAtLeast(0)) {
                        criterion = scoring.incrementRootCause(criterion, groupId, rootCauseId)
                    }
                }
                return criterion
            }
        }
    }

    private companion object {
        const val CREATE_SCHEMA = """
            CREATE TABLE IF NOT EXISTS coaching_evaluations (
                id TEXT PRIMARY KEY,
                game_id TEXT NOT NULL,
                match_date TEXT NOT NULL,
                home_team TEXT NOT NULL,
                away_team TEXT NOT NULL,
                evaluator_username TEXT NOT NULL,
                first_referee_id TEXT NOT NULL,
                first_referee_first_name TEXT NOT NULL,
                first_referee_last_name TEXT NOT NULL,
                second_referee_id TEXT NOT NULL,
                second_referee_first_name TEXT NOT NULL,
                second_referee_last_name TEXT NOT NULL,
                comment TEXT NOT NULL DEFAULT '',
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                total_score INTEGER NOT NULL,
                max_total_score INTEGER NOT NULL
            );

            CREATE TABLE IF NOT EXISTS coaching_criterion_counts (
                id TEXT PRIMARY KEY,
                evaluation_id TEXT NOT NULL,
                criterion_id TEXT NOT NULL,
                group_id TEXT NOT NULL,
                root_cause_id TEXT NOT NULL,
                count INTEGER NOT NULL,
                FOREIGN KEY (evaluation_id) REFERENCES coaching_evaluations(id) ON DELETE CASCADE
            );

            CREATE INDEX IF NOT EXISTS idx_counts_evaluation ON coaching_criterion_counts(evaluation_id, criterion_id);
            CREATE INDEX IF NOT EXISTS idx_evaluations_game ON coaching_evaluations(game_id);
            CREATE INDEX IF NOT EXISTS idx_evaluations_evaluator ON coaching_evaluations(evaluator_username);
            CREATE INDEX IF NOT EXISTS idx_evaluations_date ON coaching_evaluations(match_date);
        """

        const val UPSERT_EVALUATION = """
            INSERT INTO coaching_evaluations(
                id, game_id, match_date, home_team, away_team, evaluator_username,
                first_referee_id, first_referee_first_name, first_referee_last_name,
                second_referee_id, second_referee_first_name, second_referee_last_name,
                comment, created_at, updated_at, total_score, max_total_score
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                game_id = excluded.game_id,
                match_date = excluded.match_date,
                home_team = excluded.home_team,
                away_team = excluded.away_team,
                evaluator_username = excluded.evaluator_username,
                first_referee_id = excluded.first_referee_id,
                first_referee_first_name = excluded.first_referee_first_name,
                first_referee_last_name = excluded.first_referee_last_name,
                second_referee_id = excluded.second_referee_id,
                second_referee_first_name = excluded.second_referee_first_name,
                second_referee_last_name = excluded.second_referee_last_name,
                comment = excluded.comment,
                updated_at = excluded.updated_at,
                total_score = excluded.total_score,
                max_total_score = excluded.max_total_score
        """

        const val DELETE_COUNTS = "DELETE FROM coaching_criterion_counts WHERE evaluation_id = ?"
        const val INSERT_COUNT = """
            INSERT INTO coaching_criterion_counts(id, evaluation_id, criterion_id, group_id, root_cause_id, count)
            VALUES (?, ?, ?, ?, ?, ?)
        """

        const val SELECT_EVALUATION_BY_ID = "SELECT * FROM coaching_evaluations WHERE id = ?"
        const val SELECT_EVALUATIONS = "SELECT * FROM coaching_evaluations e"
        const val SELECT_COUNTS = """
            SELECT group_id, root_cause_id, count FROM coaching_criterion_counts
            WHERE evaluation_id = ? AND criterion_id = ?
        """
        const val DELETE_EVALUATION = "DELETE FROM coaching_evaluations WHERE id = ?"
    }
}
