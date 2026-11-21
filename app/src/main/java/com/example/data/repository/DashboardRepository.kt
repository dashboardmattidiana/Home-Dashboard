package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.BoardNote
import com.example.data.model.CalendarEvent
import com.example.data.model.HouseholdConfig
import com.example.data.model.ShoppingItem
import com.example.data.model.TodoItem
import com.example.data.model.TrashPickup
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DashboardRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireMatchedUser(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Bitte melde dich zuerst mit Google an.")
        check(uid == expectedUserId) { "Aktives Konto stimmt nicht mit dem Haushaltskonto überein." }
        return uid
    }

    fun observeConfig(userId: String): Flow<HouseholdConfig> = flow {
        val path = "users/$userId/config/household"
        emitAll(
            db.collection("users").document(userId)
                .collection("config").document("household")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        HouseholdConfig::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: HouseholdConfig()
                }
                .catch { error ->
                    val firestoreError = unwrapFirestoreError(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeEvents(userId: String): Flow<List<CalendarEvent>> =
        observeCollection(
            userId = userId,
            collectionName = "events",
            orderField = "eventDate",
            direction = Query.Direction.ASCENDING,
            clazz = CalendarEvent::class.java
        )

    fun observeShopping(userId: String): Flow<List<ShoppingItem>> =
        observeCollection(
            userId = userId,
            collectionName = "shopping",
            orderField = "createdAt",
            direction = Query.Direction.DESCENDING,
            clazz = ShoppingItem::class.java
        )

    fun observeTodos(userId: String): Flow<List<TodoItem>> =
        observeCollection(
            userId = userId,
            collectionName = "todos",
            orderField = "createdAt",
            direction = Query.Direction.DESCENDING,
            clazz = TodoItem::class.java
        )

    fun observeTrash(userId: String): Flow<List<TrashPickup>> =
        observeCollection(
            userId = userId,
            collectionName = "trash",
            orderField = "pickupDate",
            direction = Query.Direction.ASCENDING,
            clazz = TrashPickup::class.java
        )

    fun observeNotes(userId: String): Flow<List<BoardNote>> =
        observeCollection(
            userId = userId,
            collectionName = "notes",
            orderField = "createdAt",
            direction = Query.Direction.DESCENDING,
            clazz = BoardNote::class.java
        )

    private fun <T : Any> observeCollection(
        userId: String,
        collectionName: String,
        orderField: String,
        direction: Query.Direction,
        clazz: Class<T>
    ): Flow<List<T>> = flow {
        val path = "users/$userId/$collectionName"
        emitAll(
            db.collection("users").document(userId)
                .collection(collectionName)
                .orderBy(orderField, direction)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        clazz,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = unwrapFirestoreError(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureDefaultConfig(userId: String): Result<Unit> {
        val docPath = "users/$userId/config/household"
        return try {
            val uid = requireMatchedUser(userId)
            val docRef = db.collection("users").document(uid)
                .collection("config").document("household")
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val payload = mapOf(
                    "partnerA" to "Matti",
                    "partnerB" to "Diana",
                    "cityName" to "Berlin",
                    "latitude" to 52.5200,
                    "longitude" to 13.4050,
                    "nightModeEnabled" to false,
                    "nightModeStartHour" to 22L,
                    "nightModeEndHour" to 6L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleNightMode(userId: String, enabled: Boolean): Result<Unit> {
        val docPath = "users/$userId/config/household"
        return try {
            val uid = requireMatchedUser(userId)
            val docRef = db.collection("users").document(uid)
                .collection("config").document("household")
            docRef.update(
                mapOf(
                    "nightModeEnabled" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
}
