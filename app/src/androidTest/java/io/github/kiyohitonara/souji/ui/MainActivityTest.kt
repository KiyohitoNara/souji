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
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.preference.PreferenceManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kiyohitonara.souji.SoujiService
import io.github.kiyohitonara.souji.data.AppInfoRepository
import io.github.kiyohitonara.souji.data.AppInfoSharedPreferencesDataSource
import io.github.kiyohitonara.souji.data.NotificationListenerRepository
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.flow.flowOf
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var notificationListenerRepository: NotificationListenerRepository

    @InjectMocks
    private lateinit var notificationListenerViewModel: NotificationListenerViewModel

    @Mock
    private lateinit var appInfoRepository: AppInfoRepository

    private lateinit var appInfoViewModel: AppInfoViewModel

    @Before
    fun setup() {
        hiltRule.inject()
        MockitoAnnotations.openMocks(this)
        whenever(notificationListenerRepository.isNotificationListenerEnabled()).thenReturn(true)
        whenever(appInfoRepository.getAppsFlow()).thenReturn(flowOf(emptyList()))
        appInfoViewModel = AppInfoViewModel(appInfoRepository)
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
        val context = ApplicationProvider.getApplicationContext<Context>()
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putStringSet(AppInfoSharedPreferencesDataSource.KEY_APP_PACKAGE_NAMES, setOf("io.github.kiyohitonara.souji"))
            .commit()

        val countDownLatch = CountDownLatch(1)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == SoujiService.ACTION_NOTIFICATION_CANCELLED && intent.getStringExtra(SoujiService.EXTRA_CANCELLED_NOTIFICATION_PACKAGE_NAME) == "io.github.kiyohitonara.souji") {
                    countDownLatch.countDown()
                }
            }
        }

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
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                .remove(AppInfoSharedPreferencesDataSource.KEY_APP_PACKAGE_NAMES)
                .commit()
        }
    }
}
