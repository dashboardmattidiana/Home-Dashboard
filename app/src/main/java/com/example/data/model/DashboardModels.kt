package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class HouseholdConfig(
    @DocumentId val id: String = "household",
    val partnerA: String = "Matti",
    val partnerB: String = "Diana",
    val cityName: String = "Berlin",
    val latitude: Double = 52.5200,
    val longitude: Double = 13.4050,
    val nightModeEnabled: Boolean = false,
    val nightModeStartHour: Long = 22L,
    val nightModeEndHour: Long = 6L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class EventAssignee(val wireValue: String) {
    BOTH("both"),
    PARTNER_A("partnerA"),
    PARTNER_B("partnerB");

    companion object {
        fun fromWire(raw: String?): EventAssignee =
            entries.firstOrNull { it.wireValue == raw } ?: BOTH
    }
}

@Keep
@IgnoreExtraProperties
data class CalendarEvent(
    @DocumentId val id: String = "",
    val title: String = "",
    val eventDate: String = "",
    val eventTime: String = "",
    val assignee: String = "both",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedAssignee(): EventAssignee = EventAssignee.fromWire(assignee)
}

@Keep
@IgnoreExtraProperties
data class ShoppingItem(
    @DocumentId val id: String = "",
    val name: String = "",
    val quantity: String = "",
    val checked: Boolean = false,
    val addedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TodoItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val completed: Boolean = false,
    val completedBy: String = "",
    val completedDate: String = "",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class WasteType(val wireValue: String, val label: String) {
    RESTMUELL("restmuell", "Restmüll"),
    GELBER_SACK("gelber_sack", "Gelber Sack"),
    PAPIER("papier", "Papier"),
    BIO("bio", "Biomüll");

    companion object {
        fun fromWire(raw: String?): WasteType =
            entries.firstOrNull { it.wireValue == raw } ?: RESTMUELL
    }
}

@Keep
@IgnoreExtraProperties
data class TrashPickup(
    @DocumentId val id: String = "",
    val wasteType: String = "restmuell",
    val pickupDate: String = "",
    val putOut: Boolean = false,
    val putOutBy: String = "",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedWasteType(): WasteType = WasteType.fromWire(wasteType)
}

@Keep
@IgnoreExtraProperties
data class BoardNote(
    @DocumentId val id: String = "",
    val message: String = "",
    val author: String = "",
    val pinned: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class WeatherUiData(
    val cityName: String = "Berlin",
    val currentTempC: Double? = null,
    val maxTempC: Double? = null,
    val minTempC: Double? = null,
    val weatherCode: Int = 0,
    val isDayTime: Boolean = true,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
