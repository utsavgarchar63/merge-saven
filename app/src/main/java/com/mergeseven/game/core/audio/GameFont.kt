package com.mergeseven.game.core.audio

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.mergeseven.game.R

/** Reuse the bundled Nunito face for Canvas digits and shared result cards. */
object GameFont {
    @Volatile private var cached: Typeface? = null
    fun bold(context: Context): Typeface = cached ?: synchronized(this) {
        cached ?: (ResourcesCompat.getFont(context, R.font.nunito_bold) ?: Typeface.DEFAULT_BOLD).also { cached = it }
    }
}
