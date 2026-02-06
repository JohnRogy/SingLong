package com.example.singlong

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.example.singlong.ui.theme.DynamusicTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.log10
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.PaddingValues

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DynamusicTheme {
                // This key will force a recreation of the screen when the language ID changes
                val configuration = androidx.compose.ui.platform.LocalConfiguration.current

                // We use key(configuration.locale) so Compose knows to redraw when locale changes
                androidx.compose.runtime.key(configuration.locales.get(0)) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        PagerScreen(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }

    fun setLocale(languageCode: String) {
        val locale = java.util.Locale(languageCode)
        java.util.Locale.setDefault(locale)


        val config = resources.configuration
        config.setLocale(locale)

        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagerScreen(modifier: Modifier = Modifier) {
    // 1. Get the Activity Context so we can call setLocale
    val context = LocalContext.current
    val activity = context as? MainActivity

    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    var currentThemeColor by remember { mutableStateOf(Color.Red) }
    var selectedMode by remember { mutableStateOf("") }

    // Shared Language State
    var selectedLanguage by remember { mutableStateOf("") }

    // Animation state for the fade transition
    val overlayAlpha = remember { androidx.compose.animation.core.Animatable(0f) }

    fun smoothNavigateTo(pageIndex: Int) {
        coroutineScope.launch {
            overlayAlpha.animateTo(1f, tween(500, easing = LinearEasing))
            pagerState.scrollToPage(pageIndex)
            overlayAlpha.animateTo(0f, tween(500, easing = LinearEasing))
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page: Int ->
            when (page) {
                0 -> FirstPageScreen(
                    themeColor = currentThemeColor,
                    selectedLanguage = selectedLanguage,
                    onLanguageSelected = { languageName ->

                        // 1. Update the UI state button selection
                        selectedLanguage = languageName

                        // 2. Determine the language code
                        val code = when (languageName) {
                            "Español" -> "es"
                            "Français" -> "fr"
                            else -> "en"
                        }

                        // 3. Call the helper function in MainActivity to switch resources
                        activity?.setLocale(code)
                        smoothNavigateTo(1)
                    }
                )
                1 -> SecondPageScreen(
                    selectedThemeColor = currentThemeColor,
                    selectedMode = selectedMode,
                    onModeSelected = { newMode -> selectedMode = newMode },
                    onBreathControlClick = { smoothNavigateTo(2) },
                    onAboutClick = { smoothNavigateTo(3) }
                )
                2 -> FourthPageScreen(
                    selectedThemeColor = currentThemeColor,
                    onSettingsClick = { smoothNavigateTo(1) }
                )
                3 -> AboutScreen(
                    selectedThemeColor = currentThemeColor,
                    onSettingsClick = { smoothNavigateTo(1) }
                )
            }
        }

        if (overlayAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = overlayAlpha.value))
            )
        }
    }
}

