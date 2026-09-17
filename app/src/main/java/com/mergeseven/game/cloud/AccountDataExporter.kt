package com.mergeseven.game.cloud

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountDataExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val builder: CloudSnapshotBuilder
) {
    /**
     * Writes the current meta-progress snapshot to cache and returns a share [Intent].
     */
    suspend fun exportShareIntent(): Intent {
        val snapshot = builder.build()
        val json = encodeCloudSnapshot(snapshot)
        val dir = File(context.cacheDir, "exports").also { it.mkdirs() }
        val file = File(dir, "merge_seven_export_${snapshot.updatedAtEpochMs}.json")
        file.writeText(json)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Merge Seven data export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    suspend fun exportJsonString(): String = encodeCloudSnapshot(builder.build())
}
