package com.example.ui.viewmodel

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.model.BoardNote
import com.example.data.model.CalendarEvent
import com.example.data.model.HouseholdConfig
import com.example.data.model.ShoppingItem
import com.example.data.model.TodoItem
import com.example.data.model.TrashPickup
import com.example.data.model.WeatherUiData
import com.example.data.remote.OpenMeteoService
import com.example.data.repository.DashboardRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class BentoLayoutMode(val label: String) {
    ATELIER_WEST("Atelier"),
    GALERIE_PANORAMA("Galerie"),
    KAMINZIMMER_SPLIT("Kaminzimmer")
}

data class AmbientScene(
    val id: String,
    val title: String,
    val remoteUrl: String,
    @DrawableRes val fallbackRes: Int
)

data class DashboardRuntimeState(
    val formattedTime: String = "",
    val formattedDate: String = "",
    val todayIso: String = "",
    val tomorrowIso: String = "",
    val currentHour: Int = 12,
    val layoutMode: BentoLayoutMode = BentoLayoutMode.ATELIER_WEST,
    val ambientScene: AmbientScene = AMBIENT_SCENES.first(),
    val burnInShiftOffset: IntOffset = IntOffset.Zero,
    val isNightModeActive: Boolean = false,
    val manualNightOverride: Boolean? = null,
    val showTvControls: Boolean = false,
    val weather: WeatherUiData = WeatherUiData()
) {
    companion object {
        val AMBIENT_SCENES = listOf(
            AmbientScene(
                id = "forest",
                title = "Kiefernwald Dämmerung",
                remoteUrl = "https://images.unsplash.com/photo-1511497584788-876760111969?auto=format&fit=crop&w=1920&q=80",
                fallbackRes = R.drawable.img_ambient_forest
            ),
            AmbientScene(
                id = "arch",
                title = "Walnuss Wohnzimmer",
                remoteUrl = "https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?auto=format&fit=crop&w=1920&q=80",
                fallbackRes = R.drawable.img_ambient_arch
            ),
            AmbientScene(
                id = "hills",
                title = "Terrakotta Hügel",
                remoteUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=1920&q=80",
                fallbackRes = R.drawable.img_ambient_desert
            )
        )
    }
}

