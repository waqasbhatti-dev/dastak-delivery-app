package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.model.TermsVersion
import com.example.data.model.User
import com.example.data.repository.AdvertisementRepository
import com.example.data.repository.OrderRepository
import com.example.data.repository.RiderRepository
import com.example.data.repository.TermsRepository
import com.example.data.repository.UserRepository
import com.example.data.tts.TtsManager
import com.example.localization.AppLanguage
import com.example.ui.admin.AdminPortalScreen
import com.example.ui.auth.GoogleSignInHelper
import com.example.ui.auth.WelcomeScreen
import com.example.ui.customer.CustomerDashboardScreen
import com.example.ui.rider.RiderPortalScreen
import com.example.ui.terms.TermsAndConditionsScreen
import com.example.ui.theme.DastakPrimary
import com.example.ui.theme.DastakTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var ttsManager: TtsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ttsManager = TtsManager(this)

        setContent {
            var currentLanguage by remember { mutableStateOf(AppLanguage.ENGLISH) }

            // Urdu uses RTL layout, English uses LTR layout
            val layoutDirection = if (currentLanguage == AppLanguage.URDU) {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                DastakTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        DastakApp(
                            currentLanguage = currentLanguage,
                            ttsManager = ttsManager,
                            onLanguageChange = { newLang -> currentLanguage = newLang }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
    }
}

@Composable
fun DastakApp(
    currentLanguage: AppLanguage,
    ttsManager: TtsManager,
    onLanguageChange: (AppLanguage) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val userRepository = remember { UserRepository(context) }
    val termsRepository = remember { TermsRepository(context) }
    val riderRepository = remember { RiderRepository(context) }
    val orderRepository = remember { OrderRepository(context) }
    val advertisementRepository = remember { AdvertisementRepository(context) }

    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }
    var userProfile by remember { mutableStateOf<User?>(null) }
    var targetRoleIntent by remember { mutableStateOf("customer") }
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    // Terms gating state
    var isCheckingTerms by remember { mutableStateOf(false) }
    var activeTerms by remember { mutableStateOf<TermsVersion?>(null) }
    var termsAccepted by remember { mutableStateOf(false) }
    var isAcceptingTerms by remember { mutableStateOf(false) }

    // Listen to Firebase Auth state
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    // Refresh profile and check terms whenever currentUser changes
    LaunchedEffect(currentUser?.uid) {
        val user = currentUser
        if (user != null) {
            isCheckingTerms = true
            // 1. Fetch or initialize profile
            var profile = userRepository.getProfile(user.uid)
            if (profile == null) {
                val newProfile = User(
                    userId = user.uid,
                    email = user.email ?: "",
                    name = user.displayName ?: "User",
                    phone = user.phoneNumber ?: "",
                    role = targetRoleIntent,
                    preferredLanguage = currentLanguage.code,
                    accountStatus = "active"
                )
                userRepository.saveInitialUser(newProfile)
                profile = newProfile
            } else {
                // Restore user's preferred language if set
                if (profile.preferredLanguage == "ur" && currentLanguage != AppLanguage.URDU) {
                    onLanguageChange(AppLanguage.URDU)
                } else if (profile.preferredLanguage == "en" && currentLanguage != AppLanguage.ENGLISH) {
                    onLanguageChange(AppLanguage.ENGLISH)
                }
            }
            userProfile = profile

            // 2. Fetch active terms and verify acceptance
            val terms = termsRepository.getActiveTerms()
            activeTerms = terms
            val hasAccepted = termsRepository.hasUserAcceptedActiveTerms(terms.version)
            termsAccepted = hasAccepted
            isCheckingTerms = false
        } else {
            userProfile = null
            activeTerms = null
            termsAccepted = false
            isCheckingTerms = false
        }
    }

    fun handleLogout() {
        GoogleSignInHelper.signOut(context, scope) {
            currentUser = null
            userProfile = null
            termsAccepted = false
            authError = null
        }
    }

    if (currentUser == null) {
        // Welcome Screen with 3 distinct portals
        WelcomeScreen(
            currentLanguage = currentLanguage,
            onLanguageChange = onLanguageChange,
            onPortalSelected = { portalRole ->
                targetRoleIntent = portalRole
                isSigningIn = true
                authError = null
                GoogleSignInHelper.signIn(
                    context = context,
                    scope = scope,
                    onSuccess = {
                        isSigningIn = false
                        currentUser = Firebase.auth.currentUser
                    },
                    onError = { err ->
                        isSigningIn = false
                        authError = err
                    },
                    onCancelled = {
                        isSigningIn = false
                    }
                )
            },
            isLoading = isSigningIn,
            errorMessage = authError
        )
    } else if (isCheckingTerms || userProfile == null || activeTerms == null) {
        // Loading state
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = DastakPrimary)
        }
    } else if (!termsAccepted) {
        // Mandatory Terms and Conditions Gate
        TermsAndConditionsScreen(
            terms = activeTerms!!,
            currentLanguage = currentLanguage,
            ttsManager = ttsManager,
            onLanguageChange = onLanguageChange,
            onAccept = {
                isAcceptingTerms = true
                scope.launch {
                    val res = termsRepository.acceptTerms(activeTerms!!.version, currentLanguage.code)
                    isAcceptingTerms = false
                    if (res.isSuccess) {
                        termsAccepted = true
                    }
                }
            },
            onDecline = {
                handleLogout()
            },
            isSubmitting = isAcceptingTerms
        )
    } else {
        // Authenticated Dashboard according to user role and portal intent
        val profile = userProfile!!
        val effectiveRole = if (targetRoleIntent == "admin" || profile.role == "admin") {
            "admin"
        } else if (profile.role == "rider" || targetRoleIntent == "rider") {
            "rider"
        } else {
            "customer"
        }

        when (effectiveRole) {
            "admin" -> {
                AdminPortalScreen(
                    userEmail = currentUser?.email ?: "",
                    userUid = currentUser?.uid ?: "",
                    currentLanguage = currentLanguage,
                    userRepository = userRepository,
                    riderRepository = riderRepository,
                    orderRepository = orderRepository,
                    termsRepository = termsRepository,
                    advertisementRepository = advertisementRepository,
                    onLanguageChange = onLanguageChange,
                    onLogout = { handleLogout() }
                )
            }
            "rider" -> {
                RiderPortalScreen(
                    user = profile,
                    currentLanguage = currentLanguage,
                    riderRepository = riderRepository,
                    orderRepository = orderRepository,
                    onLanguageChange = onLanguageChange,
                    onLogout = { handleLogout() }
                )
            }
            else -> {
                CustomerDashboardScreen(
                    user = profile,
                    currentLanguage = currentLanguage,
                    userRepository = userRepository,
                    orderRepository = orderRepository,
                    advertisementRepository = advertisementRepository,
                    onLanguageChange = onLanguageChange,
                    onLogout = { handleLogout() }
                )
            }
        }
    }
}
