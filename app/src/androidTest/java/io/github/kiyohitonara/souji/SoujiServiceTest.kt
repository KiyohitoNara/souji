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

import android.service.notification.StatusBarNotification
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.kiyohitonara.souji.data.AppInfoRepository
import io.github.kiyohitonara.souji.model.AppInfo
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.Spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever


@RunWith(AndroidJUnit4::class)
class SoujiServiceTest {
    @Mock
    lateinit var repository: AppInfoRepository

    @Spy
    @InjectMocks
    lateinit var service: SoujiService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun updateEnabledApps_updatesEnabledApps() = runTest {
        val apps = listOf(
            AppInfo("com.example.app1", true),
            AppInfo("com.example.app2", false),
            AppInfo("com.example.app3", true)
        )
        whenever(repository.getApps()).thenReturn(flowOf(apps))

        service.updateEnabledApps()

        assertEquals(2, service.enabledApps.size)
        assertTrue(service.enabledApps.contains(AppInfo("com.example.app1", true)))
        assertTrue(service.enabledApps.contains(AppInfo("com.example.app3", true)))
    }

    @Test
    fun cancelActiveNotifications_cancelsActiveNotifications() = runTest {
        val apps = listOf(
            AppInfo("com.example.app1", true),
            AppInfo("com.example.app2", false),
            AppInfo("com.example.app3", true)
        )
        whenever(repository.getApps()).thenReturn(flowOf(apps))

        val notifications = listOf(
            createMockNotification("com.example.app1", "key1"),
            createMockNotification("com.example.app2", "key2")
        )
        whenever(service.activeNotifications).thenReturn(notifications.toTypedArray())

        service.updateEnabledApps()
        service.cancelActiveNotifications()

        verify(service, times(1)).cancelActiveNotification(anyString())
    }

    private fun createMockNotification(packageName: String, key: String): StatusBarNotification {
        val notification: StatusBarNotification = mock()
        whenever(notification.packageName).thenReturn(packageName)
        whenever(notification.key).thenReturn(key)

        return notification
    }
}