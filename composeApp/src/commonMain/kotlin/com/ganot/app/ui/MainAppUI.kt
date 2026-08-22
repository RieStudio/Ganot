package com.ganot.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import com.ganot.app.BuildKonfig
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.testTag
import com.ganot.app.getPlatformHelper
import kotlinx.datetime.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ganot.app.data.CalendarEvent
import com.ganot.app.data.Lecture
import com.ganot.app.data.GradeCriterion
import com.ganot.app.ui.theme.*

enum class AppScreen {
    HOME, TRACKING, FOCUS, CALENDAR, SETTINGS
}

@Composable
fun MainAppUI(viewModel: AcademicViewModel) {
    val userName by viewModel.userName.collectAsState()
    val completedTutorial by viewModel.completedTutorial.collectAsState()
    val forceShowTutorial by viewModel.forceShowTutorial.collectAsState()
    var showSplash by remember { mutableStateOf(true) }
    
    if (showSplash) {
        SplashScreen(onDismiss = { showSplash = false })
    } else if (forceShowTutorial) {
        TutorialScreen(onTutorialCompleted = {
            viewModel.completeTutorialReplay()
        })
    } else if (!completedTutorial && userName.isEmpty()) {
        TutorialScreen(onTutorialCompleted = {
            viewModel.completeTutorial()
        })
    } else if (userName.isEmpty()) {
        OnboardingScreen(viewModel = viewModel, onNameSubmitted = { name ->
            viewModel.saveUserName(name)
        })
    } else {
        var currentScreen by rememberSaveable { mutableStateOf(AppScreen.HOME) }
        
        val lectures by viewModel.lectures.collectAsState()
        val events by viewModel.events.collectAsState()
        val sessions by viewModel.sessions.collectAsState()

        val hazeState = remember { HazeState() }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(state = hazeState)
                        .padding(top = innerPadding.calculateTopPadding())
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            // Fast & Fluid: Subtle fade + micro-scale for snappiness
                            fadeIn(animationSpec = tween(250, easing = LinearOutSlowInEasing)) +
                            scaleIn(initialScale = 0.98f, animationSpec = tween(250, easing = LinearOutSlowInEasing)) togetherWith
                            fadeOut(animationSpec = tween(200, easing = FastOutLinearInEasing))
                        },
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            AppScreen.HOME -> HomeScreen(viewModel, lectures, events, sessions, onNavigateTo = { currentScreen = it })
                            AppScreen.TRACKING -> TrackingScreen(viewModel, lectures)
                            AppScreen.FOCUS -> FocusScreen(viewModel, lectures, sessions)
                            AppScreen.CALENDAR -> CalendarScreen(viewModel, lectures, events)
                            AppScreen.SETTINGS -> SettingsScreen(viewModel, lectures, sessions)
                        }
                    }
                }

                AcademicBottomBar(
                    currentScreen = currentScreen,
                    onScreenSelected = { currentScreen = it },
                    hazeState = hazeState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
fun OnboardingScreen(viewModel: AcademicViewModel, onNameSubmitted: (String) -> Unit) {
    var nameInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val isDark = isDarkThemeActive()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        val containerBg = if (isDark) DeepCardDark else Color(0xFFEFE6D5)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(containerBg)
                .padding(32.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profil Simgesi",
                        tint = if (isDark) AdaptWhite.copy(alpha = 0.6f) else AdaptGray,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "G A N O T",
                    fontSize = 11.sp,
                    color = NeonBlue,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Hoş Geldin!",
                    fontSize = 24.sp,
                    color = AdaptWhite,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Kurulumunuzu tamamlamak için lütfen isminizi girin.",
                    fontSize = 13.sp,
                    color = AdaptLightGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                val inputContainerBg = if (isDark) DeepSurfaceDark else Color(0xFFFAF6EE)
                
                Text(
                    text = "İsminiz",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.LightGray else Color.DarkGray,
                    modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 6.dp)
                )
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                        if (it.isNotBlank()) isError = false
                    },
                    singleLine = true,
                    isError = isError,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AdaptWhite,
                        unfocusedTextColor = AdaptWhite,
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = AdaptWhite.copy(alpha = 0.12f),
                        errorBorderColor = WarningRed,
                        focusedContainerColor = inputContainerBg,
                        unfocusedContainerColor = inputContainerBg
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isError) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Lütfen geçerli bir isim girin.",
                        color = WarningRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.align(Alignment.Start).padding(start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color(0xFF14121F) else Color(0xFFF9F6F0))
                        .border(1.dp, if (isDark) NeonBlue.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Bilgi",
                        tint = NeonBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Sistemimiz standart YÖK harf notu skalasını ve Kredi sistemini varsayılan olarak kullanacaktır. Bu ayarları dilediğiniz zaman Detaylı Ayarlar menüsünden değiştirebilirsiniz.",
                        fontSize = 11.sp,
                        color = AdaptLightGray,
                        lineHeight = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val isNameValid = nameInput.trim().isNotBlank()
                        isError = !isNameValid

                        if (isNameValid) {
                            onNameSubmitted(nameInput.trim())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonBlue
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = "Başlayalım",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun TutorialScreen(onTutorialCompleted: () -> Unit) {
    var currentSlide by remember { mutableStateOf(0) }
    val isDark = isDarkThemeActive()

    val slides = listOf(
        TutorialSlideData(
            title = "DERS TAKİBİ & AKADEMİ",
            subtitle = "Derslerinizi Kolayca Ekleyin",
            description = "Akademi sayfasından takip etmek istediğiniz dersleri, kredi değerleri ve devamsızlık limitleriyle birlikte ekleyerek ilk adımı atın.",
            icon = Icons.Default.Add,
            accentColor = ElectricBlue
        ),
        TutorialSlideData(
            title = "DEĞERLENDİRME KRİTERLERİ",
            subtitle = "Sınav Ağırlıklarını Belirleyin",
            description = "Ayarlar > Ders Değerlendirme Bölümünden vize, final, ödev gibi kriterlerin ağırlık yüzdelerini (%) özelleştirin, dönem sonu notunuzu anlık hesaplayın.",
            icon = Icons.Default.Settings,
            accentColor = NeonPurple
        ),
        TutorialSlideData(
            title = "ODAKLANMA ZAMANLAYICISI",
            subtitle = "Pomodoro & Serbest Çalışma",
            description = "Çalışma seanslarınızı Pomodoro veya her anı takip eden Serbest mod ile başlatın. Kaydettiğiniz sürelerle ders bazında verimlilik analizleri gerçekleştirin.",
            icon = Icons.Default.PlayArrow,
            accentColor = AlertOrange
        ),
        TutorialSlideData(
            title = "AKADEMİK AJANDA & BİLDİRİMLER",
            subtitle = "Sınav & Ödev Teslimlerini Kaçırmayın",
            description = "Önemli sınav ve teslimlerinizi takvime işleyin. Sistem tarafından otomatik planlanan hatırlatma bildirimleri ile her şey kontrolünüz altında olsun.",
            icon = Icons.Default.DateRange,
            accentColor = MintGreen
        ),
        TutorialSlideData(
            title = "ŞABLON VE DERS PAYLAŞIMI",
            subtitle = "Ayarlarınızı Tek Tıkla Paylaşın",
            description = "Eklediğiniz dersleri, kredi sayılarını ve sınav ağırlık yüzdelerini tek dokunuşla kod haline getirin. Arkadaşlarınızla paylaşın veya onlardan gelen şablon koduyla dersleri saniyeler içinde kopyalayın!",
            icon = Icons.Default.Send,
            accentColor = NeonPurple
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        val slide = slides[currentSlide]
        val containerBg = if (isDark) DeepCardDark else Color(0xFFEFE6D5)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 550.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(containerBg)
                .padding(32.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Atla",
                        fontSize = 13.sp,
                        color = AdaptGray,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { onTutorialCompleted() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(slide.accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = slide.icon,
                        contentDescription = slide.subtitle,
                        tint = containerBg,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = slide.title,
                    fontSize = 11.sp,
                    color = slide.accentColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = slide.subtitle,
                    fontSize = 20.sp,
                    color = AdaptWhite,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = slide.description,
                    fontSize = 13.sp,
                    color = AdaptLightGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    slides.forEachIndexed { idx, _ ->
                        val isSelected = currentSlide == idx
                        val dotWidth = if (isSelected) 18.dp else 6.dp
                        val dotColor = if (isSelected) slide.accentColor else AdaptGray.copy(alpha = 0.3f)
                        Box(
                            modifier = Modifier
                                .width(dotWidth)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                                .clickable { currentSlide = idx }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button / Prevs
                    if (currentSlide > 0) {
                        Button(
                            onClick = { currentSlide-- },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Text("Geri", color = AdaptWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(70.dp))
                    }

                    // Next / Start button
                    Button(
                        onClick = {
                            if (currentSlide < slides.lastIndex) {
                                currentSlide++
                            } else {
                                onTutorialCompleted()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = slide.accentColor
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(46.dp)
                            .padding(start = 12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = if (currentSlide == slides.lastIndex) "Hadi Başlayalım" else "İleri",
                            color = Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

data class TutorialSlideData(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accentColor: Color
)

@Composable
fun AcademicBottomBar(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        val isDark = isDarkThemeActive()
        val barShape = RoundedCornerShape(36.dp)
        
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(barShape)
                .hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        tint = HazeTint(if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)),
                        blurRadius = 40.dp,
                        noiseFactor = 0.2f
                    )
                ),
            containerColor = Color.Transparent,
            shape = barShape,
            borderWidth = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomBarItem(
                    screen = AppScreen.HOME,
                    label = "MERKEZ",
                    icon = Icons.Default.Home,
                    isSelected = currentScreen == AppScreen.HOME,
                    onClick = { onScreenSelected(AppScreen.HOME) },
                    testTag = "nav_home"
                )
                BottomBarItem(
                    screen = AppScreen.TRACKING,
                    label = "AKADEMI",
                    icon = Icons.Default.List,
                    isSelected = currentScreen == AppScreen.TRACKING,
                    onClick = { onScreenSelected(AppScreen.TRACKING) },
                    testTag = "nav_tracking"
                )
                BottomBarItem(
                    screen = AppScreen.FOCUS,
                    label = "ODAK",
                    icon = Icons.Default.PlayArrow,
                    isSelected = currentScreen == AppScreen.FOCUS,
                    onClick = { onScreenSelected(AppScreen.FOCUS) },
                    testTag = "nav_focus"
                )
                BottomBarItem(
                    screen = AppScreen.CALENDAR,
                    label = "TAKVIM",
                    icon = Icons.Default.DateRange,
                    isSelected = currentScreen == AppScreen.CALENDAR,
                    onClick = { onScreenSelected(AppScreen.CALENDAR) },
                    testTag = "nav_calendar"
                )
                BottomBarItem(
                    screen = AppScreen.SETTINGS,
                    label = "AYARLAR",
                    icon = Icons.Default.Settings,
                    isSelected = currentScreen == AppScreen.SETTINGS,
                    onClick = { onScreenSelected(AppScreen.SETTINGS) },
                    testTag = "nav_settings"
                )
            }
        }
    }
}

@Composable
fun RowScope.BottomBarItem(
    screen: AppScreen,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val duration = 300
    val isDark = isDarkThemeActive()
    
    // Modern gray selection background
    val selectionBgColor by animateColorAsState(
        targetValue = if (isSelected) {
            if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
        } else Color.Transparent,
        animationSpec = tween(duration),
        label = "SelectionBg"
    )

    val activeColor = ElectricBlue
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else AdaptWhite.copy(alpha = 0.45f),
        animationSpec = tween(duration),
        label = "BottomNavColor"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "BottomNavScale"
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(selectionBgColor)
                .scale(scale),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: AcademicViewModel,
    lectures: List<Lecture>,
    events: List<CalendarEvent>,
    sessions: List<com.ganot.app.data.FocusSession>,
    onNavigateTo: (AppScreen) -> Unit
) {
    val (gpa, totalCredits) = viewModel.getCPAGradingTotal()
    val criticalLectures = lectures.filter { it.currentAbsenteeism >= it.maxAbsenteeism * 0.75 }
    val upcomingEvents = events.filter { !it.isCompleted && it.dueDate > Clock.System.now().toEpochMilliseconds() }
        .sortedBy { it.dueDate }
        .take(3)

    val lastSession = sessions.maxByOrNull { it.timestamp }
    val lastSessionText = if (lastSession != null) {
        val courseName = lectures.find { it.id == lastSession.lectureId }?.name ?: "Genel"
        val durationMins = lastSession.durationMs / 1000 / 60
        "Son: $courseName (${durationMins}dk)"
    } else {
        "Son: Seans Bulunmuyor"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    val userName by viewModel.userName.collectAsState()
                    Text(
                        text = "Merhaba, $userName",
                        fontSize = 20.sp,
                        color = AdaptWhite,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)
        ) {
            item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                NeonPurple.copy(alpha = 0.25f),
                                ElectricBlue.copy(alpha = 0.15f)
                            )
                        )
                    )
                    .padding(1.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(DeepSurfaceDark.copy(alpha = 0.9f))
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "GENEL ORTALAMA",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = getPlatformHelper().formatDouble(gpa, 2),
                                    fontSize = 36.sp,
                                    color = AdaptWhite,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-1).sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                        
                        Box(
                            modifier = Modifier.size(76.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val arcTrackColor = if (isDarkThemeActive()) Color(0xFF262626) else Color(0xFFEFE6D5)
                            val canvasColor = NeonPurple
                            Canvas(modifier = Modifier.size(76.dp)) {
                                drawArc(
                                    color = arcTrackColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                )
                                val sweepAmount = ((gpa / 4.0).coerceIn(0.0, 1.0) * 360f).toFloat()
                                drawArc(
                                    color = canvasColor,
                                    startAngle = -90f,
                                    sweepAngle = sweepAmount,
                                    useCenter = false,
                                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            
                            Text(
                                text = "4.00",
                                fontSize = 11.sp,
                                color = AdaptWhite,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                        }
                    }
                }
            }
        }

        if (criticalLectures.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "KRİTİK DEVAMSIZLIK",
                        fontSize = 11.sp,
                        color = AdaptLightGray,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .background(WarningRed, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${criticalLectures.size} DERS RİSKTE",
                            fontSize = 8.5.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            val chunkedCritical = criticalLectures.chunked(2)
            items(chunkedCritical) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { lecture ->
                        val currentRatio = lecture.currentAbsenteeism.toFloat() / lecture.maxAbsenteeism.toFloat()
                        val barColor = if (currentRatio >= 0.90f) WarningRed else AlertOrange
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(DeepCardDark)
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = getPlatformHelper().toUppercaseTurkish(lecture.name),
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    val isDark = isDarkThemeActive()
                                    val denomColor = if (isDark) Color(0xFF475569) else Color(0xFF94A3B8)
                                    val trackBgColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                                    Text(
                                        text = buildAnnotatedString {
                                            append("${lecture.currentAbsenteeism}")
                                            withStyle(style = SpanStyle(color = denomColor)) {
                                                append("/${lecture.maxAbsenteeism}")
                                            }
                                        },
                                        fontSize = 20.sp,
                                        color = AdaptWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .width(56.dp)
                                            .height(5.dp)
                                            .clip(CircleShape)
                                            .background(trackBgColor)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(currentRatio.coerceIn(0f, 1f))
                                                .background(barColor, CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (pair.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "YAKLAŞAN AKADEMİK AJANDA",
                    fontSize = 11.sp,
                    color = AdaptLightGray,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Text(
                    text = "Tümünü Gör",
                    fontSize = 11.sp,
                    color = ElectricBlue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateTo(AppScreen.CALENDAR) },
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        if (upcomingEvents.isEmpty()) {
            item {
                GlassCard {
                    Text(
                        text = "Planlanmış yakın sınav veya ödev bulunmuyor. Eklemek için Takvim'e gidebilirsiniz.",
                        color = AdaptLightGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    )
                }
            }
        } else {
            items(upcomingEvents) { event ->
                val associatedLecture = lectures.find { it.id == event.lectureId }
                
                GlassCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val badgeColor = when (event.type) {
                                "SINAV" -> WarningRed
                                "ÖDEV" -> ElectricBlue
                                else -> NeonPurple
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = event.type,
                                        fontSize = 10.sp,
                                        color = badgeColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = associatedLecture?.name ?: "Genel",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = event.title,
                                fontSize = 16.sp,
                                color = AdaptWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = getPlatformHelper().formatDate(event.dueDate, "dd MMMM yyyy - HH:mm"),
                                fontSize = 12.sp,
                                color = AdaptLightGray
                            )
                        }
                        
                        IconButton(
                            onClick = { viewModel.toggleEventCompleted(event) },
                            modifier = Modifier.background(AdaptWhite.copy(alpha = 0.05f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Tamamla",
                                tint = ElectricBlue
                            )
                        }
                    }
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(AdaptWhite.copy(alpha = 0.02f))
                    .border(1.dp, AdaptWhite.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                    .clickable { onNavigateTo(AppScreen.FOCUS) }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .border(1.5.dp, ElectricBlue.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 0.8f,
                                targetValue = 1.4f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "PulseRadius"
                            )
                            
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .scale(pulseScale)
                                    .background(ElectricBlue, CircleShape),
                            )
                        }
                        
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Odaklanma Modu",
                                fontSize = 13.sp,
                                color = AdaptWhite,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = lastSessionText,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    
                    Button(
                        onClick = { onNavigateTo(AppScreen.FOCUS) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "BAŞLAT",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.2).sp
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun TrackingScreen(viewModel: AcademicViewModel, lectures: List<Lecture>) {
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Akademik Takip",
                            fontSize = 22.sp,
                            color = AdaptWhite,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "Ders bazında devamsızlık ve not takibi",
                            fontSize = 11.5.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricBlue)
                            .clickable { showAddDialog = true }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Ders Ekle",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Ders Ekle",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)
            ) {

            if (lectures.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(ElectricBlue.copy(alpha = 0.08f))
                                    .border(1.dp, ElectricBlue.copy(alpha = 0.25f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Bilgi",
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Henüz ders eklemedin.",
                                color = AdaptWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Başlamak için sağ üstteki '+ Ders Ekle' butonuna dokun.",
                                color = AdaptLightGray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            } else {
                items(lectures) { lecture ->
                    LectureWithDetailsCard(viewModel = viewModel, lecture = lecture)
                }
            }
        }
        }

        if (showAddDialog) {
            AddLectureDialog(
                viewModel = viewModel,
                onDismiss = { showAddDialog = false },
                onAdd = { name, credit, maxAbs, manualGrade ->
                    viewModel.addLecture(
                        Lecture(
                            name = name,
                            credit = credit,
                            maxAbsenteeism = maxAbs,
                            manualLetterGrade = manualGrade
                        )
                    )
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun LectureWithDetailsCard(viewModel: AcademicViewModel, lecture: Lecture) {
    var isExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val activeWeightType by viewModel.weightType.collectAsState()
    val activeCoefficients by viewModel.activeCoefficients.collectAsState()
    
    val currentRatio = lecture.currentAbsenteeism.toFloat() / lecture.maxAbsenteeism.toFloat()
    val isCritical = currentRatio >= 0.75f
    val arcColor = when {
        currentRatio >= 0.90f -> WarningRed
        currentRatio >= 0.75f -> AlertOrange
        else -> MintGreen
    }

    NeonCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = if (isCritical) WarningRed else ElectricBlue
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lecture.name,
                        fontSize = 18.sp,
                        color = AdaptWhite,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val dynamicLabel = if (activeWeightType == "AKTS") "AKTS" else "Kredi"

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "$dynamicLabel: ${lecture.credit}",
                            fontSize = 12.sp,
                            color = AdaptLightGray
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Not: ${lecture.getLetterGrade(activeCoefficients)}${lecture.calculateCurrentGrade()?.let { " (${getPlatformHelper().formatDouble(it, 1)})" } ?: ""}",
                            fontSize = 12.sp,
                            color = ElectricBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Box(
                        modifier = Modifier.size(52.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val isDark = isDarkThemeActive()
                        val trackColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFDCD2C1)
                        Canvas(modifier = Modifier.size(52.dp)) {
                            drawArc(
                                color = trackColor,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = arcColor,
                                startAngle = -90f,
                                sweepAngle = (currentRatio.coerceIn(0f, 1f) * 360f),
                                useCenter = false,
                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${lecture.currentAbsenteeism}",
                                fontSize = 14.sp,
                                color = AdaptWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Sınır: ${lecture.maxAbsenteeism}",
                        fontSize = 10.sp,
                        color = AdaptLightGray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Devamsızlık Kaydı:",
                    fontSize = 12.sp,
                    color = AdaptLightGray
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                        .height(1.dp)
                        .background(ElectricBlue.copy(alpha = 0.3f))
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Minus button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AdaptWhite.copy(alpha = 0.05f))
                            .border(1.2.dp, ElectricBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { viewModel.updateAbsenteeism(lecture.id, -1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp, 2.5.dp)
                                .background(AdaptWhite)
                        )
                    }
                    // Plus button
                    Box(
                         modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AdaptWhite.copy(alpha = 0.05f))
                            .border(1.2.dp, ElectricBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { viewModel.updateAbsenteeism(lecture.id, 1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Arttır",
                            tint = AdaptWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = AdaptDividerColor)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Detayları Kapat" else "Detaylar & Notlar",
                    fontSize = 11.sp,
                    color = ElectricBlue,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Detay",
                    tint = ElectricBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .background(AdaptWhite.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                        .border(1.dp, AdaptWhite.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "NOT HESAPLAYICI",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val criteria = remember(lecture) { lecture.getCriteriaList() }
                    val gradeInputs = remember(lecture) {
                        val map = mutableStateMapOf<Int, String>()
                        criteria.forEachIndexed { index, crit ->
                            map[index] = crit.grade?.toString() ?: ""
                        }
                        map
                    }

                    criteria.forEachIndexed { index, criterion ->
                        Column(modifier = Modifier.padding(bottom = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = criterion.name,
                                    fontSize = 11.sp,
                                    color = AdaptLightGray,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Ağırlık: %${criterion.weight.toInt()}",
                                    fontSize = 10.sp,
                                    color = AdaptGray
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = gradeInputs[index] ?: "",
                                onValueChange = { gradeInputs[index] = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = AdaptWhite,
                                    unfocusedTextColor = AdaptWhite,
                                    focusedBorderColor = ElectricBlue,
                                    unfocusedBorderColor = AdaptWhite.copy(alpha = 0.1f),
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                textStyle = LocalTextStyle.current.copy(color = AdaptWhite, fontSize = 13.sp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                placeholder = { Text("-", color = Color.Gray, fontSize = 13.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = WarningRed),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dersi Sil", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val updatedCriteria = criteria.mapIndexed { idx, crit ->
                                    val gradeText = gradeInputs[idx] ?: ""
                                    crit.copy(grade = gradeText.toDoubleOrNull())
                                }

                                if (lecture.customCriteriaJson.isNullOrBlank()) {
                                    var midG: Double? = null
                                    var finG: Double? = null
                                    var hwG: Double? = null
                                    var prG: Double? = null

                                    updatedCriteria.forEach { crit ->
                                        when (crit.name) {
                                            "Vize" -> midG = crit.grade
                                            "Final" -> finG = crit.grade
                                            "Ödev" -> hwG = crit.grade
                                            "Proje" -> prG = crit.grade
                                        }
                                    }

                                    viewModel.updateLecture(
                                        lecture.copy(
                                            midtermGrade = midG,
                                            finalGrade = finG,
                                            homeworkGrade = hwG,
                                            projectGrade = prG
                                        )
                                    )
                                } else {
                                    val serialized = updatedCriteria.joinToString(",") { "${it.name}:${it.weight}:${it.grade ?: ""}" }
                                    viewModel.updateLecture(
                                        lecture.copy(customCriteriaJson = serialized)
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Notları Güncelle", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = RoundedCornerShape(28.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Dikkat",
                        tint = WarningRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Dersi Sil",
                        color = AdaptWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Text(
                     text = "\"${lecture.name}\" dersini silmek istediğinize emin misiniz? Bu derse ait tüm notlar ve veriler kalıcı olarak silinecektir.",
                     color = AdaptLightGray,
                     fontSize = 13.sp,
                     lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLecture(lecture)
                        showDeleteConfirm = false
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarningRed),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Sil",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "İptal",
                        color = ElectricBlue,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        )
    }
}

@Composable
fun AddLectureDialog(
    viewModel: AcademicViewModel,
    onDismiss: () -> Unit,
    onAdd: (String, Int, Int, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var creditStr by remember { mutableStateOf("3") }
    var maxAbsStr by remember { mutableStateOf("12") }

    var nameError by remember { mutableStateOf(false) }
    var creditError by remember { mutableStateOf(false) }
    var maxAbsError by remember { mutableStateOf(false) }

    val isDark = isDarkThemeActive()
    val dialogBg = if (isDark) DeepSurfaceDark else Color.White
    val borderColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFE5E7EB)
    val textLabelColor = if (isDark) Color.Gray else Color(0xFF4B5563)

    val activeWeightType by viewModel.weightType.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = dialogBg,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, borderColor, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Yeni Ders Ekle",
                    fontSize = 20.sp,
                    color = AdaptWhite,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it 
                        nameError = false
                    },
                    label = { Text("Ders Adı") },
                    isError = nameError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AdaptWhite,
                        unfocusedTextColor = AdaptWhite,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = borderColor,
                        unfocusedLabelColor = textLabelColor,
                        focusedLabelColor = ElectricBlue,
                        errorBorderColor = WarningRed,
                        errorLabelColor = WarningRed
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                val dynamicLabel = if (activeWeightType == "AKTS") "AKTS Değeri" else "Kredi Değeri"
                OutlinedTextField(
                    value = creditStr,
                    onValueChange = { 
                        creditStr = it 
                        creditError = false
                    },
                    label = { Text(dynamicLabel) },
                    isError = creditError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AdaptWhite,
                        unfocusedTextColor = AdaptWhite,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = borderColor,
                        unfocusedLabelColor = textLabelColor,
                        focusedLabelColor = ElectricBlue,
                        errorBorderColor = WarningRed,
                        errorLabelColor = WarningRed
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = maxAbsStr,
                    onValueChange = { 
                        maxAbsStr = it 
                        maxAbsError = false
                    },
                    label = { Text("Maksimum Devamsızlık Hakkı") },
                    isError = maxAbsError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AdaptWhite,
                        unfocusedTextColor = AdaptWhite,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = borderColor,
                        unfocusedLabelColor = textLabelColor,
                        focusedLabelColor = ElectricBlue,
                        errorBorderColor = WarningRed,
                        errorLabelColor = WarningRed
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("İptal", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val isNameValid = name.isNotBlank()
                            val creditVal = creditStr.toIntOrNull()
                            val isCreditValid = creditVal != null && creditVal > 0
                            val maxAbsVal = maxAbsStr.toIntOrNull()
                            val isMaxAbsValid = maxAbsVal != null && maxAbsVal >= 0

                            nameError = !isNameValid
                            creditError = !isCreditValid
                            maxAbsError = !isMaxAbsValid

                            if (isNameValid && isCreditValid && isMaxAbsValid) {
                                onAdd(
                                    name,
                                    creditVal!!,
                                    maxAbsVal!!,
                                    null
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text("Ekle", color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun FocusScreen(viewModel: AcademicViewModel, lectures: List<Lecture>, sessions: List<com.ganot.app.data.FocusSession>) {
    val isRunning by viewModel.isTimerRunning.collectAsState()
    val timerMode by viewModel.timerMode.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val elapsedMs by viewModel.elapsedMs.collectAsState()
    val selectedLectureId by viewModel.selectedLectureForTimer.collectAsState()

    val currentLecture = lectures.find { it.id == selectedLectureId }

    val recentSessions = remember(sessions) {
        val oneWeekAgo = Clock.System.now().toEpochMilliseconds() - (7 * 24 * 60 * 60 * 1000L)
        sessions.filter { it.timestamp >= oneWeekAgo }
    }

    val stopIcon = remember {
        ImageVector.Builder(
            name = "StopSquare",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                stroke = null,
                strokeLineWidth = 1.0f
            ) {
                moveTo(6.0f, 6.0f)
                lineTo(18.0f, 6.0f)
                lineTo(18.0f, 18.0f)
                lineTo(6.0f, 18.0f)
                close()
            }
        }.build()
    }

    val currentDisplaySeconds = if (timerMode == "POMODORO") {
        (25 * 60) - elapsedSeconds
    } else {
        elapsedSeconds
    }
    
    val timeString = if (timerMode == "SERBEST") {
        val totalMs = elapsedMs
        val hours = (totalMs / 3600000).coerceAtLeast(0)
        val mins = ((totalMs % 3600000) / 60000).coerceAtLeast(0)
        val secs = ((totalMs % 60000) / 1000).coerceAtLeast(0)
        val centis = ((totalMs % 1000) / 10).coerceAtLeast(0)
        "${hours.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}.${centis.toString().padStart(2, '0')}"
    } else {
        val minutes = (currentDisplaySeconds / 60).coerceAtLeast(0)
        val seconds = (currentDisplaySeconds % 60).coerceAtLeast(0)
        "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isRunning) 1.05f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val progressRatio = if (timerMode == "POMODORO") {
        elapsedSeconds.toFloat() / (25 * 60).toFloat()
    } else {
        (elapsedSeconds % 60).toFloat() / 60.toFloat()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // FIXED TOP BAR
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Odaklanma İstasyonu",
                    fontSize = 22.sp,
                    color = AdaptWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "Pomodoro metodu veya serbest çalışma ile verimliliği arttır",
                    fontSize = 11.5.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)
        ) {

        item {
            val isDark = isDarkThemeActive()
            val toggleContainerBg = if (isDark) DeepSurfaceDark else Color(0xFFE5E7EB)
            val toggleInactiveColor = if (isDark) Color.Gray else Color(0xFF4B5563)

            Row(
                modifier = Modifier
                    .background(toggleContainerBg, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            if (timerMode == "POMODORO") ElectricBlue else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.setTimerMode("POMODORO") }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Pomodoro Modu",
                        color = if (timerMode == "POMODORO") Color.White else toggleInactiveColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            if (timerMode == "SERBEST") ElectricBlue else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.setTimerMode("SERBEST") }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Serbest",
                        color = if (timerMode == "SERBEST") Color.White else toggleInactiveColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            var showDropD by remember { mutableStateOf(false) }
            val isDark = isDarkThemeActive()
            val selectorBg = if (isDark) DeepCardDark else Color.White
            val selectorBorderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE5E7EB)

            Box {
                Button(
                    onClick = { showDropD = true },
                    colors = ButtonDefaults.buttonColors(containerColor = selectorBg)
                ) {
                    Text(
                        text = "Hedef Çalışma Dersi: ${currentLecture?.name ?: "Seçilmedi"}",
                        color = AdaptWhite,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Ders Seç", tint = AdaptWhite)
                }

                DropdownMenu(
                    expanded = showDropD,
                    onDismissRequest = { showDropD = false },
                    modifier = Modifier.background(if (isDark) DeepSurfaceDark else Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("Genel Çalışma", color = AdaptWhite) },
                        onClick = {
                            viewModel.selectLectureForTimer(null)
                            showDropD = false
                        }
                    )
                    lectures.forEach { lecture ->
                        DropdownMenuItem(
                            text = { Text(lecture.name, color = AdaptWhite) },
                            onClick = {
                                viewModel.selectLectureForTimer(lecture.id)
                                showDropD = false
                            }
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .scale(pulseScale),
                contentAlignment = Alignment.Center
            ) {
                if (timerMode == "POMODORO") {
                    val canvasColor = NeonPurple
                    val isDarkTheme = isDarkThemeActive()
                    Canvas(modifier = Modifier.size(200.dp)) {
                        drawArc(
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                        )
                        
                        drawArc(
                            color = canvasColor,
                            startAngle = -90f,
                            sweepAngle = progressRatio * 360f,
                            useCenter = false,
                            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeString,
                        fontSize = if (timerMode == "SERBEST") 30.sp else 38.sp,
                        color = AdaptWhite,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isRunning) "ODAKLANILIYOR" else "BEKLEMEDE",
                        fontSize = 11.sp,
                        color = if (isRunning) MintGreen else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        item {
            val isDark = isDarkThemeActive()
            val controlBtnBg = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f)
            val controlBtnBorder = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)
            val controlBtnTint = if (isDark) Color.LightGray else Color(0xFF4B5563)

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                IconButton(
                    onClick = { viewModel.resetTimer() },
                    modifier = Modifier
                        .size(54.dp)
                        .background(controlBtnBg, CircleShape)
                        .border(1.dp, controlBtnBorder, CircleShape)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Sıfırla", tint = controlBtnTint)
                }

                IconButton(
                    onClick = {
                        if (isRunning) viewModel.pauseTimer() else viewModel.startTimer()
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .background(
                            if (isRunning) WarningRed 
                            else if (isDarkThemeActive()) MintGreen 
                            else ElectricBlue, 
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isRunning) stopIcon else Icons.Default.PlayArrow,
                        contentDescription = "Başlat/Durdur",
                        tint = Color.Black
                    )
                }

                IconButton(
                    onClick = { viewModel.completeTimerAndSave() },
                    enabled = elapsedSeconds > 0,
                    modifier = Modifier
                        .size(54.dp)
                        .background(ElectricBlue, CircleShape)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Kaydet", tint = Color.White)
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Geçmiş",
                        tint = ElectricBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "SON SEANSLAR (SON 1 HAFTA)",
                        fontSize = 11.5.sp,
                        color = ElectricBlue,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        if (recentSessions.isEmpty()) {
            item {
                GlassCard {
                    Text(
                        text = "Son 1 hafta içinde tamamlanan çalışma seansı bulunmuyor. Hadi başlayalım!",
                        color = AdaptLightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    )
                }
            }
        } else {
            items(recentSessions.take(4)) { session ->
                val course = lectures.find { it.id == session.lectureId }
                GlassCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(NeonPurple.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Star, contentDescription = "Kupa", tint = NeonPurple, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = course?.name ?: "Genel Çalışma",
                                    fontSize = 14.sp,
                                    color = AdaptWhite,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = getPlatformHelper().formatDate(session.timestamp, "dd MMMM yyyy - HH:mm"),
                                    fontSize = 11.sp,
                                    color = AdaptGray
                                )
                            }
                        }
                        Text(
                            text = "${session.durationMs / 60000} Dk",
                            fontSize = 14.sp,
                            color = MintGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun CalendarScreen(viewModel: AcademicViewModel, lectures: List<Lecture>, events: List<CalendarEvent>) {
    var showDialog by remember { mutableStateOf(false) }
    
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    var selectedDay by remember { mutableStateOf(now.date) }
    var displayedMonth by remember { mutableStateOf(LocalDate(now.year, now.monthNumber, 1)) }
    
    var eventToDelete by remember { mutableStateOf<CalendarEvent?>(null) }

    val calendarCells = remember(displayedMonth) {
        val list = mutableListOf<LocalDate?>()
        val firstDayOfMonth = displayedMonth
        
        // Monday is 1, Sunday is 7 in ISO
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.isoDayNumber
        val paddingDays = firstDayOfWeek - 1 // 0 for Monday, 6 for Sunday

        val daysInMonth = when (firstDayOfMonth.month) {
            Month.JANUARY, Month.MARCH, Month.MAY, Month.JULY, Month.AUGUST, Month.OCTOBER, Month.DECEMBER -> 31
            Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
            Month.FEBRUARY -> if ((firstDayOfMonth.year % 4 == 0 && firstDayOfMonth.year % 100 != 0) || (firstDayOfMonth.year % 400 == 0)) 29 else 28
            else -> 31
        }

        for (i in 0 until paddingDays) {
            list.add(null)
        }
        for (day in 1..daysInMonth) {
            list.add(LocalDate(firstDayOfMonth.year, firstDayOfMonth.monthNumber, day))
        }
        while (list.size % 7 != 0) {
            list.add(null)
        }
        list
    }

    val weeks = remember(calendarCells) {
        calendarCells.chunked(7)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // FIXED TOP BAR
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Takvim",
                            fontSize = 22.sp,
                            color = AdaptWhite,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "Sınav programı ve önemli ödev teslimleri",
                            fontSize = 11.5.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    val isDark = isDarkThemeActive()
                    val addBtnBg = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f)
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricBlue)
                            .clickable { showDialog = true }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Sınav Ekle",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Sınav Ekle",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            val isDarkTheme = isDarkThemeActive()
            val calBg = if (isDarkTheme) DeepCardDark else Color.White
            val cellTodayBg = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)
            ) {

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(calBg, RoundedCornerShape(20.dp))
                        .run {
                            if (!isDarkTheme) this.border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(20.dp)) else this
                        }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val monthName = when(displayedMonth.month) {
                            Month.JANUARY -> "Ocak"
                            Month.FEBRUARY -> "Şubat"
                            Month.MARCH -> "Mart"
                            Month.APRIL -> "Nisan"
                            Month.MAY -> "Mayıs"
                            Month.JUNE -> "Haziran"
                            Month.JULY -> "Temmuz"
                            Month.AUGUST -> "Ağustos"
                            Month.SEPTEMBER -> "Eylül"
                            Month.OCTOBER -> "Ekim"
                            Month.NOVEMBER -> "Kasım"
                            Month.DECEMBER -> "Aralık"
                            else -> ""
                        }
                        Text(
                            text = "$monthName ${displayedMonth.year}".uppercase(),
                            fontSize = 16.sp,
                            color = AdaptWhite,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val current = displayedMonth
                                    displayedMonth = if (current.monthNumber == 1) LocalDate(current.year - 1, 12, 1) else LocalDate(current.year, current.monthNumber - 1, 1)
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Önceki Ay", tint = AdaptWhite)
                            }
                            IconButton(
                                onClick = {
                                    val current = displayedMonth
                                    displayedMonth = if (current.monthNumber == 12) LocalDate(current.year + 1, 1, 1) else LocalDate(current.year, current.monthNumber + 1, 1)
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Sonraki Ay", tint = AdaptWhite)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz").forEach { dayName ->
                            Text(
                                text = dayName,
                                fontSize = 11.sp,
                                color = AdaptGray,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

                    weeks.forEach { week ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            week.forEach { dayDate ->
                                if (dayDate == null) {
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                } else {
                                    val isToday = dayDate == today
                                    val isSelected = dayDate == selectedDay
                                    val dayNo = dayDate.dayOfMonth

                                    val hasEvents = events.any {
                                        val eDate = Instant.fromEpochMilliseconds(it.dueDate).toLocalDateTime(TimeZone.currentSystemDefault()).date
                                        eDate == dayDate
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) ElectricBlue else if (isToday) cellTodayBg else Color.Transparent
                                            )
                                            .clickable {
                                                selectedDay = dayDate
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = dayNo.toString(),
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.White else if (isToday) ElectricBlue else AdaptWhite
                                            )
                                            if (hasEvents) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .background(if (isSelected) Color.Black else ElectricBlue, CircleShape)
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

            val filteredEvents = events.filter {
                val eDate = Instant.fromEpochMilliseconds(it.dueDate).toLocalDateTime(TimeZone.currentSystemDefault()).date
                eDate == selectedDay
            }

            item {
                Text(
                    text = "SEÇİLEN GÜNÜN PROGRAMI",
                    fontSize = 13.sp,
                    color = ElectricBlue,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            if (filteredEvents.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.padding(vertical = 12.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Boş Gün",
                                tint = ElectricBlue.copy(alpha = 0.7f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Planlanmış Etkinlik Yok",
                                color = AdaptWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Seçilen güne kayıtlı herhangi bir sınav veya ödev bulunmuyor.",
                                color = AdaptLightGray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredEvents) { event ->
                    val lecture = lectures.find { it.id == event.lectureId }
                    
                    GlassCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val priorityColor = when (event.type) {
                                    "SINAV" -> WarningRed
                                    "ÖDEV" -> ElectricBlue
                                    else -> NeonPurple
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(priorityColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = event.type,
                                            fontSize = 9.sp,
                                            color = priorityColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = lecture?.name ?: "Genel",
                                        fontSize = 12.sp,
                                        color = AdaptGray
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = event.title,
                                    fontSize = 16.sp,
                                    color = AdaptWhite,
                                    fontWeight = FontWeight.Bold
                                )
                                if (event.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = event.description,
                                        fontSize = 12.sp,
                                        color = AdaptLightGray
                                    )
                                }
                                if (event.classroom.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Sınıf / Salon: ${event.classroom}",
                                        fontSize = 12.sp,
                                        color = AdaptLightGray,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Saat: " + getPlatformHelper().formatDate(event.dueDate, "HH:mm"),
                                    fontSize = 12.sp,
                                    color = ElectricBlue
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.toggleEventCompleted(event) }) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .border(
                                                width = 1.5.dp,
                                                color = if (event.isCompleted) MintGreen else Color.Gray.copy(alpha = 0.6f),
                                                shape = CircleShape
                                            )
                                            .background(
                                                color = if (event.isCompleted) MintGreen.copy(alpha = 0.2f) else Color.Transparent,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (event.isCompleted) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Tamamlandı",
                                                tint = MintGreen,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                                IconButton(onClick = { eventToDelete = event }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Sil",
                                        tint = WarningRed.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        if (showDialog) {
            AddCalendarEventDialog(
                lectures = lectures,
                onDismiss = { showDialog = false },
                onAdd = { title, type, dueDate, desc, classroom, lectureId ->
                    viewModel.addEvent(
                        CalendarEvent(
                            lectureId = lectureId,
                            title = title,
                            type = type,
                            dueDate = dueDate,
                            description = desc,
                            classroom = classroom
                        )
                    )
                    showDialog = false
                }
            )
        }
    }

    if (eventToDelete != null) {
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            shape = RoundedCornerShape(28.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Dikkat",
                        tint = WarningRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Etkinliği Sil",
                        color = AdaptWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Text(
                     text = "\"${eventToDelete!!.title}\" etkinliğini/sınavını silmek istediğinize emin misiniz? Bu işlem geri alınamaz.",
                     color = AdaptLightGray,
                     fontSize = 13.sp,
                     lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        eventToDelete?.let { viewModel.deleteEvent(it) }
                        eventToDelete = null
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarningRed),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Sil",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { eventToDelete = null },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "İptal",
                        color = ElectricBlue,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        )
    }
}

@Composable
fun AddCalendarEventDialog(
    lectures: List<Lecture>,
    onDismiss: () -> Unit,
    onAdd: (String, String, Long, String, String, Int?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("SINAV") }
    var desc by remember { mutableStateOf("") }
    var selectedLectureId by remember { mutableStateOf<Int?>(null) }
    var classroom by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    
    var selectedYear by remember { mutableStateOf(now.year) }
    var selectedMonth by remember { mutableStateOf(now.monthNumber) }
    var selectedDay by remember { mutableStateOf(now.dayOfMonth) }
    var selectedHour by remember { mutableStateOf(now.hour) }
    var selectedMinute by remember { mutableStateOf(now.minute) }

    val isDark = isDarkThemeActive()
    val dialogBg = if (isDark) DeepSurfaceDark else Color.White
    val borderColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFE5E7EB)
    val cardBtnBg = if (isDark) DeepCardDark else Color(0xFFF3F4F6)
    val textLabelColor = if (isDark) Color.Gray else Color(0xFF4B5563)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = dialogBg,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, borderColor, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Yeni Etkinlik / Sınav Planla",
                    color = AdaptWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { 
                        Text(
                            text = "Başlık",
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AdaptWhite,
                        unfocusedTextColor = AdaptWhite,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = borderColor,
                        unfocusedLabelColor = textLabelColor,
                        focusedLabelColor = ElectricBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Türü:", color = AdaptLightGray, fontSize = 13.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("SINAV", "ÖDEV").forEach { t ->
                            val isSel = type == t
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSel) ElectricBlue else cardBtnBg,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { type = t }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(t, color = if (isSel) Color.Black else AdaptWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                var expandedLectures by remember { mutableStateOf(false) }
                val targetLecture = lectures.find { it.id == selectedLectureId }
                
                Box {
                    Button(
                        onClick = { expandedLectures = true },
                        colors = ButtonDefaults.buttonColors(containerColor = cardBtnBg, contentColor = AdaptWhite),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Ders Seç: ${targetLecture?.name ?: "Genel Etkinlik"}", color = AdaptWhite)
                    }
                    DropdownMenu(
                        expanded = expandedLectures,
                        onDismissRequest = { expandedLectures = false },
                        modifier = Modifier.background(dialogBg)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Genel Etkinlik", color = AdaptWhite) },
                            onClick = {
                                        selectedLectureId = null
                                        expandedLectures = false
                                    }
                        )
                        lectures.forEach { l ->
                            DropdownMenuItem(
                                text = { Text(l.name, color = AdaptWhite) },
                                onClick = {
                                            selectedLectureId = l.id
                                            expandedLectures = false
                                        }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var showCustomDatePicker by remember { mutableStateOf(false) }
                    var showCustomTimePicker by remember { mutableStateOf(false) }

                    if (showCustomDatePicker) {
                        CustomDatePicker(
                            initialYear = selectedYear,
                            initialMonth = selectedMonth,
                            initialDay = selectedDay,
                            onDismiss = { showCustomDatePicker = false },
                            onConfirm = { y, m, d ->
                                selectedYear = y
                                selectedMonth = m
                                selectedDay = d
                                showCustomDatePicker = false
                            }
                        )
                    }

                    if (showCustomTimePicker) {
                        CustomTimePicker(
                            initialHour = selectedHour,
                            initialMinute = selectedMinute,
                            onDismiss = { showCustomTimePicker = false },
                            onConfirm = { h, mi ->
                                selectedHour = h
                                selectedMinute = mi
                                showCustomTimePicker = false
                            }
                        )
                    }

                    Button(
                        onClick = { showCustomDatePicker = true },
                        colors = ButtonDefaults.buttonColors(containerColor = cardBtnBg),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Tarih Seç", fontSize = 10.sp, color = AdaptGray)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${selectedDay.toString().padStart(2, '0')}.${selectedMonth.toString().padStart(2, '0')}.${selectedYear.toString().padStart(4, '0')}",
                                fontSize = 13.sp,
                                color = AdaptWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = { showCustomTimePicker = true },
                        colors = ButtonDefaults.buttonColors(containerColor = cardBtnBg),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Saat Seç", fontSize = 10.sp, color = AdaptGray)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${selectedHour.toString().padStart(2, '0')}:${selectedMinute.toString().padStart(2, '0')}",
                                fontSize = 13.sp,
                                color = AdaptWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = classroom,
                    onValueChange = { classroom = it },
                    label = { 
                        Text(
                            text = "Sınav Sınıfı / Salonu",
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AdaptWhite,
                        unfocusedTextColor = AdaptWhite,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = borderColor,
                        unfocusedLabelColor = textLabelColor,
                        focusedLabelColor = ElectricBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("İptal", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val targetInstant = LocalDateTime(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute)
                                    .toInstant(TimeZone.currentSystemDefault())
                                onAdd(title, type, targetInstant.toEpochMilliseconds(), "", classroom, selectedLectureId)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text("Oluştur", color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: AcademicViewModel, lectures: List<Lecture>, sessions: List<com.ganot.app.data.FocusSession>) {
    val totalFocusMs = sessions.sumOf { it.durationMs }
    val totalFocusMinutes = totalFocusMs / 60000
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showDetailedSettingsDialog by remember { mutableStateOf(false) }

    val shareState by viewModel.shareState.collectAsState()
    val importState by viewModel.importState.collectAsState()

    var showShareDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importCodeInput by remember { mutableStateOf("") }

    // Remove Android-specific context and permission logic
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // FIXED TOP BAR
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sistem Ayarları",
                            fontSize = 22.sp,
                            color = AdaptWhite,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "Uygulama tercihleri ve kişisel istatistikler",
                            fontSize = 11.5.sp,
                            color = AdaptGray,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Daha Fazla Ayar",
                                tint = AdaptWhite
                            )
                        }
                        
                        val isDarkMenu = isDarkThemeActive()
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .background(if (isDarkMenu) DeepSurfaceDark else Color.White)
                                .border(1.dp, if (isDarkMenu) Color.White.copy(alpha = 0.12f) else Color(0xFFE5E7EB), RoundedCornerShape(20.dp))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = null,
                                            tint = if (isDarkMenu) ElectricBlue else NeonPurple,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Sınıf Ayarlarını Paylaş",
                                            color = if (isDarkMenu) Color.White else Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    viewModel.shareLectures()
                                    showShareDialog = true
                                },
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isDarkMenu) ElectricBlue.copy(alpha = 0.08f) else NeonPurple.copy(alpha = 0.08f))
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = ElectricBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Paylaşım Kodu Gir",
                                            color = if (isDarkMenu) Color.White else Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    importCodeInput = ""
                                    viewModel.resetImportState()
                                    showImportDialog = true
                                },
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(ElectricBlue.copy(alpha = 0.08f))
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = null,
                                            tint = NeonBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Detaylı Ayarlar",
                                            color = if (isDarkMenu) Color.White else Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    showDetailedSettingsDialog = true
                                },
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(NeonBlue.copy(alpha = 0.08f))
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = NeonPurple,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Öğreticiyi Tekrar İzle",
                                            color = if (isDarkMenu) Color.White else Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    viewModel.triggerTutorialReplay()
                                },
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(NeonPurple.copy(alpha = 0.08f))
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = WarningRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Tüm Verileri Temizle",
                                            color = WarningRed,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    showDeleteConfirmationDialog = true
                                },
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(WarningRed.copy(alpha = 0.08f))
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)
            ) {

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Bilanço",
                    tint = ElectricBlue,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "GENEL AKADEMİK BİLANÇO",
                    fontSize = 13.sp,
                    color = ElectricBlue,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            NeonCard(glowColor = NeonPurple) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Takip Edilen Ders Sayısı",
                            color = AdaptLightGray,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Text(
                            text = "${lectures.size} Ders",
                            color = AdaptWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                    HorizontalDivider(color = AdaptDividerColor)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Toplam Odaklanma Süresi",
                            color = AdaptLightGray,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Text(
                            text = "$totalFocusMinutes Dakika",
                            color = AdaptWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                    HorizontalDivider(color = AdaptDividerColor)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentGPA = viewModel.getCPAGradingTotal().first
                        Text(
                            text = "Not Ortalaması",
                            color = AdaptLightGray,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Text(
                            text = "${getPlatformHelper().formatDouble(currentGPA, 2)} / 4.00",
                            color = AdaptWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Kriterler",
                    tint = ElectricBlue,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "DERS DEĞERLENDİRME AYARLARI",
                    fontSize = 13.sp,
                    color = ElectricBlue,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (lectures.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "Henüz ders eklenmemiş. Kriterleri düzenlemek için önce akademi sayfasından ders ekleyin.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    )
                }
            }
        } else {
            items(lectures) { lecture ->
                var isExpanded by remember { mutableStateOf(false) }

                val criteriaList = remember(lecture) {
                    val list = mutableStateListOf<GradeCriterion>()
                    list.addAll(lecture.getCriteriaList())
                    list
                }

                GlassCard(modifier = Modifier.padding(vertical = 4.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = lecture.name,
                                    color = AdaptWhite,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val summary = criteriaList.joinToString(", ") { "${it.name}: %${it.weight.toInt()}" }
                                Text(
                                    text = if (summary.isNotBlank()) summary else "Kriter tanımlanmamış",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                            IconButton(
                                onClick = { isExpanded = !isExpanded },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Genişlet/Daralt",
                                    tint = ElectricBlue
                                )
                            }
                        }

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = AdaptDividerColor)
                            Spacer(modifier = Modifier.height(12.dp))

                            var errorMessage by remember { mutableStateOf<String?>(null) }
                            var maxAbsenteeismText by remember(lecture.maxAbsenteeism) { mutableStateOf(lecture.maxAbsenteeism.toString()) }

                            Text(
                                text = "DEVAMSIZLIK AYARLARI",
                                fontSize = 11.sp,
                                color = ElectricBlue,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            OutlinedTextField(
                                value = maxAbsenteeismText,
                                onValueChange = { maxAbsenteeismText = it },
                                label = { Text("Maksimum Devamsızlık Hakkı") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = AdaptWhite,
                                    unfocusedTextColor = AdaptWhite,
                                    focusedBorderColor = NeonPurple,
                                    unfocusedBorderColor = AdaptWhite.copy(alpha = 0.12f),
                                    focusedLabelColor = if (isDarkThemeActive()) ElectricBlue else MintGreen,
                                    unfocusedLabelColor = Color.Gray
                                ),
                                textStyle = LocalTextStyle.current.copy(color = AdaptWhite, fontSize = 13.sp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "NOT KRİTERLERİ VE AĞIRLIKLARI",
                                fontSize = 11.sp,
                                color = ElectricBlue,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            val isDarkField = isDarkThemeActive()
                            val fieldBorderColor = if (isDarkField) Color.White.copy(alpha = 0.1f) else Color(0xFFE5E7EB)

                            criteriaList.forEachIndexed { index, criterion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = criterion.name,
                                        onValueChange = { newName ->
                                            criteriaList[index] = criterion.copy(name = newName)
                                        },
                                        placeholder = { Text("Kriter adı", color = Color.Gray) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = AdaptWhite,
                                            unfocusedTextColor = AdaptWhite,
                                            focusedBorderColor = if (isDarkThemeActive()) ElectricBlue else MintGreen,
                                            unfocusedBorderColor = fieldBorderColor
                                        ),
                                        textStyle = LocalTextStyle.current.copy(color = AdaptWhite, fontSize = 12.sp),
                                        modifier = Modifier.weight(2f),
                                        singleLine = true
                                    )

                                    var weightText by remember(criterion.weight) { mutableStateOf(criterion.weight.toInt().toString()) }
                                    OutlinedTextField(
                                        value = weightText,
                                        onValueChange = { newWeight ->
                                            weightText = newWeight
                                            newWeight.toDoubleOrNull()?.let {
                                                criteriaList[index] = criterion.copy(weight = it)
                                            }
                                        },
                                        placeholder = { Text("Ağırlık", color = Color.Gray) },
                                        trailingIcon = { Text("%", color = AdaptWhite.copy(alpha = 0.5f), fontSize = 12.sp) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = AdaptWhite,
                                            unfocusedTextColor = AdaptWhite,
                                            focusedBorderColor = if (isDarkThemeActive()) ElectricBlue else MintGreen,
                                            unfocusedBorderColor = fieldBorderColor
                                        ),
                                        textStyle = LocalTextStyle.current.copy(color = AdaptWhite, fontSize = 12.sp),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(WarningRed, CircleShape)
                                            .clickable {
                                                criteriaList.removeAt(index)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Sil",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val totalWeight = criteriaList.sumOf { it.weight }
                            if (totalWeight != 100.0) {
                                Text(
                                    text = "Değerlendirme ağırlıkları toplamı %100 olmalıdır! Şu anki toplam: %${totalWeight.toInt()}",
                                    color = WarningRed,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else if (errorMessage != null) {
                                Text(
                                    text = errorMessage ?: "",
                                    color = WarningRed,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        criteriaList.add(GradeCriterion("Yeni Kriter", 10.0, null))
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDarkThemeActive()) Color.White else Color(0xFF1A1A1A)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Ekle",
                                        tint = if (isDarkThemeActive()) Color.Black else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Kriter Ekle",
                                        color = if (isDarkThemeActive()) Color.Black else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (totalWeight != 100.0) {
                                            errorMessage = "Ağırlıkları %100 olacak şekilde güncelleyin."
                                        } else {
                                            errorMessage = null
                                            val serialized = criteriaList.joinToString(",") { "${it.name}:${it.weight}:${it.grade ?: ""}" }
                                            viewModel.updateLecture(
                                                lecture.copy(
                                                    customCriteriaJson = serialized,
                                                    maxAbsenteeism = maxAbsenteeismText.toIntOrNull() ?: lecture.maxAbsenteeism
                                                )
                                            )
                                            isExpanded = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "Ayarları Kaydet",
                                        color = if (isDarkThemeActive()) Color.Black else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Sistem",
                    tint = ElectricBlue,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "KİŞİSELLEŞTİRME & SİSTEM",
                    fontSize = 13.sp,
                    color = ElectricBlue,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        item {
            val nameState by viewModel.userName.collectAsState()
            var editName by remember(nameState) { mutableStateOf(nameState) }
            var nameUpdatedMessage by remember { mutableStateOf(false) }

            LaunchedEffect(nameUpdatedMessage) {
                if (nameUpdatedMessage) {
                    delay(2500)
                    nameUpdatedMessage = false
                }
            }

            GlassCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Kullanıcı Profili",
                        color = AdaptWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { 
                                editName = it
                                nameUpdatedMessage = false
                            },
                            label = { Text("Kullanıcı İsmi", color = Color.Gray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = AdaptWhite,
                                unfocusedTextColor = AdaptWhite,
                                focusedBorderColor = NeonPurple,
                                unfocusedBorderColor = AdaptWhite.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                if (editName.isNotBlank()) {
                                    viewModel.saveUserName(editName.trim())
                                    nameUpdatedMessage = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier.align(Alignment.CenterVertically).padding(top = 4.dp)
                        ) {
                            Text("Güncelle", color = if (isDarkThemeActive()) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    if (nameUpdatedMessage) {
                        Text(
                            text = "✓ İsim güncellendi!",
                            color = ElectricBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }

                    HorizontalDivider(color = AdaptDividerColor, thickness = 1.dp)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Widget Ekle",
                                color = AdaptWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Kritik devamsızlığını takip et.",
                                color = AdaptLightGray,
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = {
                                getPlatformHelper().requestWidgetPin()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("add_widget_button")
                        ) {
                            Text(
                                text = "Widget Ekle",
                                color = if (isDarkThemeActive()) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    HorizontalDivider(color = AdaptDividerColor, thickness = 1.dp)

                    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Koyu Tema",
                                color = AdaptWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { isChecked ->
                                viewModel.updateDarkTheme(isChecked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = if (isDarkThemeActive()) Color.Black else Color.White,
                                checkedTrackColor = ElectricBlue,
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = if (isDarkThemeActive()) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }

        item {
            val reminderDaysState = viewModel.reminderDays.collectAsState().value
            
            var daysInput by remember(reminderDaysState) { 
                mutableStateOf(if (reminderDaysState >= 0) reminderDaysState.toString() else "3") 
            }
            val isNotificationsEnabled = reminderDaysState >= 0
            val isValidInput = daysInput.trim().toIntOrNull()?.let { it >= 0 } ?: false

            var showSavedMessage by remember { mutableStateOf(false) }
            LaunchedEffect(showSavedMessage) {
                if (showSavedMessage) {
                    delay(2500)
                    showSavedMessage = false
                }
            }

            GlassCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Bildirim",
                                tint = ElectricBlue,
                                modifier = Modifier.size(18.dp)
                             )
                            Text(
                                text = "Bildirim & Hatırlatıcı",
                                color = AdaptWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Switch(
                            checked = isNotificationsEnabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    val parsedDays = daysInput.trim().toIntOrNull() ?: 3
                                    viewModel.updateReminderDays(parsedDays)
                                    showSavedMessage = true
                                } else {
                                    viewModel.updateReminderDays(-1)
                                    showSavedMessage = false
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = if (isDarkThemeActive()) Color.Black else Color.White,
                                checkedTrackColor = ElectricBlue,
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = if (isDarkThemeActive()) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
                            )
                        )
                    }

                    Text(
                        text = "Yaklaşan sınavlar veya teslim tarihi gelen ödevlerinize seçtiğiniz gün kadar süre kala sistem tarafından otomatik hatırlatma bildirimleri gönderilir.",
                        color = AdaptLightGray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    if (isNotificationsEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = daysInput,
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                        daysInput = newValue
                                    }
                                    showSavedMessage = false
                                },
                                label = { Text("Kaç Gün Kala Hatırlatılsın?", color = Color.Gray, fontSize = 12.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = AdaptWhite,
                                    unfocusedTextColor = AdaptWhite,
                                    focusedBorderColor = ElectricBlue,
                                    unfocusedBorderColor = if (isDarkThemeActive()) Color.White.copy(alpha = 0.1f) else Color(0xFFE5E7EB)
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = {
                                    val parsedDays = daysInput.trim().toIntOrNull()
                                    if (parsedDays != null && parsedDays >= 0) {
                                        viewModel.updateReminderDays(parsedDays)
                                        showSavedMessage = true
                                    }
                                },
                                enabled = isValidInput,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricBlue,
                                    disabledContainerColor = Color.White.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = if (showSavedMessage) "✓ Kaydedildi" else "Kaydet",
                                    color = if (isValidInput) (if (isDarkThemeActive()) Color.Black else Color.White) else Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "✗ Hatırlatma bildirimleri devre dışı.",
                            color = Color.Gray,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    HorizontalDivider(color = AdaptDividerColor, thickness = 1.dp)

                    val isAbsenceEnabled by viewModel.isAbsenceNotificationEnabled.collectAsState()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Devamsızlık Uyarısı",
                                color = AdaptWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Kalan devamsızlık hakkınız 1 olunca bildirim gönder.",
                                color = AdaptLightGray,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                        
                        Switch(
                            checked = isAbsenceEnabled,
                            onCheckedChange = { isChecked ->
                                viewModel.updateAbsenceNotificationEnabled(isChecked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = if (isDarkThemeActive()) Color.Black else Color.White,
                                checkedTrackColor = ElectricBlue,
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = if (isDarkThemeActive()) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Hakkında",
                    tint = ElectricBlue,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "GERİ BİLDİRİM",
                    fontSize = 13.sp,
                    color = ElectricBlue,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        item {
            GlassCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "POWERED BY",
                            fontSize = 10.sp,
                            color = AdaptWhite.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Image(
                            painter = painterResource(
                                id = if (isDarkThemeActive()) com.ganot.app.R.drawable.ic_riestudio_transparent else com.ganot.app.R.drawable.rsbeyazseffaf
                            ),
                            contentDescription = "RieStudio transparent Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .height(80.dp)
                        )
                    }

                    HorizontalDivider(color = AdaptDividerColor)

                    Button(
                        onClick = {
                            getPlatformHelper().sendEmail(
                                "contact@riestudio.com.tr",
                                "Ganot v${BuildKonfig.VERSION_NAME} - Hata & Görüş Bildirimi",
                                ""
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkThemeActive()) Color.White else Color(0xFF1A1A1A)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "E-posta Gönder",
                            tint = if (isDarkThemeActive()) Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hata & Görüş Bildir",
                            color = if (isDarkThemeActive()) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ganot v${BuildKonfig.VERSION_NAME}",
                    fontSize = 12.sp,
                    color = AdaptGray,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
    }

    if (showDeleteConfirmationDialog) {
        val isDark = isDarkThemeActive()
        val dialogBg = if (isDark) Color(0xFF121212) else Color(0xFFFFFDF9)
        val borderColors = if (isDark) {
            listOf(
                WarningRed.copy(alpha = 0.4f),
                Color.Transparent,
                WarningRed.copy(alpha = 0.1f)
            )
        } else {
            listOf(
                WarningRed.copy(alpha = 0.8f),
                Color.Transparent,
                WarningRed.copy(alpha = 0.3f)
            )
        }
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Dikkat",
                        tint = WarningRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Verileri Temizle",
                        color = AdaptWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Text(
                     text = "Tüm yerel verilerinizi (dersler, sınavlar, çalışma süreleri ve profil bilgileriniz) kalıcı olarak silmek istediğinize emin misiniz? Bu işlem geri alınamaz.",
                     color = AdaptLightGray,
                     fontSize = 13.sp,
                     lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showDeleteConfirmationDialog = false
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarningRed),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Sil",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmationDialog = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "İptal",
                        color = ElectricBlue,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            },
            containerColor = dialogBg,
            modifier = Modifier.border(
                width = 1.dp,
                brush = Brush.linearGradient(colors = borderColors),
                shape = RoundedCornerShape(28.dp)
            ),
            tonalElevation = 12.dp
        )
    }

    if (showShareDialog) {
        val isDark = isDarkThemeActive()
        val dialogBg = if (isDark) Color(0xFF121212) else Color(0xFFFFFDF9)
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        val borderColors = if (isDark) {
            listOf(ElectricBlue.copy(alpha = 0.4f), Color.Transparent, ElectricBlue.copy(alpha = 0.1f))
        } else {
            listOf(NeonPurple.copy(alpha = 0.8f), Color.Transparent, NeonPurple.copy(alpha = 0.3f))
        }

        AlertDialog(
            onDismissRequest = {
                showShareDialog = false
                viewModel.resetShareState()
            },
            shape = RoundedCornerShape(28.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Paylaş",
                        tint = if (isDark) ElectricBlue else NeonPurple,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Sınıf Ayarlarını Paylaş",
                        color = AdaptWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (val state = shareState) {
                        is ShareState.Idle -> {
                            Text(
                                text = "Paylaşım kodu oluşturuluyor...",
                                color = AdaptLightGray,
                                fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        is ShareState.Loading -> {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(color = if (isDark) ElectricBlue else Color(0xFF8B5CF6), modifier = Modifier.size(36.dp))
                                Text(
                                    text = "Ders ayarları paylaşılıyor...",
                                    color = AdaptLightGray,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        is ShareState.Success -> {
                            Text(
                                text = "Ders adlarınız, kredi sayılarınız ve sınav yüzdeleriniz için şablon kodu hazırlandı! Arkadaşınızın bu ayarları kendi uygulamasına aktarması için aşağıdaki kodu verebilirsiniz:",
                                color = AdaptLightGray,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isDark) Color.Black.copy(alpha = 0.3f) else MintGreen.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .border(1.dp, if (isDark) ElectricBlue.copy(alpha = 0.5f) else MintGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = state.code,
                                    color = if (isDark) ElectricBlue else MintGreen,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 4.sp
                                )
                            }
                            
                            Button(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(state.code))
                                    getPlatformHelper().showToast("Kod panoya kopyalandı!")
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) ElectricBlue else MintGreen),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Kopyala",
                                    tint = if (isDark) Color.Black else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Kodu Kopyala",
                                    color = if (isDark) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        is ShareState.Error -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().background(WarningRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Hata",
                                    tint = WarningRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = state.message,
                                    color = WarningRed,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showShareDialog = false
                        viewModel.resetShareState()
                    }
                ) {
                    Text(
                        text = "Kapat",
                        color = ElectricBlue,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            },
            containerColor = dialogBg,
            modifier = Modifier.border(
                width = 1.dp,
                brush = Brush.linearGradient(colors = borderColors),
                shape = RoundedCornerShape(28.dp)
            ),
            tonalElevation = 12.dp
        )
    }

    if (showImportDialog) {
        val isDark = isDarkThemeActive()
        val dialogBg = if (isDark) Color(0xFF121212) else Color(0xFFFFFDF9)
        val borderColors = if (isDark) {
            listOf(ElectricBlue.copy(alpha = 0.4f), Color.Transparent, ElectricBlue.copy(alpha = 0.1f))
        } else {
            listOf(ElectricBlue.copy(alpha = 0.8f), Color.Transparent, ElectricBlue.copy(alpha = 0.3f))
        }

        AlertDialog(
            onDismissRequest = {
                if (importState !is ImportState.Loading) {
                    showImportDialog = false
                    viewModel.resetImportState()
                }
            },
            shape = RoundedCornerShape(28.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "İçe Aktar",
                        tint = ElectricBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Ders Ayarları İçe Aktar",
                        color = AdaptWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (val state = importState) {
                        is ImportState.Idle, is ImportState.Error -> {
                            Text(
                                text = "Arkadaşınızdan aldığınız şablon kodunu aşağıdaki alana girerek ders ayarlarını hızlıca aktarın:",
                                color = AdaptLightGray,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )

                            OutlinedTextField(
                                value = importCodeInput,
                                onValueChange = { inputCode ->
                                    importCodeInput = inputCode
                                },
                                label = { Text("Paylaşım Kodu", color = AdaptLightGray) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 2.sp,
                                    color = AdaptWhite
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = AdaptWhite,
                                    unfocusedTextColor = AdaptWhite,
                                    focusedBorderColor = if (isDark) ElectricBlue else MintGreen,
                                    unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE5E7EB),
                                    focusedLabelColor = if (isDark) ElectricBlue else MintGreen
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (state is ImportState.Error) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(WarningRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Hata",
                                        tint = WarningRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = state.message,
                                        color = WarningRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (importCodeInput.isNotBlank()) {
                                        viewModel.fetchLecturesForImport(importCodeInput)
                                    } else {
                                        getPlatformHelper().showToast("Lütfen geçerli bir kod girin.")
                                    }
                                },
                                enabled = importCodeInput.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) ElectricBlue else MintGreen,
                                    disabledContainerColor = Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Text(
                                    text = "Dersleri Aktar",
                                    color = if (importCodeInput.isNotBlank()) Color.Black else Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        is ImportState.Loading -> {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(color = ElectricBlue, modifier = Modifier.size(36.dp))
                                Text(
                                    text = "Paylaşılan şablon indiriliyor...",
                                    color = AdaptLightGray,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        is ImportState.Confirm -> {
                            Text(
                                text = "Bu işlem mevcut ders ayarlarınızı sıfırlayacaktır, onaylıyor musunuz?",
                                color = AdaptWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Onayladığınız takdirde mevcut dersleriniz silinecek, indirilen şablondaki ${state.fetchedLectures.size} ders ayarı sıfırdan oluşturulacaktır. Tüm notlar ve devamsızlık geçmişleri tamamen boş olarak başlayacaktır.",
                                color = AdaptLightGray,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TextButton(
                                    onClick = { viewModel.resetImportState() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "İptal",
                                        color = AdaptWhite,
                                        fontSize = 13.sp
                                    )
                                }
                                Button(
                                    onClick = {
                                        viewModel.applyImportedLectures(
                                            state.fetchedLectures,
                                            state.gradingSystemIndex,
                                            state.weightType,
                                            state.isBellCurve
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WarningRed),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    Text(
                                        text = "Sıfırla ve Aktar",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                        is ImportState.Success -> {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Başarılı",
                                    tint = if (isDark) ElectricBlue else MintGreen,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Ders ayarları başarıyla aktarıldı!",
                                    color = AdaptWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Text(
                                    text = "Yeni ders şablonunuz başarıyla kuruldu. Yerel hafızaya kaydedilen tüm notlar ve devamsızlık geçmişleri sıfır (boş) olarak başlatıldı.",
                                    color = AdaptLightGray,
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Button(
                                    onClick = {
                                        showImportDialog = false
                                        viewModel.resetImportState()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) ElectricBlue else MintGreen),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Kapat",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                if (importState !is ImportState.Confirm && importState !is ImportState.Success) {
                    TextButton(
                        onClick = {
                            showImportDialog = false
                            viewModel.resetImportState()
                        }
                    ) {
                        Text(
                            text = "Kapat",
                            color = ElectricBlue,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            },
            containerColor = dialogBg,
            modifier = Modifier.border(
                width = 1.dp,
                brush = Brush.linearGradient(colors = borderColors),
                shape = RoundedCornerShape(28.dp)
            ),
            tonalElevation = 12.dp
        )
    }

    if (showDetailedSettingsDialog) {
        DetailedSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showDetailedSettingsDialog = false }
        )
    }
}
}

@Composable
fun SplashScreen(onDismiss: () -> Unit) {
    var startAnimate by remember { mutableStateOf(false) }
    
    LaunchedEffect(key1 = true) {
        startAnimate = true
        delay(2200)
        onDismiss()
    }
    
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimate) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = LinearOutSlowInEasing
        ),
        label = "SplashAlpha"
    )
    
    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimate) 1f else 0.82f,
        animationSpec = tween(
            durationMillis = 1200,
            easing = FastOutSlowInEasing
        ),
        label = "SplashScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = com.ganot.app.R.drawable.ic_riestudio_logo),
            contentDescription = "RieStudio Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.40f)
                .alpha(alphaAnim.value)
                .scale(scaleAnim.value)
        )
    }
}

@Composable
fun DetailedSettingsDialog(
    viewModel: AcademicViewModel,
    onDismiss: () -> Unit
) {
    val isDark = isDarkThemeActive()
    val scope = rememberCoroutineScope()
    
    val currentGradingSystemIndex by viewModel.gradingSystemIndex.collectAsState()
    val currentWeightType by viewModel.weightType.collectAsState()
    val currentIsBellCurve by viewModel.isBellCurve.collectAsState()
    val currentReminderTime by viewModel.reminderTime.collectAsState()
    
    var tempGradingSystemIndex by remember { mutableStateOf(currentGradingSystemIndex) }
    var tempWeightType by remember { mutableStateOf(currentWeightType) }
    var tempIsBellCurve by remember { mutableStateOf(currentIsBellCurve) }
    var tempReminderTime by remember { mutableStateOf(currentReminderTime) }
    
    val activeAccentColor = if (isDark) NeonBlue else NeonPurple
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) DeepCardDark else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Detaylı Ayarlar",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color.Black,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column {
                            Text(
                                text = "Akademik başarı değerlendirmelerinde hangi harf notu sistemi kullanılıyor?",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color.LightGray else Color.DarkGray,
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val options = listOf(
                                "YÖK HARF SİSTEMİ (Standart YÖK)\n(AA-FF, 4.0 Skalalı)",
                                "11'li Harf Sistemi (AB'li sistem)\n(AA-FF, AB/BC Dahil)",
                                "Harfli Eksi-Artılı Sistem\n(A, A-, B+... İle)",
                                "Sayısal Kodlu Sistem\n(A1, A2, A3... İle)"
                            )
                            
                            options.forEachIndexed { optIndex, text ->
                                val isSelected = tempGradingSystemIndex == optIndex
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) activeAccentColor.copy(alpha = 0.12f)
                                            else Color.Transparent
                                        )
                                        .clickable { tempGradingSystemIndex = optIndex }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { tempGradingSystemIndex = optIndex },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = activeAccentColor,
                                            unselectedColor = if (isDark) Color.Gray else Color.Gray.copy(alpha = 0.6f)
                                        )
                                    )
                                    Text(
                                        text = text,
                                        fontSize = 12.sp,
                                        color = if (isDark) Color.White else Color.Black,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Column {
                            Text(
                                text = "Derslerin ağırlığı hesaplanırken hangi yöntem uygulanıyor?",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color.LightGray else Color.DarkGray,
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val weightOptions = listOf("AKTS", "KREDİ")
                            weightOptions.forEach { type ->
                                val isSelected = tempWeightType == type
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) activeAccentColor.copy(alpha = 0.12f)
                                            else Color.Transparent
                                        )
                                        .clickable { tempWeightType = type }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { tempWeightType = type },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = activeAccentColor,
                                            unselectedColor = if (isDark) Color.Gray else Color.Gray.copy(alpha = 0.6f)
                                        )
                                    )
                                    Text(
                                        text = type + (if (type == "AKTS") " (Avrupa Kredi Transfer Sistemi)" else " (Yerel Kredi Sistemi)"),
                                        fontSize = 10.sp,
                                        color = if (isDark) Color.White else Color.Black,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Sınıf genelinde bağıl değerlendirme sistemi (çan eğrisi) uygulanıyor mu?",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color.LightGray else Color.DarkGray,
                                    lineHeight = 17.sp,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )
                                Switch(
                                    checked = tempIsBellCurve,
                                    onCheckedChange = { tempIsBellCurve = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = activeAccentColor,
                                        checkedTrackColor = activeAccentColor.copy(alpha = 0.5f),
                                        uncheckedThumbColor = if (isDark) Color.Gray else Color.LightGray,
                                        uncheckedTrackColor = (if (isDark) Color.Gray else Color.LightGray).copy(alpha = 0.5f)
                                    )
                                )
                            }
                            
                            if (tempIsBellCurve) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(WarningRed.copy(alpha = 0.08f))
                                        .border(1.dp, WarningRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Uyarı",
                                        tint = WarningRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Çan eğrisi (bağıl değerlendirme) sistemi, sınıf ortalaması ve standart sapmaya göre dinamik olarak değiştiği için bu uygulamada %100 kesinlikle hesaplanamaz. En doğru harf notları ve güncel sonuçlar için lütfen üniversitenizin bilgi sistemine (öğrenci otomasyonu) başvurun.",
                                        fontSize = 11.sp,
                                        color = if (isDark) Color.White.copy(alpha = 0.9f) else Color.DarkGray,
                                        lineHeight = 15.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Column {
                            Text(
                                text = "Bildirim Gönderim Zamanı Ayarla",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color.LightGray else Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            val timeParts = tempReminderTime.split(":")
                            val currentHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 20
                            val currentMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
                            
                            var showCustomTimePicker by remember { mutableStateOf(false) }
                            if (showCustomTimePicker) {
                                CustomTimePicker(
                                    initialHour = currentHour,
                                    initialMinute = currentMinute,
                                    onDismiss = { showCustomTimePicker = false },
                                    onConfirm = { h, m ->
                                        tempReminderTime = "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
                                        showCustomTimePicker = false
                                    }
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDark) Color.White.copy(alpha = 0.04f) else Color(0xFFF9FAFB))
                                    .border(
                                        1.dp,
                                        if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE5E7EB),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { showCustomTimePicker = true }
                                    .padding(vertical = 10.dp, horizontal = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = "Hatırlatma Saati",
                                            tint = activeAccentColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Seçilen Saat: $tempReminderTime",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isDark) Color.White else Color.Black
                                            )
                                            Text(
                                                text = "Sınav hatırlatıcı günü seçilen saatte fırlatılır",
                                                fontSize = 9.5.sp,
                                                color = Color.Gray,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Değiştir",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activeAccentColor,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDark) Color.LightGray else Color.DarkGray
                        )
                    ) {
                        Text("İptal", fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.setGradingSystemIndex(tempGradingSystemIndex)
                            viewModel.setWeightType(tempWeightType)
                            viewModel.setIsBellCurve(tempIsBellCurve)
                            viewModel.updateReminderTime(tempReminderTime)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonBlue
                        )
                    ) {
                        Text("Kaydet", fontSize = 14.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomDatePicker(
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int) -> Unit
) {
    var selectedYear by remember { mutableStateOf(initialYear) }
    var selectedMonth by remember { mutableStateOf(initialMonth) }
    var selectedDay by remember { mutableStateOf(initialDay) }

    val isDark = isDarkThemeActive()
    val accentColor = ElectricBlue

    val pagerState = rememberPagerState(initialPage = initialMonth - 1) { 12 }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        val opaqueColor = if (isDark) DeepSurfaceDark else Color.White
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            containerColor = opaqueColor,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Tarih Seçin", color = AdaptWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { 
                        scope.launch {
                            val target = if (pagerState.currentPage > 0) pagerState.currentPage - 1 else 11
                            if (target == 11) selectedYear--
                            pagerState.scrollToPage(target)
                        }
                    }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null, tint = AdaptWhite)
                    }
                    Text(
                        text = "${getMonthName(pagerState.currentPage + 1)} $selectedYear",
                        color = AdaptWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(onClick = { 
                        scope.launch {
                            val target = if (pagerState.currentPage < 11) pagerState.currentPage + 1 else 0
                            if (target == 0) selectedYear++
                            pagerState.scrollToPage(target)
                        }
                    }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = AdaptWhite)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.height(240.dp),
                    userScrollEnabled = false
                ) { page ->
                    val month = page + 1
                    val daysInMonth = getDaysInMonth(month, selectedYear)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items((1..daysInMonth).toList()) { day ->
                            val isSelected = day == selectedDay && month == selectedMonth
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) accentColor else Color.Transparent)
                                    .clickable { 
                                        selectedDay = day 
                                        selectedMonth = month
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.toString(),
                                    color = if (isSelected) Color.White else AdaptWhite,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("İptal", color = AdaptGray)
                    }
                    Button(
                        onClick = { onConfirm(selectedYear, selectedMonth, selectedDay) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Tamam", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomTimePicker(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }

    val isDark = isDarkThemeActive()
    val accentColor = ElectricBlue

    Dialog(onDismissRequest = onDismiss) {
        val opaqueColor = if (isDark) DeepSurfaceDark else Color.White
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            containerColor = opaqueColor,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Saat Seçin", color = AdaptWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    WheelNumberPicker(
                        value = selectedHour,
                        range = 0..23,
                        onValueChange = { selectedHour = it }
                    )
                    Text(":", color = AdaptWhite, fontSize = 24.sp, modifier = Modifier.padding(horizontal = 24.dp))
                    WheelNumberPicker(
                        value = selectedMinute,
                        range = 0..59,
                        onValueChange = { selectedMinute = it }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("İptal", color = AdaptGray)
                    }
                    Button(
                        onClick = { onConfirm(selectedHour, selectedMinute) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Tamam", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun WheelNumberPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    val items = range.toList()
    val initialIndex = items.indexOf(value)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val scope = rememberCoroutineScope()
    val isDark = isDarkThemeActive()
    
    val snapFlingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(listState.firstVisibleItemIndex) {
        onValueChange(items[listState.firstVisibleItemIndex])
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = {
                scope.launch {
                    val targetIndex = if (listState.firstVisibleItemIndex > 0) listState.firstVisibleItemIndex - 1 else items.size - 1
                    listState.animateScrollToItem(targetIndex)
                }
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Arttır", tint = if (isDark) AdaptWhite else Color.Black)
        }

        Box(
            modifier = Modifier.height(120.dp).width(60.dp),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                state = listState,
                flingBehavior = snapFlingBehavior,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(items) { item ->
                    val isSelected = listState.firstVisibleItemIndex == items.indexOf(item)
                    val textColor = if (isSelected) {
                        if (isDark) Color.White else Color.Black
                    } else {
                        if (isDark) AdaptWhite.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.3f)
                    }
                    Box(
                        modifier = Modifier.height(40.dp).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.toString().padStart(2, '0'),
                            color = textColor,
                            fontSize = if (isSelected) 32.sp else 24.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
            // Selection indicators
            Canvas(modifier = Modifier.fillMaxSize()) {
                val yTop = size.height / 2 - 20.dp.toPx()
                val yBottom = size.height / 2 + 20.dp.toPx()
                val lineColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
                drawLine(lineColor, Offset(0f, yTop), Offset(size.width, yTop), 1.dp.toPx())
                drawLine(lineColor, Offset(0f, yBottom), Offset(size.width, yBottom), 1.dp.toPx())
            }
        }

        IconButton(
            onClick = {
                scope.launch {
                    val targetIndex = if (listState.firstVisibleItemIndex < items.size - 1) listState.firstVisibleItemIndex + 1 else 0
                    listState.animateScrollToItem(targetIndex)
                }
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Azalt", tint = if (isDark) AdaptWhite else Color.Black)
        }
    }
}

fun getMonthName(month: Int): String {
    return when (month) {
        1 -> "Ocak"
        2 -> "Şubat"
        3 -> "Mart"
        4 -> "Nisan"
        5 -> "Mayıs"
        6 -> "Haziran"
        7 -> "Temmuz"
        8 -> "Ağustos"
        9 -> "Eylül"
        10 -> "Ekim"
        11 -> "Kasım"
        12 -> "Aralık"
        else -> ""
    }
}

fun getDaysInMonth(month: Int, year: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) 29 else 28
        else -> 31
    }
}

