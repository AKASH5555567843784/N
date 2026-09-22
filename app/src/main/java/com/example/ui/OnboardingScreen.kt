package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableStateOf(1) } // 1: Welcome, 2: Language, 3: Country
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // State bindings
    val selectedLang by viewModel.selectedLanguage.collectAsState()
    val selectedCountryName by viewModel.selectedCountry.collectAsState()

    // Supported languages list
    val languages = listOf(
        "English", "Hindi", "Hinglish", "Spanish", "French",
        "Japanese", "German", "Arabic", "Portuguese", "Russian",
        "Mandarin", "Italian", "Korean", "Bengali", "Telugu", "Tamil"
    )
    var langSearchQuery by remember { mutableStateOf("") }
    val filteredLanguages = remember(langSearchQuery) {
        languages.filter { it.contains(langSearchQuery, ignoreCase = true) }
    }

    // Supported countries list with flag emojis
    val countries = listOf(
        "India" to "🇮🇳",
        "United States" to "🇺🇸",
        "Canada" to "🇨🇦",
        "United Kingdom" to "🇬🇧",
        "Germany" to "🇩🇪",
        "France" to "🇫🇷",
        "Italy" to "🇮🇹",
        "Spain" to "🇪🇸",
        "Netherlands" to "🇳🇱",
        "Japan" to "🇯🇵",
        "Australia" to "🇦🇺",
        "Singapore" to "🇸🇬",
        "Brazil" to "🇧🇷",
        "Mexico" to "🇲🇽",
        "Saudi Arabia" to "🇸🇦",
        "UAE" to "🇦🇪",
        "Nepal" to "🇳🇵",
        "Bangladesh" to "🇧🇩",
        "Sri Lanka" to "🇱🇰",
        "Bhutan" to "🇧🇹",
        "Maldives" to "🇲🇻",
        "Mauritius" to "🇲🇺",
        "Fiji" to "🇫🇯",
        "Seychelles" to "🇸🇨",
        "Monaco" to "🇲🇨",
        "San Marino" to "🇸🇲",
        "Liechtenstein" to "🇱🇮",
        "Andorra" to "🇦🇩",
        "Nauru" to "🇳🇷",
        "Tuvalu" to "🇹🇻",
        "Palau" to "🇵🇼",
        "Kiribati" to "🇰🇮",
        "Tonga" to "🇹🇴",
        "Samoa" to "🇼🇸",
        "Vanuatu" to "🇻🇺",
        "Solomon Islands" to "🇸🇧",
        "Lesotho" to "🇱🇸",
        "Eswatini" to "🇸🇿",
        "Gambia" to "🇬🇲",
        "Guinea" to "🇬🇳",
        "Guinea-Bissau" to "🇬🇼",
        "Sierra Leone" to "🇸🇱",
        "Liberia" to "🇱🇷",
        "Cabo Verde" to "🇨🇻",
        "Comoros" to "🇰🇲",
        "Djibouti" to "🇩🇯",
        "São Tomé and Príncipe" to "🇸🇹"
    ).sortedBy { it.first }

    var countrySearchQuery by remember { mutableStateOf("") }
    val filteredCountries = remember(countrySearchQuery) {
        countries.filter { it.first.contains(countrySearchQuery, ignoreCase = true) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617)) // Deep space slate
    ) {
        // Futuristic backing holographic grids
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
                    )
                )
        )

        // Glow effects
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-50).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF4FD8FF).copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )

        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomCenter)
                .offset(y = 100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFD97706).copy(alpha = 0.05f), Color.Transparent)
                    )
                )
        )

        // Glass Card Container
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .align(Alignment.Center)
                .border(1.dp, Color(0xFF4FD8FF).copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                .testTag("onboarding_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1329).copy(alpha = 0.75f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Brand Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4FD8FF))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "95 INTEL LAYER PROTOCOL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4FD8FF),
                            letterSpacing = 1.5.sp
                        )
                    }
                    Text(
                        text = "STEP $step OF 3",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                }

                // Inner changing view
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "StepTransition"
                    ) { currentStep ->
                        when (currentStep) {
                            1 -> {
                                // Step 1: Welcome
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Animated pulse circle placeholder
                                    Box(
                                        modifier = Modifier
                                            .size(110.dp)
                                            .background(Color(0xFF4FD8FF).copy(alpha = 0.05f), CircleShape)
                                            .border(1.5.dp, Color(0xFF4FD8FF).copy(alpha = 0.4f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "95",
                                            color = Color.White,
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(28.dp))
                                    Text(
                                        text = "Welcome to 95",
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Your next-generation personal operating layer has arrived, Boss. Let's calibrate the voice synthesizer and system nodes.",
                                        color = Color(0xFF8B98A3),
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                            2 -> {
                                // Step 2: Language Selection
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = "Calibrate System Language",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Choose your preferred primary dialogue pattern.",
                                        color = Color.Gray,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Search Bar
                                    OutlinedTextField(
                                        value = langSearchQuery,
                                        onValueChange = { langSearchQuery = it },
                                        placeholder = { Text("Search system languages...", color = Color.Gray, fontSize = 13.sp) },
                                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF4FD8FF)) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF4FD8FF),
                                            unfocusedBorderColor = Color(0xFF4FD8FF).copy(alpha = 0.2f),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            cursorColor = Color(0xFF4FD8FF)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))

                                    // List
                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(filteredLanguages) { lang ->
                                            val isSelected = selectedLang == lang
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        if (isSelected) Color(0xFF4FD8FF).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.02f),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) Color(0xFF4FD8FF).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable { viewModel.selectedLanguage.value = lang }
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(lang, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                                if (isSelected) {
                                                    Icon(Icons.Default.Check, null, tint = Color(0xFF4FD8FF), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            3 -> {
                                // Step 3: Country / Region Selection
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = "Assign Core Region",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Select country for precise local billing integration and geo-pricing.",
                                        color = Color.Gray,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Use Device Region Button
                                    Button(
                                        onClick = {
                                            val currentLoc = Locale.getDefault()
                                            val detectedDisplayCountry = currentLoc.displayCountry
                                            // Fallback default lookup
                                            val match = countries.firstOrNull { it.first.equals(detectedDisplayCountry, ignoreCase = true) }
                                            if (match != null) {
                                                viewModel.selectedCountry.value = match.first
                                                val tier = viewModel.getPricingForCountry(match.first)
                                                viewModel.userCurrency.value = tier.currency
                                            } else {
                                                viewModel.selectedCountry.value = "United States"
                                                viewModel.userCurrency.value = "USD"
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FD8FF).copy(alpha = 0.12f)),
                                        border = BorderStroke(1.dp, Color(0xFF4FD8FF).copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.MyLocation, null, tint = Color(0xFF4FD8FF), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Detect Device Location", color = Color(0xFF4FD8FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Search Bar
                                    OutlinedTextField(
                                        value = countrySearchQuery,
                                        onValueChange = { countrySearchQuery = it },
                                        placeholder = { Text("Search countries/regions...", color = Color.Gray, fontSize = 13.sp) },
                                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF4FD8FF)) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF4FD8FF),
                                            unfocusedBorderColor = Color(0xFF4FD8FF).copy(alpha = 0.2f),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            cursorColor = Color(0xFF4FD8FF)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // List
                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(filteredCountries) { countryItem ->
                                            val countryName = countryItem.first
                                            val emoji = countryItem.second
                                            val isSelected = selectedCountryName == countryName
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        if (isSelected) Color(0xFF4FD8FF).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.02f),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) Color(0xFF4FD8FF).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable {
                                                        viewModel.selectedCountry.value = countryName
                                                        val pricing = viewModel.getPricingForCountry(countryName)
                                                        viewModel.userCurrency.value = pricing.currency
                                                    }
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(emoji, fontSize = 20.sp)
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(countryName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                                }
                                                if (isSelected) {
                                                    Icon(Icons.Default.Check, null, tint = Color(0xFF4FD8FF), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 1) {
                        OutlinedButton(
                            onClick = { step-- },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF4FD8FF).copy(alpha = 0.2f))
                        ) {
                            Text("Back", color = Color(0xFF4FD8FF))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Button(
                        onClick = {
                            if (step < 3) {
                                step++
                            } else {
                                // Finalize onboarding
                                coroutineScope.launch {
                                    viewModel.onboardingCompleted.value = true
                                    viewModel.postAssistantResponse("Online protocol active, Boss! Systems are fully calibrated for region: ${viewModel.selectedCountry.value} and language: ${viewModel.selectedLanguage.value}. Standard intelligence nodes are initialized and free to operate.")
                                    viewModel.showNotification("Calibration Complete")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FD8FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (step == 3) "Calibrate System" else "Continue",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
