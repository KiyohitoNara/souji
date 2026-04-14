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

package io.github.kiyohitonara.souji

import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import dagger.hilt.android.AndroidEntryPoint
import io.github.kiyohitonara.souji.data.AppInfoSharedPreferencesDataSource
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
open class SoujiService : NotificationListenerService() {
    @Inject
    lateinit var dataSource: AppInfoSharedPreferencesDataSource

    companion object {
        const val ACTION_NOTIFICATION_CANCELLED = "io.github.kiyohitonara.souji.NOTIFICATION_CANCELLED"
        const val ACTION_NOTIFICATIONS_CANCELLED = "io.github.kiyohitonara.souji.NOTIFICATIONS_CANCELLED"
        const val EXTRA_CANCELLED_NOTIFICATION_PACKAGE_NAMES = "io.github.kiyohitonara.souji.CANCELLED_NOTIFICATION_PACKAGE_NAMES"
        const val EXTRA_CANCELLED_NOTIFICATION_PACKAGE_NAME = "io.github.kiyohitonara.souji.CANCELLED_NOTIFICATION_PACKAGE_NAME"

        /** Starts the service to cancel notifications for enabled apps. */
        fun startService(context: Context) {
            context.startService(Intent(context, SoujiService::class.java))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("Service is started")

        val packageNames = dataSource.currentApps().map { it.packageName }
        if (packageNames.isNotEmpty()) {
            cancelActiveNotifications(packageNames)
        }

        stopSelf()

        return START_NOT_STICKY
    }

    private fun cancelActiveNotifications(packageNames: List<String>) {
        Timber.d("Cancelling active notifications")

        packageNames.forEach { packageName ->
            cancelActiveNotification(packageName)
        }

        val intent = Intent(ACTION_NOTIFICATIONS_CANCELLED)
        intent.putExtra(EXTRA_CANCELLED_NOTIFICATION_PACKAGE_NAMES, packageNames.toTypedArray())
        sendBroadcast(intent)
    }

    private fun cancelActiveNotification(packageName: String) {
        Timber.d("Cancelling active notification: $packageName")

        val cancellableNotifications = activeNotifications.filter { it.packageName == packageName }
        cancellableNotifications.forEach { notification ->
            cancelNotification(notification.key)
        }

        val intent = Intent(ACTION_NOTIFICATION_CANCELLED)
        intent.putExtra(EXTRA_CANCELLED_NOTIFICATION_PACKAGE_NAME, packageName)
        sendBroadcast(intent)
    }
}