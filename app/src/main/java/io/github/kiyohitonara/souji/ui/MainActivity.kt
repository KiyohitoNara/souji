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

package io.github.kiyohitonara.souji.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.kiyohitonara.souji.R
import io.github.kiyohitonara.souji.SoujiService
import io.github.kiyohitonara.souji.ui.theme.SoujiTheme
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d("Activity is created")

        setContent {
            SoujiTheme {
                val appInfoViewModel: AppInfoViewModel = hiltViewModel()
                lifecycle.addObserver(appInfoViewModel)

                val notificationListenerViewModel: NotificationListenerViewModel = hiltViewModel()
                lifecycle.addObserver(notificationListenerViewModel)

                SoujiApp(
                    notificationListenerViewModel = notificationListenerViewModel,
                    appInfoViewModel = appInfoViewModel,
                )
            }
        }
    }
}

enum class SoujiScreen(@StringRes val title: Int) {
    Apps(title = R.string.app_name),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoujiApp(
    notificationListenerViewModel: NotificationListenerViewModel,
    appInfoViewModel: AppInfoViewModel,
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = SoujiScreen.valueOf(backStackEntry?.destination?.route ?: SoujiScreen.Apps.name)

    val apps by appInfoViewModel.apps.collectAsStateWithLifecycle()

    NotificationAccessDialog(notificationListenerViewModel)

    Scaffold(
        topBar = {
            SoujiAppBar(
                currentScreen = currentScreen,
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() },
            )
        },
        floatingActionButton = {
            if (currentScreen == SoujiScreen.Apps) {
                SoujiFloatingActionButton {
                    val packageNames = apps.filter { it.isEnabled }.map { it.packageName }
                    SoujiService.startService(context, packageNames)
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = SoujiScreen.Apps.name,
        ) {
            composable(SoujiScreen.Apps.name) {
                AppsScreen(appInfoViewModel, padding)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoujiAppBar(
    currentScreen: SoujiScreen,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(currentScreen.title),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        },
        modifier = Modifier.testTag("SoujiAppBar"),
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(
                    onClick = { navigateUp() },
                    modifier = Modifier.testTag("SoujiAppBarNavigationButton"),
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoujiFloatingActionButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.testTag("SoujiFloatingActionButton"),
    ) {
        Icon(
            imageVector = Icons.Default.CleaningServices,
            contentDescription = stringResource(R.string.clean),
        )
    }
}
