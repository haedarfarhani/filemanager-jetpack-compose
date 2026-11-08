package ir.heydar.nexfolder.di

import android.app.Application
import android.content.Context
import ir.heydar.nexfolder.core.network.NetworkDiscoveryManager
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
