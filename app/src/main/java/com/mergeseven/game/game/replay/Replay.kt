package com.mergeseven.game.game.replay

import com.mergeseven.game.game.model.HexCoord
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * One thing the player did. Only inputs are recorded, never results — the engine recomputes those,
 * which is the whole point: if a replay diverges, the engine changed.
 */
@Serializable
sealed interface ReplayAction {

    @Serializable
    @SerialName("place")
    data class Place(val slotIndex: Int, val origin: HexCoord) : ReplayAction

    @Serializable
    @SerialName("rotate")
    data class Rotate(val slotIndex: Int) : ReplayAction

    @Serializable
    @SerialName("remove")
    data class RemoveTile(val cell: HexCoord) : ReplayAction

    @Serializable
    @SerialName("shuffle")
    data object Shuffle : ReplayAction

    @Serializable
    @SerialName("undo")
    data object Undo : ReplayAction
}

/**
 * A complete run, reduced to the two things needed to reproduce it exactly.
 *
 * Small enough to attach to a bug report: a tester who hits a broken board sends the seed and the
 * action list, and the same board appears on any machine.
 */
@Serializable
data class Replay(
    val seed: Long,
    val level: Int,
    val actions: List<ReplayAction> = emptyList()
) {
    fun encode(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun decode(text: String): Replay = json.decodeFromString(serializer(), text)
    }
}
