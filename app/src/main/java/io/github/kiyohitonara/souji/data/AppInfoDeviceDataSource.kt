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
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.kiyohitonara.souji.model.AppInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import timber.log.Timber
import javax.inject.Inject

open class AppInfoDeviceDataSource @Inject constructor(@ApplicationContext private val context: Context) : AppInfoDataSource {
    override fun getApps(): List<AppInfo> {
        val apps = context.packageManager.getInstalledPackages(PackageManager.GET_META_DATA).map { packageInfo ->
            Timber.d("Getting app: ${packageInfo.packageName}")

            AppInfo(
                packageInfo.packageName,
                packageInfo.applicationInfo.loadLabel(context.packageManager).toString(),
                packageInfo.applicationInfo.loadIcon(context.packageManager)
            )
        }

        return apps
    }

    override fun getAppsFlow(): Flow<List<AppInfo>> {
        Timber.d("Getting apps from device")

        return flowOf(getApps())
    }

    override suspend fun upsertApp(appInfo: AppInfo) {
        throw UnsupportedOperationException("Device data source does not support upserting apps")
    }

    override suspend fun upsertApps(appInfos: List<AppInfo>) {
        throw UnsupportedOperationException("Device data source does not support upserting apps")
    }
}