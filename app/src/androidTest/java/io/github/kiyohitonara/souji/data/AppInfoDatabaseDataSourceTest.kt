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

package io.github.kiyohitonara.souji.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.kiyohitonara.souji.model.AppInfo
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class AppInfoDatabaseDataSourceTest {
    @Mock
    private lateinit var appInfoDao: AppInfoDao

    private lateinit var dataSource: AppInfoDataSource

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        dataSource = AppInfoDatabaseDataSource(appInfoDao)
    }

    @Test
    fun getApps_returnsEmptyListWhenNoApps() = runBlocking {
        whenever(appInfoDao.getApps()).thenReturn(flowOf(emptyList()))

        val result = dataSource.getApps()

        result.collect { apps ->
            assert(apps.isEmpty())
        }
    }

    @Test
    fun getApps_returnsAppInfoList() = runBlocking {
        val appList = listOf(AppInfo("com.example.app1", false), AppInfo("com.example.app2", false))
        whenever(appInfoDao.getApps()).thenReturn(flowOf(appList))

        val result = dataSource.getApps()

        result.collect { apps ->
            assert(apps == appList)
        }
    }

    @Test
    fun upsertApp_insertsAppInfo() = runBlocking {
        val appInfo = AppInfo("com.example.app", false)

        dataSource.upsertApp(appInfo)

        verify(appInfoDao).upsertApp(appInfo)
    }
}