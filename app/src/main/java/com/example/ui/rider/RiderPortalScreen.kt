package com.example.ui.rider

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PricingCalculator
import com.example.data.audio.VoiceRecordingManager
import com.example.data.model.Order
import com.example.data.model.Rider
import com.example.data.model.RiderApplication
import com.example.data.model.User
import com.example.data.repository.OrderRepository
import com.example.data.repository.RiderRepository
import com.example.localization.AppLanguage
import com.example.localization.Strings
import com.example.ui.components.LanguageSelector
import com.example.ui.theme.DastakPrimary
import com.example.ui.theme.DastakSecondary
import com.example.ui.theme.UrduFontFamily
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiderPortalScreen(
    user: User,
    currentLanguage: AppLanguage,
    riderRepository: RiderRepository,
    orderRepository: OrderRepository,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var application by remember { mutableStateOf<RiderApplication?>(null) }
    var approvedRider by remember { mutableStateOf<Rider?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun refreshRiderState(showLoading: Boolean = false) {
        scope.launch {
            if (showLoading) isLoading = true
            val r = riderRepository.getApprovedRider(user.userId)
            approvedRider = r
            if (r == null) {
                application = riderRepository.getRiderApplication(user.userId)
            }
            if (showLoading) isLoading = false
        }
    }

    LaunchedEffect(user.userId) {
        isLoading = true
        val r = riderRepository.getApprovedRider(user.userId)
        approvedRider = r
        if (r == null) {
            application = riderRepository.getRiderApplication(user.userId)
        }
        isLoading = false

        // Continuously listen to rider document in real-time
        riderRepository.observeApprovedRider(user.userId).collect { liveRider ->
            if (liveRider != null) {
                approvedRider = liveRider
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = Strings.get("portal_rider", currentLanguage),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = DastakPrimary,
                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                            )
                        )
                        Text(
                            text = Strings.get("locality_name", currentLanguage),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                            )
                        )
                    }
                },
                actions = {
                    LanguageSelector(
                        currentLanguage = currentLanguage,
                        onLanguageChange = onLanguageChange,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("rider_logout_button")
                    ) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = Strings.get("nav_logout", currentLanguage),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = DastakPrimary,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (approvedRider != null) {
                // Active Approved Rider Workflow
                RiderApprovedDashboard(
                    rider = approvedRider!!,
                    currentLanguage = currentLanguage,
                    riderRepository = riderRepository,
                    orderRepository = orderRepository,
                    onStatusUpdated = { refreshRiderState() }
                )
            } else if (application == null) {
                // Application submission form
                RiderApplicationForm(
                    user = user,
                    currentLanguage = currentLanguage,
                    riderRepository = riderRepository,
                    onSubmitted = { refreshRiderState() }
                )
            } else if (application!!.status == "pending") {
                // Pending approval state
                RiderPendingStatus(
                    application = application!!,
                    currentLanguage = currentLanguage,
                    onRefresh = { refreshRiderState() }
                )
            } else if (application!!.status == "approved" || user.role == "rider") {
                // Auto-healing / provisioning transition for approved rider
                LaunchedEffect(Unit) {
                    refreshRiderState()
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = DastakPrimary)
                        Text(
                            text = if (currentLanguage == AppLanguage.URDU) "رائیڈر پورٹل لوڈ ہو رہا ہے..." else "Loading Rider Workspace...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                        )
                    }
                }
            } else {
                // Rejected state
                RiderRejectedStatus(
                    application = application!!,
                    currentLanguage = currentLanguage,
                    onReapply = { application = null }
                )
            }
        }
    }
}

