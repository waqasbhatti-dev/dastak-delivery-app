package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings

/**
 * Centralized singleton provider for FirebaseFirestore.
 * Configures persistent local disk caching (100MB) to minimize
 * repeated document downloads across sessions and optimize free-quota consumption.
 */
object FirestoreProvider {
    @Volatile
    private var instance: FirebaseFirestore? = null

    fun get(context: Context): FirebaseFirestore {
        return instance ?: synchronized(this) {
            instance ?: run {
                val dbId = context.applicationContext.getString(R.string.firestore_database_id)
                val firestore = FirebaseFirestore.getInstance(dbId)
                try {
                    val settings = firestoreSettings {
                        setLocalCacheSettings(
                            PersistentCacheSettings.newBuilder()
                                .setSizeBytes(100 * 1024 * 1024L) // 100 MB local disk cache
                                .build()
                        )
                    }
                    firestore.firestoreSettings = settings
                } catch (_: Exception) {
                    // Settings can only be applied once before first database operation
                }
                instance = firestore
                firestore
            }
        }
    }
}
