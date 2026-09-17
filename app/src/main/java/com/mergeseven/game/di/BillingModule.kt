package com.mergeseven.game.di

import com.mergeseven.game.billing.BillingRepository
import com.mergeseven.game.billing.FirebaseReceiptValidator
import com.mergeseven.game.billing.PlayBillingRepository
import com.mergeseven.game.billing.ReceiptValidator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BillingModule {

    @Binds
    @Singleton
    abstract fun bindBillingRepository(impl: PlayBillingRepository): BillingRepository

    @Binds
    @Singleton
    abstract fun bindReceiptValidator(impl: FirebaseReceiptValidator): ReceiptValidator
}
