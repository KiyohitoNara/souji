/*
 * Copyright (c) 2024 Kiyohito Nara
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.kiyohitonara.souji.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.kiyohitonara.souji.data.AppDatabase
import io.github.kiyohitonara.souji.data.AppInfoDao
import io.github.kiyohitonara.souji.data.AppInfoDataSource
import io.github.kiyohitonara.souji.data.AppInfoDatabaseDataSource
import io.github.kiyohitonara.souji.data.AppInfoDeviceDataSource
import io.github.kiyohitonara.souji.data.AppInfoRepository
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AppModule::class]
)
object FakeAppModule {
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @Singleton
    @Provides
    fun provideAppInfoDao(appDatabase: AppDatabase): AppInfoDao {
        return appDatabase.appInfoDao()
    }

    @Singleton
    @Provides
    @DatabaseDataSource
    fun provideDatabaseDataSource(dao: AppInfoDao): AppInfoDataSource {
        return AppInfoDatabaseDataSource(dao)
    }

    @Singleton
    @Provides
    fun provideDeviceDataSource(@ApplicationContext context: Context): AppInfoDeviceDataSource {
        return AppInfoDeviceDataSource(context)
    }

    @Singleton
    @Provides
    fun provideRepository(deviceDataSource: AppInfoDeviceDataSource): AppInfoRepository {
        return AppInfoRepository(deviceDataSource)
    }
}