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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.kiyohitonara.souji.model.AppInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import javax.inject.Inject

open class AppInfoDeviceDataSource @Inject constructor(@ApplicationContext private val context: Context) : AppInfoDataSource {
    override val apps: Flow<List<AppInfo>> = callbackFlow {
        // Send the initial value
        val result = trySend(currentApps())
        if (result.isFailure) {
            Timber.e("Failed to send initial value")
        }

        // Listen for changes
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val result = trySend(currentApps())
                if (result.isFailure) {
                    Timber.e("Failed to send value")
                }
            }
        }

        // Register the receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        context.registerReceiver(receiver, filter)
        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

    override fun currentApps(): List<AppInfo> {
        Timber.d("Getting apps from device")

        return context.packageManager.getInstalledPackages(PackageManager.GET_META_DATA).map { packageInfo ->
            Timber.d("Getting app: ${packageInfo.packageName}")

            AppInfo(
                packageInfo.packageName,
                packageInfo.applicationInfo.loadLabel(context.packageManager).toString(),
                packageInfo.applicationInfo.loadIcon(context.packageManager)
            )
        }
    }
}
