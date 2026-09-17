package com.mergeseven.game.ui.feel

import androidx.compose.ui.graphics.Color
import com.mergeseven.game.core.Constants
import kotlin.collections.ArrayDeque
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class Particle(
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var life: Float = 0f,
    var maxLife: Float = 0f,
    var size: Float = 4f,
    var color: Color = Color.White,
    var active: Boolean = false
) {
    val alpha: Float
        get() = if (maxLife <= 0f) 0f else (life / maxLife).coerceIn(0f, 1f)
}

/**
 * AF10-01: pooled particle allocator for merge bursts, chain sparks, confetti.
 */
class ParticleSystem(
    poolSize: Int = Constants.AF10_PARTICLE_POOL_SIZE,
    private val random: Random = Random.Default
) {
    private val pool = Array(poolSize) { Particle() }
    private val free = ArrayDeque<Particle>(poolSize).apply {
        pool.forEach { addLast(it) }
    }
    private val active = ArrayList<Particle>(poolSize)

    val activeParticles: List<Particle>
        get() = active

    val freeCount: Int
        get() = free.size

    val activeCount: Int
        get() = active.size

    fun clear() {
        for (i in active.indices.reversed()) {
            release(active[i])
        }
        active.clear()
    }

    fun emitBurst(
        x: Float,
        y: Float,
        count: Int,
        color: Color,
        maxActive: Int = Constants.AF10_PARTICLE_CAP_FULL
    ) {
        val toEmit = count.coerceAtMost(remainingCapacity(maxActive))
        repeat(toEmit) {
            val p = acquire() ?: return
            val angle = random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = 80f + random.nextFloat() * 180f
            p.x = x
            p.y = y
            p.vx = cos(angle) * speed
            p.vy = sin(angle) * speed
            p.maxLife = 0.35f + random.nextFloat() * 0.35f
            p.life = p.maxLife
            p.size = 3f + random.nextFloat() * 5f
            p.color = color
            active.add(p)
        }
    }

    fun emitSparks(
        fromX: Float,
        fromY: Float,
        toX: Float,
        toY: Float,
        count: Int,
        color: Color,
        maxActive: Int = Constants.AF10_PARTICLE_CAP_FULL
    ) {
        val toEmit = count.coerceAtMost(remainingCapacity(maxActive))
        repeat(toEmit) { i ->
            val p = acquire() ?: return
            val t = if (toEmit == 1) 0.5f else i / (toEmit - 1).toFloat()
            p.x = fromX + (toX - fromX) * t
            p.y = fromY + (toY - fromY) * t
            p.vx = (toX - fromX) * 0.4f + (random.nextFloat() - 0.5f) * 60f
            p.vy = (toY - fromY) * 0.4f + (random.nextFloat() - 0.5f) * 60f
            p.maxLife = 0.25f + random.nextFloat() * 0.25f
            p.life = p.maxLife
            p.size = 2.5f + random.nextFloat() * 3.5f
            p.color = color
            active.add(p)
        }
    }

    fun emitConfetti(
        width: Float,
        count: Int,
        colors: List<Color>,
        maxActive: Int = Constants.AF10_PARTICLE_CAP_FULL
    ) {
        if (colors.isEmpty()) return
        val toEmit = count.coerceAtMost(remainingCapacity(maxActive))
        repeat(toEmit) {
            val p = acquire() ?: return
            p.x = random.nextFloat() * width
            p.y = -10f - random.nextFloat() * 40f
            p.vx = (random.nextFloat() - 0.5f) * 80f
            p.vy = 120f + random.nextFloat() * 180f
            p.maxLife = 1.2f + random.nextFloat() * 0.8f
            p.life = p.maxLife
            p.size = 4f + random.nextFloat() * 6f
            p.color = colors[random.nextInt(colors.size)]
            active.add(p)
        }
    }

    /**
     * @param dtSeconds simulation delta already scaled by slow-mo / hit-stop
     */
    fun tick(dtSeconds: Float) {
        if (dtSeconds <= 0f) return
        var i = 0
        while (i < active.size) {
            val p = active[i]
            p.life -= dtSeconds
            if (p.life <= 0f) {
                release(p)
                active.removeAt(i)
                continue
            }
            p.x += p.vx * dtSeconds
            p.y += p.vy * dtSeconds
            p.vy += 220f * dtSeconds
            i++
        }
    }

    private fun remainingCapacity(maxActive: Int): Int =
        (maxActive - active.size).coerceAtLeast(0)

    private fun acquire(): Particle? {
        val p = free.removeFirstOrNull() ?: return null
        p.active = true
        return p
    }

    private fun release(p: Particle) {
        p.active = false
        p.life = 0f
        free.addLast(p)
    }
}
