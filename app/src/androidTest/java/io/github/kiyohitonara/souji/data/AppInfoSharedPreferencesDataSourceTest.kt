/*
 * Copyright (c) 2026 Kiyohito Nara
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppInfoSharedPreferencesDataSourceTest {
    private lateinit var context: Context
    private lateinit var dataSource: AppInfoSharedPreferencesDataSource

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().apply()

        dataSource = AppInfoSharedPreferencesDataSource(context)
    }

    // currentApps()

    @Test
    fun currentApps_returnsEmptyListWhenNoAppsStored() {
        assertTrue(dataSource.currentApps().isEmpty())
    }

    @Test
    fun currentApps_returnsStoredPackageNames() {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putStringSet(AppInfoSharedPreferencesDataSource.KEY_APP_PACKAGE_NAMES, setOf("com.example.app1", "com.example.app2"))
            .apply()

        val apps = dataSource.currentApps()

        assertEquals(2, apps.size)
        assertTrue(apps.any { it.packageName == "com.example.app1" })
        assertTrue(apps.any { it.packageName == "com.example.app2" })
    }

    @Test
    fun currentApps_returnsAppsWithIsEnabledTrue() {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putStringSet(AppInfoSharedPreferencesDataSource.KEY_APP_PACKAGE_NAMES, setOf("com.example.app1"))
            .apply()

        val apps = dataSource.currentApps()

        assertTrue(apps.all { it.isEnabled })
    }

    // apps Flow — 初期値

    @Test
    fun apps_emitsEmptyListInitially() = runBlocking {
        val apps = dataSource.apps.first()

        assertTrue(apps.isEmpty())
    }

    @Test
    fun apps_emitsStoredAppsOnSubscribe() = runBlocking {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putStringSet(AppInfoSharedPreferencesDataSource.KEY_APP_PACKAGE_NAMES, setOf("com.example.app1", "com.example.app2"))
            .apply()

        val apps = dataSource.apps.first()

        assertEquals(2, apps.size)
        assertTrue(apps.any { it.packageName == "com.example.app1" })
        assertTrue(apps.any { it.packageName == "com.example.app2" })
    }

    @Test
    fun apps_emitsUpdatedListAfterUpsertApp() = runBlocking {
        val results = mutableListOf<List<AppInfo>>()

        val job = launch(Dispatchers.IO) {
            dataSource.apps.take(2).collect { results.add(it) }
        }
        delay(100) // Wait for the flow to start and emit the initial value

        dataSource.upsertApp(AppInfo("com.example.app1", true))
        job.join()

        assertEquals(2, results.size)
        assertTrue(results[0].isEmpty())
        assertTrue(results[1].any { it.packageName == "com.example.app1" })
    }

    // upsertApp()

    @Test
    fun upsertApp_addsEnabledApp() = runBlocking {
        dataSource.upsertApp(AppInfo("com.example.app1", true))

        val apps = dataSource.currentApps()

        assertEquals(1, apps.size)
        assertTrue(apps.any { it.packageName == "com.example.app1" })
    }

    @Test
    fun upsertApp_removesDisabledApp() = runBlocking {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putStringSet(AppInfoSharedPreferencesDataSource.KEY_APP_PACKAGE_NAMES, setOf("com.example.app1"))
            .apply()

        dataSource.upsertApp(AppInfo("com.example.app1", false))

        assertTrue(dataSource.currentApps().isEmpty())
    }

    @Test
    fun upsertApp_doesNotAffectOtherApps() = runBlocking {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putStringSet(AppInfoSharedPreferencesDataSource.KEY_APP_PACKAGE_NAMES, setOf("com.example.app1", "com.example.app2"))
            .apply()

        dataSource.upsertApp(AppInfo("com.example.app1", false))

        val apps = dataSource.currentApps()
        assertFalse(apps.any { it.packageName == "com.example.app1" })
        assertTrue(apps.any { it.packageName == "com.example.app2" })
    }

    // upsertApps()

    @Test
    fun upsertApps_storesOnlyEnabledApps() = runBlocking {
        dataSource.upsertApps(listOf(AppInfo("com.example.app1", true), AppInfo("com.example.app2", false)))

        val apps = dataSource.currentApps()

        assertEquals(1, apps.size)
        assertTrue(apps.any { it.packageName == "com.example.app1" })
        assertFalse(apps.any { it.packageName == "com.example.app2" })
    }

    @Test
    fun upsertApps_overwritesPreviousApps() = runBlocking {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putStringSet(AppInfoSharedPreferencesDataSource.KEY_APP_PACKAGE_NAMES, setOf("com.example.old"))
            .apply()

        dataSource.upsertApps(listOf(AppInfo("com.example.new", true)))

        val apps = dataSource.currentApps()

        assertFalse(apps.any { it.packageName == "com.example.old" })
        assertTrue(apps.any { it.packageName == "com.example.new" })
    }
}
