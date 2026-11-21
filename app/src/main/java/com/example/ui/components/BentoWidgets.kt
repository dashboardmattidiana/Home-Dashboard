package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Recycling
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.BoardNote
import com.example.data.model.CalendarEvent
import com.example.data.model.EventAssignee
import com.example.data.model.HouseholdConfig
import com.example.data.model.ShoppingItem
import com.example.data.model.TodoItem
import com.example.data.model.TrashPickup
import com.example.data.model.WasteType
import com.example.data.model.WeatherUiData
import com.example.ui.theme.ContourSubtle
import com.example.ui.theme.EspressoBg
import com.example.ui.theme.EspressoCard
import com.example.ui.theme.MutedStone
import com.example.ui.theme.OchreAccent
import com.example.ui.theme.OliveAccent
import com.example.ui.theme.TerracottaAccent
import com.example.ui.theme.TrashBio
import com.example.ui.theme.TrashGelberSack
import com.example.ui.theme.TrashPapier
import com.example.ui.theme.TrashRestmuell
import com.example.ui.theme.WalnutCard
import com.example.ui.theme.WalnutDeep
import com.example.ui.theme.WarmCream
import com.example.ui.theme.WarmTaupe
import com.example.ui.viewmodel.DashboardViewModel
import kotlin.math.roundToInt

@Composable
fun HeimboardTile(
    modifier: Modifier = Modifier,
    containerColor: Color = EspressoCard.copy(alpha = 0.92f),
    borderColor: Color = ContourSubtle,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            content = content
        )
    }
}

@Composable
fun TileHeader(
    title: String,
    icon: ImageVector,
    accentColor: Color = OchreAccent,
    trailingBadge: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 1.4.sp,
                    fontSize = 13.sp
                ),
                color = WarmTaupe
            )
        }
        if (!trailingBadge.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = WalnutCard
            ) {
                Text(
                    text = trailingBadge,
                    style = MaterialTheme.typography.labelMedium,
                    color = WarmTaupe,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun ClockWeatherModule(
    formattedTime: String,
    formattedDate: String,
    weather: WeatherUiData,
    isNightMode: Boolean,
    compactHorizontal: Boolean = false,
    modifier: Modifier = Modifier
) {
    HeimboardTile(
        modifier = modifier,
        containerColor = WalnutCard.copy(alpha = 0.92f)
    ) {
        if (compactHorizontal) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = formattedTime.ifBlank { "--:--" },
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 60.sp),
                        color = WarmCream
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
                        color = WarmTaupe
                    )
                }
                WeatherSummaryBlock(weather = weather, isNightMode = isNightMode, alignEnd = true)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = weather.cityName,
                            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.2.sp),
                            color = OchreAccent
                        )
                        if (isNightMode) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = OliveAccent.copy(alpha = 0.22f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Bedtime,
                                        contentDescription = stringResource(R.string.night_mode_badge),
                                        tint = WarmTaupe,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Ruhemodus",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = WarmTaupe
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = formattedTime.ifBlank { "--:--" },
                        style = MaterialTheme.typography.displayLarge,
                        color = WarmCream
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
                        color = WarmTaupe
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ContourSubtle)
                )

                WeatherSummaryBlock(weather = weather, isNightMode = isNightMode, alignEnd = false)
            }
        }
    }
}

@Composable
private fun WeatherSummaryBlock(
    weather: WeatherUiData,
    isNightMode: Boolean,
    alignEnd: Boolean
) {
    val weatherMeta = resolveWeatherMeta(weather.weatherCode, weather.isDayTime)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.SpaceBetween,
        modifier = if (alignEnd) Modifier else Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(OchreAccent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = weatherMeta.icon,
                    contentDescription = weatherMeta.label,
                    tint = OchreAccent,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column {
                val tempText = weather.currentTempC?.let { "${it.roundToInt()}°" }
                    ?: if (weather.isLoading) "…" else "--°"
                Text(
                    text = tempText,
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 36.sp),
                    color = WarmCream
                )
                Text(
                    text = weather.errorMessage ?: weatherMeta.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmTaupe
                )
            }
        }

        if (!alignEnd) {
            Column(horizontalAlignment = Alignment.End) {
                val hi = weather.maxTempC?.let { "${it.roundToInt()}°" } ?: "--"
                val lo = weather.minTempC?.let { "${it.roundToInt()}°" } ?: "--"
                Text(
                    text = "H $hi",
                    style = MaterialTheme.typography.titleMedium,
                    color = WarmCream
                )
                Text(
                    text = "T $lo",
                    style = MaterialTheme.typography.titleMedium,
                    color = WarmTaupe
                )
            }
        } else {
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = weather.cityName,
                    style = MaterialTheme.typography.labelLarge,
                    color = OchreAccent
                )
                val hi = weather.maxTempC?.let { "${it.roundToInt()}°" } ?: "--"
                val lo = weather.minTempC?.let { "${it.roundToInt()}°" } ?: "--"
                Text(
                    text = "H $hi · T $lo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmTaupe
                )
            }
        }
    }
}

