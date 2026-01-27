package com.example.dynamusic

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.log10
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current

            androidx.compose.runtime.key(configuration.locales.get(0)) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PagerScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    fun setLocale(languageCode: String) {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val pagerState = rememberPagerState(pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()

    var currentThemeColor by remember { mutableStateOf(Color.Red) }
    var selectedMode by remember { mutableStateOf("") }
    var selectedLanguage by rememberSaveable { mutableStateOf("") }
    val manualDbLevels = remember {
        mutableStateMapOf(
            "pp" to 10f, "p" to 10f, "mp" to 10f,
            "mf" to 10f, "f" to 10f, "ff" to 10f
        )
    }

    val ambientDbLevel by remember { mutableFloatStateOf(60f) }

    val overlayAlpha = remember { androidx.compose.animation.core.Animatable(0f) }

    fun smoothNavigateTo(pageIndex: Int) {
        coroutineScope.launch {
            overlayAlpha.animateTo(1f, tween(500, easing = LinearEasing))
            pagerState.scrollToPage(pageIndex)
            overlayAlpha.animateTo(0f, tween(500, easing = LinearEasing))
        }
    }

    LaunchedEffect(selectedLanguage) {
        if (selectedLanguage.isNotEmpty()) {
            delay(6000)
            if (pagerState.currentPage == 0) {
                smoothNavigateTo(1)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page: Int ->
            when (page) {
                0 -> FirstPageScreen(
                    themeColor = currentThemeColor,
                    selectedLanguage = selectedLanguage,
                    onLanguageSelected = { languageName ->
                        selectedLanguage = languageName
                        val code = when (languageName) {
                            "Español" -> "es"
                            "Français" -> "fr"
                            else -> "en"
                        }
                        activity?.setLocale(code)
                    }
                )
                1 -> SecondPageScreen(
                    selectedThemeColor = currentThemeColor,
                    onThemeSelected = { newColor -> currentThemeColor = newColor },
                    selectedMode = selectedMode,
                    onModeSelected = { newMode -> selectedMode = newMode },
                    onNextClick = { smoothNavigateTo(2) },
                    onSetManualLevels = {
                        smoothNavigateTo(4)
                    },
                    onBreathControlClick = { smoothNavigateTo(3) }
                )
                2 -> ThirdPageScreen(
                    themeColor = currentThemeColor,
                    selectedMode = selectedMode,
                    manualDbLevels = manualDbLevels,
                    onNextClick = { smoothNavigateTo(3) }
                )
                3 -> FourthPageScreen(
                    selectedThemeColor = currentThemeColor,
                    onSettingsClick = { smoothNavigateTo(1) },
                    ambientDbLevel = ambientDbLevel
                )
                4 -> ManualLevelsEditor(
                    manualDbLevels = manualDbLevels,
                    themeColor = currentThemeColor,
                    onSetClick = { smoothNavigateTo(3) }
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
    onLanguageSelected: (String) -> Unit) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    val randomSymbols = remember {
        val options = listOf("pianissimo", "piano", "mezzo-piano", "mezzo-forte", "forte", "fortissimo")
        val safeZoneWidth = screenWidth * 1.0
        val safeZoneHeight = screenHeight * 0.7
        val symbols = mutableListOf<SymbolData>()

        repeat(15) {
            var xOffset: Int
            var yOffset: Int
            var safetyCheckCounter = 0
            do {
                xOffset = Random.nextInt(-screenWidth / 2, screenWidth / 2)
                yOffset = Random.nextInt(-screenHeight / 2, screenHeight / 2)
                val inSafeZoneX = xOffset > -safeZoneWidth / 2 && xOffset < safeZoneWidth / 2
                val inSafeZoneY = yOffset > -safeZoneHeight / 2 && yOffset < safeZoneHeight / 2
                safetyCheckCounter++
            } while (inSafeZoneX && inSafeZoneY && safetyCheckCounter < 50)

            symbols.add(
                SymbolData(
                    text = options.random(),
                    xOffset = xOffset,
                    yOffset = yOffset,
                    rotation = Random.nextFloat() * 360f,
                    size = Random.nextInt(15, 40),
                    color = Color(Random.nextFloat(), Random.nextFloat(), Random.nextFloat(), 0.5f)
                )
            )
        }
        symbols.toList()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        randomSymbols.forEach { symbol ->
            Text(
                text = symbol.text,
                fontSize = symbol.size.sp,
                color = symbol.color,
                modifier = Modifier
                    .offset(x = symbol.xOffset.dp, y = symbol.yOffset.dp)
                    .graphicsLayer(rotationZ = symbol.rotation)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "DYNAMUSIC",
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

            Text(
                text = stringResource(R.string.language_prompt),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

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
                            BorderStroke(3.dp, Color.Black)
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

            if (selectedLanguage.isNotEmpty()) { // <-- Add this condition
                Text(
                    text = stringResource(R.string.intro_text),
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Cursive,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp
                )
            }

            PageIndicator(pageCount = 5, currentPageIndex = 0, themeColor = themeColor)
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun SecondPageScreen(
    selectedThemeColor: Color,
    onThemeSelected: (Color) -> Unit,
    selectedMode: String,
    onModeSelected: (String) -> Unit,
    onNextClick: () -> Unit,
    onSetManualLevels: () -> Unit,
    onBreathControlClick: () -> Unit
) {
    var showManualSliders by remember { mutableStateOf(false) }
    var hasUserSelectedMode by remember { mutableStateOf(false) }
    val manualDbLevels = remember { mutableStateMapOf<String, Float>() }


    LaunchedEffect(selectedMode) {
        if (hasUserSelectedMode && selectedMode != "Manual") {
            delay(500)
            onNextClick()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (showManualSliders) {
            ManualLevelsEditor(
                manualDbLevels = manualDbLevels,
                themeColor = selectedThemeColor,
                onSetClick = {
                    onSetManualLevels()
                    showManualSliders = false
                }
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.welcome_title), // "DYNAMUSIC"
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = selectedThemeColor,
                            blurRadius = 8f
                        )
                    )
                )
                Text(
                    text = stringResource(R.string.settings_title), // "Settings"
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.theme_colour), // "Theme Colour:"
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))

                val themes = listOf(
                    Color.Red,
                    Color.Blue,
                    Color(0xFF6200EE),
                    Color(0xFFFF5722),
                    Color(0xFF006400) // Dark Green
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    themes.forEach { color ->
                        Button(
                            onClick = { onThemeSelected(color) },
                            colors = ButtonDefaults.buttonColors(containerColor = color),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .padding(4.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            border = if (selectedThemeColor == color) BorderStroke(3.dp, Color.Black) else null
                        ) {}
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.calibrate_for), // "Calibrate for..."
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))

                val modesData = listOf(
                    Triple("Voice", stringResource(R.string.mode_voice), stringResource(R.string.voice_desc)),
                    Triple("Instruments", stringResource(R.string.mode_instruments), stringResource(R.string.instr_desc)),
                    Triple("Percussion", stringResource(R.string.mode_percussion), stringResource(R.string.perc_desc)),
                    Triple("Manual", stringResource(R.string.mode_manual), stringResource(R.string.manual_desc))
                )

                modesData.forEach { (internalModeId, displayModeName, description) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Button(
                            onClick = {
                                onModeSelected(internalModeId)
                                if (internalModeId == "Manual") {
                                    manualDbLevels.clear()
                                    showManualSliders = true
                                } else {
                                    hasUserSelectedMode = true
                                }
                            },
                            modifier = Modifier
                                .width(140.dp)
                                .height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (selectedMode == internalModeId) selectedThemeColor else Color.LightGray),
                            border = if (selectedMode == internalModeId) BorderStroke(3.dp, Color.Black) else null,
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
                        ) {
                            Text(
                                text = displayModeName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedMode == internalModeId) Color.White else Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = description,
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Breath Control",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Button(
                        onClick = onBreathControlClick,
                        modifier = Modifier
                            .width(140.dp)
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = selectedThemeColor),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
                    ) {
                        Text(
                            text = "Breath Control",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                PageIndicator(pageCount = 5, currentPageIndex = 1, themeColor = selectedThemeColor)
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun ThirdPageScreen(
    themeColor: Color,
    selectedMode: String,
    manualDbLevels: SnapshotStateMap<String, Float>,
    onNextClick: () -> Unit
) {
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    var hasPermission by remember {
        mutableStateOf(
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var decibels by remember { mutableDoubleStateOf(0.0) }
    var isCalibrating by remember { mutableStateOf(false) }
    var calibrationProgress by remember { mutableIntStateOf(5) }
    var averageAmbientDb by remember { mutableIntStateOf(0) }
    val calibrationReadings = remember { mutableListOf<Double>() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
            if (!isGranted) {
                Toast.makeText(context, "Microphone permission needed", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val recorder = remember { AudioRecorder(context) }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(hasPermission) {
        if (hasPermission) { recorder.start() }
        onDispose { recorder.stop() }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            withContext(Dispatchers.IO) {
                while (isActive) {
                    val amplitude = recorder.getMaxAmplitude()
                    if (amplitude > 0) {
                        val db = 20 * log10(amplitude.toDouble())
                        withContext(Dispatchers.Main) {
                            decibels = if (db > 0) db else 0.0
                        }
                    }
                    delay(500)
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "DYNAMUSIC",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = themeColor,
                        blurRadius = 8f
                    )
                )
            )
            Text(
                text = "Dynamics",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic,
            )
            Spacer(modifier = Modifier.height(16.dp))

            val modeDisplayName = when(selectedMode) {
                "Voice" -> stringResource(R.string.mode_voice)
                "Instruments" -> stringResource(R.string.mode_instruments)
                "Percussion" -> stringResource(R.string.mode_percussion)
                "Manual" -> stringResource(R.string.mode_manual)
                else -> selectedMode
            }

            if (averageAmbientDb > 0) {
                Text(
                    text = stringResource(id = R.string.calibration_mode, modeDisplayName),
                    fontSize = 15.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (hasPermission) {
                val finalThresholds = remember(averageAmbientDb, selectedMode, manualDbLevels) {
                    val thresholds = IntArray(6)
                    if (averageAmbientDb > 0) {
                        if (selectedMode == "Manual") {
                            val dynamicKeys = listOf("pp", "p", "mp", "mf", "f", "ff")
                            var currentCumulativeThreshold = averageAmbientDb

                            for (i in dynamicKeys.indices) {
                                val key = dynamicKeys[i]
                                val offset = manualDbLevels[key]?.toInt() ?: 0
                                currentCumulativeThreshold += offset
                                thresholds[i] = currentCumulativeThreshold
                            }
                        } else {
                            val offsets = when (selectedMode) {
                                "Voice" -> listOf(18, 28, 35, 40, 44, 47)
                                "Instruments" -> listOf(18, 28, 36, 41, 44, 46)
                                "Percussion" -> listOf(18, 30, 40, 48, 55, 62)
                                else -> List(6) { 0 }
                            }
                            for (i in offsets.indices) {
                                thresholds[i] = averageAmbientDb + offsets[i]
                            }
                        }
                    }
                    thresholds
                }


                repeat(7) { index ->
                    val isCalibrationButton = (index == 6)
                    val buttonHeight = if (isCalibrationButton) screenHeight * 0.18f else screenHeight * 0.06f

                    val thresholdDb = if (index < 6) finalThresholds[5 - index] else 0

                    val isActive = !isCalibrationButton && thresholdDb > 0 && decibels >= thresholdDb
                    val baseColor = if (isCalibrationButton) Color.Transparent else if (isActive) themeColor else Color.Gray
                    val animatedColor by animateColorAsState(targetValue = baseColor, label = "ButtonColor")

                    Surface(
                        modifier = Modifier
                            .width(screenWidth * 0.8f)
                            .height(buttonHeight),
                        color = animatedColor,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 4.dp,
                        border = BorderStroke(
                            width = if (isCalibrationButton) 2.dp else 1.dp,
                            color = Color.Black
                        ),
                        onClick = {
                            if (isCalibrationButton && !isCalibrating) {
                                isCalibrating = true
                                calibrationProgress = 0
                                calibrationReadings.clear()

                                coroutineScope.launch {
                                    repeat(5) {
                                        delay(1000)
                                        calibrationReadings.add(decibels)
                                        calibrationProgress += 1
                                    }
                                    if (calibrationReadings.isNotEmpty()) {
                                        averageAmbientDb = calibrationReadings.average().toInt()
                                    }
                                    isCalibrating = false
                                }
                            }
                        },
                        enabled = true
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (isCalibrationButton) {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    repeat(5) { sectionIndex ->
                                        val sectionColor = if (sectionIndex < calibrationProgress) themeColor else Color.LightGray
                                        Box(modifier = Modifier
                                            .weight(1f)
                                            .fillMaxSize()
                                            .background(sectionColor))
                                        if (sectionIndex < 4 && isCalibrating) {
                                            Spacer(modifier = Modifier
                                                .width(1.dp)
                                                .background(Color.Black.copy(alpha = 0.1f)))
                                        }
                                    }
                                }
                            }

                            if (index == 6) {
                                val displayAverage = if (averageAmbientDb > 0) "$averageAmbientDb dB" else ""
                                val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
                                val alpha by infiniteTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = if (averageAmbientDb == 0 && !isCalibrating) 0.3f else 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(2000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "PulseAlpha"
                                )

                                val calibrationText = when {
                                    isCalibrating -> stringResource(R.string.calibrating_text)
                                    averageAmbientDb == 0 -> {
                                        stringResource(
                                            R.string.ambient_start,
                                            String.format(Locale.US, "%.0f", decibels)

                                        )
                                    }
                                    else -> {
                                        stringResource(
                                            R.string.ambient_result,
                                            String.format(Locale.US, "%.0f", decibels),
                                            displayAverage
                                        )
                                    }
                                }

                                Text(
                                    text = calibrationText,
                                    fontSize = 15.sp,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = if (averageAmbientDb == 0 && !isCalibrating) alpha else 1f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(8.dp)
                                )
                            } else {
                                val (abbrText, wordText) = when (index) {
                                    0 -> "ff" to "fortissimo"; 1 -> "f" to "forte"; 2 -> "mf" to "mezzo-forte"
                                    3 -> "mp" to "mezzo-piano"; 4 -> "p" to "piano"; 5 -> "pp" to "pianissimo"
                                    else -> "" to ""
                                }
                                val dbText = if (thresholdDb > 0) "$thresholdDb dB" else ""

                                Box(modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp)) {
                                    Text(text = abbrText, fontSize = 15.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.align(Alignment.CenterStart))
                                    Text(text = wordText, fontSize = 15.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
                                    Text(text = dbText, fontSize = 15.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.align(Alignment.CenterEnd))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                PageIndicator(pageCount = 5, currentPageIndex = 2, themeColor = themeColor)
            } else {
                Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                    Text(stringResource(R.string.grant_permission))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        IconButton(
            onClick = onNextClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(36.dp)
        ) {
            Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings", tint = Color.DarkGray, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun ManualLevelsEditor(
    manualDbLevels: SnapshotStateMap<String, Float>, themeColor: Color,
    onSetClick: () -> Unit
) {
    val dynamicKeys = listOf("ff", "f", "mf", "mp", "p", "pp")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.manual_mode_text),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.manual_desc),
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        dynamicKeys.forEach { key ->
            val sliderValue = manualDbLevels[key] ?: 10f

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = key,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp,
                    modifier = Modifier.width(60.dp)
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { newValue ->
                        manualDbLevels[key] = newValue
                    },
                    valueRange = 1f..20f,
                    steps = 18,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = themeColor,
                        activeTrackColor = themeColor,
                        inactiveTrackColor = themeColor.copy(alpha = 0.24f)
                    )
                )
                Text(
                    text = "${sliderValue.toInt()}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.width(60.dp),
                    textAlign = TextAlign.End
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                dynamicKeys.forEach { key ->
                    if (!manualDbLevels.containsKey(key)) {
                        manualDbLevels[key] = 10f
                    }
                }
                onSetClick()
            },
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = themeColor)
        ) {
            Text(
                text = stringResource(R.string.set_button),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun FourthPageScreen(
    selectedThemeColor: Color,
    onSettingsClick: () -> Unit,
    ambientDbLevel: Float
) {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var decibels by remember { mutableDoubleStateOf(0.0) }
    val maxDecibels = 120.0

    var isCalibrating by remember { mutableStateOf(false) }
    var averageAmbientDb by remember { mutableIntStateOf(0) }
    var isTimerActive by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableIntStateOf(0) }

    var peakDecibel by remember { mutableDoubleStateOf(0.0) }

    var isTimerDeactivated by remember { mutableStateOf(false) }


    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
            if (!isGranted) {
                Toast.makeText(context, "Microphone permission is required for this feature.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val recorder = remember { AudioRecorder(context) }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(hasPermission) {
        if (hasPermission) {
            recorder.start()
        }
        onDispose {
            recorder.stop()
        }
    }

    LaunchedEffect(isTimerActive) {
        if (isTimerActive) {
            while (isActive) {
                delay(1000)
                timerSeconds++
            }
        }
    }

    LaunchedEffect(hasPermission) {
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
                                    val deactivationLevel = peakDecibel - 5
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

    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (averageAmbientDb == 0 && !isCalibrating) 0.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

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
            Text(
                text = "DYNAMUSIC",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = selectedThemeColor,
                        blurRadius = 10f
                    )
                )
            )
            Text(
                text = "Breath Control",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic,
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (hasPermission) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.width(100.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val activationThreshold = averageAmbientDb + 15
                        val shouldShowDecibels = decibels >= activationThreshold && averageAmbientDb > 0
                        val displayedDecibels = if (shouldShowDecibels) decibels else 0.0
                        val decibelText = String.format(Locale.US, "%.0f", displayedDecibels.coerceAtLeast(0.0))

                        Text(
                            text = decibelText,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (shouldShowDecibels) selectedThemeColor else Color.Gray.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "decibels",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(300.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.LightGray.copy(alpha = 0.5f))
                            .border(2.dp, Color.DarkGray, RoundedCornerShape(10.dp))
                    ) {
                        val mercuryFill = (decibels / maxDecibels).coerceIn(0.0, 1.0).toFloat()
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .fillMaxHeight(mercuryFill)
                                .background(selectedThemeColor)
                        )
                        if (averageAmbientDb > 0) {
                            val linePositionDp = (300 * (averageAmbientDb.toFloat() / maxDecibels)).dp
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .offset(y = -linePositionDp)
                                    .background(Color.Black)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.width(100.dp),
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
                            text = "seconds",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        timerSeconds = 0
                        isTimerDeactivated = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                    border = BorderStroke(1.dp, Color.Black)
                ) {
                    Text("Reset Timer", color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))

                val calibrationButtonText = when {
                    isCalibrating -> " CALIBRATING...\nPlease stay silent."
                    averageAmbientDb == 0 -> " Touch to calibrate an\naverage ambient level"
                    else -> "Average ambient Level: $averageAmbientDb dB\n  TOUCH TO RE-CALIBRATE"
                }
                val buttonColor = if (averageAmbientDb == 0 && !isCalibrating) {
                    selectedThemeColor.copy(alpha = alpha)
                } else {
                    selectedThemeColor
                }
                Button(
                    onClick = {
                        if (!isCalibrating) {
                            isCalibrating = true
                            timerSeconds = 0
                            isTimerActive = false
                            peakDecibel = 0.0
                            isTimerDeactivated = false
                            val readings = mutableListOf<Double>()
                            coroutineScope.launch {
                                repeat(5) {
                                    delay(1000)
                                    readings.add(decibels)
                                }
                                if (readings.isNotEmpty()) {
                                    averageAmbientDb = readings.average().toInt()
                                }
                                isCalibrating = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    border = BorderStroke(1.dp, Color.Black)
                ) {
                    Text(
                        text = calibrationButtonText,
                        fontSize = 15.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                    Text("Grant Microphone Permission")
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            PageIndicator(pageCount = 5, currentPageIndex = 3, themeColor = selectedThemeColor)
        }

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings", modifier = Modifier.size(36.dp))
        }
    }
}

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var audioFile: File? = null

    fun start() {
        if (recorder == null) {
            audioFile = File(context.cacheDir, "audio.mp3")

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                try {
                    prepare()
                    start()
                } catch (e: Exception) {
                    // Ignored
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
                // Ignored
            }
        }
        recorder = null
        audioFile?.delete()
    }

    fun getMaxAmplitude(): Int {
        return try {
            recorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }
}

data class SymbolData(
    val text: String,
    val xOffset: Int,
    val yOffset: Int,
    val rotation: Float,
    val size: Int,
    val color: Color
)

@Composable
fun PageIndicator(
    pageCount: Int,
    currentPageIndex: Int,
    themeColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPageIndex

            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(12.dp)
                    .background(
                        color = if (isSelected) themeColor else Color.Transparent,
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) themeColor else Color.Gray,
                        shape = CircleShape
                    )
            )
        }
    }
}
