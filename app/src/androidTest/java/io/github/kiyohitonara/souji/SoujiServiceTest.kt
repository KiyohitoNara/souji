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

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import io.github.kiyohitonara.souji.data.AppInfoRepository
import io.github.kiyohitonara.souji.model.AppInfo
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SoujiServiceTest {
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant("android.permission.POST_NOTIFICATIONS")

    private lateinit var service: SoujiService

    private val repository: AppInfoRepository = mock()

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        if (ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context as Activity, arrayOf("android.permission.POST_NOTIFICATIONS"), 0)
        }

        service = SoujiService(repository)
    }

    @Test
    fun onCreate_initializesEnabledApps() = runTest {
        val apps = listOf(
            AppInfo("com.example.app1", true),
            AppInfo("com.example.app2", false),
            AppInfo("com.example.app3", true)
        )
        whenever(repository.getApps()).thenReturn(flowOf(apps))

        service.onCreate()

        assertEquals(2, service.enabledApps.size)
        assertTrue(service.enabledApps.contains(AppInfo("com.example.app1", true)))
        assertTrue(service.enabledApps.contains(AppInfo("com.example.app3", true)))
    }

    @Test
    fun onStartCommand_cancelsNotificationsForEnabledApps() = runBlockingTest {
        val apps = listOf(
            AppInfo("io.github.kiyohitonara.souji", true)
        )
        whenever(repository.getApps()).thenReturn(flowOf(apps))

        sendNotification()

        service.onCreate()
        service.onStartCommand(mock(), 0, 1)

        val activeNotifications = service.activeNotifications
        assertTrue(activeNotifications.none { it.packageName == "io.github.kiyohitonara.souji" })
    }

    @Test
    fun onStartCommand_doesNotCancelNotificationsForDisabledApps() = runBlockingTest {
        val apps = listOf(
            AppInfo("io.github.kiyohitonara.souji", false)
        )
        whenever(repository.getApps()).thenReturn(flowOf(apps))

        sendNotification()

        service.onCreate()
        service.onStartCommand(mock(), 0, 1)

        val activeNotifications = service.activeNotifications
        assertTrue(activeNotifications.none { it.packageName == "io.github.kiyohitonara.souji" })
    }

    private fun sendNotification() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val notification = Notification.Builder(context, "channel id")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("title")
            .setContentText("text")
            .build()

        val channel = NotificationChannel("channel id", "channel name", NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = "channel description"

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        manager.notify(1, notification)
    }
}