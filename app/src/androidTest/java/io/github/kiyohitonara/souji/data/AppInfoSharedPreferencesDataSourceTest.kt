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
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.kiyohitonara.souji.model.AppInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppInfoSharedPreferencesDataSourceTest {
    private lateinit var dataSource: AppInfoSharedPreferencesDataSource

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.edit().apply {
            clear()
            apply()
        }

        dataSource = AppInfoSharedPreferencesDataSource(context)
    }

    @Test
    fun getApps_returnsEmptyListWhenNoAppsStored() {
        val apps = dataSource.getApps()

        assertTrue(apps.isEmpty())
    }

    @Test
    fun getApps_returnsListOfAppsWhenAppsStored() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.edit().apply {
            putStringSet(AppInfoSharedPreferencesDataSource.KEY_APP_PACKAGE_NAMES, setOf("com.example.app1", "com.example.app2"))
            apply()
        }

        val apps = dataSource.getApps()

        assertTrue(apps.isNotEmpty())
        assertTrue(apps.any { it.packageName == "com.example.app1" })
        assertTrue(apps.any { it.packageName == "com.example.app2" })
    }

    @Test(expected = UnsupportedOperationException::class)
    fun getAppsFlow_throwsUnsupportedOperationException() {
        dataSource.getAppsFlow()
    }

    @Test(expected = UnsupportedOperationException::class)
    fun upsertApp_throwsUnsupportedOperationException() = runBlocking {
        dataSource.upsertApp(AppInfo("com.example.app", true))
    }

    @Test
    fun upsertApps_savesEnabledAppsToSharedPreferences() = runBlocking {
        val apps = listOf(AppInfo("com.example.app1", true), AppInfo("com.example.app2", false))
        dataSource.upsertApps(apps)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext<Context>())
        val packageNames = sharedPreferences.getStringSet(AppInfoSharedPreferencesDataSource.KEY_APP_PACKAGE_NAMES, emptySet())

        assertEquals(setOf("com.example.app1"), packageNames)
    }
}