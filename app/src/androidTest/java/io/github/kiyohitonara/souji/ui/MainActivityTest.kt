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
import android.provider.Settings
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kiyohitonara.souji.SoujiService
import io.github.kiyohitonara.souji.data.AppInfoDatabase
import io.github.kiyohitonara.souji.data.AppInfoDatabaseDataSource
import io.github.kiyohitonara.souji.data.AppInfoDeviceDataSource
import io.github.kiyohitonara.souji.data.AppInfoRepository
import io.github.kiyohitonara.souji.data.NotificationListenerRepository
import io.github.kiyohitonara.souji.model.AppInfo
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val executorRule = InstantTaskExecutorRule()

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val intentsRule = IntentsRule()

    @Mock
    private lateinit var appInfoDeviceDataSource: AppInfoDeviceDataSource

    private lateinit var appInfoRepository: AppInfoRepository
    private lateinit var appInfoViewModel: AppInfoViewModel

    @Mock
    private lateinit var notificationListenerRepository: NotificationListenerRepository

    @InjectMocks
    private lateinit var notificationListenerViewModel: NotificationListenerViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val appInfoDatabase = Room.inMemoryDatabaseBuilder(context, AppInfoDatabase::class.java).build()
        val appInfoDao = appInfoDatabase.appInfoDao()
        val appInfoDatabaseDataSource = AppInfoDatabaseDataSource(appInfoDao)
        appInfoRepository = spy(AppInfoRepository(appInfoDeviceDataSource, appInfoDatabaseDataSource))
        appInfoViewModel = AppInfoViewModel(appInfoRepository)
    }

    @Test
    fun appInfoListScreen_doesNotShowDialog_whenNotificationAccessIsEnabled() {
        whenever(notificationListenerRepository.isNotificationListenerEnabled()).thenReturn(true)

        val owner = mock(LifecycleOwner::class.java)
        val registry = LifecycleRegistry(owner)
        registry.addObserver(notificationListenerViewModel)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        composeRule.setContent {
            AppInfoListScreen(appInfoViewModel, notificationListenerViewModel)
        }

        composeRule.onNodeWithTag("NotificationAccessDialog").assertDoesNotExist()
    }

    @Test
    fun appInfoListScreen_showsDialog_whenNotificationAccessIsDisabled() {
        whenever(notificationListenerRepository.isNotificationListenerEnabled()).thenReturn(false)

        val owner = mock(LifecycleOwner::class.java)
        val registry = LifecycleRegistry(owner)
        registry.addObserver(notificationListenerViewModel)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        composeRule.setContent {
            AppInfoListScreen(appInfoViewModel, notificationListenerViewModel)
        }

        composeRule.onNodeWithTag("NotificationAccessDialog").assertExists()
    }

    @Test
    fun appInfoListScreen_startsSettingsActivity_whenDialogConfirmButtonIsClicked() {
        whenever(notificationListenerRepository.isNotificationListenerEnabled()).thenReturn(false)

        val owner = mock(LifecycleOwner::class.java)
        val registry = LifecycleRegistry(owner)
        registry.addObserver(notificationListenerViewModel)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        composeRule.setContent {
            AppInfoListScreen(appInfoViewModel, notificationListenerViewModel)
        }

        composeRule.onNodeWithTag("NotificationAccessDialogConfirmButton").performClick()
        Intents.intended(hasAction(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    @Test
    fun appInfoListScreen_showsAppList_whenAppsAreAvailable() {
        val apps = listOf(AppInfo("com.example.app1", false), AppInfo("com.example.app2", false))
        whenever(appInfoRepository.getAppsFlow()).thenReturn(flowOf(apps))

        val owner = mock(LifecycleOwner::class.java)
        val registry = LifecycleRegistry(owner)
        registry.addObserver(appInfoViewModel)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        composeRule.setContent {
            AppInfoListScreen(appInfoViewModel, notificationListenerViewModel)
        }

        composeRule.onNodeWithTag("AppInfoList").onChildren().apply {
            assertEquals(2, this.fetchSemanticsNodes().size)
        }
    }

    @Test
    fun appInfoListScreen_showsEmptyAppList_whenNoAppsAreAvailable() {
        whenever(appInfoRepository.getAppsFlow()).thenReturn(flowOf(emptyList()))

        val owner = mock(LifecycleOwner::class.java)
        val registry = LifecycleRegistry(owner)
        registry.addObserver(appInfoViewModel)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        composeRule.setContent {
            AppInfoListScreen(appInfoViewModel, notificationListenerViewModel)
        }

        composeRule.onNodeWithTag("AppInfoList").onChildren().apply {
            assertEquals(0, this.fetchSemanticsNodes().size)
        }
    }

    @Test
    fun appInfoListScreen_togglesAppSwitch_whenSwitchIsClicked() = runBlocking {
        val apps = listOf(AppInfo("com.example.app", false))
        whenever(appInfoDeviceDataSource.getAppsFlow()).thenReturn(flowOf(apps))

        val owner = mock(LifecycleOwner::class.java)
        val registry = LifecycleRegistry(owner)
        registry.addObserver(appInfoViewModel)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        composeRule.setContent {
            AppInfoListScreen(appInfoViewModel, notificationListenerViewModel)
        }

        composeRule.onNodeWithTag("Switch-com.example.app").performClick().assertIsOn()
        verify(appInfoRepository).upsertApp(AppInfo("com.example.app", true))
    }

    @Test
    fun appInfoListScreen_startsService_whenCleanButtonIsClicked() {
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
            val apps = listOf(AppInfo("io.github.kiyohitonara.souji", true))
            whenever(appInfoRepository.getAppsFlow()).thenReturn(flowOf(apps))

            val owner = mock(LifecycleOwner::class.java)
            val registry = LifecycleRegistry(owner)
            registry.addObserver(appInfoViewModel)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

            composeRule.setContent {
                AppInfoListScreen(appInfoViewModel, notificationListenerViewModel)
            }

            composeRule.onNodeWithTag("CleanButton").performClick()
            assertTrue(countDownLatch.await(10, TimeUnit.SECONDS))
        } finally {
            context.unregisterReceiver(receiver)
        }
    }
}