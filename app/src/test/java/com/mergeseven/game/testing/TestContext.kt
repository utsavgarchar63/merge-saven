package com.mergeseven.game.testing

import android.content.Context
import android.content.ContextWrapper

/**
 * A stand-in [Context] for JVM unit tests.
 *
 * `ContextWrapper` is concrete, so with `testOptions.unitTests.isReturnDefaultValues` enabled every
 * call on it is a no-op. That is enough for classes such as `AudioManager`, which only touch the
 * context inside guarded blocks and degrade to silence when audio is unavailable.
 *
 * A `Proxy` cannot be used here: [Context] is an abstract class, not an interface.
 */
fun fakeContext(): Context = ContextWrapper(null)
