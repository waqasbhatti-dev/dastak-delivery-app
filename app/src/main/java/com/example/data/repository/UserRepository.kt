package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.User
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import com.example.data.FirestoreProvider
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserRepository(private val db: FirebaseFirestore) {
    constructor(context: Context) : this(FirestoreProvider.get(context))

    private val auth = Firebase.auth

    @Volatile
    private var cachedProfile: User? = null

    @Volatile
    private var cachedCustomers: List<User>? = null
    private var lastCustomersFetchMs: Long = 0L

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google")
    }

    suspend fun getProfile(userId: String): User? {
        val currentCached = cachedProfile
        if (currentCached != null && currentCached.userId == userId) {
            return currentCached
        }
        val path = "users/$userId"
        return try {
            val snapshot = db.collection("users").document(userId).get().await()
            val profile = snapshot.toObject(User::class.java)
            if (profile != null) {
                cachedProfile = profile
            }
            profile
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    suspend fun saveInitialUser(user: User): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val data = hashMapOf<String, Any>(
                "userId" to uid,
                "email" to (user.email.ifBlank { auth.currentUser?.email ?: "" }),
                "name" to (user.name.ifBlank { auth.currentUser?.displayName ?: "User" }),
                "phone" to user.phone,
                "role" to user.role,
                "preferredLanguage" to user.preferredLanguage,
                "accountStatus" to "active",
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data).await()
            cachedProfile = user.copy(
                userId = uid,
                email = (user.email.ifBlank { auth.currentUser?.email ?: "" }),
                name = (user.name.ifBlank { auth.currentUser?.displayName ?: "User" })
            )
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateLanguage(language: String): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            docRef.update(
                mapOf(
                    "preferredLanguage" to language,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            cachedProfile = cachedProfile?.copy(preferredLanguage = language)
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(name: String, phone: String, language: String): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            docRef.update(
                mapOf(
                    "name" to name,
                    "phone" to phone,
                    "preferredLanguage" to language,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            cachedProfile = cachedProfile?.copy(name = name, phone = phone, preferredLanguage = language)
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun getAllCustomers(): List<User> {
        val now = System.currentTimeMillis()
        val currentCached = cachedCustomers
        if (currentCached != null && (now - lastCustomersFetchMs) < 15_000L) {
            return currentCached
        }
        val path = "users"
        return try {
            val snapshot = db.collection("users")
                .whereEqualTo("role", "customer")
                .limit(100)
                .get()
                .await()
            val list = snapshot.toObjects(User::class.java)
            cachedCustomers = list
            lastCustomersFetchMs = now
            list
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            cachedCustomers ?: emptyList()
        }
    }
}
