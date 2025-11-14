package com.example.refocus.feature.appselect

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.refocus.data.datastore.TargetsDataStore
import com.example.refocus.data.repository.TargetsRepository

@Composable
fun AppSelectScreen(
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val pm = context.packageManager

    // M1では簡易的にここで ViewModel を作ってしまう
    val viewModel: AppListViewModel = viewModel(
        factory = AppListViewModelFactory(
            pm = pm,
            repository = TargetsRepository(TargetsDataStore(app))
        )
    )

    val apps by viewModel.apps.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("対象アプリを選択")

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(apps) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleSelection(app.packageName) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f)
                    ) {
                        val iconBitmap = remember(app.icon) {
                            // サイズはお好み。40dp相当くらいでOK
                            app.icon.toBitmap(64, 64)
                        }

                        Image(
                            bitmap = iconBitmap.asImageBitmap(),
                            contentDescription = app.label,
                            modifier = Modifier
                                .size(40.dp)
                                .padding(end = 8.dp)
                        )
                        Text(
                            text = app.label,
                            modifier = Modifier.alignByBaseline()
                        )
                    }

                    Checkbox(
                        checked = app.isSelected,
                        onCheckedChange = {
                            viewModel.toggleSelection(app.packageName)
                        }
                    )
                }
            }
        }

        Button(
            onClick = { viewModel.save(onFinished) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存して完了")
        }
    }
}
