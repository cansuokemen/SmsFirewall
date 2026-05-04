package com.example.smsfirewall.di

import android.content.Context
import com.example.smsfirewall.data.ConversationMetaStore
import com.example.smsfirewall.data.SpamRetentionPreferenceStore
import com.example.smsfirewall.data.background.BackgroundImageRepository
import com.example.smsfirewall.data.background.BackgroundPreferenceStore
import com.example.smsfirewall.filter.FilterKeywordStore
import com.example.smsfirewall.notifications.MutedSenderStore
import com.example.smsfirewall.notifications.NotificationPreferenceStore
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

    @Provides
    @Singleton
    fun provideNotificationPreferenceStore(@ApplicationContext context: Context): NotificationPreferenceStore {
        return NotificationPreferenceStore(context)
    }

    @Provides
    @Singleton
    fun provideBackgroundPreferenceStore(@ApplicationContext context: Context): BackgroundPreferenceStore {
        return BackgroundPreferenceStore(context)
    }

    @Provides
    @Singleton
    fun provideBackgroundImageRepository(@ApplicationContext context: Context): BackgroundImageRepository {
        return BackgroundImageRepository(context)
    }

    @Provides
    @Singleton
    fun provideConversationMetaStore(@ApplicationContext context: Context): ConversationMetaStore {
        return ConversationMetaStore(context)
    }
}
