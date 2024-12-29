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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.kiyohitonara.souji.data.AppInfoRepository
import io.github.kiyohitonara.souji.model.AppInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import timber.log.Timber

@RunWith(AndroidJUnit4::class)
class AppInfoViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    private lateinit var repository: AppInfoRepository

    private lateinit var viewModel: AppInfoViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        viewModel = AppInfoViewModel(repository)
    }

    @Test
    fun onCreate_shouldLoadInitialApps() = runBlocking {
        val apps = listOf(AppInfo("com.example.app", false))
        whenever(repository.getAppsFlow()).thenReturn(flowOf(apps))

        val owner = mock(LifecycleOwner::class.java)
        val registry = LifecycleRegistry(owner)
        registry.addObserver(viewModel)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        delay(1000)
        assertEquals(viewModel.apps.value, apps)
    }

    @Test
    fun upsertApp_shouldCallRepositoryUpsertApp() = runBlocking {
        val apps = listOf(AppInfo("com.example.app", false))
        whenever(repository.getAppsFlow()).thenReturn(flowOf(apps))

        val app = AppInfo("com.example.app", true)
        viewModel.upsertApp(app)

        delay(1000)
        verify(repository).upsertApp(app)
    }

    @Test
    fun onStop_shouldCallRepositoryUpsertApps() = runBlocking {
        Timber.plant(Timber.DebugTree())
        val apps = listOf(AppInfo("com.example.app", false))
        whenever(repository.getAppsFlow()).thenReturn(flowOf(apps))

        val owner = mock(LifecycleOwner::class.java)
        val registry = LifecycleRegistry(owner)
        registry.addObserver(viewModel)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        delay(1000)

        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)

        registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        delay(1000)

        verify(repository).upsertApps(apps)
    }
}