@Composable
fun FirstPageScreen(
    themeColor: Color,
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // LAYER 2: MAIN CONTENT
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            // 1. HEADER
            Text(
                text = "SingLong",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = themeColor,
                        blurRadius = 8f
                    )
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // This prompt stays trilingual so users can recognize it
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.language_prompt_en),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.language_prompt_es),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.language_prompt_fr),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            val languages = listOf("English", "Español", "Français")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                languages.forEach { language ->
                    val isSelected = (selectedLanguage == language)

                    OutlinedButton(
                        onClick = { onLanguageSelected(language) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .padding(4.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
                        border = if (isSelected) {
                            BorderStroke(2.dp, Color.Black)
                        } else {
                            ButtonDefaults.outlinedButtonBorder
                        },
                        colors = if (isSelected) {
                            ButtonDefaults.outlinedButtonColors(containerColor = themeColor)
                        } else {
                            ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent)
                        }
                    ) {
                        Text(
                            text = language,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color.Black,
                            maxLines = 1
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(36.dp))

            PageIndicator(pageCount = 4, currentPageIndex = 0, themeColor = themeColor)
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun SecondPageScreen(
    selectedThemeColor: Color,
    selectedMode: String,
    onModeSelected: (String) -> Unit,
    onBreathControlClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(16.dp)
        ) {

            // --- 1. HEADER ---
            Text(
                text = "SingLong",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = selectedThemeColor,
                        blurRadius = 8f
                    )
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_title), // Sub-header "Settings"
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic
            )

            Spacer(modifier = Modifier.height(20.dp))


            Text(
                text = stringResource(R.string.intro_text),
                fontSize = 24.sp,
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.ExtraBold,
                color = Color.DarkGray,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // --- 4. BREATH CONTROL SECTION ---

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Button(
                    onClick = {
                        onModeSelected("Breath Control")
                        onBreathControlClick()
                    },
                    modifier = Modifier
                        .width(150.dp)
                        .height(55.dp),

                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedMode == "Breath Control") selectedThemeColor else Color.White,
                        contentColor = if (selectedMode == "Breath Control") Color.White else Color.Black
                    ),
                    border = BorderStroke(if (selectedMode == "Breath Control") 2.dp else 1.dp, if (selectedMode == "Breath Control") Color.Black else Color.Gray),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.breath_control_title),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    text = stringResource(R.string.breath_control_description),
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.notes_content),
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                color = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                OutlinedButton(
                    onClick = onAboutClick,
                    border = ButtonDefaults.outlinedButtonBorder,
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = Color.Black)
                ) {
                    Text(stringResource(R.string.about_singlong))
                }
            }

            PageIndicator(pageCount = 4, currentPageIndex = 1, themeColor = selectedThemeColor)
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun FourthPageScreen(
    selectedThemeColor: Color,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current

    // --- Audio & Timer State ---
    var hasPermission by remember {
        mutableStateOf(
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var decibels by remember { mutableDoubleStateOf(0.0) }

    // --- Gauge Constants ---
    val minVisibleDb = 10.0
    val maxVisibleDb = 110.0
    val visibleDbRange = maxVisibleDb - minVisibleDb
    val gaugeHeight = 250.dp

    // --- Timer and ambient level states ---
    var isCalibrating by remember { mutableStateOf(false) }
    var calibrationProgress by remember { mutableIntStateOf(0) }
    var averageAmbientDb by remember { mutableIntStateOf(0) }
    var isTimerActive by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableIntStateOf(0) }

    // --- NEW STATE: Track the peak decibel level during a breath ---
    var peakDecibel by remember { mutableDoubleStateOf(0.0) }

    // --- NEW STATE: Lock timer after one use ---
    var isTimerDeactivated by remember { mutableStateOf(false) }

    // --- NEW SLIDER STATE ---
    var sensitivitySliderPosition by remember { mutableFloatStateOf(1f) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
            if (!isGranted) {
                Toast.makeText(context, context.getString(R.string.microphone_permission_required), Toast.LENGTH_SHORT).show()
            }
        }
    )

    val recorder = remember { AudioRecorder(context) }
    val coroutineScope = rememberCoroutineScope()

    // --- Lifecycle management ---
    DisposableEffect(hasPermission) {
        if (hasPermission) {
            recorder.start()
        }
        onDispose {
            recorder.stop()
        }
    }

    // --- Timer counting logic ---
    LaunchedEffect(isTimerActive) {
        if (isTimerActive) {
            while (isActive) {
                delay(1000)
                timerSeconds++
            }
        }
    }

    // --- Audio processing and timer activation loop ---
    LaunchedEffect(hasPermission, sensitivitySliderPosition) {
        if (hasPermission) {
            withContext(Dispatchers.IO) {
                while (isActive) {
                    val amplitude = recorder.getMaxAmplitude()
                    if (amplitude > 0) {
                        val db = 20 * log10(amplitude.toDouble())
                        withContext(Dispatchers.Main) {
                            decibels = if (db > 0) db else 0.0

                            if (averageAmbientDb > 0) {
                                val activationThreshold = averageAmbientDb + 15
                                if (decibels >= activationThreshold && !isTimerActive && !isTimerDeactivated) {
                                    isTimerActive = true
                                    peakDecibel = decibels
                                }
                                if (isTimerActive) {
                                    if (decibels > peakDecibel) {
                                        peakDecibel = decibels
                                    }
                                    // --- Slider Controls Sensitivity ---
                                    val sensitivityOffset = when (sensitivitySliderPosition.roundToInt()) {
                                        0 -> 6.0 // Wide
                                        1 -> 4.0  // Medium
                                        else -> 2.0 // Narrow
                                    }
                                    val deactivationLevel = peakDecibel - sensitivityOffset
                                    if (decibels < deactivationLevel) {
                                        isTimerActive = false
                                        isTimerDeactivated = true
                                    }
                                }
                            }
                        }
                    }
                    delay(1000)
                }
            }
        }
    }

    // --- Pulsing animation for the calibration button ---
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (averageAmbientDb == 0 && !isCalibrating) 0.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    // --- Pulsing animation for the Reset button ---
    val resetPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ResetPulse"
    )
    val resetButtonAlpha = if (isTimerDeactivated) resetPulseAlpha else 1f

    // --- Pulsing animation for the prompt message ---
    val promptPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PromptPulse"
    )

    // --- UI ---
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // --- HEADER ---
            Text(
                text = "SingLong",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = selectedThemeColor,
                        blurRadius = 10f
                    )
                )
            )

            // Sub-header Breath Control
            Text(
                text = stringResource(R.string.breath_control_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic,
            )
            
            // Box to hold the message and reserve space to prevent layout jumps
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(85.dp) // Reserves space for text and padding
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (averageAmbientDb > 0 && timerSeconds == 0 && !isTimerDeactivated && !isCalibrating) {
                    Text(
                        text = stringResource(R.string.sing_or_hum_prompt),
                        fontSize = 22.sp,
                        fontFamily = FontFamily.Cursive,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Red.copy(alpha = promptPulseAlpha),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }

            if (hasPermission) {
                // This Box now controls the alignment of the counters and the gauge
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // --- Decibel Counter (Aligned to Start) ---
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val activationThreshold = averageAmbientDb + 15
                        val shouldShowDecibels = decibels >= activationThreshold && averageAmbientDb > 0
                        val displayedDecibels = if (shouldShowDecibels) decibels else 0.0
                        val decibelText = String.format("%.0f", displayedDecibels.coerceAtLeast(0.0))

                        Text(
                            text = decibelText,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (shouldShowDecibels) selectedThemeColor else Color.Gray.copy(alpha = 0.5f)
                        )
                        Text(
                            text = stringResource(R.string.decibels),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray
                        )
                        // --- SPACER FOR ALIGNMENT ---
                        Spacer(modifier = Modifier.height(78.dp))
                    }

                    // --- GAUGE WITH LABELS (Aligned to Center) ---
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val labelWidth = 40.dp
                        // --- Labels on the left ---
                        Box(
                            modifier = Modifier
                                .height(gaugeHeight)
                                .width(labelWidth)
                                .padding(end = 4.dp), // Create space between labels and gauge
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            val markerLevels = listOf(20, 40, 60, 80, 100)
                            markerLevels.forEach { level ->
                                val positionFraction = ((level - minVisibleDb) / visibleDbRange).coerceIn(0.0, 1.0)
                                Text(
                                    text = "${level}dB",
                                    fontSize = 10.sp,
                                    color = Color.Black,
                                    modifier = Modifier
                                        .offset(
                                            y = -(gaugeHeight * positionFraction.toFloat()) + 5.dp
                                        )
                                )
                            }
                        }

                        // --- GAUGE ---
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .height(gaugeHeight)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.LightGray.copy(alpha = 0.5f))
                                .border(2.dp, Color.DarkGray, RoundedCornerShape(10.dp))
                        ) {
                            val fillFraction = ((decibels - minVisibleDb) / visibleDbRange).coerceIn(0.0, 1.0).toFloat()
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .fillMaxHeight(fillFraction)
                                    .background(selectedThemeColor)
                            )

                            // --- Decibel Markers ---
                            val markerLevels = listOf(20, 40, 60, 80, 100)
                            markerLevels.forEach { level ->
                                val positionFraction = ((level - minVisibleDb) / visibleDbRange).coerceIn(0.0, 1.0)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .width(10.dp) // Corrected width
                                        .height(1.dp)
                                        .offset(y = -(gaugeHeight * positionFraction.toFloat()))
                                        .background(Color.Black)
                                )
                            }

                            // --- Average Ambient Level Line ---
                            if (averageAmbientDb > minVisibleDb) {
                                val avgPositionFraction = ((averageAmbientDb - minVisibleDb) / visibleDbRange).coerceIn(0.0, 1.0)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .offset(y = -(gaugeHeight * avgPositionFraction.toFloat()))
                                        .background(Color.Black)
                                )
                            }
                        }
                        // --- SPACER FOR CENTERING ---
                        Spacer(modifier = Modifier.width(labelWidth + 4.dp))
                    }

                    // --- Timer Counter & Reset Button (Aligned to End) ---
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(100.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$timerSeconds",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTimerActive) selectedThemeColor else Color.Gray.copy(alpha = 0.5f)
                        )
                        Text(
                            text = stringResource(R.string.seconds),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(30.dp))

                        // --- MOVED RESET BUTTON ---
                        Button(
                            onClick = {
                                timerSeconds = 0
                                isTimerDeactivated = false // Unlock the timer
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(width = 100.dp, height = 50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red.copy(alpha = resetButtonAlpha)
                            ),
                            border = BorderStroke(2.dp, Color.Black)
                        ) {
                            Text(
                                stringResource(R.string.reset_timer_button),
                                color = Color.White,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // --- NEW SENSITIVITY SLIDER ---
                Column(
                    modifier = Modifier.fillMaxWidth(0.88f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.variability_of_your_note),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic
                    )
                    Slider(
                        value = sensitivitySliderPosition,
                        onValueChange = { sensitivitySliderPosition = it },
                        colors = SliderDefaults.colors(
                            thumbColor = selectedThemeColor,
                            activeTrackColor = selectedThemeColor,
                            inactiveTrackColor = selectedThemeColor.copy(alpha = 0.3f)
                        ),
                        steps = 1,
                        valueRange = 0f..2f
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.slider_wide), fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.slider_medium), fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.slider_narrow), fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))

                // --- MODIFIED CALIBRATION BUTTON ---
                val calibrationButtonText = when {
                    isCalibrating -> stringResource(R.string.calibrating_text)
                    averageAmbientDb == 0 -> stringResource(R.string.breath_control_calibrate_start)
                    else -> stringResource(R.string.breath_control_calibrate_result, averageAmbientDb)
                }
                val animatedColor by animateColorAsState(
                    targetValue = if (averageAmbientDb == 0 && !isCalibrating) selectedThemeColor.copy(alpha = alpha) else selectedThemeColor,
                    label = "ButtonColor"
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if(isCalibrating) Color.Transparent else animatedColor,
                    border = BorderStroke(2.dp, Color.Black),
                    onClick = {
                        if (!isCalibrating) {
                            isCalibrating = true
                            calibrationProgress = 0 // Reset progress
                            timerSeconds = 0
                            isTimerActive = false
                            peakDecibel = 0.0
                            isTimerDeactivated = false
                            val readings = mutableListOf<Double>()
                            coroutineScope.launch {
                                repeat(5) {
                                    delay(1000)
                                    readings.add(decibels)
                                    calibrationProgress++ // Increment progress
                                }
                                if (readings.isNotEmpty()) {
                                    averageAmbientDb = readings.average().toInt()
                                }
                                isCalibrating = false
                            }
                        }
                    }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (isCalibrating) {
                            Row(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))) {
                                repeat(5) { sectionIndex ->
                                    val sectionColor = if (sectionIndex < calibrationProgress) selectedThemeColor else Color.LightGray
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(sectionColor)
                                    )
                                    if (sectionIndex < 4) { // Dividers
                                        Spacer(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .fillMaxHeight()
                                                .background(Color.Black.copy(alpha = 0.2f))
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = calibrationButtonText,
                            fontSize = 15.sp,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                    Text(stringResource(R.string.grant_permission))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            PageIndicator(pageCount = 4, currentPageIndex = 2, themeColor = selectedThemeColor)
        }

        // --- Settings Button ---
        SettingsButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )
    }
}

