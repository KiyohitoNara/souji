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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
    fun currentApps_returnsNonEmptyList() {
        val apps = dataSource.currentApps()

        assertTrue(apps.isNotEmpty())
    }

    @Test
    fun currentApps_returnsAppsWithNonBlankPackageName() {
        val apps = dataSource.currentApps()

        assertTrue(apps.all { it.packageName.isNotBlank() })
    }

    @Test
    fun currentApps_returnsAppsWithLabel() {
        val apps = dataSource.currentApps()

        assertTrue(apps.all { it.label != null && it.label.isNotBlank() })
    }

    @Test
    fun currentApps_returnsAppsWithIsEnabledFalse() {
        val apps = dataSource.currentApps()

        assertTrue(apps.all { !it.isEnabled })
    }

    @Test
    fun apps_emitsInitialListMatchingCurrentApps() = runBlocking {
        val fromCurrentApps = dataSource.currentApps().map { it.packageName }.sorted()
        val fromFlow = dataSource.apps.first().map { it.packageName }.sorted()

        assertEquals(fromCurrentApps, fromFlow)
    }

    @Test
    fun apps_emitsAppsWithIsEnabledFalse() = runBlocking {
        val apps = dataSource.apps.first()

        assertFalse(apps.any { it.isEnabled })
    }

    @Test
    fun apps_emitsAppsWithLabel() = runBlocking {
        val apps = dataSource.apps.first()

        assertTrue(apps.all { it.label != null && it.label.isNotBlank() })
    }

    @Test
    fun apps_includesTestApp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val apps = dataSource.apps.first()

        assertNotNull(apps.find { it.packageName == context.packageName })
    }
}
