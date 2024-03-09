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

import io.github.kiyohitonara.souji.model.AppInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import timber.log.Timber
import javax.inject.Inject

class AppInfoRepository @Inject constructor(private val deviceDataSource: AppInfoDataSource, private val databaseDataSource: AppInfoDataSource) {
    fun getApps(): Flow<List<AppInfo>> {
        Timber.d("Getting apps")

        val apps = combine(deviceDataSource.getApps(), databaseDataSource.getApps()) { deviceApps, databaseApps ->
            val apps = mutableListOf<AppInfo>()

            for (deviceApp in deviceApps) {
                val databaseApp = databaseApps.find { it.packageName == deviceApp.packageName }

                if (databaseApp != null) {
                    val app = deviceApp.copy(isEnabled = databaseApp.isEnabled)
                    apps.add(app)
                } else {
                    apps.add(deviceApp)
                }
            }

            return@combine apps
        }

        return apps
    }

    suspend fun upsertApp(appInfo: AppInfo) {
        Timber.d("Upserting app: $appInfo")

        databaseDataSource.upsertApp(appInfo)
    }
}