class DashboardViewModel(
    private val repository: DashboardRepository,
    private val weatherService: OpenMeteoService,
    private val currentUserId: String
) : ViewModel() {

    val configState: StateFlow<UiState<HouseholdConfig>> =
        observeWrapped(repository.observeConfig(currentUserId), "Konfiguration")

    val eventsState: StateFlow<UiState<List<CalendarEvent>>> =
        observeWrapped(repository.observeEvents(currentUserId), "Kalender")

    val shoppingState: StateFlow<UiState<List<ShoppingItem>>> =
        observeWrapped(repository.observeShopping(currentUserId), "Einkaufszettel")

    val todosState: StateFlow<UiState<List<TodoItem>>> =
        observeWrapped(repository.observeTodos(currentUserId), "Aufgaben")

    val trashState: StateFlow<UiState<List<TrashPickup>>> =
        observeWrapped(repository.observeTrash(currentUserId), "Müllkalender")

    val notesState: StateFlow<UiState<List<BoardNote>>> =
        observeWrapped(repository.observeNotes(currentUserId), "Schwarzes Brett")

    private val _runtimeState = MutableStateFlow(DashboardRuntimeState())
    val runtimeState: StateFlow<DashboardRuntimeState> = _runtimeState.asStateFlow()

    private val germanLocale = Locale.GERMANY
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", germanLocale)
    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM", germanLocale)
    private val isoDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private var lastFetchedLat: Double? = null
    private var lastFetchedLon: Double? = null
    private var lastConfigNightEnabled: Boolean = false
    private var lastNightStartHour: Int = 22
    private var lastNightEndHour: Int = 6

    init {
        ensureInitialHouseholdConfig()
        startClockAndBurnInLoop()
        startAmbientRotationLoop()
        observeWeatherLocationChanges()
        startWeatherRefreshLoop()
    }

    private fun ensureInitialHouseholdConfig() {
        viewModelScope.launch {
            repository.ensureDefaultConfig(currentUserId)
        }
    }

    private fun startClockAndBurnInLoop() {
        viewModelScope.launch {
            val shiftCycle = listOf(
                IntOffset(0, 0),
                IntOffset(4, 2),
                IntOffset(6, -3),
                IntOffset(-3, -4),
                IntOffset(-6, 2),
                IntOffset(2, 5)
            )
            var tickCount = 0
            while (isActive) {
                val now = LocalDateTime.now()
                val hour = now.hour
                val isScheduledNight = if (lastNightStartHour > lastNightEndHour) {
                    hour >= lastNightStartHour || hour < lastNightEndHour
                } else {
                    hour in lastNightStartHour until lastNightEndHour
                }
                val activeNight = _runtimeState.value.manualNightOverride
                    ?: (lastConfigNightEnabled || isScheduledNight)

                // Subtle pixel shift cycles every 5 minutes (10 * 30s ticks)
                val shift = shiftCycle[(tickCount / 10) % shiftCycle.size]

                _runtimeState.update { state ->
                    state.copy(
                        formattedTime = now.format(timeFormatter),
                        formattedDate = now.format(dateFormatter),
                        todayIso = now.toLocalDate().format(isoDateFormatter),
                        tomorrowIso = now.toLocalDate().plusDays(1).format(isoDateFormatter),
                        currentHour = hour,
                        burnInShiftOffset = shift,
                        isNightModeActive = activeNight
                    )
                }
                tickCount++
                delay(30_000L)
            }
        }
    }

    private fun startAmbientRotationLoop() {
        viewModelScope.launch {
            var index = 0
            while (isActive) {
                delay(ROTATION_INTERVAL_MS)
                index++
                val layouts = BentoLayoutMode.entries
                val scenes = DashboardRuntimeState.AMBIENT_SCENES
                _runtimeState.update { state ->
                    state.copy(
                        layoutMode = layouts[index % layouts.size],
                        ambientScene = scenes[index % scenes.size]
                    )
                }
            }
        }
    }

    private fun observeWeatherLocationChanges() {
        viewModelScope.launch {
            configState
                .map { state ->
                    (state as? UiState.Success)?.data
                }
                .distinctUntilChanged()
                .collect { config ->
                    if (config != null) {
                        lastConfigNightEnabled = config.nightModeEnabled
                        lastNightStartHour = config.nightModeStartHour.toInt().coerceIn(0, 23)
                        lastNightEndHour = config.nightModeEndHour.toInt().coerceIn(0, 23)
                        val nowHour = LocalDateTime.now().hour
                        val isScheduledNight = if (lastNightStartHour > lastNightEndHour) {
                            nowHour >= lastNightStartHour || nowHour < lastNightEndHour
                        } else {
                            nowHour in lastNightStartHour until lastNightEndHour
                        }
                        _runtimeState.update { state ->
                            state.copy(
                                isNightModeActive = state.manualNightOverride
                                    ?: (config.nightModeEnabled || isScheduledNight),
                                weather = state.weather.copy(cityName = config.cityName)
                            )
                        }
                        if (lastFetchedLat != config.latitude || lastFetchedLon != config.longitude) {
                            fetchWeather(config.cityName, config.latitude, config.longitude)
                        }
                    }
                }
        }
    }

    private fun startWeatherRefreshLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(WEATHER_REFRESH_INTERVAL_MS)
                val config = (configState.value as? UiState.Success)?.data ?: HouseholdConfig()
                fetchWeather(config.cityName, config.latitude, config.longitude)
            }
        }
    }

    fun refreshWeatherNow() {
        val config = (configState.value as? UiState.Success)?.data ?: HouseholdConfig()
        viewModelScope.launch {
            fetchWeather(config.cityName, config.latitude, config.longitude)
        }
    }

    private suspend fun fetchWeather(cityName: String, lat: Double, lon: Double) {
        lastFetchedLat = lat
        lastFetchedLon = lon
        try {
            val response = weatherService.getForecast(latitude = lat, longitude = lon)
            val currentTemp = response.current?.temperature2m
            val code = response.current?.weatherCode ?: 0
            val isDay = (response.current?.isDay ?: 1) == 1
            val maxTemp = response.daily?.temperature2mMax?.firstOrNull()
            val minTemp = response.daily?.temperature2mMin?.firstOrNull()

            _runtimeState.update { state ->
                state.copy(
                    weather = WeatherUiData(
                        cityName = cityName,
                        currentTempC = currentTemp,
                        maxTempC = maxTemp,
                        minTempC = minTemp,
                        weatherCode = code,
                        isDayTime = isDay,
                        isLoading = false,
                        errorMessage = null
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Open-Meteo weather fetch failed: ${e.message}")
            _runtimeState.update { state ->
                state.copy(
                    weather = state.weather.copy(
                        cityName = cityName,
                        isLoading = false,
                        errorMessage = "Wetter offline"
                    )
                )
            }
        }
    }

    fun cycleLayoutManually() {
        val layouts = BentoLayoutMode.entries
        val scenes = DashboardRuntimeState.AMBIENT_SCENES
        _runtimeState.update { state ->
            val nextLayoutIdx = (layouts.indexOf(state.layoutMode) + 1) % layouts.size
            val nextSceneIdx = (scenes.indexOf(state.ambientScene) + 1) % scenes.size
            state.copy(
                layoutMode = layouts[nextLayoutIdx],
                ambientScene = scenes[nextSceneIdx]
            )
        }
    }

    fun toggleNightModeFromTv() {
        val next = !_runtimeState.value.isNightModeActive
        _runtimeState.update { it.copy(manualNightOverride = next, isNightModeActive = next) }
        viewModelScope.launch {
            repository.toggleNightMode(currentUserId, next)
        }
    }

    fun setShowTvControls(visible: Boolean) {
        _runtimeState.update { it.copy(showTvControls = visible) }
    }

    private fun <T> observeWrapped(flow: kotlinx.coroutines.flow.Flow<T>, label: String): StateFlow<UiState<T>> =
        flow
            .map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Fehler beim Laden ($label)"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    companion object {
        private const val TAG = "DashboardVM"
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val ROTATION_INTERVAL_MS = 15L * 60L * 1000L
        private const val WEATHER_REFRESH_INTERVAL_MS = 30L * 60L * 1000L

        fun isTrashUrgent(pickup: TrashPickup, todayIso: String, tomorrowIso: String, currentHour: Int): Boolean {
            if (pickup.putOut) return false
            if (pickup.pickupDate == todayIso) return true
            if (pickup.pickupDate == tomorrowIso && currentHour >= 18) return true
            return false
        }

        fun formatGermanShortDate(isoDate: String): String {
            return try {
                val parsed = LocalDate.parse(isoDate)
                val today = LocalDate.now()
                when (parsed) {
                    today -> "Heute"
                    today.plusDays(1) -> "Morgen"
                    else -> parsed.format(DateTimeFormatter.ofPattern("EE, d. MMM", Locale.GERMANY))
                }
            } catch (_: Exception) {
                isoDate
            }
        }
    }
}
