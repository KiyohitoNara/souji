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

import android.content.Intent
import android.service.notification.NotificationListenerService
import dagger.hilt.android.AndroidEntryPoint
import io.github.kiyohitonara.souji.data.AppInfoRepository
import io.github.kiyohitonara.souji.model.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
open class SoujiService : NotificationListenerService() {
    @Inject
    lateinit var repository: AppInfoRepository

    val enabledApps = mutableListOf<AppInfo>()

    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        Timber.d("Creating SoujiService")

        serviceScope.launch {
            repository.getApps().collect { apps ->
                Timber.d("Got ${apps.size} apps")

                enabledApps.clear()
                enabledApps.addAll(apps.filter { it.isEnabled })
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("Starting SoujiService")

        activeNotifications.filter { notification ->
                enabledApps.any { it.packageName == notification.packageName }
            }.forEach { notification ->
                Timber.d("Cancelling notification: ${notification.packageName}")

                cancelNotification(notification.key)
            }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Timber.d("Destroying SoujiService")

        serviceScope.cancel()
    }
}