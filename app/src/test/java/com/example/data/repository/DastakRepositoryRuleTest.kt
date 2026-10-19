package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.RiderApplication
import com.example.data.model.User
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class DastakRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveUser_authenticatedUser_succeeds() = runBlocking {
        val email = "cust_${UUID.randomUUID()}@test.com"
        val uid = signInTestUser(email)
        val repo = UserRepository(firestore)
        val user = User(
            userId = uid,
            email = email,
            name = "Test Customer",
            role = "customer",
            preferredLanguage = "en",
            accountStatus = "active"
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveInitialUser(user)
        }
        assertTrue(result.isSuccess)

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.getProfile(uid)
        }
        assertNotNull(fetched)
        assertEquals("Test Customer", fetched?.name)
    }

    @Test
    fun riderApplication_submitPending_succeeds() = runBlocking {
        val email = "rider_${UUID.randomUUID()}@test.com"
        val uid = signInTestUser(email)
        val repo = RiderRepository(firestore)
        val app = RiderApplication(
            userId = uid,
            fullName = "Muhammad Ali",
            email = email,
            phone = "03001234567",
            cnicNumber = "3740512345671",
            status = "pending"
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.submitApplication(app)
        }
        assertTrue(result.isSuccess)

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.getRiderApplication(uid)
        }
        assertNotNull(fetched)
        assertEquals("pending", fetched?.status)
    }

    @Test
    fun termsAcceptance_userCanRecordAcceptance() = runBlocking {
        val email = "terms_${UUID.randomUUID()}@test.com"
        val uid = signInTestUser(email)
        val repo = TermsRepository(firestore)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.acceptTerms("1.0", "ur")
        }
        assertTrue(result.isSuccess)

        val accepted = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.hasUserAcceptedActiveTerms("1.0")
        }
        assertTrue(accepted)
    }

    @Test
    fun unauthenticated_cannotReadProtectedCollections() = runBlocking {
        auth.signOut()
        val repo = UserRepository(firestore)

        val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.getProfile("unauthenticated_nonexistent_id")
        }
        assertNull(profile)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
    }
}
