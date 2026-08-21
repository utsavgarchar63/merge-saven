package com.mergeseven.game.di

import com.mergeseven.game.core.DefaultDispatcherProvider
import com.mergeseven.game.core.DispatcherProvider
import com.mergeseven.game.game.engine.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing app-wide dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider {
        return DefaultDispatcherProvider()
    }

    @Provides
    @Singleton
    fun provideBoardEngine(): BoardEngine {
        return BoardEngine()
    }

    @Provides
    @Singleton
    fun provideScoreEngine(): ScoreEngine {
        return ScoreEngine()
    }

    @Provides
    @Singleton
    fun provideSpawnEngine(): SpawnEngine {
        return SpawnEngine()
    }

    @Provides
    @Singleton
    fun providePlacementEngine(): PlacementEngine {
        return PlacementEngine()
    }

    @Provides
    @Singleton
    fun provideMergeEngine(scoreEngine: ScoreEngine): MergeEngine {
        return MergeEngine(scoreEngine)
    }

    @Provides
    @Singleton
    fun provideGameOverEngine(placementEngine: PlacementEngine): GameOverEngine {
        return GameOverEngine(placementEngine)
    }

    @Provides
    @Singleton
    fun provideChainReactionEngine(mergeEngine: MergeEngine): ChainReactionEngine {
        return ChainReactionEngine(mergeEngine)
    }

    @Provides
    @Singleton
    fun provideGameEngine(
        boardEngine: BoardEngine,
        mergeEngine: MergeEngine,
        placementEngine: PlacementEngine,
        spawnEngine: SpawnEngine,
        scoreEngine: ScoreEngine,
        gameOverEngine: GameOverEngine,
        chainReactionEngine: ChainReactionEngine
    ): GameEngine {
        return GameEngineImpl(
            boardEngine = boardEngine,
            mergeEngine = mergeEngine,
            placementEngine = placementEngine,
            spawnEngine = spawnEngine,
            scoreEngine = scoreEngine,
            gameOverEngine = gameOverEngine,
            chainReactionEngine = chainReactionEngine
        )
    }
}
