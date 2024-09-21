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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kiyohitonara.souji.data.AppInfoRepository
import io.github.kiyohitonara.souji.model.AppInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SoujiServiceTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val serviceRule = ServiceTestRule()

    @Inject
    lateinit var repository: AppInfoRepository

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun onStartCommand_callsCancelActiveNotification() = runBlocking {
        val countDownLatch = CountDownLatch(1)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == SoujiService.ACTION_NOTIFICATION_CANCELLED && intent.getStringExtra(SoujiService.EXTRA_CANCELLED_NOTIFICATION_PACKAGE_NAME) == "io.github.kiyohitonara.souji") {
                    countDownLatch.countDown()
                }
            }
        }

        val context = ApplicationProvider.getApplicationContext<Context>()
        val intentFilter = IntentFilter(SoujiService.ACTION_NOTIFICATION_CANCELLED)
        context.registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED)

        try {
            repository.upsertApp(AppInfo("io.github.kiyohitonara.souji", true))

            val intent = Intent(ApplicationProvider.getApplicationContext(), SoujiService::class.java)
            intent.putExtra(SoujiService.EXTRA_CANCELLABLE_NOTIFICATION_PACKAGE_NAMES, arrayOf("io.github.kiyohitonara.souji"))

            serviceRule.startService(intent)
            assertTrue(countDownLatch.await(10, TimeUnit.SECONDS))
        } finally {
            context.unregisterReceiver(receiver)
        }
    }
}