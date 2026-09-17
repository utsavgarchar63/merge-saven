package com.mergeseven.game.di

import com.mergeseven.game.competitive.DailyScoreValidator
import com.mergeseven.game.competitive.FirebaseDailyScoreValidator
import com.mergeseven.game.competitive.FirestoreTournamentRepository
import com.mergeseven.game.competitive.LeaderboardRepository
import com.mergeseven.game.competitive.PlayGamesLeaderboardRepository
import com.mergeseven.game.competitive.TournamentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CompetitiveModule {

    @Binds
    @Singleton
    abstract fun bindLeaderboardRepository(impl: PlayGamesLeaderboardRepository): LeaderboardRepository

    @Binds
    @Singleton
    abstract fun bindDailyScoreValidator(impl: FirebaseDailyScoreValidator): DailyScoreValidator

    @Binds
    @Singleton
    abstract fun bindTournamentRepository(impl: FirestoreTournamentRepository): TournamentRepository
}
