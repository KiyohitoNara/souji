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
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.kiyohitonara.souji.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

open class AppInfoDeviceDataSource @Inject constructor(@ApplicationContext private val context: Context) : AppInfoDataSource {
    override val apps: Flow<List<AppInfo>> = callbackFlow {
        // null means "reload every app from scratch"; a package name means "only that app changed".
        // Requests are processed one at a time, on a single worker, so the cache is never mutated
        // concurrently.
        val refreshRequests = Channel<String?>(Channel.UNLIMITED)
        val cache = linkedMapOf<String, AppInfo>()

        launch(Dispatchers.IO) {
            refreshRequests.consumeEach { packageName ->
                if (packageName == null) {
                    cache.clear()
                    currentApps().forEach { app -> cache[app.packageName] = app }
                } else {
                    val app = resolveApp(packageName)
                    if (app != null) {
                        cache[packageName] = app
                    } else {
                        cache.remove(packageName)
                    }
                }

                val result = trySend(cache.values.toList())
                if (result.isFailure) {
                    Timber.e("Failed to send value")
                }
            }
        }

        // Request the initial value
        refreshRequests.trySend(null)

        // Listen for changes
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                refreshRequests.trySend(intent.data?.schemeSpecificPart)
            }
        }

        // Register the receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        awaitClose {
            context.unregisterReceiver(receiver)
            refreshRequests.close()
        }
    }

    override fun currentApps(): List<AppInfo> {
        Timber.d("Getting apps from device")

        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = context.packageManager.queryIntentActivities(launcherIntent, 0)
        val distinctResolveInfos = resolveInfos.distinctBy { it.activityInfo.packageName }

        return distinctResolveInfos.map { resolveInfo ->
            val applicationInfo = resolveInfo.activityInfo.applicationInfo

            Timber.d("Getting app: ${applicationInfo.packageName}")

            AppInfo(
                applicationInfo.packageName,
                applicationInfo.loadLabel(context.packageManager).toString(),
                applicationInfo.loadIcon(context.packageManager),
            )
        }
    }

    /**
     * Resolves a single app's current info, or `null` if it no longer has a launcher activity
     * (e.g. it was uninstalled).
     */
    private fun resolveApp(packageName: String): AppInfo? {
        Timber.d("Getting app: $packageName")

        val launcherIntent =
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(packageName)
        val resolveInfo = context.packageManager.resolveActivity(launcherIntent, 0) ?: return null
        val applicationInfo = resolveInfo.activityInfo.applicationInfo

        return AppInfo(
            applicationInfo.packageName,
            applicationInfo.loadLabel(context.packageManager).toString(),
            applicationInfo.loadIcon(context.packageManager),
        )
    }
}
