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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlannerEntity

@Composable
fun PlannerSheet(
    viewModel: AssistantViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items by viewModel.plannerItems.collectAsState(initial = emptyList())

    var taskTitle by remember { mutableStateOf("") }
    var itemType by remember { mutableStateOf("TASK") }

    Card(
        modifier = modifier
            .fillMaxHeight(0.85f)
            .fillMaxWidth()
            .testTag("planner_panel"),
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
                    text = "95 PERSONAL PLANNER",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close planner",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Add new item input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    placeholder = { Text("E.g. Call client tomorrow at 9", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    )
                )

                IconButton(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            viewModel.addManualPlannerItem(taskTitle, itemType, "Created manually from terminal", 2)
                            taskTitle = ""
                        }
                    },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item", tint = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Toggle type buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("TASK", "NOTE", "REMINDER").forEach { type ->
                    Button(
                        onClick = { itemType = type },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (itemType == type) MaterialTheme.colorScheme.primary else Color(0xFF1E293B)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(type, fontSize = 11.sp, color = if (itemType == type) Color.Black else Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // List of active items
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (items.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No scheduling logs registered, sir.", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                } else {
                    items(items) { item ->
                        PlannerRow(item = item, onCheckChange = { isChecked ->
                            viewModel.updatePlannerItemStatus(item.id, isChecked)
                        }, onDelete = {
                            viewModel.deletePlannerItem(item)
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun PlannerRow(
    item: PlannerEntity,
    onCheckChange: (Boolean) -> Unit,
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Checkbox(
                checked = item.isCompleted,
                onCheckedChange = onCheckChange
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = item.title,
                    color = if (item.isCompleted) Color.Gray else Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "[${item.type}] Priority: ${if (item.priority == 3) "High" else if (item.priority == 2) "Medium" else "Low"}",
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete item", tint = Color(0xFFEF4444))
        }
    }
}
