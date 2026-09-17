package com.mergeseven.game.cloud

import android.content.Context
import android.util.Log
import com.google.android.gms.games.Games
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.snapshot.SnapshotMetadataChange
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface CloudSaveRepository {
    suspend fun read(): CloudSnapshot?
    suspend fun write(snapshot: CloudSnapshot)
    suspend fun deleteSlot()
}

private val cloudJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}

/**
 * Prefers Play Games Snapshots when signed in and Games Services is configured.
 * Falls back to a per-account file under filesDir so sync logic works without a live Games project.
 */
@Singleton
class PlayGamesCloudSaveRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: PlayGamesAuth
) : CloudSaveRepository {

    private val mutex = Mutex()

    override suspend fun read(): CloudSnapshot? = mutex.withLock {
        val account = auth.lastAccount() ?: return null
        readFromPlayGames(account)?.let { return it }
        readFallback(accountId(account))
    }

    override suspend fun write(snapshot: CloudSnapshot) = mutex.withLock {
        val account = auth.lastAccount()
            ?: error("Cannot upload cloud save while signed out")
        val bytes = cloudJson.encodeToString(CloudSnapshot.serializer(), snapshot)
            .encodeToByteArray()
        val uploaded = writeToPlayGames(account, bytes)
        if (!uploaded) {
            writeFallback(accountId(account), bytes)
        }
    }

    override suspend fun deleteSlot() = mutex.withLock {
        val account = auth.lastAccount()
        if (account != null) {
            runCatching { deleteFromPlayGames(account) }
            fallbackFile(accountId(account)).delete()
        }
        // Also clear any leftover fallback for current auth player id
        auth.account.value?.playerId?.let { fallbackFile(it).delete() }
        Unit
    }

    private suspend fun readFromPlayGames(
        account: com.google.android.gms.auth.api.signin.GoogleSignInAccount
    ): CloudSnapshot? {
        return try {
            val client = Games.getSnapshotsClient(context, account)
            val opened = client.open(
                CloudSnapshot.SNAPSHOT_NAME,
                /* createIfNotFound = */ false,
                SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED
            ).await()
            val snapshot = opened.data ?: return null
            val bytes = snapshot.snapshotContents.readFully()
            parseOrNull(bytes)
        } catch (e: Exception) {
            Log.d(TAG, "Play Games read skipped: ${e.message}")
            null
        }
    }

    private suspend fun writeToPlayGames(
        account: com.google.android.gms.auth.api.signin.GoogleSignInAccount,
        bytes: ByteArray
    ): Boolean {
        return try {
            val client = Games.getSnapshotsClient(context, account)
            val opened = client.open(
                CloudSnapshot.SNAPSHOT_NAME,
                /* createIfNotFound = */ true,
                SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED
            ).await()
            val snapshot = opened.data ?: return false
            snapshot.snapshotContents.writeBytes(bytes)
            val metadata = SnapshotMetadataChange.Builder()
                .setDescription("Merge Seven meta progress")
                .build()
            client.commitAndClose(snapshot, metadata).await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Play Games write failed, using local slot: ${e.message}")
            false
        }
    }

    private suspend fun deleteFromPlayGames(
        account: com.google.android.gms.auth.api.signin.GoogleSignInAccount
    ) {
        val client = Games.getSnapshotsClient(context, account)
        val opened = client.open(
            CloudSnapshot.SNAPSHOT_NAME,
            false,
            SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED
        ).await()
        val meta = opened.data?.metadata ?: return
        client.delete(meta).await()
    }

    private fun accountId(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount): String =
        account.id ?: account.email ?: "unknown"

    private fun fallbackDir(): File =
        File(context.filesDir, "cloud_slot").also { it.mkdirs() }

    private fun fallbackFile(accountId: String): File =
        File(fallbackDir(), "${accountId.hashCode()}.json")

    private fun readFallback(accountId: String): CloudSnapshot? {
        val file = fallbackFile(accountId)
        if (!file.exists()) return null
        return parseOrNull(file.readBytes())
    }

    private fun writeFallback(accountId: String, bytes: ByteArray) {
        fallbackFile(accountId).writeBytes(bytes)
    }

    private fun parseOrNull(bytes: ByteArray?): CloudSnapshot? {
        if (bytes == null || bytes.isEmpty()) return null
        return try {
            val snap = cloudJson.decodeFromString(CloudSnapshot.serializer(), bytes.decodeToString())
            if (snap.schemaVersion != CloudSnapshot.SCHEMA_VERSION) {
                Log.w(TAG, "Unknown schema ${snap.schemaVersion}; treating as empty")
                null
            } else {
                snap
            }
        } catch (e: Exception) {
            Log.w(TAG, "Corrupt cloud snapshot ignored: ${e.message}")
            null
        }
    }

    private companion object {
        const val TAG = "CloudSaveRepository"
    }
}

/** In-memory cloud for unit tests. */
class InMemoryCloudSaveRepository : CloudSaveRepository {
    var stored: CloudSnapshot? = null
    var readCount: Int = 0
    var writeCount: Int = 0
    var deleteCount: Int = 0
    var failWrites: Boolean = false

    override suspend fun read(): CloudSnapshot? {
        readCount++
        return stored
    }

    override suspend fun write(snapshot: CloudSnapshot) {
        writeCount++
        if (failWrites) error("simulated write failure")
        stored = snapshot
    }

    override suspend fun deleteSlot() {
        deleteCount++
        stored = null
    }
}

fun encodeCloudSnapshot(snapshot: CloudSnapshot): String =
    cloudJson.encodeToString(CloudSnapshot.serializer(), snapshot)

fun decodeCloudSnapshotOrNull(raw: String): CloudSnapshot? = try {
    val snap = cloudJson.decodeFromString(CloudSnapshot.serializer(), raw)
    if (snap.schemaVersion != CloudSnapshot.SCHEMA_VERSION) null else snap
} catch (_: Exception) {
    null
}
