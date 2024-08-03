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

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.kiyohitonara.souji.model.AppInfo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppInfoDaoTest {
    private lateinit var database: AppInfoDatabase
    private lateinit var dao: AppInfoDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppInfoDatabase::class.java).allowMainThreadQueries().build()
        dao = database.appInfoDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getApps_returnsEmptyListWhenNoData() = runBlocking {
        val apps = dao.getApps().first()
        assertEquals(emptyList<AppInfo>(), apps)
    }

    @Test
    fun upsertApp_insertsAndUpdatesAppInfo() = runBlocking {
        val appInfo = AppInfo("io.github.kiyohitonara.souji", false)
        dao.upsertApp(appInfo)

        val apps = dao.getApps().first()
        assertEquals(listOf(appInfo), apps)

        val updatedAppInfo = AppInfo("io.github.kiyohitonara.souji", true)
        dao.upsertApp(updatedAppInfo)

        val updatedApps = dao.getApps().first()
        assertEquals(listOf(updatedAppInfo), updatedApps)
    }

    @Test
    fun upsertApp_handlesMultipleApps() = runBlocking {
        val appInfo1 = AppInfo("com.google.android.apps.maps", false)
        val appInfo2 = AppInfo("com.google.android.apps.photos", false)
        dao.upsertApp(appInfo1)
        dao.upsertApp(appInfo2)

        val apps = dao.getApps().first()
        assertEquals(listOf(appInfo1, appInfo2), apps)
    }
}