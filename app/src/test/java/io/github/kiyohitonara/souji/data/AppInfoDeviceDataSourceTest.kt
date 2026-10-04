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
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import io.github.kiyohitonara.souji.model.AppInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AppInfoDeviceDataSourceTest {
    private lateinit var context: Context
    private lateinit var dataSource: AppInfoDeviceDataSource

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
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

        assertTrue(apps.all { !it.label.isNullOrBlank() })
    }

    @Test
    fun currentApps_returnsAppsWithIsEnabledFalse() {
        val apps = dataSource.currentApps()

        assertTrue(apps.all { !it.isEnabled })
    }

    @Test
    fun apps_emitsInitialListMatchingCurrentApps() = runBlocking {
        val fromCurrentApps = dataSource.currentApps().map { it.packageName }.sorted()
        val fromFlow = dataSource.apps
            .first()
            .map { it.packageName }
            .sorted()

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

        assertTrue(apps.all { !it.label.isNullOrBlank() })
    }

    @Test
    fun apps_includesTestApp() = runBlocking {
        val apps = dataSource.apps.first()

        assertNotNull(apps.find { it.packageName == context.packageName })
    }

    private fun installLauncherApp(packageName: String) {
        val applicationInfo =
            ApplicationInfo().apply {
                this.packageName = packageName
            }
        val activityInfo =
            ActivityInfo().apply {
                this.packageName = packageName
                name = "MainActivity"
                this.applicationInfo = applicationInfo
            }
        val resolveInfo = ResolveInfo().apply { this.activityInfo = activityInfo }

        val shadowPackageManager = shadowOf(context.packageManager)
        // currentApps() queries without a package filter, while resolveApp() queries with one;
        // register both so the fake app is visible to each.
        shadowPackageManager.addResolveInfoForIntent(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            resolveInfo,
        )
        shadowPackageManager.addResolveInfoForIntent(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(packageName),
            resolveInfo,
        )
    }

    private fun uninstallLauncherApp(packageName: String) {
        val shadowPackageManager = shadowOf(context.packageManager)
        shadowPackageManager.removeResolveInfosForIntent(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            packageName,
        )
        shadowPackageManager.removeResolveInfosForIntent(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(packageName),
            packageName,
        )
        shadowPackageManager.removePackage(packageName)
    }

    @Test
    fun apps_addsNewlyInstalledPackage() = runBlocking {
        val newPackageName = "com.example.newapp"

        val results = mutableListOf<List<AppInfo>>()
        val initialEmission = CompletableDeferred<Unit>()
        val job =
            launch(Dispatchers.IO) {
                dataSource.apps.take(2).collect {
                    results.add(it)
                    initialEmission.complete(Unit)
                }
            }

        initialEmission.await()
        installLauncherApp(newPackageName)
        context.sendBroadcast(Intent(Intent.ACTION_PACKAGE_ADDED, Uri.parse("package:$newPackageName")))
        shadowOf(Looper.getMainLooper()).idle()
        try {
            withTimeout(5000) { job.join() }
        } finally {
            job.cancel()
        }

        assertEquals(2, results.size)
        assertFalse(results[0].any { it.packageName == newPackageName })
        assertTrue(results[1].any { it.packageName == newPackageName })
    }

    @Test
    fun apps_removesUninstalledPackage() = runBlocking {
        val newPackageName = "com.example.newapp"
        installLauncherApp(newPackageName)

        val results = mutableListOf<List<AppInfo>>()
        val initialEmission = CompletableDeferred<Unit>()
        val job =
            launch(Dispatchers.IO) {
                dataSource.apps.take(2).collect {
                    results.add(it)
                    initialEmission.complete(Unit)
                }
            }

        initialEmission.await()
        uninstallLauncherApp(newPackageName)
        context.sendBroadcast(Intent(Intent.ACTION_PACKAGE_REMOVED, Uri.parse("package:$newPackageName")))
        shadowOf(Looper.getMainLooper()).idle()
        try {
            withTimeout(5000) { job.join() }
        } finally {
            job.cancel()
        }

        assertEquals(2, results.size)
        assertTrue(results[0].any { it.packageName == newPackageName })
        assertFalse(results[1].any { it.packageName == newPackageName })
    }
}
