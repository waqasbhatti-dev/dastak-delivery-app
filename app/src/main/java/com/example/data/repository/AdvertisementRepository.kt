package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.Advertisement
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID

import com.example.data.FirestoreProvider
import java.util.concurrent.ConcurrentHashMap

class AdvertisementRepository(
    private val db: FirebaseFirestore,
    private val appContext: Context? = null
) {
    constructor(context: Context) : this(
        FirestoreProvider.get(context),
        context.applicationContext
    )

    private val auth = Firebase.auth
    private val collection = db.collection("advertisements")

    private val viewedAdIdsInSession = ConcurrentHashMap.newKeySet<String>()
    private val clickedAdIdsInSession = ConcurrentHashMap.newKeySet<String>()

    companion object {
        const val AUTHORIZED_ADMIN_EMAIL = "mwaqasbhatti1230@gmail.com"
        const val DEFAULT_DASTAK_PHONE = "03049457463"
        const val MAX_VIDEO_SIZE_BYTES = 25 * 1024 * 1024L // 25 MB
        const val MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024L  // 5 MB

        fun getInitialDastakPromotion(context: Context? = null): Advertisement {
            val pkg = context?.packageName ?: "com.aistudio.dastak.jhlm"
            val heroUri = "android.resource://$pkg/drawable/hero_delivery"
            return Advertisement(
                id = "dastak_initial_promo",
                title = "Dastak Delivery Service / دستک ڈیلیوری سروس",
                businessName = "Dastak (دستک)",
                category = "Delivery & Logistics",
                description = "Serving Jhelum City Housing with fast doorstep delivery of groceries, food, and essentials. No fixed catalog—request whatever you need! App Coming Soon to Play Store.",
                offer = "Fast & Reliable Neighborhood Delivery",
                price = null,
                phone = DEFAULT_DASTAK_PHONE,
                mediaUrl = heroUri,
                mediaType = "IMAGE",
                posterUrl = "",
                isActive = true,
                status = "PUBLISHED",
                startDate = null,
                endDate = null,
                viewsCount = 12,
                clicksCount = 5,
                createdByEmail = AUTHORIZED_ADMIN_EMAIL,
                updatedByEmail = AUTHORIZED_ADMIN_EMAIL
            )
        }
    }

    private fun checkAdminPrivilege() {
        val email = auth.currentUser?.email
        val isAuthorized = email != null && email.equals(AUTHORIZED_ADMIN_EMAIL, ignoreCase = true)
        if (!isAuthorized) {
            throw SecurityException("Unauthorized: Only authorized admin can manage advertisements.")
        }
    }

    /**
     * Real-time stream of all active and published advertisements for customers.
     * Evaluates active status and scheduling dates.
     * Uses lifecycle-aware callbackFlow with query limit to avoid excessive reads.
     */
    fun observeActiveAdvertisements(): Flow<List<Advertisement>> = callbackFlow {
        val registration = collection
            .whereEqualTo("isActive", true)
            .whereEqualTo("status", "PUBLISHED")
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(listOf(getInitialDastakPromotion(appContext)))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val now = System.currentTimeMillis()
                    val list = snapshot.toObjects(Advertisement::class.java).filter { ad ->
                        ad.isScheduleValid(now)
                    }
                    if (list.isEmpty()) {
                        trySend(listOf(getInitialDastakPromotion(appContext)))
                    } else {
                        trySend(list)
                    }
                }
            }
        awaitClose { registration.remove() }
    }

    /**
     * Real-time stream of advertisements for Admin portal with reasonable limit.
     */
    fun observeAllAdvertisementsForAdmin(): Flow<List<Advertisement>> = callbackFlow {
        val registration = collection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(listOf(getInitialDastakPromotion(appContext)))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.toObjects(Advertisement::class.java)
                    if (list.isEmpty()) {
                        trySend(listOf(getInitialDastakPromotion(appContext)))
                    } else {
                        trySend(list)
                    }
                }
            }
        awaitClose { registration.remove() }
    }

    /**
     * Creates a new advertisement (Admin only).
     */
    suspend fun createAdvertisement(ad: Advertisement): Result<String> {
        return try {
            checkAdminPrivilege()
            val docRef = collection.document()
            val currentAdminEmail = auth.currentUser?.email ?: AUTHORIZED_ADMIN_EMAIL
            val finalAd = ad.copy(
                id = docRef.id,
                createdByEmail = currentAdminEmail,
                updatedByEmail = currentAdminEmail
            )

            val dataMap = hashMapOf<String, Any?>(
                "title" to finalAd.title.trim(),
                "businessName" to finalAd.businessName.trim(),
                "category" to finalAd.category.trim(),
                "description" to finalAd.description.trim(),
                "offer" to finalAd.offer.trim(),
                "price" to finalAd.price,
                "phone" to finalAd.phone.trim(),
                "mediaUrl" to finalAd.mediaUrl.trim(),
                "mediaType" to finalAd.mediaType,
                "posterUrl" to finalAd.posterUrl.trim(),
                "isActive" to finalAd.isActive,
                "status" to finalAd.status,
                "startDate" to finalAd.startDate,
                "endDate" to finalAd.endDate,
                "viewsCount" to 0L,
                "clicksCount" to 0L,
                "createdByEmail" to currentAdminEmail,
                "updatedByEmail" to currentAdminEmail,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            docRef.set(dataMap).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "advertisements")
            Result.failure(e)
        }
    }

    /**
     * Updates an existing advertisement (Admin only).
     */
    suspend fun updateAdvertisement(ad: Advertisement): Result<Unit> {
        return try {
            checkAdminPrivilege()
            require(ad.id.isNotBlank()) { "Advertisement ID must not be blank" }

            val updateMap = hashMapOf<String, Any?>(
                "title" to ad.title.trim(),
                "businessName" to ad.businessName.trim(),
                "category" to ad.category.trim(),
                "description" to ad.description.trim(),
                "offer" to ad.offer.trim(),
                "price" to ad.price,
                "phone" to ad.phone.trim(),
                "mediaUrl" to ad.mediaUrl.trim(),
                "mediaType" to ad.mediaType,
                "posterUrl" to ad.posterUrl.trim(),
                "isActive" to ad.isActive,
                "status" to ad.status,
                "startDate" to ad.startDate,
                "endDate" to ad.endDate,
                "updatedByEmail" to (auth.currentUser?.email ?: AUTHORIZED_ADMIN_EMAIL),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            collection.document(ad.id).update(updateMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "advertisements/${ad.id}")
            Result.failure(e)
        }
    }

    /**
     * Deletes an advertisement (Admin only).
     */
    suspend fun deleteAdvertisement(adId: String): Result<Unit> {
        return try {
            checkAdminPrivilege()
            collection.document(adId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "advertisements/$adId")
            Result.failure(e)
        }
    }

    /**
     * Toggles active status (Admin only).
     */
    suspend fun toggleAdActive(adId: String, isActive: Boolean): Result<Unit> {
        return try {
            checkAdminPrivilege()
            collection.document(adId).update(
                mapOf(
                    "isActive" to isActive,
                    "updatedByEmail" to AUTHORIZED_ADMIN_EMAIL,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "advertisements/$adId")
            Result.failure(e)
        }
    }

    /**
     * Updates publication status: DRAFT, PUBLISHED, PAUSED, ARCHIVED (Admin only).
     */
    suspend fun updateAdStatus(adId: String, newStatus: String): Result<Unit> {
        return try {
            checkAdminPrivilege()
            val isActive = (newStatus == "PUBLISHED")
            collection.document(adId).update(
                mapOf(
                    "status" to newStatus,
                    "isActive" to isActive,
                    "updatedByEmail" to AUTHORIZED_ADMIN_EMAIL,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "advertisements/$adId")
            Result.failure(e)
        }
    }

    /**
     * Increments view count for an advertisement.
     * Deduplicated per user session to avoid continuous writes during banner rotation.
     */
    suspend fun recordView(adId: String) {
        if (adId.isBlank() || adId == "dastak_initial_promo") return
        if (!viewedAdIdsInSession.add(adId)) return // Only write once per session
        try {
            collection.document(adId).update("viewsCount", FieldValue.increment(1)).await()
        } catch (_: Exception) {
            // Non-critical background telemetry
        }
    }

    /**
     * Increments call / click count for an advertisement.
     * Deduplicated per user session to save unnecessary writes.
     */
    suspend fun recordClick(adId: String) {
        if (adId.isBlank() || adId == "dastak_initial_promo") return
        if (!clickedAdIdsInSession.add(adId)) return // Only write once per session
        try {
            collection.document(adId).update("clicksCount", FieldValue.increment(1)).await()
        } catch (_: Exception) {
            // Non-critical background telemetry
        }
    }

    /**
     * Uploads media file to Firebase Storage with progress tracking.
     * If Firebase Storage is unprovisioned, compresses image to data URI or returns helpful error.
     */
    suspend fun uploadMedia(
        context: Context,
        uri: Uri,
        isVideo: Boolean,
        onProgress: (Int) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: if (isVideo) "video/mp4" else "image/jpeg"

            // Check file size
            var fileSize = 0L
            contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                fileSize = pfd.statSize
            }

            if (isVideo && fileSize > MAX_VIDEO_SIZE_BYTES) {
                return@withContext Result.failure(
                    IllegalArgumentException("Video exceeds 25MB limit (size: ${fileSize / (1024 * 1024)}MB)")
                )
            }
            if (!isVideo && fileSize > MAX_IMAGE_SIZE_BYTES) {
                return@withContext Result.failure(
                    IllegalArgumentException("Image exceeds 5MB limit (size: ${fileSize / (1024 * 1024)}MB)")
                )
            }

            // Attempt Firebase Storage upload
            val filename = "${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
            val ext = if (isVideo) "mp4" else "jpg"
            val storageRef = FirebaseStorage.getInstance().reference.child("advertisements/$filename.$ext")

            onProgress(10)

            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            if (inputStream == null) {
                return@withContext Result.failure(IllegalArgumentException("Could not read selected media file"))
            }

            var storageSucceeded = false
            var downloadUrl = ""

            try {
                val uploadTask = storageRef.putStream(inputStream)
                uploadTask.addOnProgressListener { taskSnapshot ->
                    val total = taskSnapshot.totalByteCount
                    val transferred = taskSnapshot.bytesTransferred
                    if (total > 0) {
                        val progress = ((transferred * 100) / total).toInt().coerceIn(10, 95)
                        onProgress(progress)
                    }
                }

                uploadTask.await()
                downloadUrl = storageRef.downloadUrl.await().toString()
                storageSucceeded = true
                onProgress(100)
            } catch (storageEx: Exception) {
                // If storage bucket is not enabled or fails, fallback for image
                if (!isVideo) {
                    val fallbackInputStream = contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(fallbackInputStream)
                    if (bitmap != null) {
                        val out = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out)
                        val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                        onProgress(100)
                        return@withContext Result.success("data:image/jpeg;base64,$base64")
                    }
                }
                return@withContext Result.failure(
                    Exception("Firebase Storage upload error: ${storageEx.localizedMessage}. You can also paste a direct media URL below.")
                )
            }

            if (storageSucceeded && downloadUrl.isNotBlank()) {
                Result.success(downloadUrl)
            } else {
                Result.failure(Exception("Could not retrieve upload URL"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
