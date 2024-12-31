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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.github.kiyohitonara.souji.R
import io.github.kiyohitonara.souji.model.AppInfo
import io.github.kiyohitonara.souji.ui.theme.SoujiTheme
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(appInfoViewModel: AppInfoViewModel, contentPadding: PaddingValues = PaddingValues(0.dp)) {
    val apps by appInfoViewModel.apps.collectAsStateWithLifecycle()

    AppList(
        apps = apps,
        onCheckedChange = { app, isEnabled ->
            Timber.i("AppListItemSwitch clicked: ${app.packageName}")

            appInfoViewModel.upsertApp(app.copy(isEnabled = isEnabled))
        },
        contentPadding = contentPadding,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppList(
    apps: List<AppInfo>,
    onCheckedChange: ((AppInfo, Boolean) -> Unit)?,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(
        modifier = Modifier.testTag("AppList"),
        contentPadding = contentPadding,
    ) {
        items(apps) { app ->
            AppListItem(app, onCheckedChange)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListItem(
    app: AppInfo,
    onCheckedChange: ((AppInfo, Boolean) -> Unit)?,
) {
    ListItem(
        headlineContent = {
            Text(
                text = app.label ?: stringResource(id = R.string.unknown_app),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        },
        modifier = Modifier.testTag("AppListItem_${app.packageName}"),
        supportingContent = {
            Text(
                text = app.packageName,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        },
        leadingContent = {
            Image(
                painter = rememberDrawablePainter(app.icon),
                contentDescription = app.label,
                modifier = Modifier.size(40.dp)
            )
        },
        trailingContent = {
            Switch(
                checked = app.isEnabled,
                onCheckedChange = { onCheckedChange?.invoke(app, it) },
                modifier = Modifier.testTag("AppListItemSwitch_${app.packageName}")
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun AppsScreenPreview() {
    SoujiTheme {
        val appInfoViewModel: AppInfoViewModel = hiltViewModel()

        AppsScreen(appInfoViewModel)
    }
}
