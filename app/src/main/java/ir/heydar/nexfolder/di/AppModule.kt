package ir.heydar.nexfolder.di

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ir.heydar.nexfolder.data.local.RecentFileDao
import ir.heydar.nexfolder.data.local.RecentsDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppContext(app: Application): Context = app.applicationContext

    @Provides
    @Singleton
    fun provideRecentsDatabase(app: Application): RecentsDatabase =
        RecentsDatabase.build(app)

    @Provides
    @Singleton
    fun provideRecentFileDao(database: RecentsDatabase): RecentFileDao =
        database.recentFileDao()
}