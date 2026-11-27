package cn.primeessence.music

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.SessionToken
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun provideSessionToken(@ApplicationContext context: Context): SessionToken =
        SessionToken(context, ComponentName(context, PlaybackService::class.java))
}
