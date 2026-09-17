package com.mergeseven.game.testing

import com.mergeseven.game.cloud.CloudEconomyNotifier
import com.mergeseven.game.core.DateProvider
import com.mergeseven.game.core.DispatcherProvider
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.core.flags.InMemoryFeatureFlags
import com.mergeseven.game.data.local.store.InMemoryLevelProgressStore
import com.mergeseven.game.data.local.store.InMemoryUserProfileStore
import com.mergeseven.game.data.local.store.LevelProgressStore
import com.mergeseven.game.data.local.store.UserProfileStore
import com.mergeseven.game.data.repository.LevelRepository
import com.mergeseven.game.data.repository.UserDataRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/**
 * Builders for repositories under test.
 *
 * Everything runs on an unconfined test dispatcher so the asynchronous hydration in each
 * repository's `init` block has completed by the time the constructor returns, which keeps the
 * tests free of arbitrary waiting.
 */
@OptIn(ExperimentalCoroutinesApi::class)
object TestPersistence {

    const val TODAY = "2026-01-01"

    fun userDataRepository(
        store: UserProfileStore = InMemoryUserProfileStore(),
        scope: CoroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
        today: String = TODAY,
        featureFlags: FeatureFlags = InMemoryFeatureFlags(isDebug = true),
        cloudEconomyNotifier: CloudEconomyNotifier = CloudEconomyNotifier()
    ): UserDataRepository = UserDataRepository(
        store = store,
        scope = scope,
        dateProvider = DateProvider { today },
        featureFlags = featureFlags,
        cloudEconomyNotifier = cloudEconomyNotifier
    )

    fun levelRepository(
        store: LevelProgressStore = InMemoryLevelProgressStore(),
        scope: CoroutineScope = CoroutineScope(UnconfinedTestDispatcher())
    ): LevelRepository = LevelRepository(store = store, scope = scope)

    fun dispatchers(dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()): DispatcherProvider =
        object : DispatcherProvider {
            override val main = dispatcher
            override val io = dispatcher
            override val default = dispatcher
            override val unconfined = dispatcher
        }
}
