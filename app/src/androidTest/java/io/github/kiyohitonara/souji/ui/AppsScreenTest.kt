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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.kiyohitonara.souji.data.AppInfoRepository
import io.github.kiyohitonara.souji.model.AppInfo
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class AppsScreenTest {
    @get:Rule
    val executorTestRule = InstantTaskExecutorRule()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var appInfoRepository: AppInfoRepository

    @InjectMocks
    private lateinit var appInfoViewModel: AppInfoViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun appsScreen_displaysAppList() {
        val apps = listOf(
            AppInfo("com.example.app1", "App 1", null, true),
            AppInfo("com.example.app2", "App 2", null, false),
        )
        whenever(appInfoRepository.getAppsFlow()).thenReturn(flowOf(apps))

        val owner = Mockito.mock(LifecycleOwner::class.java)
        val registry = LifecycleRegistry(owner)
        registry.addObserver(appInfoViewModel)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        composeTestRule.setContent {
            AppsScreen(appInfoViewModel = appInfoViewModel)
        }

        composeTestRule.onNodeWithTag("AppList").assertIsDisplayed()
        composeTestRule.onNodeWithTag("AppListItem_com.example.app1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("AppListItem_com.example.app2").assertIsDisplayed()
    }

    @Test
    fun appListItem_switchToggles() = runBlocking {
        val app = AppInfo("com.example.app", "App", null, false)
        whenever(appInfoRepository.getAppsFlow()).thenReturn(flowOf(listOf(app)))

        val owner = Mockito.mock(LifecycleOwner::class.java)
        val registry = LifecycleRegistry(owner)
        registry.addObserver(appInfoViewModel)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        composeTestRule.setContent {
            AppsScreen(appInfoViewModel = appInfoViewModel)
        }

        composeTestRule.onNodeWithTag("AppListItemSwitch_com.example.app").performClick()
        verify(appInfoRepository).upsertApp(app.copy(isEnabled = true))
    }
}