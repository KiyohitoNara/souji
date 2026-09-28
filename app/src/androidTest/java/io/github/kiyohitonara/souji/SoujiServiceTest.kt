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

package io.github.kiyohitonara.souji

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kiyohitonara.souji.data.AppInfoSharedPreferencesDataSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SoujiServiceTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val serviceRule = ServiceTestRule()

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
    }

    private fun storeTestAppPackageName() {
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .putStringSet(AppInfoSharedPreferencesDataSource.KEY_APP_PACKAGE_NAMES, setOf(TEST_APP_PACKAGE_NAME))
            .commit()
    }

    private fun clearTestAppPackageName() {
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .remove(AppInfoSharedPreferencesDataSource.KEY_APP_PACKAGE_NAMES)
            .commit()
    }

    @Test
    fun onStartCommand_cancelsActiveNotifications() =
        runBlocking {
            storeTestAppPackageName()

            val countDownLatch = CountDownLatch(1)
            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        context: Context,
                        intent: Intent,
                    ) {
                        if (intent.action == SoujiService.ACTION_NOTIFICATION_CANCELLED &&
                            intent.getStringExtra(SoujiService.EXTRA_CANCELLED_NOTIFICATION_PACKAGE_NAME) == TEST_APP_PACKAGE_NAME
                        ) {
                            countDownLatch.countDown()
                        }
                    }
                }

            val intentFilter = IntentFilter(SoujiService.ACTION_NOTIFICATION_CANCELLED)
            context.registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED)

            try {
                serviceRule.startService(Intent(context, SoujiService::class.java))
                assertTrue(countDownLatch.await(10, TimeUnit.SECONDS))
            } finally {
                context.unregisterReceiver(receiver)
                clearTestAppPackageName()
            }
        }

    @Test
    fun onStartCommand_broadcastsCancelledCount() =
        runBlocking {
            storeTestAppPackageName()

            val countDownLatch = CountDownLatch(1)
            var cancelledCount = -1
            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        context: Context,
                        intent: Intent,
                    ) {
                        if (intent.action == SoujiService.ACTION_NOTIFICATIONS_CANCELLED) {
                            cancelledCount = intent.getIntExtra(SoujiService.EXTRA_CANCELLED_NOTIFICATION_COUNT, -1)
                            countDownLatch.countDown()
                        }
                    }
                }

            val intentFilter = IntentFilter(SoujiService.ACTION_NOTIFICATIONS_CANCELLED)
            context.registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED)

            try {
                serviceRule.startService(Intent(context, SoujiService::class.java))
                assertTrue(countDownLatch.await(10, TimeUnit.SECONDS))
                assertTrue(cancelledCount >= 0)
            } finally {
                context.unregisterReceiver(receiver)
                clearTestAppPackageName()
            }
        }

    private companion object {
        const val TEST_APP_PACKAGE_NAME = "io.github.kiyohitonara.souji"
    }
}
