package com.example.refocus.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingIntroScreen(
    onStartSetup: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Refocusでアプリ使用時間をリアルタイムで可視化しよう！")

            Text("Refocusを動作させるために、いくつか権限を設定する必要があります。")

            Button(
                onClick = onStartSetup,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("今すぐ設定")
            }
        }
    }
}
