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

import android.provider.Settings
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.kiyohitonara.souji.data.NotificationListenerRepository
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class NotificationAccessDialogTest {
    @get:Rule
    val executorTestRule = InstantTaskExecutorRule()

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val intentsTestRule = IntentsRule()

    @Mock
    private lateinit var notificationListenerRepository: NotificationListenerRepository

    @InjectMocks
    private lateinit var notificationListenerViewModel: NotificationListenerViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun notificationAccessDialog_showsDialog_whenNotificationAccessIsDisabled() {
        whenever(notificationListenerRepository.isNotificationListenerEnabled()).thenReturn(false)

        val owner = Mockito.mock(LifecycleOwner::class.java)
        val registry = LifecycleRegistry(owner)
        registry.addObserver(notificationListenerViewModel)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        composeTestRule.setContent {
            NotificationAccessDialog(notificationListenerViewModel = notificationListenerViewModel)
        }

        composeTestRule.onNodeWithTag("NotificationAccessDialog").assertExists()
    }

    @Test
    fun notificationAccessDialog_doesNotShowsDialog_whenNotificationAccessIsEnabled() {
        whenever(notificationListenerRepository.isNotificationListenerEnabled()).thenReturn(true)

        val owner = Mockito.mock(LifecycleOwner::class.java)
        val registry = LifecycleRegistry(owner)
        registry.addObserver(notificationListenerViewModel)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        composeTestRule.setContent {
            NotificationAccessDialog(notificationListenerViewModel = notificationListenerViewModel)
        }

        composeTestRule.onNodeWithTag("NotificationAccessDialog").assertDoesNotExist()
    }

    @Test
    fun notificationAccessDialog_clickConfirmButton_opensSettings() {
        whenever(notificationListenerRepository.isNotificationListenerEnabled()).thenReturn(false)

        val owner = Mockito.mock(LifecycleOwner::class.java)
        val registry = LifecycleRegistry(owner)
        registry.addObserver(notificationListenerViewModel)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        composeTestRule.setContent {
            NotificationAccessDialog(notificationListenerViewModel = notificationListenerViewModel)
        }

        composeTestRule.onNodeWithTag("NotificationAccessDialogConfirmButton").performClick()
        Intents.intended(hasAction(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }
}