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

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import dagger.hilt.android.AndroidEntryPoint
import io.github.kiyohitonara.souji.R
import io.github.kiyohitonara.souji.SoujiService
import io.github.kiyohitonara.souji.data.AppInfoRepository
import io.github.kiyohitonara.souji.ui.theme.SoujiTheme
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var repository: AppInfoRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.w("Before accessing repository: ${::repository.isInitialized}")

        setContent {
            SoujiTheme {
                val viewModel: AppInfoViewModel = hiltViewModel()
                lifecycle.addObserver(viewModel)

                AppInfoListScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInfoListScreen(viewModel: AppInfoViewModel) {
    val context = LocalContext.current
    val intent = remember { Intent(context, SoujiService::class.java) }

    val apps by viewModel.apps.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.testTag("CleanButton"),
                onClick = {
                    context.startService(intent)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.CleaningServices,
                    contentDescription = stringResource(id = R.string.clean),
                )
            }
        },
        content = { padding ->
            LazyColumn(
                modifier = Modifier.testTag("AppInfoList"),
                contentPadding = padding,
                content = {
                    items(apps) { app ->
                        ListItem(
                            modifier = Modifier.testTag("ListItem-${app.packageName}"),
                            headlineContent = {
                                Text(
                                    text = app.label ?: stringResource(id = R.string.unknown_app),
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = app.packageName,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                )
                            },
                            leadingContent = {
                                Image(
                                    modifier = Modifier.size(40.dp),
                                    painter = rememberDrawablePainter(drawable = app.icon),
                                    contentDescription = app.label,
                                )
                            },
                            trailingContent = {
                                Switch(
                                    modifier = Modifier.testTag("Switch-${app.packageName}"),
                                    checked = app.isEnabled,
                                    onCheckedChange = { checked ->
                                        viewModel.upsertApp(app.copy(isEnabled = checked))
                                    },
                                )
                            }
                        )
                    }
                }
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
fun AppInfoListScreenPreview() {
    SoujiTheme {
        val viewModel: AppInfoViewModel = hiltViewModel()

        AppInfoListScreen(viewModel)
    }
}