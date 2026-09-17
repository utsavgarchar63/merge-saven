package com.mergeseven.game.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mergeseven.game.core.DispatcherProvider
import com.mergeseven.game.game.engine.BoardEngine
import com.mergeseven.game.game.model.GameState
import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.game.model.PieceCell
import com.mergeseven.game.game.model.Tile
import com.mergeseven.game.game.model.TilePiece
import com.mergeseven.game.game.repository.RoomGameRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Resume after process death (AF0-12).
 *
 * A killed process loses every in-memory object but keeps the database file. This test reproduces
 * exactly that: it writes through one `GameDatabase` instance, closes it, opens a completely new
 * instance against the same file, and checks the game comes back. Nothing is shared between the two
 * except the bytes on disk.
 *
 * The remaining manual check, which no instrumented test can perform on itself:
 * 1. Start a level and make a few moves.
 * 2. `adb shell am kill com.mergeseven.game` (background kill, like low memory).
 * 3. Relaunch — the board, tray, score and coins must match.
 * 4. Repeat with `adb shell am force-stop com.mergeseven.game`.
 */
@RunWith(AndroidJUnit4::class)
class ActiveGamePersistenceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dispatchers = object : DispatcherProvider {
        override val main = Dispatchers.Unconfined
        override val io = Dispatchers.Unconfined
        override val default = Dispatchers.Unconfined
        override val unconfined = Dispatchers.Unconfined
    }

    @Before
    fun deleteAnyLeftoverDatabase() {
        context.deleteDatabase(TEST_DB)
    }

    @After
    fun cleanUp() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun aSavedGameSurvivesTheDatabaseBeingClosedAndReopened() = runBlocking {
        val original = gameState()

        withFreshDatabase { database ->
            RoomGameRepository(database.activeGameDao(), dispatchers).saveActiveGame(original)
        }

        withFreshDatabase { database ->
            val resumed = RoomGameRepository(database.activeGameDao(), dispatchers).loadActiveGame()
            assertEquals(original, resumed)
        }
    }

    @Test
    fun theResumedBoardMatchesTileForTile() = runBlocking {
        val original = gameState()

        withFreshDatabase { database ->
            RoomGameRepository(database.activeGameDao(), dispatchers).saveActiveGame(original)
        }

        withFreshDatabase { database ->
            val resumed = RoomGameRepository(database.activeGameDao(), dispatchers).loadActiveGame()
            assertEquals(
                original.board.activeTiles().toSet(),
                resumed?.board?.activeTiles()?.toSet()
            )
            assertEquals(original.trayPieces, resumed?.trayPieces)
            assertEquals(original.score, resumed?.score)
            assertEquals(original.coins, resumed?.coins)
            assertEquals(original.moves, resumed?.moves)
        }
    }

    @Test
    fun clearingIsAlsoDurable() = runBlocking {
        withFreshDatabase { database ->
            val repository = RoomGameRepository(database.activeGameDao(), dispatchers)
            repository.saveActiveGame(gameState())
            repository.clearActiveGame()
        }

        withFreshDatabase { database ->
            assertNull(RoomGameRepository(database.activeGameDao(), dispatchers).loadActiveGame())
        }
    }

    /** Opens a brand new database instance over the same file, then closes it. */
    private inline fun withFreshDatabase(block: (GameDatabase) -> Unit) {
        val database = Room.databaseBuilder(context, GameDatabase::class.java, TEST_DB).build()
        try {
            block(database)
        } finally {
            database.close()
        }
    }

    private fun gameState(): GameState {
        val board = BoardEngine().createBoard()
            .withTile(Tile(id = 1L, value = 4, cell = HexCoord(0, 0)))
            .withTile(Tile(id = 2L, value = 8, cell = HexCoord(1, -1)))
            .withTile(Tile(id = 3L, value = 16, cell = HexCoord(-2, 1)))

        return GameState(
            board = board,
            trayPieces = listOf(
                TilePiece(id = 10L, cells = listOf(PieceCell(HexCoord(0, 0), 2))),
                null,
                TilePiece(
                    id = 12L,
                    cells = listOf(PieceCell(HexCoord(0, 0), 8), PieceCell(HexCoord(1, 0), 8)),
                    rotation = 4
                )
            ),
            score = 12_345L,
            bestScore = 20_000L,
            coins = 640,
            level = 6,
            targetValue = 512,
            moves = 88,
            isPaused = false,
            isGameOver = false,
            isBusy = false
        )
    }

    private companion object {
        const val TEST_DB = "resume-test.db"
    }
}
