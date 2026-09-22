package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ApiKeySheet(
    viewModel: AssistantViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeProv by viewModel.activeProvider.collectAsState()
    val geminiKey by viewModel.userGeminiKey.collectAsState()
    val openAiKey by viewModel.userOpenAiKey.collectAsState()
    val anthropicKey by viewModel.userAnthropicKey.collectAsState()

    var activeTab by remember { mutableStateOf("gemini") } // "gemini", "openai", "anthropic"
    var inputKeyVal by remember { mutableStateOf("") }
    var testResultText by remember { mutableStateOf("") }
    var testStatusSuccess by remember { mutableStateOf<Boolean?>(null) }
    var isTestingKey by remember { mutableStateOf(false) }

    // Synchronize local input field when tab switches
    LaunchedEffect(activeTab) {
        inputKeyVal = when (activeTab) {
            "gemini" -> geminiKey
            "openai" -> openAiKey
            "anthropic" -> anthropicKey
            else -> ""
        }
        testResultText = ""
        testStatusSuccess = null
    }

    // Helper to mask key for safe UI rendering (🔑 BYOK API KEY SECURITY)
    fun maskKey(key: String): String {
        if (key.isBlank()) return "Not configured"
        return if (key.length > 8) {
            "•••• •••• •••• " + key.takeLast(4)
        } else {
            "•••• ••••"
        }
    }

    Card(
        modifier = modifier
            .fillMaxHeight(0.85f)
            .fillMaxWidth()
            .testTag("api_key_panel"),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = "Keys",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "95 BYOK CREDENTIAL CENTRE",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close api key control sheet",
                        tint = Color.White
                    )
                }
            }

            Divider(color = Color(0xFF1E293B), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // EXPLANATION BANNER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    "Transparent Operation Model",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "You are paying for Ninety Five's premium client-side software layers (Memory, Specialist Agents, File Guardian), not marked-up AI tokens. All API usage is paid directly to your selected provider without middleman fees.",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TAB ROW
            ScrollableTabRow(
                selectedTabIndex = listOf("gemini", "openai", "anthropic").indexOf(activeTab).coerceAtLeast(0),
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                listOf("gemini" to "Google Gemini", "openai" to "OpenAI Core", "anthropic" to "Anthropic").forEach { (tabId, label) ->
                    Tab(
                        selected = activeTab == tabId,
                        onClick = { activeTab = tabId },
                        text = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ACTIVE ROUTE CONTROL
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Routing Target", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Toggle active provider route", color = Color.Gray, fontSize = 11.sp)
                            }
                            Switch(
                                checked = activeProv == activeTab,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        viewModel.activeProvider.value = activeTab
                                        viewModel.showNotification("AI Active Route: ${activeTab.uppercase()}")
                                    }
                                }
                            )
                        }
                    }
                }

                // CURRENT STATUS CARD
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "SECURED CREDENTIAL STATE",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val activeSavedKey = when (activeTab) {
                                "gemini" -> geminiKey
                                "openai" -> openAiKey
                                "anthropic" -> anthropicKey
                                else -> ""
                            }
                            Text(
                                text = maskKey(activeSavedKey),
                                color = if (activeSavedKey.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.LightGray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )

                            if (activeSavedKey.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        when (activeTab) {
                                            "gemini" -> viewModel.userGeminiKey.value = ""
                                            "openai" -> viewModel.userOpenAiKey.value = ""
                                            "anthropic" -> viewModel.userAnthropicKey.value = ""
                                        }
                                        inputKeyVal = ""
                                        viewModel.showNotification("Key purged successfully.")
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, "Delete Key", tint = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }

                // INPUT AREA
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "CONFIGURE ${activeTab.uppercase()} API KEY",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        TextField(
                            value = inputKeyVal,
                            onValueChange = { inputKeyVal = it },
                            placeholder = { Text("Enter private secret key...", fontSize = 12.sp, color = Color.Gray) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    when (activeTab) {
                                        "gemini" -> viewModel.userGeminiKey.value = inputKeyVal.trim()
                                        "openai" -> viewModel.userOpenAiKey.value = inputKeyVal.trim()
                                        "anthropic" -> viewModel.userAnthropicKey.value = inputKeyVal.trim()
                                    }
                                    viewModel.showNotification("Key updated successfully")
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("SAVE KEY", color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    isTestingKey = true
                                    testResultText = "Contacting routing server..."
                                    testStatusSuccess = null
                                    viewModel.testApiKey(activeTab, inputKeyVal.trim()) { ok, msg ->
                                        isTestingKey = false
                                        testStatusSuccess = ok
                                        testResultText = msg
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isTestingKey) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text("TEST KEY", color = Color.White)
                                }
                            }
                        }

                        if (testResultText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        when (testStatusSuccess) {
                                            true -> Color(0xFF065F46).copy(alpha = 0.2f)
                                            false -> Color(0xFF991B1B).copy(alpha = 0.2f)
                                            else -> Color(0xFF0F172A)
                                        },
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        when (testStatusSuccess) {
                                            true -> Color(0xFF34D399)
                                            false -> Color(0xFFF87171)
                                            else -> Color(0xFF334155)
                                        },
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(10.dp)
                            ) {
                                Text(
                                    testResultText,
                                    color = when (testStatusSuccess) {
                                        true -> Color(0xFF34D399)
                                        false -> Color(0xFFF87171)
                                        else -> Color.White
                                    },
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
