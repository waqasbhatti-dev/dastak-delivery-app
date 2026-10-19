package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.TermsAcceptance
import com.example.data.model.TermsVersion
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

import com.example.data.FirestoreProvider
import java.util.concurrent.ConcurrentHashMap

class TermsRepository(private val db: FirebaseFirestore) {
    constructor(context: Context) : this(FirestoreProvider.get(context))

    private val auth = Firebase.auth

    @Volatile
    private var cachedActiveTerms: TermsVersion? = null

    private val acceptedTermsCache = ConcurrentHashMap.newKeySet<String>()

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google")
    }

    suspend fun getActiveTerms(): TermsVersion {
        val current = cachedActiveTerms
        if (current != null) return current

        val path = "termsVersions/v1"
        return try {
            val snapshot = db.collection("termsVersions").document("v1").get().await()
            val terms = snapshot.toObject(TermsVersion::class.java)
            val result = if (terms != null && terms.contentEn.isNotBlank()) {
                terms
            } else {
                defaultInitialTerms()
            }
            cachedActiveTerms = result
            result
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            defaultInitialTerms()
        }
    }

    suspend fun hasUserAcceptedActiveTerms(termsVersion: String): Boolean {
        val uid = requireUserId()
        val cacheKey = "${uid}_$termsVersion"
        if (acceptedTermsCache.contains(cacheKey)) {
            return true
        }

        val docPath = "termsAcceptances/$cacheKey"
        return try {
            // Point read by ID is cached locally and cheaper than querying collection
            val docSnap = db.collection("termsAcceptances").document(cacheKey).get().await()
            if (docSnap.exists()) {
                acceptedTermsCache.add(cacheKey)
                true
            } else {
                // Fallback for legacy auto-id acceptance documents if any exist
                val query = db.collection("termsAcceptances")
                    .whereEqualTo("userId", uid)
                    .whereEqualTo("termsVersion", termsVersion)
                    .limit(1)
                    .get()
                    .await()
                val accepted = !query.isEmpty
                if (accepted) {
                    acceptedTermsCache.add(cacheKey)
                }
                accepted
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            false
        }
    }

    suspend fun acceptTerms(termsVersion: String, language: String): Result<Unit> {
        val uid = requireUserId()
        val docId = "${uid}_$termsVersion"
        val path = "termsAcceptances/$docId"
        return try {
            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "termsVersion" to termsVersion,
                "language" to language,
                "acceptedAt" to FieldValue.serverTimestamp()
            )
            db.collection("termsAcceptances").document(docId).set(payload).await()
            acceptedTermsCache.add(docId)
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveTermsAsAdmin(terms: TermsVersion): Result<Unit> {
        val path = "termsVersions/v1"
        return try {
            val payload = hashMapOf<String, Any>(
                "version" to terms.version,
                "titleEn" to terms.titleEn,
                "titleUr" to terms.titleUr,
                "contentEn" to terms.contentEn,
                "contentUr" to terms.contentUr,
                "isActive" to terms.isActive,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("termsVersions").document("v1").set(payload).await()
            cachedActiveTerms = terms
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    companion object {
        fun defaultInitialTerms(): TermsVersion {
            val enText = """
1. Service Nature: Dastak is an on-demand courier and delivery service operating within Jhelum City Housing, not a pre-stocked catalog or warehouse.
2. Custom Item Requests: Customers can request required goods, groceries, medicines, or daily essentials by providing a clear written item description.
3. Accurate Delivery Details: Customers must provide a reachable phone number and precise residential address including Block, Street, and House Number in Jhelum City Housing.
4. Rider Onboarding: Prospective riders must complete mandatory verification by submitting valid Pakistani CNIC details and clear credentials for administrative vetting.
5. Authorized Deliveries Only: Only riders whose applications have received official administrative approval are authorized to receive and execute customer orders.
6. Receipt Verification: The rider purchases the requested items from local stores and uploads the merchant receipt or bill for customer review.
7. Customer Bill Approval: The customer must inspect and approve the purchase receipt amount before the rider proceeds to the customer's doorstep.
8. Transparent Tariff: Delivery charges, service charges, and platform fees are clearly calculated and displayed before checkout confirmation.
9. Mutual Respect: Customers and delivery riders must maintain respectful conduct and provide truthful details throughout every interaction.
10. Feedback & Complaints: Any concern, delay, or dispute can be submitted via the in-app Complaints and Feedback channel.
11. Data Privacy: Contact numbers, residential addresses, and identity documents are strictly confidential and protected from unauthorized misuse.
12. Security & Compliance: Account usage and delivery requests are subject to Dastak platform security rules and role-based permissions.
13. Policy Transparency: All cancellation and dispute rules are implemented transparently without unsupported legal guarantees.
""".trimIndent()

            val urText = """
۱۔ سروس کی نوعیت: دستک جہلم سٹی ہاؤسنگ میں ایک آن ڈیمانڈ ترسیل اور ڈیلیوری سروس ہے، یہ کوئی فکسڈ گودام یا پہلے سے اسٹاک شدہ دکان نہیں۔
۲۔ کسٹم فرمائش: صارفین اپنی ضرورت کا سامان، سودا سلف یا ادویات تحریری تفصیل درج کر کے منگوا سکتے ہیں۔
۳۔ درست پتہ اور فون: صارف کے لیے رابطہ نمبر اور جہلم سٹی ہاؤسنگ کا مکمل پتہ (بلاک، گلی، مکان نمبر) فراہم کرنا لازمی ہے۔
۴۔ رائیڈر رجسٹریشن: رائیڈرز کو قومی شناختی کارڈ (CNIC) کی تفصیلات جمع کروا کر انتظامیہ کی باقاعدہ تصدیق کا انتظار کرنا ہوگا۔
۵۔ صرف منظور شدہ رائیڈرز: صرف وہی رائیڈرز آرڈر وصول کر سکتے ہیں جن کی درخواست ایڈمنسٹریٹر نے منظور کر لی ہو۔
۶۔ بل اور رسید: رائیڈر خریداری کے بعد دکاندار کا اصلی بل یا رسید اپلوڈ کرے گا تاکہ صارف تصدیق کر سکے۔
۷۔ بل کی منظوری: رائیڈر کے ڈیلیوری کے لیے روانہ ہونے سے قبل صارف کو بل کی رقم دیکھ کر منظور کرنا لازمی ہے۔
۸۔ شفاف فیس: ڈیلیوری چارجز، سروس فیس اور پلیٹ فارم چارجز صارف کے سامنے بالکل واضح طور پر ظاہر کیے جائیں گے۔
۹۔ باہمی احترام: صارفین اور رائیڈرز کا ایک دوسرے سے باوقار، شائستہ اور درست معلومات پر مبنی رویہ رکھنا ضروری ہے۔
۱۰۔ شکایات اور رائے: کسی بھی مسئلے یا تاخیر کی صورت میں ایپ کے ذریعے شکایت اور رائے درج کروائی جا سکتی ہے۔
۱۱۔ ذاتی معلومات کا تحفظ: شناختی دستاویزات، فون نمبرز اور رہائشی پتے مکمل طور پر محفوظ ہیں اور ان کا غلط استعمال ممنوع ہے۔
۱۲۔ اکاؤنٹ کی سیکیورٹی: تمام اکاؤنٹ اور آرڈرز دستک پلیٹ فارم کے سیکیورٹی اور رازداری کے قواعد کے پابند ہیں۔
۱۳۔ شفاف پالیسی: کینسلیشن اور تمام قواعد واضح ہیں اور کوئی بھی غیر مصدقہ دعویٰ نہیں کیا جاتا۔
""".trimIndent()

            return TermsVersion(
                id = "v1",
                version = "1.0",
                titleEn = "Terms & Conditions - Dastak Delivery",
                titleUr = "دستک ڈیلیوری سروس - شرائط و ضوابط",
                contentEn = enText,
                contentUr = urText,
                isActive = true
            )
        }
    }
}