@Composable
fun SettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = stringResource(R.string.settings_button_description),
            modifier = Modifier.size(36.dp),
            tint = Color.Black
        )
    }
}

@Composable
fun AboutScreen(
    selectedThemeColor: Color,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val githubUrl = "https://github.com/JohnRogy"
    val liberapayUrl = "https://liberapay.com/JohnRogy/donate"

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.about_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = selectedThemeColor,
                        blurRadius = 8f
                    )
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.about_subtitle),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(id = R.string.about_placeholder),
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                color = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, liberapayUrl.toUri())
                            try {
                                context.startActivity(intent)
                            } catch (_: ActivityNotFoundException) {
                                // Handle the case where the user doesn't have a web browser installed
                                Toast.makeText(context, "No web browser found.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(width = 120.dp, height = 34.dp),
                        border = ButtonDefaults.outlinedButtonBorder,
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = Color.Black)
                    ) {
                        Text(stringResource(id = R.string.support_button))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(id = R.string.support_placeholder),
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color.DarkGray
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, githubUrl.toUri())
                            try {
                                context.startActivity(intent)
                            } catch (_: ActivityNotFoundException) {
                                // Handle the case where the user doesn't have a web browser installed
                                Toast.makeText(context, "No web browser found.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(width = 120.dp, height = 34.dp),
                        border = ButtonDefaults.outlinedButtonBorder,
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = Color.Black)
                    ) {
                        Text(stringResource(id = R.string.view_button))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(id = R.string.view_placeholder),
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color.DarkGray
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            PageIndicator(pageCount = 4, currentPageIndex = 3, themeColor = selectedThemeColor)
        }
        SettingsButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )
    }
}


