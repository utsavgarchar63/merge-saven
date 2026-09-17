package com.mergeseven.game.core.liveops

import android.content.Context
import android.util.Log
import com.mergeseven.game.core.DispatcherProvider
import com.mergeseven.game.core.crash.CrashReporter
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.di.PersistenceScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads remote level JSON packs when RC provides a manifest URL (AF6-09).
 */
@Singleton
class LevelPackDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val featureFlags: FeatureFlags,
    private val dispatchers: DispatcherProvider,
    private val crashReporter: CrashReporter,
    @PersistenceScope private val scope: CoroutineScope
) {
    private val packDir: File
        get() = File(context.filesDir, "level_packs").also { it.mkdirs() }

    fun cachedFile(levelId: Int): File = File(packDir, "level_%02d.json".format(levelId))

    fun refreshAsync() {
        if (!featureFlags.isEnabled(Feature.AF6)) return
        scope.launch {
            withContext(dispatchers.io) {
                runCatching { refreshBlocking() }
                    .onFailure {
                        crashReporter.recordNonFatal(it, "Level pack refresh failed")
                        Log.w(TAG, "Level pack refresh failed: ${it.message}")
                    }
            }
        }
    }

    fun refreshBlocking() {
        val url = remoteConfigRepository.liveConfig.levelPackManifestUrl()
        if (url.isBlank()) return
        val body = httpGet(url) ?: return
        val manifest = runCatching {
            LiveConfigDefaults.json.decodeFromString<LevelPackManifest>(body)
        }.getOrElse {
            crashReporter.recordNonFatal(it, "Bad level pack manifest")
            return
        }
        for (entry in manifest.levels) {
            downloadEntry(entry)
        }
    }

    private fun downloadEntry(entry: LevelPackEntry) {
        val bytes = httpGetBytes(entry.url) ?: return
        val digest = sha256Hex(bytes)
        if (!digest.equals(entry.sha256, ignoreCase = true)) {
            Log.w(TAG, "SHA mismatch for level ${entry.levelId}")
            return
        }
        val target = cachedFile(entry.levelId)
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(target)) {
            target.writeBytes(bytes)
            tmp.delete()
        }
    }

    private fun httpGet(url: String): String? =
        httpGetBytes(url)?.toString(Charsets.UTF_8)

    private fun httpGetBytes(url: String): ByteArray? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 8_000
            requestMethod = "GET"
        }
        return try {
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val TAG = "LevelPackDownloader"
    }
}