@Composable
fun CalendarModule(
    events: List<CalendarEvent>,
    config: HouseholdConfig,
    todayIso: String,
    maxItems: Int = 5,
    modifier: Modifier = Modifier
) {
    val upcoming = events.filter { it.eventDate >= todayIso }.take(maxItems)
    HeimboardTile(modifier = modifier) {
        TileHeader(
            title = stringResource(R.string.module_calendar),
            icon = Icons.Rounded.CalendarMonth,
            accentColor = TerracottaAccent,
            trailingBadge = if (upcoming.isNotEmpty()) "${upcoming.size} anstehend" else null
        )
        Spacer(modifier = Modifier.height(14.dp))
        if (upcoming.isEmpty()) {
            EmptyModuleNotice(stringResource(R.string.empty_calendar))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                upcoming.forEach { evt ->
                    CalendarEventRow(event = evt, config = config)
                }
            }
        }
    }
}

@Composable
private fun CalendarEventRow(event: CalendarEvent, config: HouseholdConfig) {
    val (badgeLabel, badgeColor) = when (event.parsedAssignee()) {
        EventAssignee.PARTNER_A -> config.partnerA to TerracottaAccent
        EventAssignee.PARTNER_B -> config.partnerB to OliveAccent
        EventAssignee.BOTH -> "Beide" to OchreAccent
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = WalnutDeep.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, ContourSubtle.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 34.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = WarmCream,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val dateText = DashboardViewModel.formatGermanShortDate(event.eventDate)
                    val timePart = if (event.eventTime.isNotBlank()) " · ${event.eventTime} Uhr" else ""
                    Text(
                        text = "$dateText$timePart",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmTaupe
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = badgeColor.copy(alpha = 0.2f)
            ) {
                Text(
                    text = badgeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = WarmCream,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShoppingModule(
    items: List<ShoppingItem>,
    asChips: Boolean = false,
    maxItems: Int = 8,
    modifier: Modifier = Modifier
) {
    val openItems = items.filter { !it.checked }
    val checkedItems = items.filter { it.checked }
    val displayItems = (openItems + checkedItems).take(maxItems)

    HeimboardTile(modifier = modifier) {
        TileHeader(
            title = stringResource(R.string.module_shopping),
            icon = Icons.Rounded.ShoppingBasket,
            accentColor = OliveAccent,
            trailingBadge = if (openItems.isNotEmpty()) "${openItems.size} offen" else null
        )
        Spacer(modifier = Modifier.height(14.dp))
        if (displayItems.isEmpty()) {
            EmptyModuleNotice(stringResource(R.string.empty_shopping))
        } else if (asChips) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                displayItems.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (item.checked) WalnutDeep.copy(alpha = 0.5f) else WalnutCard,
                        border = BorderStroke(1.dp, ContourSubtle.copy(alpha = 0.7f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (item.checked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (item.checked) OliveAccent else WarmTaupe,
                                modifier = Modifier.size(16.dp)
                            )
                            val label = if (item.quantity.isNotBlank()) "${item.quantity} ${item.name}" else item.name
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                color = if (item.checked) MutedStone else WarmCream,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                displayItems.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (item.checked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (item.checked) OliveAccent else OchreAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                color = if (item.checked) MutedStone else WarmCream,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (item.quantity.isNotBlank()) {
                            Text(
                                text = item.quantity,
                                style = MaterialTheme.typography.labelLarge,
                                color = WarmTaupe
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TodosModule(
    todos: List<TodoItem>,
    config: HouseholdConfig,
    todayIso: String,
    maxItems: Int = 7,
    modifier: Modifier = Modifier
) {
    val active = todos.filter { !it.completed }
    val doneToday = todos.filter { it.completed && it.completedDate == todayIso }
    val combined = (active + doneToday).take(maxItems)

    HeimboardTile(modifier = modifier) {
        TileHeader(
            title = stringResource(R.string.module_todos),
            icon = Icons.Rounded.TaskAlt,
            accentColor = OchreAccent,
            trailingBadge = if (active.isNotEmpty()) "${active.size} offen" else "Alles erledigt"
        )
        Spacer(modifier = Modifier.height(14.dp))
        if (combined.isEmpty()) {
            EmptyModuleNotice(stringResource(R.string.empty_todos))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val activeToDisplay = combined.filter { !it.completed }
                val doneToDisplay = combined.filter { it.completed }

                activeToDisplay.forEach { todo ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(OchreAccent)
                        )
                        Text(
                            text = todo.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = WarmCream,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (doneToDisplay.isNotEmpty()) {
                    if (activeToDisplay.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.module_todos_done_today).uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                            color = OliveAccent
                        )
                    }
                    doneToDisplay.forEach { todo ->
                        val whoColor = when {
                            todo.completedBy.equals(config.partnerA, ignoreCase = true) -> TerracottaAccent
                            todo.completedBy.equals(config.partnerB, ignoreCase = true) -> OliveAccent
                            else -> OchreAccent
                        }
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
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = OliveAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = todo.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 16.sp,
                                        textDecoration = TextDecoration.LineThrough
                                    ),
                                    color = MutedStone,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (todo.completedBy.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = whoColor.copy(alpha = 0.24f)
                                ) {
                                    Text(
                                        text = todo.completedBy,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = WarmCream,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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

@Composable
fun TrashModule(
    pickups: List<TrashPickup>,
    todayIso: String,
    tomorrowIso: String,
    currentHour: Int,
    modifier: Modifier = Modifier
) {
    val urgent = pickups.filter {
        DashboardViewModel.isTrashUrgent(it, todayIso, tomorrowIso, currentHour)
    }
    val nextQuiet = pickups.firstOrNull { !it.putOut && it.pickupDate >= todayIso }

    val hasUrgent = urgent.isNotEmpty()
    val primaryWaste = urgent.firstOrNull()?.parsedWasteType() ?: nextQuiet?.parsedWasteType()
    val accent = when (primaryWaste) {
        WasteType.RESTMUELL -> TrashRestmuell
        WasteType.GELBER_SACK -> TrashGelberSack
        WasteType.PAPIER -> TrashPapier
        WasteType.BIO -> TrashBio
        null -> OliveAccent
    }

    HeimboardTile(
        modifier = modifier,
        containerColor = if (hasUrgent) accent.copy(alpha = 0.22f) else EspressoCard.copy(alpha = 0.90f),
        borderColor = if (hasUrgent) accent else ContourSubtle
    ) {
        TileHeader(
            title = stringResource(R.string.module_trash),
            icon = Icons.Rounded.Recycling,
            accentColor = accent,
            trailingBadge = if (hasUrgent) "Rausstellen!" else null
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (hasUrgent) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                urgent.take(2).forEach { pickup ->
                    val waste = pickup.parsedWasteType()
                    val wasteColor = when (waste) {
                        WasteType.RESTMUELL -> TrashRestmuell
                        WasteType.GELBER_SACK -> TrashGelberSack
                        WasteType.PAPIER -> TrashPapier
                        WasteType.BIO -> TrashBio
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(wasteColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = waste.label,
                                tint = EspressoBg,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column {
                            Text(
                                text = waste.label,
                                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
                                color = WarmCream
                            )
                            Text(
                                text = if (pickup.pickupDate == todayIso) {
                                    stringResource(R.string.trash_today)
                                } else {
                                    stringResource(R.string.trash_tomorrow_evening)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = WarmCream.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        } else if (nextQuiet != null) {
            val waste = nextQuiet.parsedWasteType()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Column {
                    Text(
                        text = "${stringResource(R.string.trash_next_pickup)}: ${waste.label}",
                        style = MaterialTheme.typography.titleMedium,
                        color = WarmCream
                    )
                    Text(
                        text = DashboardViewModel.formatGermanShortDate(nextQuiet.pickupDate),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmTaupe
                    )
                }
            }
        } else {
            EmptyModuleNotice(stringResource(R.string.empty_trash))
        }
    }
}

@Composable
fun BulletinModule(
    notes: List<BoardNote>,
    maxItems: Int = 3,
    modifier: Modifier = Modifier
) {
    val visibleNotes = (notes.filter { it.pinned } + notes.filter { !it.pinned })
        .distinctBy { it.id }
        .take(maxItems)

    HeimboardTile(
        modifier = modifier,
        containerColor = WalnutCard.copy(alpha = 0.88f)
    ) {
        TileHeader(
            title = stringResource(R.string.module_notes),
            icon = Icons.Rounded.PushPin,
            accentColor = TerracottaAccent
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (visibleNotes.isEmpty()) {
            EmptyModuleNotice(stringResource(R.string.empty_notes))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                visibleNotes.forEach { note ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EspressoCard.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, ContourSubtle.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = note.message,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    lineHeight = 24.sp
                                ),
                                color = WarmCream,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "— ${note.author}",
                                style = MaterialTheme.typography.labelMedium,
                                color = OchreAccent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyModuleNotice(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MutedStone
        )
    }
}

private data class WeatherMeta(val label: String, val icon: ImageVector)

private fun resolveWeatherMeta(code: Int, isDay: Boolean): WeatherMeta {
    return when (code) {
        0 -> WeatherMeta("Klar", if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay)
        1, 2 -> WeatherMeta("Leicht bewölkt", if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.Cloud)
        3 -> WeatherMeta("Bewölkt", Icons.Rounded.Cloud)
        45, 48 -> WeatherMeta("Nebel", Icons.Rounded.Grain)
        51, 53, 55, 56, 57 -> WeatherMeta("Nieselregen", Icons.Rounded.WaterDrop)
        61, 63, 65, 66, 67, 80, 81, 82 -> WeatherMeta("Regen", Icons.Rounded.WaterDrop)
        71, 73, 75, 77, 85, 86 -> WeatherMeta("Schnee", Icons.Rounded.AcUnit)
        95, 96, 99 -> WeatherMeta("Gewitter", Icons.Rounded.Thunderstorm)
        else -> WeatherMeta("Wechselhaft", Icons.Rounded.Cloud)
    }
}
