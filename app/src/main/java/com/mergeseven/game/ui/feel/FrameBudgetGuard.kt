package com.mergeseven.game.ui.feel

import com.mergeseven.game.core.Constants

/**
 * AF10-09: EMA of frame times; lowers particle quality when over budget.
 */
class FrameBudgetGuard(
    private val budgetMs: Float = Constants.AF10_FRAME_BUDGET_MS,
    private val emaAlpha: Float = 0.2f
) {
    enum class QualityTier {
        FULL,
        HALF,
        QUARTER,
        OFF
    }

    var emaFrameMs: Float = budgetMs
        private set

    var tier: QualityTier = QualityTier.FULL
        private set

    fun recordFrameMs(frameMs: Float) {
        emaFrameMs = emaAlpha * frameMs + (1f - emaAlpha) * emaFrameMs
        tier = when {
            emaFrameMs > budgetMs * 1.6f -> QualityTier.OFF
            emaFrameMs > budgetMs * 1.3f -> QualityTier.QUARTER
            emaFrameMs > budgetMs -> QualityTier.HALF
            emaFrameMs < budgetMs * 0.85f && tier != QualityTier.FULL -> {
                when (tier) {
                    QualityTier.OFF -> QualityTier.QUARTER
                    QualityTier.QUARTER -> QualityTier.HALF
                    else -> QualityTier.FULL
                }
            }
            else -> tier
        }
    }

    fun activeParticleCap(): Int = when (tier) {
        QualityTier.FULL -> Constants.AF10_PARTICLE_CAP_FULL
        QualityTier.HALF -> Constants.AF10_PARTICLE_CAP_HALF
        QualityTier.QUARTER -> Constants.AF10_PARTICLE_CAP_QUARTER
        QualityTier.OFF -> 0
    }

    fun reset() {
        emaFrameMs = budgetMs
        tier = QualityTier.FULL
    }
}
