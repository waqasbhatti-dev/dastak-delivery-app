package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class User(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val role: String = "customer", // customer, rider, admin
    val preferredLanguage: String = "en", // en, ur
    val accountStatus: String = "active", // active, suspended
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class RiderApplication(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val cnicNumber: String = "",
    val cnicFrontImageUrl: String = "",
    val cnicBackImageUrl: String = "",
    val preferredLanguage: String = "en",
    val status: String = "pending", // pending, approved, rejected
    val rejectionReason: String = "",
    @ServerTimestamp val submittedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val reviewedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val reviewedBy: String = ""
)

@IgnoreExtraProperties
data class Rider(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val phone: String = "",
    val email: String = "",
    val cnicNumber: String = "",
    val status: String = "approved", // approved, suspended
    @get:PropertyName("isOnline") @set:PropertyName("isOnline") @PropertyName("isOnline") var isOnline: Boolean = false,
    val preferredLanguage: String = "en",
    val activeOrderId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TermsVersion(
    @DocumentId val id: String = "v1",
    val version: String = "1.0",
    val titleEn: String = "Terms & Conditions - Dastak Delivery",
    val titleUr: String = "دستک ڈیلیوری سروس - شرائط و ضوابط",
    val contentEn: String = "",
    val contentUr: String = "",
    val isActive: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TermsAcceptance(
    @DocumentId val id: String = "",
    val userId: String = "",
    val termsVersion: String = "1.0",
    val language: String = "en",
    @ServerTimestamp val acceptedAt: Timestamp? = null,
    val ipOrDeviceId: String = ""
)

@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val assignedRiderId: String = "",
    val itemDescription: String = "",
    val itemPhotoUrls: List<String> = emptyList(),
    val voiceAudioUrl: String = "",
    val customerPhone: String = "",
    val customerLatitude: Double = 0.0,
    val customerLongitude: Double = 0.0,
    val centralLatitude: Double = 32.9957231,
    val centralLongitude: Double = 73.6522227,
    val deliveryBlock: String = "Block A",
    val deliveryStreet: String = "",
    val deliveryHouseNumber: String = "",
    val additionalAddressInstructions: String = "",
    val orderStatus: String = "Pending", // Pending, Assigned, Shopping, Awaiting Customer Bill Approval, Bill Rejected, Bill Approved, Out for Delivery, Delivered, Cancelled
    val estimatedItemCost: Double = 0.0,
    val billAmount: Double = 0.0,
    val billPhotoUrl: String = "",
    val purchasedItemsDescription: String = "",
    val substitutionsNotes: String = "",
    val riderNotes: String = "",
    val billRejectionReason: String = "",
    val billApprovalStatus: String = "none", // none, pending_approval, approved, rejected
    val deliveryDistanceKm: Double = 0.0,
    val deliveryCharge: Double = 0.0, // distanceKm * 70
    val serviceCharge: Double = 0.0, // 5% or 7% if bill < 300
    val platformFee: Double = 4.0, // PKR 4
    val totalAmount: Double = 4.0, // billAmount + serviceCharge + deliveryCharge + platformFee
    val paymentMethod: String = "cash_on_delivery", // cash_on_delivery, easypaisa_qr
    val paymentStatus: String = "unpaid", // unpaid, payment_uploaded, verified, rejected
    val paymentScreenshotUrl: String = "",
    val paymentAmountPaid: Double = 0.0,
    val paymentRejectionReason: String = "",
    @ServerTimestamp val paymentReviewedAt: Timestamp? = null,
    val paymentReviewedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    @ServerTimestamp val riderAcceptedAt: Timestamp? = null,
    @ServerTimestamp val deliveryStartedAt: Timestamp? = null,
    val hasArrivedOutside: Boolean = false,
    @ServerTimestamp val arrivedOutsideAt: Timestamp? = null,
    val arrivalAnnounced: Boolean = false,
    @ServerTimestamp val billSubmittedAt: Timestamp? = null,
    @ServerTimestamp val billApprovedAt: Timestamp? = null,
    @ServerTimestamp val billRejectedAt: Timestamp? = null,
    @ServerTimestamp val deliveredAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Complaint(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userRole: String = "customer",
    val orderId: String = "",
    val category: String = "",
    val subject: String = "",
    val description: String = "",
    val status: String = "Open", // Open, In Progress, Resolved
    val adminResponse: String = "",
    @ServerTimestamp val respondedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FeedbackItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderId: String = "",
    val riderId: String = "",
    val rating: Int = 5,
    val comments: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AdminSettings(
    @DocumentId val id: String = "global",
    val authorizedAdminEmail: String = "mwaqasbhatti1230@gmail.com",
    val centralLatitude: Double = 32.9957231,
    val centralLongitude: Double = 73.6522227,
    val locality: String = "Jhelum City Housing",
    val termsVersion: String = "1.0",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Advertisement(
    @DocumentId val id: String = "",
    val title: String = "",
    val businessName: String = "",
    val category: String = "General",
    val description: String = "",
    val offer: String = "",
    val price: Double? = null,
    val phone: String = "03049457463",
    val mediaUrl: String = "",
    val mediaType: String = "IMAGE", // "IMAGE" or "VIDEO"
    val posterUrl: String = "",
    val isActive: Boolean = true,
    val status: String = "PUBLISHED", // DRAFT, PUBLISHED, PAUSED, ARCHIVED
    val startDate: Long? = null,
    val endDate: Long? = null,
    val viewsCount: Long = 0L,
    val clicksCount: Long = 0L,
    val createdByEmail: String = "mwaqasbhatti1230@gmail.com",
    val updatedByEmail: String = "mwaqasbhatti1230@gmail.com",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isScheduleValid(now: Long = System.currentTimeMillis()): Boolean {
        if (!isActive || status != "PUBLISHED") return false
        if (startDate != null && now < startDate) return false
        if (endDate != null && now > endDate) return false
        return true
    }
}

