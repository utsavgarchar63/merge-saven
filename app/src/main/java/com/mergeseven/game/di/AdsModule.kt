package com.mergeseven.game.di

import com.mergeseven.game.ads.AdMobAdService
import com.mergeseven.game.ads.AdService
import com.mergeseven.game.ads.ConsentManager
import com.mergeseven.game.ads.DefaultInterstitialPolicy
import com.mergeseven.game.ads.InterstitialPolicy
import com.mergeseven.game.ads.UmpConsentManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AdsModule {

    @Binds
    @Singleton
    abstract fun bindConsentManager(impl: UmpConsentManager): ConsentManager

    @Binds
    @Singleton
    abstract fun bindAdService(impl: AdMobAdService): AdService

    @Binds
    @Singleton
    abstract fun bindInterstitialPolicy(impl: DefaultInterstitialPolicy): InterstitialPolicy
}
