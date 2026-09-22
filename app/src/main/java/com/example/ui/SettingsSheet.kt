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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun SettingsSheet(
    viewModel: AssistantViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOnline by viewModel.isOnlineMode.collectAsState()
    val isPrivacy by viewModel.isPrivacyMode.collectAsState()
    val wakeWord by viewModel.wakeWordEnabled.collectAsState()
    val speed by viewModel.speechSpeed.collectAsState()
    val pitch by viewModel.speechPitch.collectAsState()
    val lang by viewModel.selectedVoiceLanguage.collectAsState()

    var activeTab by remember { mutableStateOf("PLANS") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_panel")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "95 SYSTEM CONTROL PANEL",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close settings",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Tab bar
            ScrollableTabRow(
                selectedTabIndex = listOf("PLANS", "AI", "VOICE", "PRIVACY", "AUTOMATION", "LANG", "ACCOUNT").indexOf(activeTab).coerceAtLeast(0),
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                listOf("PLANS", "AI", "VOICE", "PRIVACY", "AUTOMATION", "LANG", "ACCOUNT").forEach { tab ->
                    Tab(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        text = { Text(tab, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab content
            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    "AI" -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Online Core Service", color = Color.White)
                                        Text("Query Gemini-3.5-Flash for intelligent processing.", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Switch(
                                        checked = isOnline,
                                        onCheckedChange = { viewModel.isOnlineMode.value = it }
                                    )
                                }
                            }
                            item {
                                Column {
                                    Text("Model Priority Target", color = Color.White)
                                    Text("Primary: gemini-3.5-flash (Online)\nFallback: Local offline reasoning engine (Offline)", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                    "VOICE" -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Background Wake Phrase", color = Color.White)
                                        Text("Listen continuously to 'Hello 95'.", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Switch(
                                        checked = wakeWord,
                                        onCheckedChange = { viewModel.wakeWordEnabled.value = it }
                                    )
                                }
                            }
                            item {
                                Column {
                                    Text("Speech Rate (Speed: ${String.format("%.2f", speed)}x)", color = Color.White)
                                    Slider(
                                        value = speed,
                                        onValueChange = { viewModel.speechSpeed.value = it },
                                        valueRange = 0.6f..1.6f
                                    )
                                }
                            }
                            item {
                                Column {
                                    Text("Speech Deep Male Pitch (Pitch: ${String.format("%.2f", pitch)}x)", color = Color.White)
                                    Slider(
                                        value = pitch,
                                        onValueChange = { viewModel.speechPitch.value = it },
                                        valueRange = 0.5f..1.1f
                                    )
                                }
                            }
                            item {
                                Column {
                                    Text("Default Interaction Language", color = Color.White)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("en-US" to "English", "hi-IN" to "Hindi/Hinglish").forEach { pair ->
                                            Button(
                                                onClick = { viewModel.selectedVoiceLanguage.value = pair.first },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (lang == pair.first) MaterialTheme.colorScheme.primary else Color(0xFF1E293B)
                                                )
                                            ) {
                                                Text(pair.second, color = if (lang == pair.first) Color.Black else Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "PRIVACY" -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Assurance Privacy Mode", color = Color.White)
                                        Text("Disconnect cloud API, lock microphone and prevent trace telemetry.", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Switch(
                                        checked = isPrivacy,
                                        onCheckedChange = { viewModel.isPrivacyMode.value = it }
                                    )
                                }
                            }
                            item {
                                Column {
                                    Text("Secure Credentials Storage", color = Color.White)
                                    Text("API keys stored securely in sandbox environment.", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                    "AUTOMATION" -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("95 Companion PC Link", color = Color.White)
                                        Text("Host: localhost (authenticated).", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Text("CONNECTED", color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                                }
                            }
                            item {
                                Button(
                                    onClick = { viewModel.clearAllMemories() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Wipe Structured AI Memory", color = Color.White)
                                }
                            }
                        }
                    }
                    "LANG" -> {
                        val currentLang by viewModel.selectedLanguage.collectAsState()
                        val currentCountry by viewModel.selectedCountry.collectAsState()

                        val languages = listOf("English", "Hindi", "Hinglish", "Spanish", "French", "German", "Japanese")
                        val countries = listOf("India", "United States", "Canada", "United Kingdom", "Germany", "Australia")

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            item {
                                Column {
                                    Text("System Language Calibration", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Choose your conversational speech interface.", color = Color.Gray, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        languages.forEach { l ->
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (currentLang == l) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (currentLang == l) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { viewModel.selectedLanguage.value = l }
                                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                            ) {
                                                Text(l, color = Color.White, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Column {
                                    Text("Operational Region Calibration", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Calibrates default geo-pricing models and locale parameters.", color = Color.Gray, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        countries.forEach { c ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        if (currentCountry == c) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.02f),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        viewModel.selectedCountry.value = c
                                                        val p = viewModel.getPricingForCountry(c)
                                                        viewModel.userCurrency.value = p.currency
                                                    }
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(c, color = Color.White, fontSize = 13.sp)
                                                if (currentCountry == c) {
                                                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "ACCOUNT" -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                                        .padding(14.dp)
                                ) {
                                    Text("95 Terminal Handshake Sync", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Ensure all your peripheral devices share the current system license.", color = Color.Gray, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    var testEmail by remember { mutableStateOf("") }
                                    OutlinedTextField(
                                        value = testEmail,
                                        onValueChange = { testEmail = it },
                                        placeholder = { Text("Account email", color = Color.Gray, fontSize = 12.sp) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            if (testEmail.contains("@")) {
                                                viewModel.showNotification("Connected to $testEmail")
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Sync Device", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    "PLANS" -> {
                        val currentPlan by viewModel.currentPlan.collectAsState()
                        val selectedCountryName by viewModel.selectedCountry.collectAsState()
                        val pricing = viewModel.getPricingForCountry(selectedCountryName)

                        var billingCycle by remember { mutableStateOf("monthly") }
                        var isPurchasing by remember { mutableStateOf(false) }
                        var transactionFeedback by remember { mutableStateOf("") }

                        val liteSavePercentage = ((1.0 - (pricing.liteYearly / (pricing.liteMonthly * 12.0))) * 100.0).roundToInt()
                        val proSavePercentage = ((1.0 - (pricing.proYearly / (pricing.proMonthly * 12.0))) * 100.0).roundToInt()

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Active License Tier", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text(currentPlan.uppercase(), color = MaterialTheme.colorScheme.primary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                        Icon(Icons.Default.WorkspacePremium, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Limits & Cognitive Status:", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = when(currentPlan) {
                                            "FREE" -> "- Memory nodes cap: 3 facts max.\n- Core Specialist AI: Research only."
                                            "LITE" -> "- Memory nodes cap: 10 facts max.\n- Multi-Agent specialty active."
                                            else -> "- Memory nodes: Unlimited Vault.\n- Full Autonomous Operations active."
                                        },
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            // BILLING CYCLE TOGGLE
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF060B16), RoundedCornerShape(12.dp))
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (billingCycle == "monthly") Color(0xFF1E293B) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { billingCycle = "monthly" }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Monthly Billing", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (billingCycle == "annual") Color(0xFF1E293B) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { billingCycle = "annual" }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Annual Billing", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "Save",
                                                color = Color(0xFF10B981),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .background(Color(0xFF064E3B), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // PLAN COMPARISON TIERS
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // 1. FREE PLAN CARD
                                    PlanCard(
                                        title = "FREE PLAN",
                                        subtitle = "Basic operational layer",
                                        price = "${pricing.symbol}0",
                                        billingLabel = "Forever",
                                        features = listOf(
                                            "3 AI Cognition Memories max",
                                            "Research Agent only",
                                            "Basic command processing"
                                        ),
                                        isActive = currentPlan == "FREE",
                                        actionButtonText = "Current Tier",
                                        onActionClick = {},
                                        enabled = false
                                    )

                                    // 2. LITE PLAN CARD
                                    val litePrice = if (billingCycle == "monthly") pricing.liteMonthly else pricing.liteYearly
                                    val liteBillingText = if (billingCycle == "monthly") "per month" else "per year (Save $liteSavePercentage%)"
                                    PlanCard(
                                        title = "LITE PLAN",
                                        subtitle = "Expanded tactical capability",
                                        price = "${pricing.symbol}$litePrice",
                                        billingLabel = liteBillingText,
                                        features = listOf(
                                            "10 AI Cognition Memories max",
                                            "Research, Planning, Coding Agents",
                                            "Task continuity auto-resume"
                                        ),
                                        isActive = currentPlan == "LITE",
                                        actionButtonText = if (currentPlan == "LITE") "Active Plan" else "Upgrade to LITE",
                                        onActionClick = {
                                            isPurchasing = true
                                            viewModel.purchaseProSubscription(
                                                plan = "LITE",
                                                type = billingCycle,
                                                currency = pricing.currency,
                                                gateway = "google_play"
                                            ) { ok, msg ->
                                                isPurchasing = false
                                                transactionFeedback = msg
                                            }
                                        },
                                        enabled = currentPlan != "LITE"
                                    )

                                    // 3. PRO PLAN CARD
                                    val proPrice = if (billingCycle == "monthly") pricing.proMonthly else pricing.proYearly
                                    val proBillingText = if (billingCycle == "monthly") "per month" else "per year (Save $proSavePercentage%)"
                                    PlanCard(
                                        title = "PRO PLAN",
                                        subtitle = "Unlimited cognitive autonomy",
                                        price = "${pricing.symbol}$proPrice",
                                        billingLabel = proBillingText,
                                        features = listOf(
                                            "Unlimited AI Memories Vault",
                                            "Full Specialist Multi-Agents",
                                            "Autonomous background tasks",
                                            "Priority LLM latency routing"
                                        ),
                                        isActive = currentPlan == "PRO",
                                        actionButtonText = if (currentPlan == "PRO") "Active Plan" else "Upgrade to PRO",
                                        onActionClick = {
                                            isPurchasing = true
                                            viewModel.purchaseProSubscription(
                                                plan = "PRO",
                                                type = billingCycle,
                                                currency = pricing.currency,
                                                gateway = "google_play"
                                            ) { ok, msg ->
                                                isPurchasing = false
                                                transactionFeedback = msg
                                            }
                                        },
                                        enabled = currentPlan != "PRO"
                                    )
                                }
                            }

                            if (transactionFeedback.isNotEmpty()) {
                                item {
                                    Text(
                                        text = transactionFeedback,
                                        color = if (transactionFeedback.contains("Success", ignoreCase = true)) Color(0xFF34D399) else Color(0xFFF87171),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
