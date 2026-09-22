package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumOSDashboard(
    viewModel: AssistantViewModel,
    onClose: () -> Unit,
    onNavigateToPro: () -> Unit,
    onNavigateToByok: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val coreState by viewModel.coreState.collectAsState()
    val activeAgent by viewModel.activeSpecialistAgent.collectAsState()
    val activeModel by viewModel.activeModelName.collectAsState()
    val isOnline by viewModel.isOnlineMode.collectAsState()
    val isPrivacy by viewModel.isPrivacyMode.collectAsState()
    val currentLog by viewModel.currentActivityLog.collectAsState()
    val activities by viewModel.activityList.collectAsState()
    val memories by viewModel.memories.collectAsState(initial = emptyList())
    val workflows by viewModel.customWorkflows.collectAsState()
    val fileGuardianOn by viewModel.fileGuardianEnabled.collectAsState()
    val firewallMode by viewModel.permissionFirewallMode.collectAsState()
    val audits by viewModel.fileAuditHistory.collectAsState()
    
    // Mission states
    val missionGoalText by viewModel.missionGoal.collectAsState()
    val missionTasksList by viewModel.missionTasks.collectAsState()

    // Local sandbox interactives
    var selectedSandboxFile by remember { mutableStateOf<AssistantViewModel.GuardianFile?>(null) }
    var guardianAlertActive by remember { mutableStateOf(false) }
    var firewallSimulationActive by remember { mutableStateOf(false) }
    var firewallResultLog by remember { mutableStateOf<String?>(null) }

    // Memory input fields
    var newFactText by remember { mutableStateOf("") }
    
    // Skill builder input fields
    var newSkillName by remember { mutableStateOf("") }
    var newSkillChain by remember { mutableStateOf("") }
    var newSkillDesc by remember { mutableStateOf("") }
    var showSkillBuilderForm by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxHeight(0.92f)
            .fillMaxWidth()
            .testTag("premium_hud_panel"),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF090D14))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "95 SYSTEM HUD & COGNITIVE CONTROLLER",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF4FD8FF),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "VVIPV Operating Layer • Secure Sandbox Active",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.background(Color(0xFF1E293B), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close HUD", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFF1E293B), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // ================= PREMIUM LICENSE STATUS BANNER =================
            val isProActive by viewModel.isPro.collectAsState()
            
            if (!isProActive) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFD97706).copy(alpha = 0.15f), Color(0xFFB45309).copy(alpha = 0.05f))
                            ),
                            RoundedCornerShape(16.dp)
                        )
                        .border(1.dp, Color(0xFFD97706), RoundedCornerShape(16.dp))
                        .clickable { onNavigateToPro() }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("UPGRADE TO NINETY FIVE PRO", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Unlock multi-agent workflows, custom skills, infinite memories, and advanced file guard.", color = Color.LightGray, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onNavigateToPro,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBBF24)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("UPGRADE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF065F46).copy(alpha = 0.15f), Color(0xFF047857).copy(alpha = 0.05f))
                            ),
                            RoundedCornerShape(16.dp)
                        )
                        .border(1.dp, Color(0xFF059669), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("NINETY FIVE PRO ACTIVE LICENSE", color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("All premium client-side software layers and autonomous executors are active.", color = Color.LightGray, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onNavigateToByok,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("KEYS (BYOK)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ================= 1. REAL-TIME 95 SYSTEM STATUS & PULSE =================
            SectionCard(title = "95 Cognitive Status & Pulse", icon = Icons.Default.DeveloperMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Glowing state pulse orb
                    val stateColor = when (coreState) {
                        CoreState.IDLE -> Color(0xFF4FD8FF)
                        CoreState.LISTENING -> Color(0xFF10B981)
                        CoreState.THINKING, CoreState.SEARCHING -> Color(0xFF06B6D4)
                        CoreState.ACTING -> Color(0xFF8B5CF6)
                        CoreState.ERROR -> Color(0xFFEF4444)
                        else -> Color(0xFF94A3B8)
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(stateColor.copy(alpha = 0.15f), CircleShape)
                            .border(1.5.dp, stateColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(stateColor)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SYSTEM ENGINE: ${coreState.name}",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Current activity: $currentLog",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ================= 2. MULTI-AGENT ROUTER & SMART MODELS =================
            SectionCard(title = "Multi-Agent Specialist Router", icon = Icons.Default.SmartToy) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Automatically routes tasks to dedicated specialist agents based on prompt context.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ACTIVE SPECIALIST AGENT", fontSize = 10.sp, color = Color(0xFF4FD8FF), fontWeight = FontWeight.Bold)
                            Text(activeAgent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("COGNITIVE LLM MODEL", fontSize = 10.sp, color = Color(0xFF4FD8FF), fontWeight = FontWeight.Bold)
                            Text(activeModel, fontSize = 14.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("MANUAL SPECIALIST OVERRIDE:", fontSize = 11.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val agents = listOf("Research Agent", "Planning Agent", "Coding Agent", "Vision Agent", "Files Agent", "Automation Agent")
                        agents.forEach { agent ->
                            val isSel = activeAgent == agent
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSel) Color(0xFF4FD8FF) else Color(0xFF1E293B),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        viewModel.activeSpecialistAgent.value = agent
                                        viewModel.activeModelName.value = when (agent) {
                                            "Planning Agent", "Coding Agent" -> "gemini-3.1-pro-preview"
                                            else -> "gemini-3.5-flash"
                                        }
                                        viewModel.currentActivityLog.value = "Manually forced routing to $agent."
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(agent, fontSize = 11.sp, color = if (isSel) Color.Black else Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ================= 3. MEMORY VAULT & EPISODIC FACTS =================
            SectionCard(title = "Cognitive Memory Vault", icon = Icons.Default.Psychology) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "95 injects these remembered facts into interaction sessions to establish contextual awareness.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    // Fact listing
                    if (memories.isEmpty()) {
                        Text(
                            text = "Memory vaults currently clear, sir.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            memories.take(5).forEach { mem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "• ${mem.value}",
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteMemory(mem) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Purge", tint = Color.Red, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Add new fact
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = newFactText,
                            onValueChange = { newFactText = it },
                            placeholder = { Text("Add fact manually, e.g., 'Boss drinks green tea'", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B)
                            )
                        )
                        IconButton(
                            onClick = {
                                if (newFactText.isNotBlank()) {
                                    viewModel.addManualMemory("fact_${System.currentTimeMillis() / 1000}", newFactText, "FACT")
                                    newFactText = ""
                                }
                            },
                            modifier = Modifier.background(Color(0xFF4FD8FF), RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add memory", tint = Color.Black)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ================= 4. MISSION MODE & GOALS =================
            SectionCard(title = "Mission Mode Operations", icon = Icons.Default.TrackChanges) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Orchestrate multi-step operational campaigns with auto-resume task continuity.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    if (missionGoalText == null) {
                        Button(
                            onClick = {
                                viewModel.processUserInput("95, prepare my project for release.")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF4FD8FF))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("INITIATE 'PREPARE PROJECT FOR RELEASE' MISSION", color = Color.White, fontSize = 11.sp)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TARGET GOAL: ${missionGoalText?.uppercase()}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4FD8FF),
                                    fontSize = 13.sp
                                )
                                IconButton(onClick = {
                                    viewModel.missionGoal.value = null
                                    viewModel.missionTasks.value = emptyList()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel mission", tint = Color.Red, modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            
                            val completedCount = missionTasksList.count { it.isCompleted }
                            val totalCount = missionTasksList.size
                            val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
                            
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF10B981),
                                trackColor = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Mission Progress: $completedCount of $totalCount steps completed (${(progress * 100).toInt()}%)",
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            missionTasksList.forEach { task ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (task.isCompleted) Color(0xFF10B981) else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = task.title,
                                        color = if (task.isCompleted) Color.Gray else Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ================= 5. SMART FILE GUARDIAN SANDBOX (🛡️) =================
            SectionCard(title = "Smart File Guardian (🛡️)", icon = Icons.Default.Shield) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Autonomous Risk Analysis",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (fileGuardianOn) "🟢 File Protection Shields: ONLINE" else "🔴 Protection Shields: BYPASSED",
                                fontSize = 12.sp,
                                color = if (fileGuardianOn) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        }
                        Switch(
                            checked = fileGuardianOn,
                            onCheckedChange = { viewModel.fileGuardianEnabled.value = it }
                        )
                    }

                    Text(
                        text = "Test risk levels by selecting a workspace file to simulate deletion under active File Guardian shields:",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )

                    // Sandbox files
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        viewModel.sandboxFilesList.forEach { file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedSandboxFile = file
                                        guardianAlertActive = true
                                    }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val riskColor = when (file.risk) {
                                        "RED" -> Color(0xFFEF4444)
                                        "YELLOW" -> Color(0xFFF59E0B)
                                        else -> Color(0xFF10B981)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(riskColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(file.name, fontSize = 12.sp, color = Color.White)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(file.risk, fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.DeleteForever, contentDescription = "Simulate Deletion", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Alert Dialogue for File Guardian Deletion
                    if (guardianAlertActive && selectedSandboxFile != null) {
                        val activeFile = selectedSandboxFile!!
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E1E2F), RoundedCornerShape(12.dp))
                                .border(1.5.dp, if (fileGuardianOn && activeFile.risk == "RED") Color(0xFFEF4444) else Color(0xFF4FD8FF), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (activeFile.risk == "RED") Color(0xFFEF4444) else Color(0xFFF59E0B)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (fileGuardianOn) "🛡️ FILE GUARDIAN RISK INTERVENTION" else "⚠️ UNPROTECTED DELETION",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }

                                Text(
                                    text = "Target Path: ${activeFile.path}\nSize: ${activeFile.size}\nRisk Assessment Level: [${activeFile.risk}]",
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )

                                if (fileGuardianOn) {
                                    Text(
                                        text = "Risk Analysis:\n${activeFile.desc}",
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                guardianAlertActive = false
                                                viewModel.showNotification("Deletion Cancelled by Guardian shields.")
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                                        ) {
                                            Text("Cancel Action")
                                        }
                                        Button(
                                            onClick = {
                                                guardianAlertActive = false
                                                val newAudit = AssistantViewModel.FileAuditRecord(activeFile.name, activeFile.risk, "User manually bypassed shield and forced deletion of critical dependency.")
                                                viewModel.fileAuditHistory.value = listOf(newAudit) + audits
                                                viewModel.showNotification("File ${activeFile.name} purged under override.")
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                                        ) {
                                            Text("Delete Anyway")
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Protection is currently disabled, sir. File will be overwritten/deleted permanently without safety analysis.",
                                        fontSize = 12.sp,
                                        color = Color(0xFFEF4444)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = {
                                                guardianAlertActive = false
                                                val newAudit = AssistantViewModel.FileAuditRecord(activeFile.name, "UNPROTECTED", "File was deleted without active Guardian checks.")
                                                viewModel.fileAuditHistory.value = listOf(newAudit) + audits
                                                viewModel.showNotification("Simulated Purge successful.")
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                                        ) {
                                            Text("Confirm Deletion (No Analysis)")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("GUARDIAN AUDIT HISTORY LOGS:", fontSize = 11.sp, color = Color(0xFF4FD8FF), fontWeight = FontWeight.Bold)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        audits.take(3).forEach { audit ->
                            Text(
                                text = "• [${audit.riskLevel}] ${audit.filename}: ${audit.explanation}",
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ================= 6. PERMISSIONS FIREWALL (🔐) =================
            SectionCard(title = "Permissions Firewall (🔐)", icon = Icons.Default.Security) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Configures security firewall level for high-impact automation tasks and terminal processes.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val modes = listOf("Always Allowed" to 0, "Ask Before Action" to 1, "Never Allowed" to 2)
                        modes.forEach { (label, mode) ->
                            val isSel = firewallMode == mode
                            Button(
                                onClick = { viewModel.permissionFirewallMode.value = mode },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) Color(0xFF4FD8FF) else Color(0xFF1E293B)
                                ),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text(label, fontSize = 9.sp, color = if (isSel) Color.Black else Color.White)
                            }
                        }
                    }

                    // Test Firewall Simulation
                    Button(
                        onClick = {
                            firewallSimulationActive = true
                            firewallResultLog = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Icon(Icons.Default.Dns, contentDescription = null, tint = Color(0xFF4FD8FF))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SIMULATE COMPANION COMMAND EMULATION", color = Color.White, fontSize = 11.sp)
                    }

                    if (firewallSimulationActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E1E2F), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🔥 FIREWALL SECURITY GATEWAY", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                Text("Automation Agent attempts to execute local bash script: './gradlew cleanBuildCache'", fontSize = 12.sp, color = Color.LightGray)

                                when (firewallMode) {
                                    0 -> {
                                        Text("🟢 Firewall State: ALWAYS ALLOWED. Command bypassed confirmation and executed.", fontSize = 11.sp, color = Color(0xFF10B981))
                                        Button(onClick = { firewallSimulationActive = false }) { Text("Acknowledge") }
                                    }
                                    2 -> {
                                        Text("🔴 Firewall State: NEVER ALLOWED. Action automatically blocked by firewall rules.", fontSize = 11.sp, color = Color(0xFFEF4444))
                                        Button(onClick = { firewallSimulationActive = false }) { Text("Acknowledge") }
                                    }
                                    1 -> {
                                        Text("🟡 Firewall State: ASK BEFORE ACTION. User prompt requested.", fontSize = 11.sp, color = Color(0xFFF59E0B))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(onClick = {
                                                firewallResultLog = "Bypassed: Command executed."
                                                firewallSimulationActive = false
                                                viewModel.showNotification("Action granted once.")
                                            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))) {
                                                Text("Allow Once")
                                            }
                                            Button(onClick = {
                                                firewallResultLog = "Blocked: Command denied by user."
                                                firewallSimulationActive = false
                                                viewModel.showNotification("Action blocked by user.")
                                            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))) {
                                                Text("Block Action")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ================= 7. REUSABLE CUSTOM SKILL BUILDER =================
            SectionCard(title = "Workflow Skill Builder", icon = Icons.Default.Construction) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Build and persist custom, reusable analytical automation chains.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    workflows.forEach { flow ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(flow.name, fontWeight = FontWeight.Bold, color = Color(0xFF4FD8FF), fontSize = 13.sp)
                            Text("Sequence: ${flow.chain}", fontSize = 11.sp, color = Color.White)
                            Text(flow.desc, fontSize = 11.sp, color = Color.LightGray)
                        }
                    }

                    if (!showSkillBuilderForm) {
                        Button(
                            onClick = { showSkillBuilderForm = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("BUILD CUSTOM REUSABLE WORKFLOW", fontSize = 11.sp)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("CREATE REUSABLE PIPELINE", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            
                            TextField(
                                value = newSkillName,
                                onValueChange = { newSkillName = it },
                                placeholder = { Text("Workflow Name, e.g., 'Clean & Deploy'", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            TextField(
                                value = newSkillChain,
                                onValueChange = { newSkillChain = it },
                                placeholder = { Text("Chain: 'Step A -> Step B -> Step C'", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            TextField(
                                value = newSkillDesc,
                                onValueChange = { newSkillDesc = it },
                                placeholder = { Text("Description/Purpose of skill", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (newSkillName.isNotBlank() && newSkillChain.isNotBlank()) {
                                            val created = AssistantViewModel.CustomWorkflow(newSkillName, newSkillChain, newSkillDesc)
                                            viewModel.customWorkflows.value = viewModel.customWorkflows.value + created
                                            viewModel.showNotification("Custom Workflow Registered.")
                                            newSkillName = ""
                                            newSkillChain = ""
                                            newSkillDesc = ""
                                            showSkillBuilderForm = false
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Save Skill")
                                }
                                Button(
                                    onClick = { showSkillBuilderForm = false },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                                ) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ================= 8. REAL-TIME ACTIVITY LOGS & DIAGNOSTICS =================
            SectionCard(title = "Diagnostics & Real-time Logs", icon = Icons.Default.Receipt) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Visual telemetry logs showing internal system loops & diagnostic status:",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        activities.forEach { act ->
                            Text(
                                text = ">> $act",
                                color = Color(0xFF00FFCC),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131A26)),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF4FD8FF), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4FD8FF),
                    letterSpacing = 1.sp
                )
            }
            content()
        }
    }
}
