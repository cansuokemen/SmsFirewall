package com.example.smsfirewall.di

import android.content.Context
import com.example.smsfirewall.data.SpamRetentionPreferenceStore
import com.example.smsfirewall.filter.FilterKeywordStore
import com.example.smsfirewall.notifications.MutedSenderStore
import com.example.smsfirewall.ui.theme.ThemePreferenceStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFilterKeywordStore(@ApplicationContext context: Context): FilterKeywordStore {
        return FilterKeywordStore(context)
    }

    @Provides
    @Singleton
    fun provideMutedSenderStore(@ApplicationContext context: Context): MutedSenderStore {
        return MutedSenderStore(context)
    }

    @Provides
    @Singleton
    fun provideThemePreferenceStore(@ApplicationContext context: Context): ThemePreferenceStore {
        return ThemePreferenceStore(context)
    }

    @Provides
    @Singleton
    fun provideSpamRetentionPreferenceStore(@ApplicationContext context: Context): SpamRetentionPreferenceStore {
        return SpamRetentionPreferenceStore(context)
    }
}
