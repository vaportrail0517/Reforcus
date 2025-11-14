package com.example.refocus.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingReadyScreen(
    onSelectApps: () -> Unit
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
            Text("準備ができました")
            Text("次に、Refocusで監視する対象アプリを選びましょう。")

            Button(
                onClick = onSelectApps,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("対象アプリを選択")
            }
        }
    }
}
