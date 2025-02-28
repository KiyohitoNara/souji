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
import kotlinx.coroutines.flow.toList
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
class AppInfoRepositoryTest {
    @Mock
    private lateinit var deviceDataSource: AppInfoDeviceDataSource

    @Mock
    private lateinit var sharedPreferencesDataSource: AppInfoSharedPreferencesDataSource

    @Mock
    private lateinit var databaseDataSource: AppInfoDatabaseDataSource

    private lateinit var repository: AppInfoRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = AppInfoRepository(deviceDataSource, sharedPreferencesDataSource, databaseDataSource)
    }

    @Test
    fun getApps_returnsAppInfoList() {
        val deviceApps = listOf(AppInfo("com.example.app1", false), AppInfo("com.example.app2", false))
        whenever(deviceDataSource.getApps()).thenReturn(deviceApps)

        val sharedPreferencesApps = listOf(AppInfo("com.example.app1", true))
        whenever(sharedPreferencesDataSource.getApps()).thenReturn(sharedPreferencesApps)

        val result = repository.getApps()

        assertEquals(2, result.size)
        assertEquals(true, result.find { it.packageName == "com.example.app1" }?.isEnabled)
        assertEquals(false, result.find { it.packageName == "com.example.app2" }?.isEnabled)
    }

    @Test
    fun getApps_returnsAppInfoListWhenNoSharedPreferencesApps() {
        val deviceApps = listOf(AppInfo("com.example.app1", false), AppInfo("com.example.app2", false))
        whenever(deviceDataSource.getApps()).thenReturn(deviceApps)

        whenever(sharedPreferencesDataSource.getApps()).thenReturn(emptyList())

        val result = repository.getApps()

        assertEquals(2, result.size)
        assertEquals(false, result.find { it.packageName == "com.example.app1" }?.isEnabled)
        assertEquals(false, result.find { it.packageName == "com.example.app2" }?.isEnabled)
    }

    @Test
    fun getAppsFlow_returnsAppInfoList() = runBlocking {
        val deviceApps = listOf(AppInfo("com.example.app1", false), AppInfo("com.example.app2", false))
        whenever(deviceDataSource.getAppsFlow()).thenReturn(flowOf(deviceApps))

        val databaseApps = listOf(AppInfo("com.example.app1", true))
        whenever(databaseDataSource.getAppsFlow()).thenReturn(flowOf(databaseApps))

        val result = repository.getAppsFlow().toList().flatten()

        assertEquals(2, result.size)
        assertEquals(true, result.find { it.packageName == "com.example.app1" }?.isEnabled)
        assertEquals(false, result.find { it.packageName == "com.example.app2" }?.isEnabled)
    }

    @Test
    fun getAppsFlow_returnsAppInfoListWhenNoDatabaseApps() = runBlocking {
        val deviceApps = listOf(AppInfo("com.example.app1", false), AppInfo("com.example.app2", false))
        whenever(deviceDataSource.getAppsFlow()).thenReturn(flowOf(deviceApps))

        whenever(databaseDataSource.getAppsFlow()).thenReturn(flowOf(emptyList()))

        val result = repository.getAppsFlow().toList().flatten()

        assertEquals(2, result.size)
        assertEquals(false, result.find { it.packageName == "com.example.app1" }?.isEnabled)
        assertEquals(false, result.find { it.packageName == "com.example.app2" }?.isEnabled)
    }

    @Test
    fun upsertApp_insertsAppInfo() = runBlocking {
        val appInfo = AppInfo("com.example.app1", true)

        repository.upsertApp(appInfo)

        verify(databaseDataSource).upsertApp(appInfo)
    }

    @Test
    fun upsertApps_insertsAppInfoList() = runBlocking {
        val appInfos = listOf(AppInfo("com.example.app1", true), AppInfo("com.example.app2", true))

        repository.upsertApps(appInfos)

        verify(sharedPreferencesDataSource).upsertApps(appInfos)
    }
}