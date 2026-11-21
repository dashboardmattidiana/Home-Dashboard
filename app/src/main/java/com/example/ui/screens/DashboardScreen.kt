package com.example.ui.screens

import android.view.KeyEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.BoardNote
import com.example.data.model.CalendarEvent
import com.example.data.model.HouseholdConfig
import com.example.data.model.ShoppingItem
import com.example.data.model.TodoItem
import com.example.data.model.TrashPickup
import com.example.ui.auth.signOut
import com.example.ui.components.BulletinModule
import com.example.ui.components.CalendarModule
import com.example.ui.components.ClockWeatherModule
import com.example.ui.components.ShoppingModule
import com.example.ui.components.TodosModule
import com.example.ui.components.TrashModule
import com.example.ui.theme.ContourSubtle
import com.example.ui.theme.EspressoBg
import com.example.ui.theme.EspressoCard
import com.example.ui.theme.OchreAccent
import com.example.ui.theme.OliveAccent
import com.example.ui.theme.TerracottaAccent
import com.example.ui.theme.WalnutCard
import com.example.ui.theme.WarmCream
import com.example.ui.theme.WarmTaupe
import com.example.ui.viewmodel.BentoLayoutMode
import com.example.ui.viewmodel.DashboardRuntimeState
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val runtime by viewModel.runtimeState.collectAsStateWithLifecycle()
    val configState by viewModel.configState.collectAsStateWithLifecycle()
    val eventsState by viewModel.eventsState.collectAsStateWithLifecycle()
    val shoppingState by viewModel.shoppingState.collectAsStateWithLifecycle()
    val todosState by viewModel.todosState.collectAsStateWithLifecycle()
    val trashState by viewModel.trashState.collectAsStateWithLifecycle()
    val notesState by viewModel.notesState.collectAsStateWithLifecycle()

    val config = (configState as? UiState.Success)?.data ?: HouseholdConfig()
    val events = (eventsState as? UiState.Success)?.data.orEmpty()
    val shopping = (shoppingState as? UiState.Success)?.data.orEmpty()
    val todos = (todosState as? UiState.Success)?.data.orEmpty()
    val trash = (trashState as? UiState.Success)?.data.orEmpty()
    val notes = (notesState as? UiState.Success)?.data.orEmpty()

    val firstError = listOf(configState, eventsState, shoppingState, todosState, trashState, notesState)
        .filterIsInstance<UiState.Error>()
        .firstOrNull()?.message

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EspressoBg)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_MENU,
                        KeyEvent.KEYCODE_INFO,
                        KeyEvent.KEYCODE_SETTINGS -> {
                            viewModel.setShowTvControls(!runtime.showTvControls)
                            true
                        }
                        KeyEvent.KEYCODE_BACK -> {
                            if (runtime.showTvControls) {
                                viewModel.setShowTvControls(false)
                                true
                            } else false
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // 1. Curated atmospheric background with 15-min crossfade & Coil disk cache + local fallback
        Crossfade(
            targetState = runtime.ambientScene,
            animationSpec = tween(durationMillis = 1800),
            label = "ambient_bg_crossfade"
        ) { scene ->
            val fallbackPainter = painterResource(id = scene.fallbackRes)
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(scene.remoteUrl)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .crossfade(1200)
                    .build(),
                contentDescription = scene.title,
                placeholder = fallbackPainter,
                error = fallbackPainter,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Warm dark Espresso scrim for legibility & adjustable Night/Quiet Mode dimming
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    EspressoBg.copy(
                        alpha = if (runtime.isNightModeActive) 0.85f else 0.68f
                    )
                )
        )

        // 3. Rotating 3-mode Bento grid with subtle burn-in mitigation pixel shift
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp)
                .offset { runtime.burnInShiftOffset }
        ) {
            AnimatedContent(
                targetState = runtime.layoutMode,
                transitionSpec = {
                    fadeIn(tween(1400)) togetherWith fadeOut(tween(1400))
                },
                label = "bento_layout_rotation"
            ) { mode ->
                when (mode) {
                    BentoLayoutMode.ATELIER_WEST -> BentoLayoutAtelierWest(
                        runtime = runtime,
                        config = config,
                        events = events,
                        shopping = shopping,
                        todos = todos,
                        trash = trash,
                        notes = notes
                    )
                    BentoLayoutMode.GALERIE_PANORAMA -> BentoLayoutGaleriePanorama(
                        runtime = runtime,
                        config = config,
                        events = events,
                        shopping = shopping,
                        todos = todos,
                        trash = trash,
                        notes = notes
                    )
                    BentoLayoutMode.KAMINZIMMER_SPLIT -> BentoLayoutKaminzimmerSplit(
                        runtime = runtime,
                        config = config,
                        events = events,
                        shopping = shopping,
                        todos = todos,
                        trash = trash,
                        notes = notes
                    )
                }
            }

            // Subtle corner TV controls trigger (accessible via D-pad focus or Menu button)
            IconButton(
                onClick = { viewModel.setShowTvControls(true) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = EspressoCard.copy(alpha = 0.55f),
                    contentColor = WarmTaupe.copy(alpha = 0.8f)
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(48.dp)
                    .testTag("tv_controls_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = stringResource(R.string.tv_controls_title),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Non-intrusive sync error banner if Firestore has a transient error
            if (firstError != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = TerracottaAccent.copy(alpha = 0.92f)
                ) {
                    Text(
                        text = firstError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmCream,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // 4. Night/Quiet Mode extra soft luminance reduction veil
        if (runtime.isNightModeActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(EspressoBg.copy(alpha = 0.25f))
            )
        }

        // 5. TV Controls Overlay (D-pad friendly)
        AnimatedVisibility(
            visible = runtime.showTvControls,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(220))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(EspressoBg.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .padding(24.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = EspressoCard,
                    border = BorderStroke(1.dp, ContourSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.tv_controls_title),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = WarmCream
                                )
                                Text(
                                    text = stringResource(R.string.tv_controls_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = WarmTaupe
                                )
                            }
                            IconButton(
                                onClick = { viewModel.setShowTvControls(false) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Schließen",
                                    tint = WarmCream
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.cycleLayoutManually() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WalnutCard,
                                    contentColor = WarmCream
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("cycle_layout_button")
                            ) {
                                Icon(Icons.Rounded.DashboardCustomize, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text("Layout: ${runtime.layoutMode.label}")
                            }

                            Button(
                                onClick = { viewModel.toggleNightModeFromTv() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (runtime.isNightModeActive) OliveAccent else WalnutCard,
                                    contentColor = if (runtime.isNightModeActive) EspressoBg else WarmCream
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("night_mode_button")
                            ) {
                                Icon(Icons.Rounded.Bedtime, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text(if (runtime.isNightModeActive) "Ruhemodus: An" else "Ruhemodus: Aus")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.refreshWeatherNow() },
                                border = BorderStroke(1.dp, ContourSubtle),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmCream),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, tint = OchreAccent)
                                Spacer(Modifier.size(8.dp))
                                Text("Wetter aktualisieren")
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.setShowTvControls(false)
                                    signOut(credentialManager, scope)
                                },
                                border = BorderStroke(1.dp, TerracottaAccent.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TerracottaAccent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text("Konto abmelden")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Layout 1: "Atelier West"
 * Left (35%): Clock/Weather (top, tall) + Bulletin Board (bottom)
 * Center (34%): Calendar Events (top, large) + Shopping List (bottom)
 * Right (31%): Trash Alert (top) + Todos (bottom, tall)
 */
@Composable
private fun BentoLayoutAtelierWest(
    runtime: DashboardRuntimeState,
    config: HouseholdConfig,
    events: List<CalendarEvent>,
    shopping: List<ShoppingItem>,
    todos: List<TodoItem>,
    trash: List<TrashPickup>,
    notes: List<BoardNote>
) {
    val gap = 16.dp
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        Column(
            modifier = Modifier
                .weight(0.35f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            ClockWeatherModule(
                formattedTime = runtime.formattedTime,
                formattedDate = runtime.formattedDate,
                weather = runtime.weather,
                isNightMode = runtime.isNightModeActive,
                modifier = Modifier.weight(0.56f)
            )
            BulletinModule(
                notes = notes,
                maxItems = 2,
                modifier = Modifier.weight(0.44f)
            )
        }

        Column(
            modifier = Modifier
                .weight(0.34f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            CalendarModule(
                events = events,
                config = config,
                todayIso = runtime.todayIso,
                maxItems = 5,
                modifier = Modifier.weight(0.54f)
            )
            ShoppingModule(
                items = shopping,
                asChips = false,
                maxItems = 6,
                modifier = Modifier.weight(0.46f)
            )
        }

        Column(
            modifier = Modifier
                .weight(0.31f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            TrashModule(
                pickups = trash,
                todayIso = runtime.todayIso,
                tomorrowIso = runtime.tomorrowIso,
                currentHour = runtime.currentHour,
                modifier = Modifier.weight(0.28f)
            )
            TodosModule(
                todos = todos,
                config = config,
                todayIso = runtime.todayIso,
                maxItems = 7,
                modifier = Modifier.weight(0.72f)
            )
        }
    }
}

/**
 * Layout 2: "Galerie Panorama"
 * Top Row (38%): Calendar (28%) | Bulletin & Trash Hero (40%) | Clock/Weather (32%)
 * Bottom Row (62%): Todos (34%) | Shopping Chips (36%) | Trash / Upcoming Events / Notes split (30%)
 */
@Composable
private fun BentoLayoutGaleriePanorama(
    runtime: DashboardRuntimeState,
    config: HouseholdConfig,
    events: List<CalendarEvent>,
    shopping: List<ShoppingItem>,
    todos: List<TodoItem>,
    trash: List<TrashPickup>,
    notes: List<BoardNote>
) {
    val gap = 16.dp
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        Row(
            modifier = Modifier
                .weight(0.40f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            BulletinModule(
                notes = notes,
                maxItems = 2,
                modifier = Modifier.weight(0.36f)
            )
            TrashModule(
                pickups = trash,
                todayIso = runtime.todayIso,
                tomorrowIso = runtime.tomorrowIso,
                currentHour = runtime.currentHour,
                modifier = Modifier.weight(0.28f)
            )
            ClockWeatherModule(
                formattedTime = runtime.formattedTime,
                formattedDate = runtime.formattedDate,
                weather = runtime.weather,
                isNightMode = runtime.isNightModeActive,
                compactHorizontal = true,
                modifier = Modifier.weight(0.36f)
            )
        }

        Row(
            modifier = Modifier
                .weight(0.60f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            TodosModule(
                todos = todos,
                config = config,
                todayIso = runtime.todayIso,
                maxItems = 6,
                modifier = Modifier.weight(0.33f)
            )
            ShoppingModule(
                items = shopping,
                asChips = true,
                maxItems = 10,
                modifier = Modifier.weight(0.35f)
            )
            CalendarModule(
                events = events,
                config = config,
                todayIso = runtime.todayIso,
                maxItems = 5,
                modifier = Modifier.weight(0.32f)
            )
        }
    }
}

/**
 * Layout 3: "Kaminzimmer Split"
 * Left (32%): Shopping (top) + Clock/Weather (bottom)
 * Right (68%):
 *   Top (44%): Todos (58%) + Trash (42%)
 *   Bottom (56%): Calendar (55%) + Bulletin Board (45%)
 */
@Composable
private fun BentoLayoutKaminzimmerSplit(
    runtime: DashboardRuntimeState,
    config: HouseholdConfig,
    events: List<CalendarEvent>,
    shopping: List<ShoppingItem>,
    todos: List<TodoItem>,
    trash: List<TrashPickup>,
    notes: List<BoardNote>
) {
    val gap = 16.dp
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        Column(
            modifier = Modifier
                .weight(0.32f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            ShoppingModule(
                items = shopping,
                asChips = false,
                maxItems = 6,
                modifier = Modifier.weight(0.48f)
            )
            ClockWeatherModule(
                formattedTime = runtime.formattedTime,
                formattedDate = runtime.formattedDate,
                weather = runtime.weather,
                isNightMode = runtime.isNightModeActive,
                modifier = Modifier.weight(0.52f)
            )
        }

        Column(
            modifier = Modifier
                .weight(0.68f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            Row(
                modifier = Modifier
                    .weight(0.46f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                TodosModule(
                    todos = todos,
                    config = config,
                    todayIso = runtime.todayIso,
                    maxItems = 5,
                    modifier = Modifier.weight(0.58f)
                )
                TrashModule(
                    pickups = trash,
                    todayIso = runtime.todayIso,
                    tomorrowIso = runtime.tomorrowIso,
                    currentHour = runtime.currentHour,
                    modifier = Modifier.weight(0.42f)
                )
            }

            Row(
                modifier = Modifier
                    .weight(0.54f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                CalendarModule(
                    events = events,
                    config = config,
                    todayIso = runtime.todayIso,
                    maxItems = 4,
                    modifier = Modifier.weight(0.55f)
                )
                BulletinModule(
                    notes = notes,
                    maxItems = 3,
                    modifier = Modifier.weight(0.45f)
                )
            }
        }
    }
}
