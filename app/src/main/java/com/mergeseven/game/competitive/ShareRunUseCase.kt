package com.mergeseven.game.competitive

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.mergeseven.game.core.analytics.AnalyticsEvents
import com.mergeseven.game.core.analytics.AnalyticsTracker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class ShareRunData(
    val score: Long,
    val maxTile: Int,
    val modeLabel: String,
    val dateLabel: String
)

@Singleton
class ResultCardRenderer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun render(data: ShareRunData): Bitmap {
        val template = runCatching {
            context.assets.open("art/share_result_card.png").use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
            ?: BitmapFactory.decodeResource(context.resources, android.R.drawable.dialog_holo_light_frame)

        val mutable = template.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFD54A.toInt()
            textAlign = Paint.Align.CENTER
            textSize = mutable.width * 0.07f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textAlign = Paint.Align.CENTER
            textSize = mutable.width * 0.12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE8D5B5.toInt()
            textAlign = Paint.Align.CENTER
            textSize = mutable.width * 0.045f
        }
        val cx = mutable.width / 2f
        canvas.drawText(data.modeLabel.uppercase(), cx, mutable.height * 0.22f, titlePaint)
        canvas.drawText(data.score.toString(), cx, mutable.height * 0.48f, scorePaint)
        canvas.drawText("Biggest tile ${data.maxTile}", cx, mutable.height * 0.58f, bodyPaint)
        canvas.drawText(data.dateLabel, cx, mutable.height * 0.66f, bodyPaint)
        canvas.drawText("MERGE SEVEN", cx, mutable.height * 0.88f, titlePaint)
        return mutable
    }
}

@Singleton
class ShareRunUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val renderer: ResultCardRenderer,
    private val analytics: AnalyticsTracker
) {
    fun createShareIntent(data: ShareRunData): Intent {
        val bitmap = renderer.render(data)
        val dir = File(context.cacheDir, "exports").also { it.mkdirs() }
        val file = File(dir, "merge_seven_run_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 92, out)
        }
        if (!bitmap.isRecycled) bitmap.recycle()
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        analytics.logEvent(
            AnalyticsEvents.SHARE_RUN,
            mapOf("mode" to data.modeLabel, "score" to data.score.toString())
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Merge Seven — ${data.score}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
