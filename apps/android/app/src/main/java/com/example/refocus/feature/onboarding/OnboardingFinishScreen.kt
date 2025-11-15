package com.example.refocus.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingFinishScreen(
    onCloseApp: () -> Unit,
    onOpenApp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("設定が完了しました")
            Text("このままアプリを閉じるか、Refocusの画面に進んで設定を確認できます。")

            Button(
                onClick = onCloseApp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("アプリを閉じる")
            }

            Button(
                onClick = onOpenApp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refocusを開く")
            }
        }
    }
}
