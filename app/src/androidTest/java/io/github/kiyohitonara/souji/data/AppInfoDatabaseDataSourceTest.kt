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

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kiyohitonara.souji.di.DatabaseDataSource
import io.github.kiyohitonara.souji.model.AppInfo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class AppInfoDatabaseDataSourceTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @DatabaseDataSource
    @Inject
    lateinit var dataSource: AppInfoDataSource

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testGetApps() = runBlocking {
        val apps = dataSource.getApps().first()

        assertTrue("apps size is ${apps.size}.", apps.isEmpty())
    }

    @Test
    fun testUpsertApp() = runBlocking {
        val appInfo = AppInfo("io.github.kiyohitonara.souji", false)
        dataSource.upsertApp(appInfo)

        val apps = dataSource.getApps().first()
        Assert.assertEquals(1, apps.size)
        Assert.assertEquals(appInfo, apps[0])

        val updatedAppInfo = AppInfo("io.github.kiyohitonara.souji", true)
        dataSource.upsertApp(updatedAppInfo)

        val updatedApps = dataSource.getApps().first()
        Assert.assertEquals(1, updatedApps.size)
        Assert.assertEquals(updatedAppInfo, updatedApps[0])
    }
}