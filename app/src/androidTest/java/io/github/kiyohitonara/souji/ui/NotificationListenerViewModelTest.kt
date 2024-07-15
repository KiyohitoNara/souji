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

import androidx.lifecycle.LifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.kiyohitonara.souji.data.NotificationListenerRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class NotificationListenerViewModelTest {
    @Mock
    private lateinit var repository: NotificationListenerRepository

    @InjectMocks
    private lateinit var viewModel: NotificationListenerViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun isEnable_returnsTrue_whenNotificationListenerEnabled() = runBlocking {
        whenever(repository.isNotificationListenerEnabled()).thenReturn(true)

        viewModel.checkNotificationListener()

        assertTrue(viewModel.isEnable.first())
    }

    @Test
    fun isEnable_returnsFalse_whenNotificationListenerNotEnabled() = runBlocking {
        whenever(repository.isNotificationListenerEnabled()).thenReturn(false)

        viewModel.checkNotificationListener()

        assertFalse(viewModel.isEnable.first())
    }

    @Test
    fun onResume_callsCheckNotificationListener() {
        val lifecycleOwner = mock(LifecycleOwner::class.java)
        viewModel.onResume(lifecycleOwner)

        verify(repository).isNotificationListenerEnabled()
    }
}