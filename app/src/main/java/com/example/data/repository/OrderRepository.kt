package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.PricingCalculator
import com.example.data.handleFirestoreError
import com.example.data.model.AdminSettings
import com.example.data.model.Complaint
import com.example.data.model.FeedbackItem
import com.example.data.model.Order
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

import com.example.data.FirestoreProvider
import com.google.firebase.firestore.Query

class OrderRepository(private val db: FirebaseFirestore) {
    constructor(context: Context) : this(FirestoreProvider.get(context))

    private val auth = Firebase.auth

    @Volatile
    private var cachedAdminOrders: List<Order>? = null
    private var lastAdminOrdersFetchMs: Long = 0L

    @Volatile
    private var cachedAdminComplaints: List<Complaint>? = null
    private var lastAdminComplaintsFetchMs: Long = 0L

    @Volatile
    private var cachedAdminFeedback: List<FeedbackItem>? = null
    private var lastAdminFeedbackFetchMs: Long = 0L

    @Volatile
    private var cachedCentralSettings: AdminSettings? = null

    private fun isOrderAdmin(email: String?): Boolean =
        email != null && email.equals("mwaqasbhatti1230@gmail.com", ignoreCase = true)

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google")
    }

    suspend fun createOrder(order: Order): Result<String> {
        val uid = requireUserId()
        val path = "orders"
        if (order.customerLatitude == 0.0 && order.customerLongitude == 0.0) {
            return Result.failure(IllegalArgumentException("Customer delivery location coordinates are required before submitting an order"))
        }
        if (order.deliveryDistanceKm <= 0.0) {
            return Result.failure(IllegalArgumentException("Real road route distance calculation is required"))
        }
        return try {
            val docRef = db.collection(path).document()
            val distance = Math.round(order.deliveryDistanceKm * 100.0) / 100.0
            val deliveryCharge = PricingCalculator.calculateDeliveryCharge(distance)
            val platformFee = PricingCalculator.PLATFORM_FEE

            val payload = hashMapOf<String, Any>(
                "customerId" to uid,
                "customerName" to order.customerName,
                "assignedRiderId" to "",
                "itemDescription" to order.itemDescription,
                "itemPhotoUrls" to order.itemPhotoUrls,
                "voiceAudioUrl" to order.voiceAudioUrl,
                "customerPhone" to order.customerPhone,
                "customerLatitude" to order.customerLatitude,
                "customerLongitude" to order.customerLongitude,
                "centralLatitude" to (if (order.centralLatitude != 0.0) order.centralLatitude else 32.9957231),
                "centralLongitude" to (if (order.centralLongitude != 0.0) order.centralLongitude else 73.6522227),
                "deliveryBlock" to order.deliveryBlock,
                "deliveryStreet" to order.deliveryStreet,
                "deliveryHouseNumber" to order.deliveryHouseNumber,
                "additionalAddressInstructions" to order.additionalAddressInstructions,
                "orderStatus" to "Pending",
                "estimatedItemCost" to 0.0,
                "billAmount" to 0.0,
                "billPhotoUrl" to "",
                "purchasedItemsDescription" to "",
                "substitutionsNotes" to "",
                "riderNotes" to "",
                "billRejectionReason" to "",
                "billApprovalStatus" to "none",
                "deliveryDistanceKm" to distance,
                "deliveryCharge" to deliveryCharge,
                "serviceCharge" to 0.0,
                "platformFee" to platformFee,
                "totalAmount" to 0.0,
                "paymentMethod" to order.paymentMethod,
                "paymentStatus" to order.paymentStatus,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeCustomerOrders(): Flow<List<Order>> = flow {
        val uid = requireUserId()
        val path = "orders"
        try {
            val stream = db.collection(path)
                .whereEqualTo("customerId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .map { snap -> snap.toObjects(Order::class.java) }
            emitAll(stream)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    suspend fun getCustomerOrders(
        limit: Long = 50,
        startAfterTimestamp: com.google.firebase.Timestamp? = null
    ): List<Order> {
        val uid = requireUserId()
        val path = "orders"
        return try {
            var query = db.collection(path)
                .whereEqualTo("customerId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
            if (startAfterTimestamp != null) {
                query = query.startAfter(startAfterTimestamp)
            }
            val snapshot = query.limit(limit).get().await()
            snapshot.toObjects(Order::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            emptyList()
        }
    }

    fun observeUnassignedOrders(): Flow<List<Order>> = callbackFlow {
        val path = "orders"
        val registration = db.collection(path)
            .whereEqualTo("orderStatus", "Pending")
            .whereEqualTo("assignedRiderId", "")
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Order::class.java)
                        } catch (_: Exception) {
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun getUnassignedOrders(): List<Order> {
        val path = "orders"
        return try {
            val snapshot = db.collection(path)
                .whereEqualTo("orderStatus", "Pending")
                .whereEqualTo("assignedRiderId", "")
                .limit(50)
                .get()
                .await()
            snapshot.toObjects(Order::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            emptyList()
        }
    }

    // Atomic transaction to claim order (Only approved riders can claim)
    suspend fun claimOrder(orderId: String): Result<Unit> {
        val riderUid = requireUserId()
        val docRef = db.collection("orders").document(orderId)
        return try {
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) {
                    throw IllegalStateException("Order does not exist")
                }
                val currentStatus = snapshot.getString("orderStatus")
                val currentRider = snapshot.getString("assignedRiderId") ?: ""

                if (currentStatus != "Pending" || currentRider.isNotBlank()) {
                    throw IllegalStateException("Order has already been claimed by another rider")
                }

                // Verify that the rider account is approved
                val riderDoc = transaction.get(db.collection("riders").document(riderUid))
                if (!riderDoc.exists() || riderDoc.getString("status") != "approved") {
                    throw IllegalStateException("Only approved riders are permitted to accept orders.")
                }

                transaction.update(
                    docRef,
                    mapOf(
                        "assignedRiderId" to riderUid,
                        "orderStatus" to "Assigned",
                        "riderAcceptedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeRiderAssignedOrders(): Flow<List<Order>> = callbackFlow {
        val riderUid = try {
            requireUserId()
        } catch (_: Exception) {
            close()
            return@callbackFlow
        }
        val path = "orders"
        val registration = db.collection(path)
            .whereEqualTo("assignedRiderId", riderUid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Order::class.java)
                        } catch (_: Exception) {
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun getRiderActiveOrders(
        limit: Long = 50,
        startAfterTimestamp: com.google.firebase.Timestamp? = null
    ): List<Order> {
        val riderUid = requireUserId()
        val path = "orders"
        return try {
            var query = db.collection(path)
                .whereEqualTo("assignedRiderId", riderUid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
            if (startAfterTimestamp != null) {
                query = query.startAfter(startAfterTimestamp)
            }
            val snapshot = query.limit(limit).get().await()
            snapshot.toObjects(Order::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            emptyList()
        }
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: String): Result<Unit> {
        val docRef = db.collection("orders").document(orderId)
        val uid = requireUserId()

        try {
            val snapshot = docRef.get().await()
            val currentOrder = snapshot.toObject(Order::class.java)
            if (currentOrder != null) {
                if (currentOrder.orderStatus == newStatus) {
                    return Result.success(Unit)
                }
                val isAdminUser = isOrderAdmin(auth.currentUser?.email)
                if (newStatus == "Out for Delivery") {
                    if (currentOrder.assignedRiderId != uid && !isAdminUser) {
                        return Result.failure(IllegalStateException("Only the assigned rider can update this order status"))
                    }
                    if (currentOrder.billApprovalStatus != "approved") {
                        return Result.failure(IllegalStateException("Customer has not yet approved the purchase bill. Cannot start delivery journey."))
                    }
                } else if (newStatus == "Delivered") {
                    if (currentOrder.assignedRiderId != uid && !isAdminUser) {
                        return Result.failure(IllegalStateException("Only the assigned rider can mark this order as Delivered"))
                    }
                    if (currentOrder.orderStatus != "Out for Delivery" && currentOrder.orderStatus != "Arrived Outside") {
                        return Result.failure(IllegalStateException("Order must be Out for Delivery or Arrived Outside before marking Delivered"))
                    }
                    if (currentOrder.billApprovalStatus != "approved") {
                        return Result.failure(IllegalStateException("Customer must accept the bill before delivery can be completed"))
                    }
                }
            }
        } catch (e: Exception) {
            if (e is IllegalStateException) return Result.failure(e)
        }

        val updateMap = mutableMapOf<String, Any>(
            "orderStatus" to newStatus,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (newStatus == "Out for Delivery") {
            updateMap["deliveryStartedAt"] = FieldValue.serverTimestamp()
        } else if (newStatus == "Delivered") {
            updateMap["deliveredAt"] = FieldValue.serverTimestamp()
        }

        return try {
            docRef.update(updateMap).await()
            cachedAdminOrders = cachedAdminOrders?.map { if (it.id == orderId) it.copy(orderStatus = newStatus) else it }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markRiderArrivedOutside(orderId: String): Result<Unit> {
        val docRef = db.collection("orders").document(orderId)
        val uid = requireUserId()
        return try {
            val snap = docRef.get().await()
            val currentOrder = snap.toObject(Order::class.java)
            if (currentOrder != null) {
                val isAdminUser = isOrderAdmin(auth.currentUser?.email)
                if (currentOrder.assignedRiderId != uid && !isAdminUser) {
                    return Result.failure(IllegalStateException("Only assigned rider can announce arrival outside."))
                }
                // Prevent duplicate taps from triggering duplicate arrival events
                if (currentOrder.hasArrivedOutside) {
                    return Result.success(Unit)
                }
            }

            val updateMap = mapOf(
                "orderStatus" to "Arrived Outside",
                "hasArrivedOutside" to true,
                "arrivedOutsideAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updateMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun submitRiderBill(
        orderId: String,
        billAmount: Double,
        billPhotoUrl: String,
        purchasedDesc: String,
        substitutions: String,
        notes: String,
        distanceKm: Double = 0.0
    ): Result<Unit> {
        val docRef = db.collection("orders").document(orderId)
        return try {
            val snap = docRef.get().await()
            val savedDistance = snap.getDouble("deliveryDistanceKm") ?: (if (distanceKm > 0) distanceKm else 1.0)
            val sc = PricingCalculator.calculateServiceCharge(billAmount)
            val dc = PricingCalculator.calculateDeliveryCharge(savedDistance)
            val total = PricingCalculator.calculateTotal(billAmount, savedDistance)

            val updateMap = mapOf(
                "orderStatus" to "Awaiting Customer Bill Approval",
                "billApprovalStatus" to "pending_approval",
                "billAmount" to billAmount,
                "billPhotoUrl" to billPhotoUrl,
                "purchasedItemsDescription" to purchasedDesc,
                "substitutionsNotes" to substitutions,
                "riderNotes" to notes,
                "deliveryDistanceKm" to savedDistance,
                "deliveryCharge" to dc,
                "serviceCharge" to sc,
                "platformFee" to PricingCalculator.PLATFORM_FEE,
                "totalAmount" to total,
                "billSubmittedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updateMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun reviewCustomerBill(
        orderId: String,
        isApproved: Boolean,
        rejectionReason: String = ""
    ): Result<Unit> {
        val docRef = db.collection("orders").document(orderId)
        val updateMap = if (isApproved) {
            mapOf(
                "orderStatus" to "Bill Approved",
                "billApprovalStatus" to "approved",
                "billApprovedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        } else {
            mapOf(
                "orderStatus" to "Bill Rejected",
                "billApprovalStatus" to "rejected",
                "billRejectionReason" to rejectionReason,
                "billRejectedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        }

        return try {
            docRef.update(updateMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getAllOrdersForAdmin(
        forceRefresh: Boolean = false,
        limit: Long = 100,
        startAfterTimestamp: com.google.firebase.Timestamp? = null
    ): List<Order> {
        val now = System.currentTimeMillis()
        val current = cachedAdminOrders
        if (!forceRefresh && startAfterTimestamp == null && current != null && (now - lastAdminOrdersFetchMs) < 15_000L) {
            return current
        }
        val path = "orders"
        return try {
            var query = db.collection(path)
                .orderBy("createdAt", Query.Direction.DESCENDING)
            if (startAfterTimestamp != null) {
                query = query.startAfter(startAfterTimestamp)
            }
            val snapshot = query.limit(limit).get().await()
            val list = snapshot.toObjects(Order::class.java)
            if (startAfterTimestamp == null) {
                cachedAdminOrders = list
                lastAdminOrdersFetchMs = now
            }
            list
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            if (startAfterTimestamp == null) (cachedAdminOrders ?: emptyList()) else emptyList()
        }
    }

    suspend fun submitComplaint(complaint: Complaint): Result<String> {
        val uid = requireUserId()
        val path = "complaints"
        return try {
            val docRef = db.collection(path).document()
            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "userRole" to complaint.userRole,
                "subject" to complaint.subject,
                "description" to complaint.description,
                "status" to "Open",
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (complaint.category.isNotBlank()) {
                payload["category"] = complaint.category
            }
            if (complaint.orderId.isNotBlank()) {
                payload["orderId"] = complaint.orderId
            }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateComplaintStatus(
        complaintId: String,
        status: String,
        adminResponse: String = ""
    ): Result<Unit> {
        val docRef = db.collection("complaints").document(complaintId)
        val updateMap = mutableMapOf<String, Any>(
            "status" to status,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (adminResponse.isNotBlank()) {
            updateMap["adminResponse"] = adminResponse
            updateMap["respondedAt"] = FieldValue.serverTimestamp()
        }
        return try {
            docRef.update(updateMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getUserComplaints(): List<Complaint> {
        val uid = requireUserId()
        val path = "complaints"
        return try {
            val snapshot = db.collection(path)
                .whereEqualTo("userId", uid)
                .limit(50)
                .get()
                .await()
            snapshot.toObjects(Complaint::class.java).sortedByDescending { it.createdAt?.seconds ?: 0L }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            emptyList()
        }
    }

    suspend fun getAllComplaintsForAdmin(forceRefresh: Boolean = false): List<Complaint> {
        val now = System.currentTimeMillis()
        val current = cachedAdminComplaints
        if (!forceRefresh && current != null && (now - lastAdminComplaintsFetchMs) < 15_000L) {
            return current
        }
        val path = "complaints"
        return try {
            val snapshot = db.collection(path)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()
            val list = snapshot.toObjects(Complaint::class.java)
            cachedAdminComplaints = list
            lastAdminComplaintsFetchMs = now
            list
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            cachedAdminComplaints ?: emptyList()
        }
    }

    suspend fun submitFeedback(
        rating: Int,
        comments: String,
        orderId: String = "",
        riderId: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val path = "feedback"
        return try {
            if (orderId.isNotBlank()) {
                val existing = db.collection(path)
                    .whereEqualTo("userId", uid)
                    .whereEqualTo("orderId", orderId)
                    .get()
                    .await()
                if (!existing.isEmpty) {
                    return Result.failure(IllegalStateException("Feedback already submitted for this order"))
                }
            }

            val docRef = db.collection(path).document()
            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "rating" to rating,
                "comments" to comments,
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (orderId.isNotBlank()) {
                payload["orderId"] = orderId
            }
            if (riderId.isNotBlank()) {
                payload["riderId"] = riderId
            }
            docRef.set(payload).await()
            lastAdminFeedbackFetchMs = 0L
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun getUserFeedback(): List<FeedbackItem> {
        val uid = requireUserId()
        val path = "feedback"
        return try {
            val snapshot = db.collection(path)
                .whereEqualTo("userId", uid)
                .limit(50)
                .get()
                .await()
            snapshot.toObjects(FeedbackItem::class.java).sortedByDescending { it.createdAt?.seconds ?: 0L }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            emptyList()
        }
    }

    suspend fun getAllFeedbackForAdmin(forceRefresh: Boolean = false): List<FeedbackItem> {
        val now = System.currentTimeMillis()
        val current = cachedAdminFeedback
        if (!forceRefresh && current != null && (now - lastAdminFeedbackFetchMs) < 15_000L) {
            return current
        }
        val path = "feedback"
        return try {
            val snapshot = db.collection(path)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()
            val list = snapshot.toObjects(FeedbackItem::class.java)
            cachedAdminFeedback = list
            lastAdminFeedbackFetchMs = now
            list
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            cachedAdminFeedback ?: emptyList()
        }
    }

    suspend fun getCentralSettings(): AdminSettings {
        val current = cachedCentralSettings
        if (current != null) return current
        val path = "adminSettings"
        return try {
            val snap = db.collection(path).document("global").get().await()
            val settings = snap.toObject(AdminSettings::class.java) ?: AdminSettings()
            cachedCentralSettings = settings
            settings
        } catch (e: Exception) {
            cachedCentralSettings ?: AdminSettings()
        }
    }

    suspend fun updateCentralSettings(settings: AdminSettings): Result<Unit> {
        val path = "adminSettings"
        return try {
            db.collection(path).document("global").set(settings).await()
            cachedCentralSettings = settings
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitPaymentProof(
        orderId: String,
        paymentMethod: String,
        screenshotUrl: String,
        amountPaid: Double
    ): Result<Unit> {
        val docRef = db.collection("orders").document(orderId)
        val updateMap = mapOf(
            "paymentMethod" to paymentMethod,
            "paymentStatus" to "payment_uploaded",
            "paymentScreenshotUrl" to screenshotUrl,
            "paymentAmountPaid" to amountPaid,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updateMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun reviewPayment(
        orderId: String,
        isVerified: Boolean,
        rejectionReason: String = ""
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("orders").document(orderId)
        val updateMap = if (isVerified) {
            mapOf(
                "paymentStatus" to "verified",
                "paymentReviewedBy" to uid,
                "paymentReviewedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        } else {
            mapOf(
                "paymentStatus" to "rejected",
                "paymentRejectionReason" to rejectionReason,
                "paymentReviewedBy" to uid,
                "paymentReviewedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        }
        return try {
            docRef.update(updateMap).await()
            cachedAdminOrders = cachedAdminOrders?.map {
                if (it.id == orderId) it.copy(paymentStatus = if (isVerified) "verified" else "rejected") else it
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }
}
