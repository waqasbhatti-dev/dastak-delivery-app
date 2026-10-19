package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.Rider
import com.example.data.model.RiderApplication
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import com.example.data.FirestoreProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class RiderRepository(private val db: FirebaseFirestore) {
    constructor(context: Context) : this(FirestoreProvider.get(context))

    private val auth = Firebase.auth

    @Volatile
    private var cachedRider: Rider? = null

    @Volatile
    private var cachedApplications: List<RiderApplication>? = null
    private var lastApplicationsFetchMs: Long = 0L

    @Volatile
    private var cachedApprovedRiders: List<Rider>? = null
    private var lastApprovedRidersFetchMs: Long = 0L

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google")
    }

    suspend fun getRiderApplication(userId: String): RiderApplication? {
        val path = "riderApplications"
        return try {
            val docSnap = db.collection(path).document(userId).get().await()
            if (docSnap.exists()) {
                return docSnap.toObject(RiderApplication::class.java)
            }
            val query = db.collection(path)
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .await()
            if (!query.isEmpty) {
                query.documents[0].toObject(RiderApplication::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            null
        }
    }

    suspend fun uploadCnicImage(context: Context, uri: Uri, isFront: Boolean): Result<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val uid = requireUserId()
        try {
            val contentResolver = context.contentResolver
            val filename = if (isFront) "cnic_front_${System.currentTimeMillis()}.jpg" else "cnic_back_${System.currentTimeMillis()}.jpg"
            val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference.child("riders/$uid/$filename")

            val inputStream: java.io.InputStream? = contentResolver.openInputStream(uri)
            if (inputStream == null) {
                return@withContext Result.failure(IllegalArgumentException("Could not read selected image file"))
            }

            try {
                storageRef.putStream(inputStream).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()
                Result.success(downloadUrl)
            } catch (storageEx: Exception) {
                // High-reliability fallback: encode as base64 data URI if storage bucket encounters issue
                val fallbackStream = contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(fallbackStream)
                if (bitmap != null) {
                    val out = java.io.ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, out)
                    val base64 = android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP)
                    Result.success("data:image/jpeg;base64,$base64")
                } else {
                    Result.failure(storageEx)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitApplication(application: RiderApplication): Result<String> {
        val uid = requireUserId()
        val path = "riderApplications"
        return try {
            val docRef = db.collection(path).document(uid)
            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "fullName" to application.fullName,
                "email" to application.email,
                "phone" to application.phone,
                "cnicNumber" to application.cnicNumber,
                "cnicFrontImageUrl" to application.cnicFrontImageUrl,
                "cnicBackImageUrl" to application.cnicBackImageUrl,
                "preferredLanguage" to application.preferredLanguage,
                "status" to "pending",
                "submittedAt" to FieldValue.serverTimestamp(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toRiderSafe(): Rider? {
        val r = try {
            toObject(Rider::class.java)
        } catch (_: Exception) {
            null
        }
        if (r != null) {
            val actualIsOnline = getBoolean("isOnline") ?: getBoolean("online") ?: r.isOnline
            return r.copy(isOnline = actualIsOnline)
        }
        val actualIsOnline = getBoolean("isOnline") ?: getBoolean("online") ?: false
        return Rider(
            id = id,
            userId = getString("userId") ?: id,
            fullName = getString("fullName") ?: "",
            phone = getString("phone") ?: "",
            email = getString("email") ?: "",
            cnicNumber = getString("cnicNumber") ?: "",
            status = getString("status") ?: "approved",
            isOnline = actualIsOnline,
            preferredLanguage = getString("preferredLanguage") ?: "en",
            activeOrderId = getString("activeOrderId") ?: "",
            createdAt = getTimestamp("createdAt"),
            updatedAt = getTimestamp("updatedAt")
        )
    }

    fun observeApprovedRider(userId: String): Flow<Rider?> = callbackFlow {
        val path = "riders/$userId"
        val docRef = db.collection("riders").document(userId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, path)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val rider = snapshot.toRiderSafe()
                cachedRider = rider
                trySend(rider)
            } else {
                trySend(null)
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun getApprovedRider(userId: String): Rider? {
        val currentCached = cachedRider
        if (currentCached != null && currentCached.userId == userId) {
            return currentCached
        }
        val path = "riders/$userId"
        return try {
            val docRef = db.collection("riders").document(userId)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val r = snapshot.toRiderSafe()
                if (r != null) {
                    cachedRider = r
                    return r
                }
            }

            // Auto-heal / provision if the application was approved by admin
            val app = getRiderApplication(userId)
            if (app != null && app.status == "approved") {
                val riderPayload = hashMapOf<String, Any>(
                    "userId" to userId,
                    "fullName" to app.fullName,
                    "phone" to app.phone,
                    "email" to app.email,
                    "cnicNumber" to app.cnicNumber,
                    "status" to "approved",
                    "isOnline" to false,
                    "preferredLanguage" to app.preferredLanguage.ifBlank { "en" },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(riderPayload).await()
                val r = docRef.get().await().toRiderSafe() ?: Rider(
                    id = userId,
                    userId = userId,
                    fullName = app.fullName,
                    phone = app.phone,
                    email = app.email,
                    cnicNumber = app.cnicNumber,
                    status = "approved",
                    isOnline = false,
                    preferredLanguage = app.preferredLanguage.ifBlank { "en" }
                )
                cachedRider = r
                return r
            }
            null
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    suspend fun setOnlineStatus(isOnline: Boolean): Result<Unit> {
        val uid = requireUserId()
        val path = "riders/$uid"
        return try {
            val docRef = db.collection("riders").document(uid)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val app = getRiderApplication(uid)
                if (app != null && app.status == "approved") {
                    val riderPayload = hashMapOf<String, Any>(
                        "userId" to uid,
                        "fullName" to app.fullName,
                        "phone" to app.phone,
                        "email" to app.email,
                        "cnicNumber" to app.cnicNumber,
                        "status" to "approved",
                        "isOnline" to isOnline,
                        "preferredLanguage" to app.preferredLanguage.ifBlank { "en" },
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(riderPayload).await()
                    cachedRider = cachedRider?.copy(isOnline = isOnline)
                    return Result.success(Unit)
                } else {
                    return Result.failure(IllegalStateException("Rider account is not approved."))
                }
            }

            // Save unnecessary write if already at requested status
            val currentOnline = snapshot.getBoolean("isOnline") ?: snapshot.getBoolean("online") ?: false
            if (currentOnline == isOnline) {
                cachedRider = cachedRider?.copy(isOnline = isOnline)
                return Result.success(Unit)
            }

            docRef.update(
                mapOf(
                    "isOnline" to isOnline,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            cachedRider = cachedRider?.copy(isOnline = isOnline)
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // Admin methods
    suspend fun getAllApplications(): List<RiderApplication> {
        val now = System.currentTimeMillis()
        val current = cachedApplications
        if (current != null && (now - lastApplicationsFetchMs) < 15_000L) {
            return current
        }
        val path = "riderApplications"
        return try {
            val snapshot = db.collection(path).limit(100).get().await()
            val list = snapshot.toObjects(RiderApplication::class.java)
            cachedApplications = list
            lastApplicationsFetchMs = now
            list
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            cachedApplications ?: emptyList()
        }
    }

    suspend fun approveApplication(application: RiderApplication, adminUid: String): Result<Unit> {
        val path = "riderApplications/${application.userId}"
        return try {
            // Update application status
            db.collection("riderApplications").document(application.userId).update(
                mapOf(
                    "status" to "approved",
                    "reviewedAt" to FieldValue.serverTimestamp(),
                    "reviewedBy" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Create rider record
            val riderPayload = hashMapOf<String, Any>(
                "userId" to application.userId,
                "fullName" to application.fullName,
                "phone" to application.phone,
                "email" to application.email,
                "cnicNumber" to application.cnicNumber,
                "status" to "approved",
                "isOnline" to false,
                "preferredLanguage" to application.preferredLanguage,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("riders").document(application.userId).set(riderPayload).await()

            // Update user role to rider
            try {
                db.collection("users").document(application.userId).update(
                    mapOf(
                        "role" to "rider",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (_: Exception) {
                // If user document didn't exist yet, create it
                db.collection("users").document(application.userId).set(
                    mapOf(
                        "userId" to application.userId,
                        "name" to application.fullName,
                        "email" to application.email,
                        "phone" to application.phone,
                        "role" to "rider",
                        "preferredLanguage" to application.preferredLanguage,
                        "accountStatus" to "active",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun rejectApplication(userId: String, reason: String, adminUid: String): Result<Unit> {
        val path = "riderApplications/$userId"
        return try {
            db.collection("riderApplications").document(userId).update(
                mapOf(
                    "status" to "rejected",
                    "rejectionReason" to reason,
                    "reviewedAt" to FieldValue.serverTimestamp(),
                    "reviewedBy" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun getAllApprovedRiders(): List<Rider> {
        val now = System.currentTimeMillis()
        val current = cachedApprovedRiders
        if (current != null && (now - lastApprovedRidersFetchMs) < 15_000L) {
            return current
        }
        val path = "riders"
        return try {
            val snapshot = db.collection(path).limit(100).get().await()
            val list = snapshot.documents.mapNotNull { it.toRiderSafe() }
            cachedApprovedRiders = list
            lastApprovedRidersFetchMs = now
            list
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            cachedApprovedRiders ?: emptyList()
        }
    }
}
