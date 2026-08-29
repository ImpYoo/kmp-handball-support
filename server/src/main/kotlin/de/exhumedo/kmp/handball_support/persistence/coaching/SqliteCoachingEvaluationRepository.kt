package de.exhumedo.kmp.handball_support.persistence.coaching

import de.exhumedo.kmp.handball_support.referee_coaching.data.DefaultCriterionCatalog
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.CoachingGame
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.CoachingHistoryEntry
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.CoachingHistoryEventType
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

                // Delete old history, then insert current history
                connection.prepareStatement(DELETE_HISTORY).use { ps ->
                    ps.setString(1, evaluation.id)
                    ps.executeUpdate()
                }

                connection.prepareStatement(INSERT_HISTORY).use { ps ->
                    evaluation.history.forEach { entry ->
                        ps.setString(1, UUID.randomUUID().toString())
                        ps.setString(2, evaluation.id)
                        ps.setString(3, entry.id)
                        ps.setLong(4, entry.gameTimeMillis)
                        ps.setInt(5, entry.homeScore)
                        ps.setInt(6, entry.guestScore)
                        ps.setString(7, entry.type.name)
                        ps.setString(8, entry.criterionId)
                        ps.setString(9, entry.defectGroupId)
                        ps.setString(10, entry.rootCauseId)
                        ps.setString(11, entry.goalTeam)
                        ps.setInt(12, if (entry.selected) 1 else 0)
                        ps.setString(13, entry.team)
                        ps.setString(14, entry.teamLabel)
                        ps.setString(15, entry.playerId)
                        ps.setString(16, entry.playerLabel)
                        ps.setString(17, entry.refereeName)
                        ps.setString(18, entry.note)
                        ps.addBatch()
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
            history = loadHistory(connection, id),
        )
    }

    private fun loadHistory(connection: Connection, evaluationId: String): List<CoachingHistoryEntry> {
        return connection.prepareStatement(SELECT_HISTORY).use { ps ->
            ps.setString(1, evaluationId)
            ps.executeQuery().use { rs ->
                val entries = mutableListOf<CoachingHistoryEntry>()
                while (rs.next()) {
                    entries += CoachingHistoryEntry(
                        id = rs.getString("entry_id"),
                        gameTimeMillis = rs.getLong("game_time_millis"),
                        homeScore = rs.getInt("home_score"),
                        guestScore = rs.getInt("guest_score"),
                        type = runCatching { CoachingHistoryEventType.valueOf(rs.getString("type")) }
                            .getOrDefault(CoachingHistoryEventType.ROOT_CAUSE),
                        criterionId = rs.getString("criterion_id"),
                        defectGroupId = rs.getString("defect_group_id"),
                        rootCauseId = rs.getString("root_cause_id"),
                        goalTeam = rs.getString("goal_team"),
                        selected = rs.getInt("selected") == 1,
                        team = rs.getString("team"),
                        teamLabel = rs.getString("team_label"),
                        playerId = rs.getString("player_id"),
                        playerLabel = rs.getString("player_label"),
                        refereeName = rs.getString("referee_name"),
                        note = rs.getString("note") ?: "",
                    )
                }
                entries
            }
        }
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

            CREATE TABLE IF NOT EXISTS coaching_history_entries (
                id TEXT PRIMARY KEY,
                evaluation_id TEXT NOT NULL,
                entry_id TEXT NOT NULL,
                game_time_millis INTEGER NOT NULL,
                home_score INTEGER NOT NULL,
                guest_score INTEGER NOT NULL,
                type TEXT NOT NULL,
                criterion_id TEXT,
                defect_group_id TEXT,
                root_cause_id TEXT,
                goal_team TEXT,
                selected INTEGER NOT NULL,
                team TEXT,
                team_label TEXT,
                player_id TEXT,
                player_label TEXT,
                referee_name TEXT,
                note TEXT NOT NULL DEFAULT '',
                FOREIGN KEY (evaluation_id) REFERENCES coaching_evaluations(id) ON DELETE CASCADE
            );

            CREATE INDEX IF NOT EXISTS idx_counts_evaluation ON coaching_criterion_counts(evaluation_id, criterion_id);
            CREATE INDEX IF NOT EXISTS idx_history_evaluation ON coaching_history_entries(evaluation_id);
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

        const val DELETE_HISTORY = "DELETE FROM coaching_history_entries WHERE evaluation_id = ?"
        const val INSERT_HISTORY = """
            INSERT INTO coaching_history_entries(
                id, evaluation_id, entry_id, game_time_millis, home_score, guest_score, type,
                criterion_id, defect_group_id, root_cause_id, goal_team, selected,
                team, team_label, player_id, player_label, referee_name, note
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
        const val SELECT_HISTORY = """
            SELECT entry_id, game_time_millis, home_score, guest_score, type,
                   criterion_id, defect_group_id, root_cause_id, goal_team, selected,
                   team, team_label, player_id, player_label, referee_name, note
            FROM coaching_history_entries
            WHERE evaluation_id = ?
            ORDER BY game_time_millis ASC, entry_id ASC
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
