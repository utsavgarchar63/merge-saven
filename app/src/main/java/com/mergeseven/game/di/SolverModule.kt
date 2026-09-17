package com.mergeseven.game.di

import com.mergeseven.game.game.solver.DifficultyProfileProvider
import com.mergeseven.game.game.solver.RemoteAwareDifficultyProfileProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SolverModule {

    @Binds
    @Singleton
    abstract fun bindDifficultyProfileProvider(
        impl: RemoteAwareDifficultyProfileProvider
    ): DifficultyProfileProvider
}