// --- HELPER CLASSES ---

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var audioFile: File? = null

    fun start() {if (recorder == null) {
        audioFile = File(context.cacheDir, "audio.mp3")

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            // --- CORRECTED LINE ---
            // The AudioEncoder constants are inside MediaRecorder.
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFile?.absolutePath)
            try {
                prepare()
                start()
            } catch (e: Exception) {
                // It's good practice to log the exception to see what's wrong
                Log.e("AudioRecorder", "Prepare failed", e)
                e.printStackTrace()
            }
        }
    }
    }

    fun stop() {
        recorder?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                // It's good practice to log exceptions here too
                Log.e("AudioRecorder", "Stop/release failed", e)
                e.printStackTrace()
            }
        }
        recorder = null
        audioFile?.delete()
    }

    fun getMaxAmplitude(): Int {
        return try {
            recorder?.maxAmplitude ?: 0
        } catch (_:Exception) {
            // This can happen if called after the recorder is released
            0
        }
    }
}

@Composable
fun PageIndicator(
    pageCount: Int,
    currentPageIndex: Int, // 0 for Page 1, 1 for Page 2, etc.
    themeColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 16.dp) // Add some breathing room
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPageIndex

            // Draw the circle for the page indicators.
            Box(
                modifier = Modifier
                    .padding(4.dp) // Space between circles
                    .size(12.dp)   // Size of the circle
                    .background(
                        color = if (isSelected) themeColor else Color.Transparent,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) themeColor else Color.Gray,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
        }
    }
}
