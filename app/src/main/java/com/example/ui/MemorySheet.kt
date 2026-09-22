package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MemoryEntity

@Composable
fun MemorySheet(
    viewModel: AssistantViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val memoryList by viewModel.memories.collectAsState(initial = emptyList())

    var memoryKey by remember { mutableStateOf("") }
    var memoryValue by remember { mutableStateOf("") }

    Card(
        modifier = modifier
            .fillMaxHeight(0.85f)
            .fillMaxWidth()
            .testTag("memory_panel"),
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
                    text = "95 MEMORY DATABASE",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close memory",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            val isPro by viewModel.isPro.collectAsState()
            val totalCount = memoryList.size
            if (!isPro) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MEMORIES SECURED: $totalCount / 3 SLOTS (FREE LIMIT)",
                        color = if (totalCount >= 3) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "MEMORIES SECURED: $totalCount (UNLIMITED PRO ACTIVE)",
                    color = Color(0xFF34D399),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Manual insert fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = memoryKey,
                    onValueChange = { memoryKey = it },
                    placeholder = { Text("Key", fontSize = 12.sp) },
                    modifier = Modifier.weight(0.35f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    )
                )

                TextField(
                    value = memoryValue,
                    onValueChange = { memoryValue = it },
                    placeholder = { Text("Information / Fact", fontSize = 12.sp) },
                    modifier = Modifier.weight(0.65f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (memoryKey.isNotBlank() && memoryValue.isNotBlank()) {
                        viewModel.addManualMemory(memoryKey, memoryValue, "USER_PREFERENCE")
                        memoryKey = ""
                        memoryValue = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("SECURE FACT TO CORE MEMORY", color = Color.Black)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Memory logs
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (memoryList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No facts secured in system vaults, sir.", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                } else {
                    items(memoryList) { memory ->
                        MemoryRow(memory = memory, onDelete = {
                            viewModel.deleteMemory(memory)
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun MemoryRow(
    memory: MemoryEntity,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111827), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${memory.key.uppercase()}:",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
            Text(
                text = memory.value,
                color = Color.White,
                fontSize = 14.sp
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete memory", tint = Color(0xFFEF4444))
        }
    }
}
