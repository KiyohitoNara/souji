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
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kiyohitonara.souji.R
import io.github.kiyohitonara.souji.data.AppInfoRepository
import io.github.kiyohitonara.souji.data.NotificationListenerRepository
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @get:Rule
    val intentsRule = IntentsRule()

    @Inject
    lateinit var appInfoRepository: AppInfoRepository

    private lateinit var appInfoViewModel: AppInfoViewModel

    @Mock
    private lateinit var notificationListenerRepository: NotificationListenerRepository

    @InjectMocks
    private lateinit var notificationListenerViewModel: NotificationListenerViewModel

    @Before
    fun setup() {
        hiltRule.inject()
        MockitoAnnotations.openMocks(this)

        appInfoViewModel = AppInfoViewModel(appInfoRepository)
    }

    @Test
    fun testOnResume() {
        val owner = Mockito.mock(LifecycleOwner::class.java)
        val registry = LifecycleRegistry(owner)
        registry.addObserver(appInfoViewModel)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        composeTestRule.setContent {
            AppInfoListScreen(appInfoViewModel, notificationListenerViewModel)
        }

        composeTestRule
            .onNodeWithTag("AppInfoList")
            .onChildren()
            .apply {
                Assert.assertTrue(
                    "apps size is ${this.fetchSemanticsNodes().size}.", this.fetchSemanticsNodes().isNotEmpty()
                )
            }

        composeTestRule.onNodeWithTag("AppInfoList").performScrollToNode(hasTestTag("ListItem-io.github.kiyohitonara.souji"))
        composeTestRule.onNodeWithTag("ListItem-io.github.kiyohitonara.souji").assertExists()
        composeTestRule.onNodeWithTag("Switch-io.github.kiyohitonara.souji").assertExists()
        composeTestRule.onNodeWithTag("Switch-io.github.kiyohitonara.souji").performClick().assertIsOn()
    }

    @Test
    fun appInfoListScreen_cleanButtonStartsService() {
        val serviceStartedSignal = CountDownLatch(1)
        val intentFilter = IntentFilter("io.github.kiyohitonara.souji.NOTIFICATION_CANCELLED")

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "io.github.kiyohitonara.souji.NOTIFICATION_CANCELLED") {
                    serviceStartedSignal.countDown()
                }
            }
        }

        val context = ApplicationProvider.getApplicationContext<Context>()
        try {
            context.registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED)

            composeTestRule.setContent {
                AppInfoListScreen(appInfoViewModel, notificationListenerViewModel)
            }

            composeTestRule
                .onNodeWithTag("CleanButton")
                .performClick()

            assertTrue(serviceStartedSignal.await(10, TimeUnit.SECONDS))
        } finally {
            context.unregisterReceiver(receiver)
        }
    }

    @Test
    fun appInfoListScreen_showsDialog_whenNotificationAccessIsDisabled() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        whenever(notificationListenerRepository.isNotificationListenerEnabled()).thenReturn(false)

        val owner = Mockito.mock(LifecycleOwner::class.java)
        val registry = LifecycleRegistry(owner)
        registry.addObserver(notificationListenerViewModel)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        composeTestRule.setContent {
            AppInfoListScreen(appInfoViewModel, notificationListenerViewModel)
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.notification_access_required))
            .assertExists()
    }

    @Test
    fun appInfoListScreen_notShowsDialog_whenNotificationAccessIsEnabled() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        whenever(notificationListenerRepository.isNotificationListenerEnabled()).thenReturn(true)

        val owner = Mockito.mock(LifecycleOwner::class.java)
        val registry = LifecycleRegistry(owner)
        registry.addObserver(notificationListenerViewModel)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        composeTestRule.setContent {
            AppInfoListScreen(appInfoViewModel, notificationListenerViewModel)
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.notification_access_required))
            .assertDoesNotExist()
    }

    @Test
    fun appInfoListScreen_startsSettingsActivity_whenDialogConfirmButtonIsPerformed() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        composeTestRule.setContent {
            AppInfoListScreen(appInfoViewModel, notificationListenerViewModel)
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.settings))
            .performClick()

        Intents.intended(hasAction(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }
}