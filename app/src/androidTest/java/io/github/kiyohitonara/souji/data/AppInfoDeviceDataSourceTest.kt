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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.kiyohitonara.souji.model.AppInfo
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppInfoDeviceDataSourceTest {
    private lateinit var dataSource: AppInfoDeviceDataSource

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        dataSource = AppInfoDeviceDataSource(context)
    }

    @Test
    fun getApps_returnsAppInfoList() {
        val result = dataSource.getApps()

        assert(result.isNotEmpty())
    }

    @Test
    fun getAppsFlow_returnsAppInfoList() = runBlocking {
        val result = dataSource.getAppsFlow()

        result.collect { apps ->
            assert(apps.isNotEmpty())
        }
    }

    @Test(expected = UnsupportedOperationException::class)
    fun upsertApp_throwsUnsupportedOperationException() = runBlocking {
        val appInfo = AppInfo("com.example.app", false)
        dataSource.upsertApp(appInfo)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun upsertApps_throwsUnsupportedOperationException() = runBlocking {
        val appInfos = listOf(AppInfo("com.example.app1", false), AppInfo("com.example.app2", false))

        dataSource.upsertApps(appInfos)
    }
}