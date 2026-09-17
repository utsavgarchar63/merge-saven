package com.mergeseven.game.core.crash

import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrashlyticsReporter @Inject constructor(
    private val crashlytics: FirebaseCrashlytics
) : CrashReporter {

    override fun setCustomKey(key: String, value: String) {
        runCatching { crashlytics.setCustomKey(key, value) }
    }

    override fun setCustomKey(key: String, value: Int) {
        runCatching { crashlytics.setCustomKey(key, value) }
    }

    override fun setCustomKey(key: String, value: Long) {
        runCatching { crashlytics.setCustomKey(key, value) }
    }

    override fun log(message: String) {
        runCatching { crashlytics.log(message) }
    }

    override fun recordNonFatal(throwable: Throwable, message: String?) {
        runCatching {
            if (message != null) crashlytics.log(message)
            crashlytics.recordException(throwable)
        }
    }
}

/** Unit-test / offline stub. */
class NoOpCrashReporter : CrashReporter {
    val keys = mutableMapOf<String, String>()
    val logs = mutableListOf<String>()

    override fun setCustomKey(key: String, value: String) {
        keys[key] = value
    }

    override fun setCustomKey(key: String, value: Int) {
        keys[key] = value.toString()
    }

    override fun setCustomKey(key: String, value: Long) {
        keys[key] = value.toString()
    }

    override fun log(message: String) {
        logs += message
    }

    override fun recordNonFatal(throwable: Throwable, message: String?) {
        logs += message ?: throwable.message.orEmpty()
    }
}
