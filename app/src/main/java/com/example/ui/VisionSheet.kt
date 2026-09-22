package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VisionSheet(
    viewModel: AssistantViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scenario by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxHeight(0.85f)
            .fillMaxWidth()
            .testTag("vision_panel"),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "95 HOLOGRAPHIC VISION",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close vision",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle
            Text(
                "Secure camera stream proxy. Input a physical scan scenario or upload simulation parameters to verify the visual intelligence engine.",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Input field
            TextField(
                value = scenario,
                onValueChange = { scenario = it },
                placeholder = { Text("E.g. A desk with a glowing blue matrix orb and a cup of coffee.") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF1E293B)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (scenario.isNotBlank()) {
                        isAnalyzing = true
                        resultText = "Opening optical filters... Locking coordinates... Analyzing..."
                        viewModel.processUserInput("Describe what is seen in this environment: $scenario")
                        isAnalyzing = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("INITIATE OPTICAL FEED SCAN", color = Color.Black)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Results Log
            Text("SECURE OPTICAL SCAN RESULTS:", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF111827), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = if (scenario.isEmpty()) "Awaiting lens projection command, sir." else "Processing visual coordinates through Ninety Five's analytical core model layers...",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}
