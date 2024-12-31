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

package io.github.kiyohitonara.souji.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.kiyohitonara.souji.SoujiService
import io.github.kiyohitonara.souji.data.AppInfoRepository
import io.github.kiyohitonara.souji.data.NotificationListenerRepository
import io.github.kiyohitonara.souji.model.AppInfo
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val executorTestRule = InstantTaskExecutorRule()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var notificationListenerRepository: NotificationListenerRepository

    @InjectMocks
    private lateinit var notificationListenerViewModel: NotificationListenerViewModel

    @Mock
    private lateinit var appInfoRepository: AppInfoRepository

    @InjectMocks
    private lateinit var appInfoViewModel: AppInfoViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun soujiApp_clickAboutMenuItem_navigatesToAboutScreen() {
        composeTestRule.setContent {
            SoujiApp(
                notificationListenerViewModel = notificationListenerViewModel,
                appInfoViewModel = appInfoViewModel
            )
        }

        composeTestRule.onNodeWithTag("SoujiAppBarMenuButton").performClick()
        composeTestRule.onNodeWithTag("SoujiAppBarAboutMenuItem").performClick()

        composeTestRule.onNodeWithTag("SoujiAppBarTitle").assertTextEquals("About")
        composeTestRule.onNodeWithTag("SoujiFloatingActionButton").assertDoesNotExist()
    }

    @Test
    fun soujiApp_clickBackButton_navigatesBackToAppsScreen() {
        composeTestRule.setContent {
            SoujiApp(
                notificationListenerViewModel = notificationListenerViewModel,
                appInfoViewModel = appInfoViewModel
            )
        }

        composeTestRule.onNodeWithTag("SoujiAppBarMenuButton").performClick()
        composeTestRule.onNodeWithTag("SoujiAppBarAboutMenuItem").performClick()
        composeTestRule.onNodeWithTag("SoujiAppBarNavigationButton").performClick()

        composeTestRule.onNodeWithTag("SoujiAppBarTitle").assertTextEquals("Souji")
        composeTestRule.onNodeWithTag("SoujiFloatingActionButton").assertExists()
    }

    @Test
    fun soujiApp_clickFloatingActionButton_startsSoujiService() {
        whenever(notificationListenerRepository.isNotificationListenerEnabled()).thenReturn(false)

        val apps = listOf(AppInfo("io.github.kiyohitonara.souji", true))
        whenever(appInfoRepository.getAppsFlow()).thenReturn(flowOf(apps))

        val owner = mock(LifecycleOwner::class.java)
        val registry = LifecycleRegistry(owner)
        registry.addObserver(appInfoViewModel)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        val countDownLatch = CountDownLatch(1)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == SoujiService.ACTION_NOTIFICATION_CANCELLED && intent.getStringExtra(SoujiService.EXTRA_CANCELLED_NOTIFICATION_PACKAGE_NAME) == "io.github.kiyohitonara.souji") {
                    countDownLatch.countDown()
                }
            }
        }

        val context = ApplicationProvider.getApplicationContext<Context>()
        val intentFilter = IntentFilter(SoujiService.ACTION_NOTIFICATION_CANCELLED)
        context.registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED)

        try {
            composeTestRule.setContent {
                SoujiApp(
                    notificationListenerViewModel = notificationListenerViewModel,
                    appInfoViewModel = appInfoViewModel
                )
            }

            composeTestRule.onNodeWithTag("SoujiFloatingActionButton").performClick()
            assertTrue(countDownLatch.await(10, TimeUnit.SECONDS))
        } finally {
            context.unregisterReceiver(receiver)
        }
    }
}