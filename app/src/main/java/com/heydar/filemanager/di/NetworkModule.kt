package com.heydar.filemanager.di

import android.app.Application
import android.content.Context
import com.heydar.filemanager.core.network.NetworkDiscoveryManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideNetworkDiscoveryManager(app: Application): NetworkDiscoveryManager {
        return NetworkDiscoveryManager(app)
    }
}
