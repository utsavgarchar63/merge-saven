package com.mergeseven.game.di

import javax.inject.Qualifier

/**
 * An application-lifetime coroutine scope for storage writes.
 *
 * Repository writes must not be tied to a ViewModel: a coin grant issued as the player leaves the
 * screen still has to reach disk. The scope is backed by a single-threaded dispatcher so writes are
 * applied in the order they were issued.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PersistenceScope
