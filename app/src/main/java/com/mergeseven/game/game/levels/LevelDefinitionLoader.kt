package com.mergeseven.game.game.levels

import android.content.Context
import android.util.Log
import com.mergeseven.game.core.liveops.LevelPackDownloader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Optional cached remote level JSON (AF6-09). */
fun interface LevelPackCache {
    fun readCachedJson(levelId: Int): String?
}

@Singleton
class FileLevelPackCache @Inject constructor(
    private val downloader: LevelPackDownloader
) : LevelPackCache {
    override fun readCachedJson(levelId: Int): String? {
        val cached = downloader.cachedFile(levelId)
        if (!cached.isFile) return null
        return runCatching { cached.readText() }
            .onFailure { Log.w("LevelPackCache", "Failed to read ${cached.name}: ${it.message}") }
            .getOrNull()
    }
}

/**
 * Loads optional per-level JSON: cached remote pack first, then assets (AF1-10 / AF6-09).
 */
@Singleton
class LevelDefinitionLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val levelPackCache: LevelPackCache
) {

    fun load(level: Int): LevelDefinition? {
        levelPackCache.readCachedJson(level)?.let { text ->
            return runCatching { parse(text) }
                .onFailure { Log.w(TAG, "Failed to parse cached level $level: ${it.message}") }
                .getOrNull()
        }
        val path = pathFor(level)
        return try {
            context.assets.open(path).bufferedReader().use { reader ->
                parse(reader.readText())
            }
        } catch (_: IOException) {
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse $path: ${e.message}")
            null
        }
    }

    fun parse(jsonText: String): LevelDefinition =
        json.decodeFromString(LevelDefinition.serializer(), jsonText)

    fun pathFor(level: Int): String = "levels/level_%02d.json".format(level)

    companion object {
        private const val TAG = "LevelDefinitionLoader"

        val json: Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            isLenient = true
            classDiscriminator = "type"
        }

        /** Assets-only loader for unit tests. */
        fun assetsOnly(context: Context): LevelDefinitionLoader =
            LevelDefinitionLoader(context, LevelPackCache { null })
    }
}
