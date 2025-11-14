package com.a307.linkcare.common.di

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import com.a307.linkcare.feature.caregroup.data.CareGroupApi
import com.a307.linkcare.feature.healthgroup.data.HealthGroupApi
import com.a307.linkcare.feature.commongroup.data.GroupApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HealthConnectModule {

    @Provides
    @Singleton
    fun provideHealthConnectClient(
        @ApplicationContext context: Context
    ): HealthConnectClient {
        return HealthConnectClient.getOrCreate(context)
    }

    @Provides
    @Singleton
    fun provideHealthGroupApi(retrofit: Retrofit): HealthGroupApi =
        retrofit.create(HealthGroupApi::class.java)

    @Provides
    @Singleton
    fun provideCareGroupApi(
        retrofit: Retrofit
    ): CareGroupApi = retrofit.create(CareGroupApi::class.java)

    @Provides
    @Singleton
    fun provideGroupApi(retrofit: Retrofit): GroupApi {
        return retrofit.create(GroupApi::class.java)
    }
}