@Composable
private fun RiderApplicationForm(
    user: User,
    currentLanguage: AppLanguage,
    riderRepository: RiderRepository,
    onSubmitted: () -> Unit
) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf(user.name) }
    var phone by remember { mutableStateOf(user.phone) }
    var cnic by remember { mutableStateOf("") }
    var cnicFrontUri by remember { mutableStateOf<Uri?>(null) }
    var cnicBackUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var uploadStatusText by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val frontImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            cnicFrontUri = uri
            errorMsg = null
        }
    }

    val backImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            cnicBackUri = uri
            errorMsg = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = Strings.get("rider_app_title", currentLanguage),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = Strings.get("rider_app_desc", currentLanguage),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                    )
                )
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Full Name
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it; errorMsg = null },
                    label = { Text(Strings.get("full_name", currentLanguage)) },
                    modifier = Modifier.fillMaxWidth().testTag("rider_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // 2. Registered Gmail Address (Verified via Google Sign-In)
                OutlinedTextField(
                    value = user.email,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(if (currentLanguage == AppLanguage.URDU) "رجسٹرڈ جی میل ایڈریس (گوگل سائن ان)" else "Registered Gmail Address") },
                    leadingIcon = {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = DastakPrimary)
                    },
                    trailingIcon = {
                        Badge(containerColor = Color(0xFF2E7D32)) {
                            Text("Google", color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontSize = 10.sp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("rider_email_display"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DastakPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // 3. Contact Phone Number
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; errorMsg = null },
                    label = { Text(Strings.get("phone_number", currentLanguage)) },
                    placeholder = { Text("03001234567") },
                    modifier = Modifier.fillMaxWidth().testTag("rider_phone_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // 4. CNIC Number (13 Digits)
                OutlinedTextField(
                    value = cnic,
                    onValueChange = { if (it.length <= 15) { cnic = it; errorMsg = null } },
                    label = { Text(Strings.get("cnic_number", currentLanguage)) },
                    placeholder = { Text("3740512345671") },
                    modifier = Modifier.fillMaxWidth().testTag("rider_cnic_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                // 5. Mandatory CNIC Front Image
                Text(
                    text = if (currentLanguage == AppLanguage.URDU)
                        "1. شناختی کارڈ (سامنے کا رخ) — لازمی"
                    else
                        "1. CNIC Front Image (Mandatory)",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                    )
                )

                if (cnicFrontUri != null) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            coil.compose.AsyncImage(
                                model = cnicFrontUri,
                                contentDescription = "CNIC Front Preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (currentLanguage == AppLanguage.URDU) "تصویر منتخب ہو گئی" else "Image Selected",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = Color(0xFF2E7D32))
                                    )
                                }
                                TextButton(
                                    onClick = { frontImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                    modifier = Modifier.testTag("change_cnic_front_btn")
                                ) {
                                    Text(if (currentLanguage == AppLanguage.URDU) "تبدیل کریں" else "Change")
                                }
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { frontImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("select_cnic_front_btn")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.URDU)
                                "گیلری سے شناختی کارڈ سامنے کی تصویر چنیں"
                            else
                                "Select CNIC Front Image from Gallery",
                            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                        )
                    }
                }

                // 6. Mandatory CNIC Back Image
                Text(
                    text = if (currentLanguage == AppLanguage.URDU)
                        "2. شناختی کارڈ (پیچھے کا رخ) — لازمی"
                    else
                        "2. CNIC Back Image (Mandatory)",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                    )
                )

                if (cnicBackUri != null) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            coil.compose.AsyncImage(
                                model = cnicBackUri,
                                contentDescription = "CNIC Back Preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (currentLanguage == AppLanguage.URDU) "تصویر منتخب ہو گئی" else "Image Selected",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = Color(0xFF2E7D32))
                                    )
                                }
                                TextButton(
                                    onClick = { backImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                    modifier = Modifier.testTag("change_cnic_back_btn")
                                ) {
                                    Text(if (currentLanguage == AppLanguage.URDU) "تبدیل کریں" else "Change")
                                }
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { backImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("select_cnic_back_btn")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.URDU)
                                "گیلری سے شناختی کارڈ پیچھے کی تصویر چنیں"
                            else
                                "Select CNIC Back Image from Gallery",
                            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                        )
                    }
                }

                if (uploadStatusText != null) {
                    Text(
                        text = uploadStatusText!!,
                        color = DastakPrimary,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                }

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // 7. Submit Application Button
                Button(
                    onClick = {
                        val cleanCnic = cnic.replace("-", "").trim()
                        if (fullName.isBlank()) {
                            errorMsg = if (currentLanguage == AppLanguage.URDU) "براہِ کرم اپنا مکمل نام درج کریں۔" else "Please enter your full name."
                            return@Button
                        }
                        if (phone.trim().length < 7) {
                            errorMsg = if (currentLanguage == AppLanguage.URDU) "براہِ کرم درست موبائل نمبر درج کریں۔" else "Please enter a valid phone number."
                            return@Button
                        }
                        if (cleanCnic.length < 13) {
                            errorMsg = if (currentLanguage == AppLanguage.URDU) "براہِ کرم 13 ہندسوں کا درست شناختی کارڈ نمبر درج کریں۔" else "Please enter a valid 13-digit CNIC number."
                            return@Button
                        }
                        if (cnicFrontUri == null || cnicBackUri == null) {
                            errorMsg = if (currentLanguage == AppLanguage.URDU)
                                "شناختی کارڈ کے دونوں رخ (سامنے اور پیچھے) کی تصاویر منتخب کرنا لازمی ہے۔"
                            else
                                "Both CNIC front and back images are mandatory."
                            return@Button
                        }

                        isSubmitting = true
                        errorMsg = null
                        uploadStatusText = if (currentLanguage == AppLanguage.URDU) "شناختی کارڈ کی تصاویر اپ لوڈ کی جا رہی ہیں..." else "Uploading CNIC images..."

                        scope.launch {
                            val frontRes = riderRepository.uploadCnicImage(context, cnicFrontUri!!, isFront = true)
                            if (frontRes.isFailure) {
                                isSubmitting = false
                                uploadStatusText = null
                                errorMsg = if (currentLanguage == AppLanguage.URDU)
                                    "سامنے والی تصویر اپ لوڈ نہ ہو سکی۔ براہِ کرم دوبارہ کوشش فرمائیں۔"
                                else
                                    "Failed to upload CNIC front image. Please try again."
                                return@launch
                            }

                            val backRes = riderRepository.uploadCnicImage(context, cnicBackUri!!, isFront = false)
                            if (backRes.isFailure) {
                                isSubmitting = false
                                uploadStatusText = null
                                errorMsg = if (currentLanguage == AppLanguage.URDU)
                                    "پیچھے والی تصویر اپ لوڈ نہ ہو سکی۔ براہِ کرم دوبارہ کوشش فرمائیں۔"
                                else
                                    "Failed to upload CNIC back image. Please try again."
                                return@launch
                            }

                            uploadStatusText = if (currentLanguage == AppLanguage.URDU) "درخواست جمع کی جا رہی ہے..." else "Submitting application..."

                            val app = RiderApplication(
                                userId = user.userId,
                                fullName = fullName.trim(),
                                email = user.email,
                                phone = phone.trim(),
                                cnicNumber = cleanCnic,
                                cnicFrontImageUrl = frontRes.getOrThrow(),
                                cnicBackImageUrl = backRes.getOrThrow(),
                                preferredLanguage = currentLanguage.code,
                                status = "pending"
                            )
                            val res = riderRepository.submitApplication(app)
                            isSubmitting = false
                            uploadStatusText = null
                            if (res.isSuccess) {
                                onSubmitted()
                            } else {
                                errorMsg = Strings.get("error_generic", currentLanguage)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("submit_rider_app_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.URDU) "پروسیسنگ جاری ہے..." else "Processing...",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = Strings.get("submit_application", currentLanguage),
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RiderPendingStatus(
    application: RiderApplication,
    currentLanguage: AppLanguage,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(72.dp).background(DastakSecondary.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.HourglassTop, contentDescription = null, tint = DastakSecondary, modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Badge(containerColor = DastakSecondary, contentColor = Color.White) {
            Text(
                text = Strings.get("status_pending", currentLanguage),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = Strings.get("pending_msg", currentLanguage),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onRefresh,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("refresh_rider_status_button")
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = if (currentLanguage == AppLanguage.URDU) "کیفیت تازہ کریں" else "Check Approval Status")
        }
    }
}

@Composable
private fun RiderRejectedStatus(
    application: RiderApplication,
    currentLanguage: AppLanguage,
    onReapply: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = Strings.get("status_rejected", currentLanguage),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = Strings.get("rejected_msg", currentLanguage), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        if (application.rejectionReason.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Reason: ${application.rejectionReason}", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onReapply, shape = RoundedCornerShape(12.dp)) {
            Text(if (currentLanguage == AppLanguage.URDU) "دوبارہ درخواست دیں" else "Re-apply")
        }
    }
}

@Composable
private fun RiderApprovedDashboard(
    rider: Rider,
    currentLanguage: AppLanguage,
    riderRepository: RiderRepository,
    orderRepository: OrderRepository,
    onStatusUpdated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audioPlayer = remember { VoiceRecordingManager(context) }

    var isOnline by remember(rider.isOnline) { mutableStateOf(rider.isOnline) }
    var isUpdatingOnline by remember { mutableStateOf(false) }
    var onlineStatusError by remember { mutableStateOf<String?>(null) }

    // Keep online state in sync with real-time updates from the database
    LaunchedEffect(rider.isOnline) {
        isOnline = rider.isOnline
    }

    var unassignedOrders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var assignedOrders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var selectedOrderTab by remember { mutableIntStateOf(0) }
    var isClaimingOrder by remember { mutableStateOf(false) }
    var claimMessage by remember { mutableStateOf<String?>(null) }

    // Dialog state for Bill Submission
    var orderForBillSubmission by remember { mutableStateOf<Order?>(null) }
    var billAmountInput by remember { mutableStateOf("") }
    var receiptPhotoUri by remember { mutableStateOf("") }
    var purchasedItemsInput by remember { mutableStateOf("") }
    var substitutionsInput by remember { mutableStateOf("") }
    var riderNoteInput by remember { mutableStateOf("") }
    var distanceKmInput by remember { mutableStateOf("2.0") }
    var isSubmittingBill by remember { mutableStateOf(false) }
    var billError by remember { mutableStateOf<String?>(null) }

    // Receipt photo picker
    val receiptPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                    val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                    receiptPhotoUri = "data:image/jpeg;base64,$base64"
                }
            } catch (e: Exception) { /* skip */ }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.release()
        }
    }

    var ordersStreamError by remember { mutableStateOf<String?>(null) }

    // Real-time observation of unassigned orders & assigned orders
    LaunchedEffect(isOnline) {
        if (isOnline) {
            ordersStreamError = null
            orderRepository.observeUnassignedOrders()
                .catch { e ->
                    android.util.Log.e("RiderApprovedDashboard", "Error in unassigned orders stream", e)
                    ordersStreamError = e.localizedMessage ?: "Unable to stream orders. Please check your connection."
                }
                .collect { list ->
                    ordersStreamError = null
                    unassignedOrders = list
                }
        } else {
            unassignedOrders = emptyList()
            ordersStreamError = null
        }
    }

    LaunchedEffect(rider.userId) {
        orderRepository.observeRiderAssignedOrders()
            .catch { e ->
                android.util.Log.e("RiderApprovedDashboard", "Error in assigned orders stream", e)
            }
            .collect { list ->
                assignedOrders = list
            }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Rider Profile Card (Requirement 5: Rider Information)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("rider_profile_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(DastakPrimary.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.DirectionsBike, contentDescription = null, tint = DastakPrimary)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = rider.fullName.ifBlank { "Approved Rider" },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = rider.email,
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                        Badge(containerColor = Color(0xFF2E7D32), contentColor = Color.White) {
                            Text(
                                text = "Approved",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Phone: ${rider.phone.ifBlank { "Registered" }}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (rider.cnicNumber.isNotBlank()) {
                            Text(
                                text = "CNIC: ${rider.cnicNumber}",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }
            }
        }

        // Online / Offline Switch
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOnline) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth().testTag("rider_online_status_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Strings.get("online_status", currentLanguage),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isOnline) Strings.get("status_online", currentLanguage) else Strings.get("status_offline", currentLanguage),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (isOnline) DastakPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        if (isUpdatingOnline) {
                            CircularProgressIndicator(
                                color = DastakPrimary,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Switch(
                                checked = isOnline,
                                onCheckedChange = { newStatus ->
                                    isOnline = newStatus
                                    isUpdatingOnline = true
                                    onlineStatusError = null
                                    scope.launch {
                                        val res = riderRepository.setOnlineStatus(newStatus)
                                        isUpdatingOnline = false
                                        if (res.isSuccess) {
                                            onStatusUpdated()
                                        } else {
                                            isOnline = !newStatus
                                            onlineStatusError = if (currentLanguage == AppLanguage.URDU)
                                                "کیفیت تبدیل نہیں ہو سکی۔ براہِ کرم انٹرنیٹ کنکشن چیک کریں۔"
                                            else
                                                "Could not update status: ${res.exceptionOrNull()?.localizedMessage ?: "Please try again"}"
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("rider_online_switch"),
                                enabled = !isUpdatingOnline
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isOnline) {
                            if (currentLanguage == AppLanguage.URDU)
                                "آپ آن لائن ہیں اور نئے آرڈرز وصول کر رہے ہیں۔"
                            else
                                "You are Online and receiving newly placed customer orders in real time."
                        } else {
                            if (currentLanguage == AppLanguage.URDU)
                                "آپ آف لائن ہیں۔ قریبی آرڈرز وصول کرنے کے لیے بٹن کو آن کریں۔"
                            else
                                "You are currently Offline. Turn toggle ON to start receiving customer orders."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    if (onlineStatusError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = onlineStatusError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        if (claimMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = claimMessage!!,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // Section 1: Available Unassigned Orders (Shown to online approved riders)
        if (isOnline) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Strings.get("available_orders_title", currentLanguage),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DastakPrimary,
                            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                        )
                    )
                    Badge(containerColor = if (unassignedOrders.isNotEmpty()) DastakSecondary else MaterialTheme.colorScheme.surfaceVariant) {
                        Text("${unassignedOrders.size} Available", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }

            if (ordersStreamError != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().testTag("orders_stream_error_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = ordersStreamError!!,
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onErrorContainer)
                            )
                        }
                    }
                }
            } else if (unassignedOrders.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DastakPrimary, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (currentLanguage == AppLanguage.URDU)
                                    "آپ آن لائن ہیں۔ فی الحال کوئی غیر تفویض شدہ آرڈر زیر التوا نہیں ہے۔ جیسے ہی کوئی نیا آرڈر آئے گا، وہ یہاں فوراً ظاہر ہوگا۔"
                                else
                                    "You are Online! There are currently no unassigned pending orders in your area. New orders will appear here automatically in real time.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(unassignedOrders) { order ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("unassigned_order_${order.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Badge(containerColor = DastakSecondary, contentColor = Color.White) {
                                Text("Available", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Text(
                                text = "Block: ${order.deliveryBlock}, Street: ${order.deliveryStreet}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = order.itemDescription,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Estimated delivery charge: PKR ${order.deliveryCharge.toInt()} | Platform: PKR 4",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (isClaimingOrder) return@Button
                                isClaimingOrder = true
                                scope.launch {
                                    val res = orderRepository.claimOrder(order.id)
                                    isClaimingOrder = false
                                    if (res.isSuccess) {
                                        claimMessage = Strings.get("order_claimed_by_you", currentLanguage)
                                    } else {
                                        claimMessage = "Could not claim order. It may have been taken by another rider."
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("claim_order_button_${order.id}"),
                            enabled = !isClaimingOrder
                        ) {
                            Text(
                                text = Strings.get("accept_order_btn", currentLanguage),
                                fontWeight = FontWeight.Bold,
                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Rider Orders: Active Orders vs Order History
        item {
            Column {
                TabRow(selectedTabIndex = selectedOrderTab) {
                    Tab(
                        selected = selectedOrderTab == 0,
                        onClick = { selectedOrderTab = 0 },
                        text = {
                            Text(
                                Strings.get("active_orders_tab", currentLanguage),
                                fontWeight = FontWeight.Bold,
                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                            )
                        },
                        modifier = Modifier.testTag("rider_active_orders_tab")
                    )
                    Tab(
                        selected = selectedOrderTab == 1,
                        onClick = { selectedOrderTab = 1 },
                        text = {
                            Text(
                                Strings.get("history_tab", currentLanguage),
                                fontWeight = FontWeight.Bold,
                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                            )
                        },
                        modifier = Modifier.testTag("rider_history_orders_tab")
                    )
                }
            }
        }

        val filteredRiderOrders = if (selectedOrderTab == 0) {
            assignedOrders.filter { it.orderStatus != "Delivered" && it.orderStatus != "Cancelled" }
        } else {
            assignedOrders.filter { it.orderStatus == "Delivered" || it.orderStatus == "Cancelled" }
        }

        if (filteredRiderOrders.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (selectedOrderTab == 0) {
                            if (currentLanguage == AppLanguage.URDU)
                                "فی الحال کوئی فعال آرڈر تفویض نہیں ہے۔ آن لائن رہیں تاکہ قریبی آرڈرز وصول ہو سکیں۔"
                            else
                                "No active orders assigned. Stay Online to receive newly placed orders."
                        } else {
                            if (currentLanguage == AppLanguage.URDU)
                                "تاریخ میں کوئی مکمل شدہ آرڈر موجود نہیں ہے۔"
                            else
                                "No completed delivery orders in history."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(20.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(filteredRiderOrders) { order ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("assigned_order_card_${order.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Order #${order.id.take(6).uppercase()}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Badge(
                                containerColor = when (order.orderStatus) {
                                    "Delivered" -> Color(0xFF2E7D32)
                                    "Awaiting Customer Bill Approval" -> DastakSecondary
                                    "Out for Delivery" -> Color(0xFF0288D1)
                                    else -> DastakPrimary
                                }
                            ) {
                                Text(
                                    text = Strings.getOrderStatusLabel(order.orderStatus, currentLanguage),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 12.sp,
                                    fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                                )
                            }
                        }

                        Text(
                            text = order.itemDescription,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )

                        // Customer Details (Full address & phone unlocked only for assigned rider)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Customer: ${order.customerName.ifBlank { "Resident" }} (${order.customerPhone})",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    IconButton(
                                        onClick = {
                                            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                                data = Uri.parse("tel:${order.customerPhone}")
                                            }
                                            context.startActivity(dialIntent)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = "Call Customer", tint = DastakPrimary, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Text(
                                    text = "Address: ${order.deliveryBlock}, ${order.deliveryStreet}, ${order.deliveryHouseNumber}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (order.additionalAddressInstructions.isNotBlank()) {
                                    Text(
                                        text = "Directions: ${order.additionalAddressInstructions}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = DastakSecondary)
                                    )
                                }
                            }
                        }

                        // Voice request playback if customer attached voice note
                        if (order.voiceAudioUrl.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { audioPlayer.playRecording(order.voiceAudioUrl) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Play Customer Voice Note", fontSize = 12.sp)
                                }
                            }
                        }

                        // Customer item photos preview
                        if (order.itemPhotoUrls.isNotEmpty()) {
                            Text(text = "Customer Item Photos:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(order.itemPhotoUrls) { photo ->
                                    val base64 = photo.substringAfter("base64,")
                                    val bytes = try { Base64.decode(base64, Base64.DEFAULT) } catch (e: Exception) { null }
                                    val bmp = if (bytes != null) BitmapFactory.decodeByteArray(bytes, 0, bytes.size) else null
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "Customer Photo",
                                            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(6.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        // Workflow Action Buttons based on status
                        when (order.orderStatus) {
                            "Assigned" -> {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            orderRepository.updateOrderStatus(order.id, "Shopping")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("start_shopping_button_${order.id}")
                                ) {
                                    Text(Strings.get("order_stage_shopping", currentLanguage))
                                }
                            }
                            "Shopping", "Bill Rejected" -> {
                                if (order.orderStatus == "Bill Rejected" && order.billRejectionReason.isNotBlank()) {
                                    Text(
                                        text = "Customer Rejection Reason: ${order.billRejectionReason}",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Button(
                                    onClick = {
                                        orderForBillSubmission = order
                                        billAmountInput = if (order.billAmount > 0) order.billAmount.toString() else ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DastakSecondary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("open_submit_bill_button_${order.id}")
                                ) {
                                    Text(Strings.get("submit_bill_btn", currentLanguage))
                                }
                            }
                            "Awaiting Customer Bill Approval" -> {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = Strings.get("awaiting_bill_approval", currentLanguage),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(10.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            "Bill Approved" -> {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFE8F5E9),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = Strings.get("bill_accepted_banner", currentLanguage),
                                        color = Color(0xFF2E7D32),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(8.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            orderRepository.updateOrderStatus(order.id, "Out for Delivery")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("start_delivery_button_${order.id}")
                                ) {
                                    Text(Strings.get("start_delivery_btn", currentLanguage))
                                }
                            }
                            "Out for Delivery", "Arrived Outside" -> {
                                var isMarkingArrival by remember { mutableStateOf(false) }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (!order.hasArrivedOutside && order.orderStatus != "Arrived Outside") {
                                        Button(
                                            onClick = {
                                                if (isMarkingArrival) return@Button
                                                isMarkingArrival = true
                                                scope.launch {
                                                    orderRepository.markRiderArrivedOutside(order.id)
                                                    isMarkingArrival = false
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("rider_arrived_outside_button_${order.id}"),
                                            enabled = !isMarkingArrival
                                        ) {
                                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = Strings.get("rider_btn_arrived_outside", currentLanguage),
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                                            )
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFFFF3E0),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = Strings.get("rider_arrival_marked", currentLanguage),
                                                        color = Color(0xFFE65100),
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                                                    )
                                                    if (order.arrivedOutsideAt != null) {
                                                        Text(
                                                            text = "Time: ${order.arrivedOutsideAt.toDate()}",
                                                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFE65100))
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                orderRepository.updateOrderStatus(order.id, "Delivered")
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("complete_delivery_button_${order.id}")
                                    ) {
                                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(Strings.get("complete_delivery_btn", currentLanguage), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            "Delivered" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = Strings.get("order_delivered_banner", currentLanguage),
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(text = "Distance: ${order.deliveryDistanceKm} km | Delivery Charge: PKR ${order.deliveryCharge.toInt()}", style = MaterialTheme.typography.bodySmall)
                                            Text(text = "Purchase Bill: PKR ${String.format("%.2f", order.billAmount)} | 5% Service Charge: PKR ${String.format("%.2f", order.serviceCharge)}", style = MaterialTheme.typography.bodySmall)
                                            Text(text = "Total Order Amount: PKR ${String.format("%.2f", order.totalAmount)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                            if (order.deliveredAt != null) {
                                                Text(
                                                    text = "${Strings.get("delivery_completed_at", currentLanguage)}: ${order.deliveredAt.toDate()}",
                                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF2E7D32))
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Submit Purchase Bill Dialog
    if (orderForBillSubmission != null) {
        val o = orderForBillSubmission!!
        val confirmedDistance = if (o.deliveryDistanceKm > 0.0) o.deliveryDistanceKm else 1.0
        AlertDialog(
            onDismissRequest = { orderForBillSubmission = null },
            title = {
                Text(
                    text = Strings.get("submit_bill_btn", currentLanguage),
                    fontWeight = FontWeight.Bold,
                    fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = billAmountInput,
                        onValueChange = {
                            billAmountInput = it
                            billError = null
                        },
                        label = { Text(Strings.get("bill_amount_label", currentLanguage)) },
                        placeholder = { Text("e.g. 450") },
                        modifier = Modifier.fillMaxWidth().testTag("bill_amount_input"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Receipt photo picker
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                receiptPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("pick_receipt_photo_button")
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Strings.get("receipt_photo_label", currentLanguage), fontSize = 12.sp)
                        }

                        if (receiptPhotoUri.isNotBlank()) {
                            Badge(containerColor = Color(0xFF2E7D32), contentColor = Color.White) {
                                Text("Photo Attached", modifier = Modifier.padding(4.dp))
                            }
                        }
                    }

                    OutlinedTextField(
                        value = purchasedItemsInput,
                        onValueChange = { purchasedItemsInput = it },
                        label = { Text(Strings.get("purchased_items_desc", currentLanguage)) },
                        placeholder = { Text("e.g. 2 Milk Olpers, 4 Naan...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = substitutionsInput,
                        onValueChange = { substitutionsInput = it },
                        label = { Text(Strings.get("substitutions_label", currentLanguage)) },
                        placeholder = { Text("e.g. Milk brand replaced with Milkpak") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = riderNoteInput,
                        onValueChange = { riderNoteInput = it },
                        label = { Text(Strings.get("rider_note_label", currentLanguage)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Confirmed Delivery Distance from customer's locked GPS location
                    val parsedBill = billAmountInput.toDoubleOrNull() ?: 0.0

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Confirmed Distance: $confirmedDistance km (Locked from Customer GPS)",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                            if (parsedBill > 0.0) {
                                val sc = PricingCalculator.calculateServiceCharge(parsedBill)
                                val dc = PricingCalculator.calculateDeliveryCharge(confirmedDistance)
                                val tot = PricingCalculator.calculateTotal(parsedBill, confirmedDistance)
                                val serviceRateStr = if (parsedBill < 300.0) "7% (< PKR 300)" else "5% (>= PKR 300)"
                                Text(text = "Service Fee ($serviceRateStr): PKR ${String.format("%.2f", sc)}", fontSize = 11.sp)
                                Text(text = "Delivery Fee (PKR 70/km): PKR ${String.format("%.2f", dc)}", fontSize = 11.sp)
                                Text(text = "Platform Fee: PKR 4.00", fontSize = 11.sp)
                                Divider(modifier = Modifier.padding(vertical = 2.dp))
                                Text(text = "Total Charged to Customer: PKR ${String.format("%.2f", tot)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (billError != null) {
                        Text(text = billError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = billAmountInput.toDoubleOrNull()
                        if (amount == null || amount <= 0.0) {
                            billError = "Please enter a valid purchase amount from the receipt."
                            return@Button
                        }
                        if (receiptPhotoUri.isBlank() && o.billPhotoUrl.isBlank()) {
                            billError = "Please attach a photo of the shop receipt before submitting."
                            return@Button
                        }
                        isSubmittingBill = true
                        scope.launch {
                            val res = orderRepository.submitRiderBill(
                                orderId = o.id,
                                billAmount = amount,
                                billPhotoUrl = if (receiptPhotoUri.isNotBlank()) receiptPhotoUri else o.billPhotoUrl,
                                purchasedDesc = purchasedItemsInput.trim(),
                                substitutions = substitutionsInput.trim(),
                                notes = riderNoteInput.trim(),
                                distanceKm = confirmedDistance
                            )
                            isSubmittingBill = false
                            if (res.isSuccess) {
                                orderForBillSubmission = null
                                billAmountInput = ""
                                receiptPhotoUri = ""
                                purchasedItemsInput = ""
                                substitutionsInput = ""
                                riderNoteInput = ""
                            } else {
                                billError = "Failed to submit bill. Try again."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary),
                    enabled = !isSubmittingBill,
                    modifier = Modifier.testTag("submit_bill_dialog_button")
                ) {
                    if (isSubmittingBill) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(Strings.get("btn_send_bill", currentLanguage))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { orderForBillSubmission = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
