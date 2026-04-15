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

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.kiyohitonara.souji.data.AppInfoRepository
import io.github.kiyohitonara.souji.model.AppInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class AppInfoViewModelTest {
    @Mock
    private lateinit var repository: AppInfoRepository

    private lateinit var viewModel: AppInfoViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(repository.getAppsFlow()).thenReturn(flowOf(emptyList()))
        viewModel = AppInfoViewModel(repository)
    }

    @Test
    fun apps_emitsEmptyListInitially() {
        assertEquals(emptyList<AppInfo>(), viewModel.apps.value)
    }

    @Test
    fun apps_emitsAppsFromRepository() = runBlocking {
        val apps = listOf(AppInfo("com.example.app", false))
        whenever(repository.getAppsFlow()).thenReturn(flowOf(apps))
        viewModel = AppInfoViewModel(repository)

        val result = viewModel.apps.first { it.isNotEmpty() }

        assertEquals(apps, result)
    }

    @Test
    fun upsertApp_shouldCallRepositoryUpsertApp() = runBlocking {
        val app = AppInfo("com.example.app", true)
        viewModel.upsertApp(app)

        delay(100)
        verify(repository).upsertApp(app)
    }
}
