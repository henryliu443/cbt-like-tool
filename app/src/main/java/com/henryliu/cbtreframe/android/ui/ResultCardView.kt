package com.henryliu.cbtreframe.android.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ResultCardView(
    aiResponse: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Text(
            text = aiResponse,
            modifier = Modifier.padding(16.dp)
        )
    }
}
