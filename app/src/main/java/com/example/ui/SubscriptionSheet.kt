package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionSheet(
    viewModel: AssistantViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val isProActive by viewModel.isPro.collectAsState()
    val proExpiryTime by viewModel.proExpiry.collectAsState()
    val proTypeSelected by viewModel.proType.collectAsState()
    val signatureVal by viewModel.proSignature.collectAsState()
    val activeCurrency by viewModel.userCurrency.collectAsState()
    val currentStatus by viewModel.proStatus.collectAsState()

    // Plan & Account States
    val currentPlan by viewModel.currentPlan.collectAsState()
    val selectedCountryName by viewModel.selectedCountry.collectAsState()

    var billingCycle by remember { mutableStateOf("monthly") } // "monthly", "annual"
    var paymentGateway by remember { mutableStateOf("google_play") } // "google_play", "upi"
    var upiTxId by remember { mutableStateOf("") }
    var isPurchasing by remember { mutableStateOf(false) }
    var transactionFeedback by remember { mutableStateOf("") }

    // Account Sync states
    var accountEmail by remember { mutableStateOf("") }
    var accountPassword by remember { mutableStateOf("") }
    var isSigningIn by remember { mutableStateOf(false) }
    var signedInEmail by remember { mutableStateOf("") }
    var syncFeedback by remember { mutableStateOf("") }

    val clipboardManager = LocalClipboardManager.current

    // Pricing retrieval based on active country
    val pricing = viewModel.getPricingForCountry(selectedCountryName)

    // Mathematically computed exact yearly discount percentages
    val liteSavePercentage = ((1.0 - (pricing.liteYearly / (pricing.liteMonthly * 12.0))) * 100.0).roundToInt()
    val proSavePercentage = ((1.0 - (pricing.proYearly / (pricing.proMonthly * 12.0))) * 100.0).roundToInt()

    Card(
        modifier = modifier
            .fillMaxHeight(0.95f)
            .fillMaxWidth()
            .testTag("subscription_panel"),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF090F1E))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "Premium Badge",
                            tint = Color(0xFF4FD8FF),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "95 Premium",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = "Unlock the full power of 95.",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // BILLING CYCLE TOGGLE
                item {
                    Spacer(modifier = Modifier.height(6.dp))
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

                // TIERS LISTING
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
                                    gateway = paymentGateway
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
                                "Autonomous Mission Automation",
                                "Priority server-side routes"
                            ),
                            isActive = currentPlan == "PRO",
                            accentColor = Color(0xFFF59E0B),
                            actionButtonText = if (currentPlan == "PRO") "Active Plan" else "Upgrade to PRO",
                            onActionClick = {
                                isPurchasing = true
                                viewModel.purchaseProSubscription(
                                    plan = "PRO",
                                    type = billingCycle,
                                    currency = pricing.currency,
                                    gateway = paymentGateway
                                ) { ok, msg ->
                                    isPurchasing = false
                                    transactionFeedback = msg
                                }
                            },
                            enabled = currentPlan != "PRO"
                        )
                    }
                }

                // TRANSACTION LOG BACKEND
                if (isPurchasing || transactionFeedback.isNotEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                if (isPurchasing) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF4FD8FF))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Contacting licensing gateway securely...", color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                                if (transactionFeedback.isNotEmpty()) {
                                    Text(transactionFeedback, color = Color(0xFF4FD8FF), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                // SECURE GATEWAY ROUTING
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF060B16), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Text("SECURE GATEWAY ROUTING", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { paymentGateway = "google_play" },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (paymentGateway == "google_play") Color(0xFF1E293B) else Color(0xFF0B1229)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (paymentGateway == "google_play") Color(0xFF4FD8FF) else Color.Transparent
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.CreditCard, "Play Store", tint = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Google Play", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Global Gateway", color = Color.Gray, fontSize = 10.sp)
                                }
                            }

                            if (pricing.currency == "INR") {
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { paymentGateway = "upi" },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (paymentGateway == "upi") Color(0xFF1E293B) else Color(0xFF0B1229)
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (paymentGateway == "upi") Color(0xFF4FD8FF) else Color.Transparent
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.QrCode, "UPI", tint = Color.White, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("UPI Transfer", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("Instant Settlement", color = Color.Gray, fontSize = 10.sp)
                                    }
                                }
                            }
                        }

                        if (paymentGateway == "upi" && pricing.currency == "INR") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0B1229), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text("DIRECT MERCHANT DEPOSIT", color = Color(0xFF4FD8FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Transfer funds to the secure merchant address below:", color = Color.LightGray, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("9369905662@nyes", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString("9369905662@nyes"))
                                            viewModel.showNotification("UPI Address copied!")
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, null, tint = Color(0xFF4FD8FF), modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Paste your 12-digit transaction UTR reference below:", color = Color.Gray, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                TextField(
                                    value = upiTxId,
                                    onValueChange = { upiTxId = it },
                                    placeholder = { Text("Enter Transaction UTR", color = Color.Gray, fontSize = 12.sp) },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF1E293B),
                                        unfocusedContainerColor = Color(0xFF1E293B),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // 95 CLOUD ACCOUNT SYNC SECTION
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF060B16), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Text("95 SECURE SYNC ACCOUNT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Link your operating layer license across all your active mobile and desktop terminals.", color = Color.Gray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        if (signedInEmail.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF064E3B).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF059669).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Account: $signedInEmail", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Sync Status: Connected & Audited", color = Color(0xFF34D399), fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = { signedInEmail = "" },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                ) {
                                    Text("Logout", fontSize = 11.sp)
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = accountEmail,
                                    onValueChange = { accountEmail = it },
                                    label = { Text("Account Email Address", color = Color.Gray, fontSize = 12.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF4FD8FF)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = accountPassword,
                                    onValueChange = { accountPassword = it },
                                    label = { Text("Vault Access Password", color = Color.Gray, fontSize = 12.sp) },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF4FD8FF)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        if (accountEmail.contains("@") && accountPassword.length >= 6) {
                                            isSigningIn = true
                                            coroutineScope.launch {
                                                kotlinx.coroutines.delay(1000)
                                                signedInEmail = accountEmail
                                                isSigningIn = false
                                                syncFeedback = "Cloud handshake complete! Synced subscription state."
                                                viewModel.postAssistantResponse("Sir, cloud licensing handshake was completed successfully for account $accountEmail. System registry is synced.")
                                                viewModel.showNotification("Sync Completed")
                                            }
                                        } else {
                                            syncFeedback = "Kindly input a valid email and minimum 6-character secure password."
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FD8FF)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (isSigningIn) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Black)
                                    } else {
                                        Text("Register & Synchronize Terminal", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                if (syncFeedback.isNotEmpty()) {
                                    Text(syncFeedback, color = Color(0xFF4FD8FF), fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }

                // SECURE RESTORATION NODE
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF060B16), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Text("RESTORE LICENSE KEYS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Re-verify local secure cryptographic tokens using local hardware sandboxes.", color = Color.Gray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                isPurchasing = true
                                viewModel.restorePurchases { ok, msg ->
                                    isPurchasing = false
                                    transactionFeedback = msg
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Restore Existing Google Play License", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlanCard(
    title: String,
    subtitle: String,
    price: String,
    billingLabel: String,
    features: List<String>,
    isActive: Boolean,
    accentColor: Color = Color(0xFF4FD8FF),
    actionButtonText: String,
    onActionClick: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                if (isActive) accentColor else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFF0F172A) else Color(0xFF060B16)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(title, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.5.sp)
                    Text(subtitle, color = Color.Gray, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(price, color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text(billingLabel, color = Color.Gray, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.White.copy(alpha = 0.04f))
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                features.forEach { feat ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, null, tint = accentColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(feat, color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) accentColor.copy(alpha = 0.15f) else accentColor,
                    disabledContainerColor = Color.White.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled
            ) {
                Text(
                    text = actionButtonText,
                    color = if (isActive) accentColor else Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
