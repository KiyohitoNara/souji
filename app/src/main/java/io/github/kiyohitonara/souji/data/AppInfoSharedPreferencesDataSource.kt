/*
 * Copyright (c) 2026 Kiyohito Nara
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
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.kiyohitonara.souji.model.AppInfo
import jakarta.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

open class AppInfoSharedPreferencesDataSource @Inject constructor(@ApplicationContext private val context: Context) : AppInfoDataSource {
    companion object {
        const val KEY_APP_PACKAGE_NAMES = "io.github.kiyohitonara.souji.APP_PACKAGE_NAMES"
    }

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    override val apps: Flow<List<AppInfo>> = callbackFlow {
        // Send the initial value
        val result = trySend(currentApps())
        if (result.isFailure) {
            Timber.e("Failed to send initial value")
        }

        // Listen for changes
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_APP_PACKAGE_NAMES) {
                val result = trySend(currentApps())
                if (result.isFailure) {
                    Timber.e("Failed to send value")
                }
            }
        }

        // Register the listener
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    override fun currentApps(): List<AppInfo> {
        Timber.d("Getting apps from shared preferences")

        return getStoredPackageNames().map { packageName ->
            Timber.d("Getting app: $packageName")
            AppInfo(packageName, true)
        }
    }

    /**
     * Upserts the given app information.
     * If the app is enabled, it will be added to the stored package names.
     * If the app is disabled, it will be removed from the stored package names.
     *
     * @param appInfo The app information to upsert.
     */
    open suspend fun upsertApp(appInfo: AppInfo) {
        Timber.d("Upserting app: ${appInfo.packageName}")

        val updatedPackageNames = getStoredPackageNames().toMutableSet().apply {
            if (appInfo.isEnabled) {
                add(appInfo.packageName)
            } else {
                remove(appInfo.packageName)
            }
        }

        sharedPreferences.edit {
            putStringSet(KEY_APP_PACKAGE_NAMES, updatedPackageNames)
        }
    }

    private fun getStoredPackageNames(): Set<String> {
        return sharedPreferences.getStringSet(KEY_APP_PACKAGE_NAMES, emptySet()) ?: emptySet()
    }
}
