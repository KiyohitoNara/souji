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

import io.github.kiyohitonara.souji.data.AppInfoRepository
import io.github.kiyohitonara.souji.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AppInfoViewModelTest {
    @Mock
    private lateinit var repository: AppInfoRepository

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: AppInfoViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        MockitoAnnotations.openMocks(this)
        whenever(repository.getAppsFlow()).thenReturn(flowOf(emptyList()))
        viewModel = AppInfoViewModel(repository, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun apps_emitsEmptyListInitially() {
        assertEquals(emptyList<AppInfo>(), viewModel.apps.value)
    }

    @Test
    fun apps_emitsAppsFromRepository() = runTest(testDispatcher) {
        val apps = listOf(AppInfo("com.example.app", false))
        whenever(repository.getAppsFlow()).thenReturn(flowOf(apps))

        viewModel = AppInfoViewModel(repository, testDispatcher)
        val result = viewModel.apps.first { it.isNotEmpty() }

        assertEquals(apps, result)
    }

    @Test
    fun upsertApp_delegatesToRepository() = runTest(testDispatcher) {
        val app = AppInfo("com.example.app", true)
        viewModel.upsertApp(app)

        advanceUntilIdle()

        verify(repository).upsertApp(app)
    }
}
