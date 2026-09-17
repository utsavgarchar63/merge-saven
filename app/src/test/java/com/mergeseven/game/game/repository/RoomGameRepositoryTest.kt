package com.mergeseven.game.game.repository

import com.mergeseven.game.data.local.dao.ActiveGameDao
import com.mergeseven.game.data.local.entity.ActiveGameEntity
import com.mergeseven.game.data.local.snapshot.GameStateSnapshot
import com.mergeseven.game.game.engine.BoardEngine
import com.mergeseven.game.game.model.GameState
import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.game.model.PieceCell
import com.mergeseven.game.game.model.Tile
import com.mergeseven.game.game.model.TilePiece
import com.mergeseven.game.testing.TestPersistence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoomGameRepositoryTest {

    private val dao = FakeActiveGameDao()
    private val repository = RoomGameRepository(dao, TestPersistence.dispatchers())

    @Test
    fun `a saved game comes back intact`() = runTest {
        val state = gameState()

        repository.saveActiveGame(state)

        assertEquals(state, repository.loadActiveGame())
    }

    @Test
    fun `there is nothing to resume on a fresh install`() = runTest {
        assertNull(repository.loadActiveGame())
    }

    @Test
    fun `saving twice keeps only the latest game`() = runTest {
        repository.saveActiveGame(gameState(score = 100L))
        repository.saveActiveGame(gameState(score = 900L))

        assertEquals(900L, repository.loadActiveGame()?.score)
        assertEquals(1, dao.rows.value.size)
    }

    @Test
    fun `clearing removes the save`() = runTest {
        repository.saveActiveGame(gameState())

        repository.clearActiveGame()

        assertNull(repository.loadActiveGame())
    }

    @Test
    fun `each mode keeps its own save`() = runTest {
        repository.saveActiveGame(gameState(score = 10L), slotId = "campaign")
        repository.saveActiveGame(gameState(score = 20L), slotId = "endless")

        assertEquals(10L, repository.loadActiveGame("campaign")?.score)
        assertEquals(20L, repository.loadActiveGame("endless")?.score)
    }

    @Test
    fun `a save from an older supported version still loads`() = runTest {
        repository.saveActiveGame(gameState())
        dao.rows.value = dao.rows.value.mapValues { (_, entity) ->
            entity.copy(schemaVersion = GameStateSnapshot.MIN_SUPPORTED_VERSION)
        }

        assertEquals(gameState().score, repository.loadActiveGame()?.score)
    }

    @Test
    fun `a save from an incompatible version is discarded rather than crashing`() = runTest {
        repository.saveActiveGame(gameState())
        dao.rows.value = dao.rows.value.mapValues { (_, entity) ->
            entity.copy(schemaVersion = GameStateSnapshot.CURRENT_VERSION + 1)
        }

        assertNull(repository.loadActiveGame())
    }

    @Test
    fun `an incompatible save is deleted so it cannot fail again`() = runTest {
        repository.saveActiveGame(gameState())
        dao.rows.value = dao.rows.value.mapValues { (_, entity) ->
            entity.copy(schemaVersion = GameStateSnapshot.CURRENT_VERSION + 1)
        }

        repository.loadActiveGame()

        assertEquals(0, dao.rows.value.size)
    }

    @Test
    fun `unreadable json is discarded rather than crashing`() = runTest {
        repository.saveActiveGame(gameState())
        dao.rows.value = dao.rows.value.mapValues { (_, entity) ->
            entity.copy(snapshotJson = "{ this is not the json you are looking for")
        }

        assertNull(repository.loadActiveGame())
    }

    @Test
    fun `the observed save reflects what was written`() = runTest {
        repository.saveActiveGame(gameState(score = 4_242L))

        assertEquals(4_242L, repository.getActiveGame().first()?.score)
    }

    @Test
    fun `the level is stored alongside the snapshot so continue can show it`() = runTest {
        repository.saveActiveGame(gameState().copy(level = 7))

        val entity = dao.rows.value.values.single()
        assertEquals(7, entity.levelId)
        assertTrue(entity.updatedAt > 0)
    }

    private fun gameState(score: Long = 1_234L): GameState {
        val board = BoardEngine().createBoard(radius = 2)
            .withTile(Tile(id = 1L, value = 4, cell = HexCoord(0, 0)))
            .withTile(Tile(id = 2L, value = 8, cell = HexCoord(1, -1)))

        return GameState(
            board = board,
            trayPieces = listOf(
                TilePiece(id = 10L, cells = listOf(PieceCell(HexCoord(0, 0), 2))),
                null,
                TilePiece(id = 12L, cells = listOf(PieceCell(HexCoord(0, 0), 8)), rotation = 3)
            ),
            score = score,
            bestScore = 9_000L,
            coins = 250,
            level = 3,
            targetValue = 64,
            moves = 17,
            isPaused = false,
            isGameOver = false,
            isBusy = false,
            objectives = listOf(
                com.mergeseven.game.game.objectives.LevelObjective.ReachValue(
                    id = "reach_target",
                    value = 64
                )
            )
        )
    }

    private class FakeActiveGameDao : ActiveGameDao {
        val rows = MutableStateFlow<Map<String, ActiveGameEntity>>(emptyMap())

        override fun observe(slotId: String): Flow<ActiveGameEntity?> =
            rows.map { it[slotId] }

        override suspend fun get(slotId: String): ActiveGameEntity? = rows.value[slotId]

        override suspend fun upsert(entity: ActiveGameEntity) {
            rows.value = rows.value + (entity.slotId to entity)
        }

        override suspend fun delete(slotId: String) {
            rows.value = rows.value - slotId
        }
    }
}
