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
import io.github.kiyohitonara.souji.model.AppInfo
import timber.log.Timber

class AppInfoDeviceDataSource(private val context: Context) {
    fun getApps(): List<AppInfo> {
        Timber.d("Getting apps from device")

        val apps = mutableListOf<AppInfo>()

        val manager = context.packageManager
        val packages = manager.getInstalledPackages(PackageManager.GET_META_DATA)
        for (packageInfo in packages) {
            Timber.d("Getting app: ${packageInfo.packageName}")

            val appInfo = AppInfo(
                packageInfo.packageName,
                packageInfo.applicationInfo.loadLabel(manager).toString(),
                packageInfo.applicationInfo.loadIcon(manager)
            )

            apps.add(appInfo)
        }

        return apps
    }
}