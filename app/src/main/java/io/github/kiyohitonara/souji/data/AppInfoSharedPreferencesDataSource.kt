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

package io.github.kiyohitonara.souji.data

import android.content.Context
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.kiyohitonara.souji.model.AppInfo
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

open class AppInfoSharedPreferencesDataSource @Inject constructor(@ApplicationContext private val context: Context) : AppInfoDataSource {
    companion object {
        const val KEY_APP_PACKAGE_NAMES = "io.github.kiyohitonara.souji.APP_PACKAGE_NAMES"
    }

    override fun getApps(): List<AppInfo> {
        Timber.d("Getting apps from shared preferences")

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        val packageNames = sharedPreferences.getStringSet(KEY_APP_PACKAGE_NAMES, emptySet()) ?: emptySet()
        val apps = packageNames.map { packageName ->
            Timber.d("Getting app: $packageName")

            AppInfo(
                packageName,
                true,
            )
        }

        return apps
    }

    override fun getAppsFlow(): Flow<List<AppInfo>> {
        throw UnsupportedOperationException("Shared preferences data source does not support getting apps")
    }

    override suspend fun upsertApp(appInfo: AppInfo) {
        throw UnsupportedOperationException("Shared preferences data source does not support upserting apps")
    }

    override suspend fun upsertApps(appInfos: List<AppInfo>) {
        Timber.d("Upserting ${appInfos.size} apps")

        val packageNames = appInfos.filter { it.isEnabled }.map { appInfo -> appInfo.packageName }.toSet()

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.edit().apply {
            putStringSet(KEY_APP_PACKAGE_NAMES, packageNames)
            apply()
        }
    }
}