package com.example.ui.customer

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.data.location.RoadRoutingService
import com.example.data.model.Complaint
import com.example.data.model.Order
import com.example.data.model.User
import com.example.data.repository.AdvertisementRepository
import com.example.data.repository.OrderRepository
import com.example.data.repository.UserRepository
import com.example.localization.AppLanguage
import com.example.localization.Strings
import com.example.ui.ad.CustomerAdvertisementBanner
import com.example.ui.components.LanguageSelector
import com.example.ui.theme.DastakPrimary
import com.example.ui.theme.DastakSecondary
import com.example.ui.theme.UrduFontFamily
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDashboardScreen(
    user: User,
    currentLanguage: AppLanguage,
    userRepository: UserRepository,
    orderRepository: OrderRepository,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    advertisementRepository: AdvertisementRepository,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val voiceRecorder = remember { VoiceRecordingManager(context) }

    DisposableEffect(Unit) {
        onDispose {
            voiceRecorder.release()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = Strings.get("app_name", currentLanguage),
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
                    // Call Dastak Button
                    FilledTonalButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${Strings.DASTAK_PHONE}")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("customer_call_dastak_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.URDU) "کال کریں" else "Call",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LanguageSelector(
                        currentLanguage = currentLanguage,
                        onLanguageChange = onLanguageChange,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("customer_logout_button")
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
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.AddShoppingCart, contentDescription = null) },
                    label = {
                        Text(
                            Strings.get("nav_home", currentLanguage),
                            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                        )
                    },
                    modifier = Modifier.testTag("nav_home_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.ShoppingBag, contentDescription = null) },
                    label = {
                        Text(
                            Strings.get("active_orders_tab", currentLanguage),
                            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                        )
                    },
                    modifier = Modifier.testTag("nav_orders_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = {
                        Text(
                            Strings.get("history_tab", currentLanguage),
                            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                        )
                    },
                    modifier = Modifier.testTag("nav_history_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.ReportProblem, contentDescription = null) },
                    label = {
                        Text(
                            Strings.get("nav_complaints", currentLanguage),
                            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                        )
                    },
                    modifier = Modifier.testTag("nav_complaints_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    label = {
                        Text(
                            Strings.get("nav_feedback", currentLanguage),
                            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                        )
                    },
                    modifier = Modifier.testTag("nav_feedback_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = {
                        Text(
                            Strings.get("nav_profile", currentLanguage),
                            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                        )
                    },
                    modifier = Modifier.testTag("nav_profile_tab")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> CustomerHomeContent(
                    user = user,
                    currentLanguage = currentLanguage,
                    orderRepository = orderRepository,
                    advertisementRepository = advertisementRepository,
                    voiceRecorder = voiceRecorder,
                    onLanguageChange = onLanguageChange,
                    onOrderCreated = { selectedTab = 1 }
                )
                1 -> CustomerOrdersContent(
                    user = user,
                    currentLanguage = currentLanguage,
                    orderRepository = orderRepository,
                    showHistoryOnly = false,
                    onSwitchToHistory = { selectedTab = 2 }
                )
                2 -> CustomerOrdersContent(
                    user = user,
                    currentLanguage = currentLanguage,
                    orderRepository = orderRepository,
                    showHistoryOnly = true,
                    onSwitchToHistory = {}
                )
                3 -> CustomerComplaintsContent(
                    user = user,
                    currentLanguage = currentLanguage,
                    orderRepository = orderRepository
                )
                4 -> CustomerFeedbackContent(
                    user = user,
                    currentLanguage = currentLanguage,
                    orderRepository = orderRepository
                )
                5 -> CustomerProfileContent(
                    user = user,
                    currentLanguage = currentLanguage,
                    userRepository = userRepository,
                    onLanguageChange = onLanguageChange
                )
            }
        }
    }
}

@Composable
private fun CustomerHomeContent(
    user: User,
    currentLanguage: AppLanguage,
    orderRepository: OrderRepository,
    advertisementRepository: AdvertisementRepository,
    voiceRecorder: VoiceRecordingManager,
    onLanguageChange: (AppLanguage) -> Unit,
    onOrderCreated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var itemDescription by remember { mutableStateOf("") }
    var deliveryBlock by remember { mutableStateOf("") }
    var deliveryStreet by remember { mutableStateOf("") }
    var deliveryHouse by remember { mutableStateOf("") }
    var additionalInstructions by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf(user.phone) }
    var photoUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var estimatedItemCostInput by remember { mutableStateOf("") }

    var isConfirmingLocation by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var confirmedOrderId by remember { mutableStateOf<String?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val isRecording by voiceRecorder.isRecording.collectAsState()
    val isPlayingVoice by voiceRecorder.isPlaying.collectAsState()
    val recordedAudioData by voiceRecorder.recordedAudioData.collectAsState()

    // Microphone permission launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceRecorder.startRecording()
        } else {
            validationError = if (currentLanguage == AppLanguage.URDU)
                "مائیکروفون کی اجازت درکار ہے۔"
            else
                "Microphone permission is required for voice requests."
        }
    }

    // Photo picker launcher (zero-permission modern Android Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val encodedList = photoUris.toMutableList()
            for (uri in uris) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        // Compress bitmap for fast upload
                        val outputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                        val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                        encodedList.add("data:image/jpeg;base64,$base64")
                    }
                } catch (e: Exception) {
                    /* skip corrupted image */
                }
            }
            photoUris = encodedList.take(5)
        }
    }

    if (isConfirmingLocation) {
        CustomerLocationScreen(
            initialBlock = deliveryBlock,
            initialStreet = deliveryStreet,
            initialHouse = deliveryHouse,
            initialInstructions = additionalInstructions,
            estimatedItemCost = 0.0,
            currentLanguage = currentLanguage,
            onLanguageChange = onLanguageChange,
            onBack = { isConfirmingLocation = false },
            onLocationConfirmed = { confirmedLat, confirmedLng, distanceKm, deliveryCharge, serviceFee, platformFee, total, block, street, house, instructions, paymentMethod ->
                isSubmitting = true
                scope.launch {
                    val hasText = itemDescription.trim().isNotBlank()
                    val hasVoice = !recordedAudioData.isNullOrBlank()
                    val hasPhotos = photoUris.isNotEmpty()
                    val finalDescription = if (hasText) {
                        itemDescription.trim()
                    } else if (hasVoice && hasPhotos) {
                        if (currentLanguage == AppLanguage.URDU) "صوتی اور تصویری آرڈر" else "Voice & Photo Order"
                    } else if (hasVoice) {
                        if (currentLanguage == AppLanguage.URDU) "صوتی آرڈر (ریکارڈنگ منسلک ہے)" else "Voice Order (Audio Attached)"
                    } else {
                        if (currentLanguage == AppLanguage.URDU) "تصویری آرڈر (تصویر منسلک ہے)" else "Photo Order (Image Attached)"
                    }

                    val newOrder = Order(
                        customerId = user.userId,
                        customerName = user.name,
                        customerPhone = phone.trim(),
                        itemDescription = finalDescription,
                        itemPhotoUrls = photoUris,
                        voiceAudioUrl = recordedAudioData ?: "",
                        customerLatitude = confirmedLat,
                        customerLongitude = confirmedLng,
                        centralLatitude = RoadRoutingService.DEFAULT_CENTRAL_LATITUDE,
                        centralLongitude = RoadRoutingService.DEFAULT_CENTRAL_LONGITUDE,
                        deliveryBlock = block,
                        deliveryStreet = street,
                        deliveryHouseNumber = house,
                        additionalAddressInstructions = instructions,
                        orderStatus = "Pending",
                        estimatedItemCost = 0.0,
                        deliveryDistanceKm = distanceKm,
                        deliveryCharge = deliveryCharge,
                        serviceCharge = 0.0,
                        platformFee = platformFee,
                        totalAmount = 0.0,
                        paymentMethod = paymentMethod,
                        paymentStatus = "unpaid"
                    )
                    val res = orderRepository.createOrder(newOrder)
                    isSubmitting = false
                    if (res.isSuccess) {
                        confirmedOrderId = res.getOrThrow()
                        itemDescription = ""
                        deliveryStreet = ""
                        deliveryHouse = ""
                        additionalInstructions = ""
                        photoUris = emptyList()
                        estimatedItemCostInput = ""
                        voiceRecorder.clearRecording()
                        isConfirmingLocation = false
                    } else {
                        validationError = Strings.get("error_generic", currentLanguage)
                    }
                }
            }
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Prominent Advertisement Banner / Carousel
        item {
            CustomerAdvertisementBanner(
                advertisementRepository = advertisementRepository,
                currentLanguage = currentLanguage,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Welcome and Call Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (currentLanguage == AppLanguage.URDU) "خوش آمدید، ${user.name}!" else "Welcome, ${user.name}!",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                            )
                        )
                        Text(
                            text = if (currentLanguage == AppLanguage.URDU)
                                "جہلم سٹی ہاؤسنگ میں جو چاہیں منگوائیں۔ کوئی مقررہ کیٹلاگ نہیں ہے!"
                            else
                                "Request any item in Jhelum City Housing without a fixed catalog!",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                            )
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${Strings.DASTAK_PHONE}")
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = DastakPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Place Order Form
        item {
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
                    Text(
                        text = Strings.get("create_order_title", currentLanguage),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                        )
                    )

                    // A. Item Description
                    OutlinedTextField(
                        value = itemDescription,
                        onValueChange = {
                            itemDescription = it
                            validationError = null
                        },
                        label = {
                            Text(
                                Strings.get("item_desc_label", currentLanguage),
                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                            )
                        },
                        placeholder = {
                            Text(
                                if (currentLanguage == AppLanguage.URDU)
                                    "مثلاً: 2 دودھ کے ڈبے (الپرز)، نان بائی سے 4 روٹی، اور پیناڈول گولیوں کا پتہ..."
                                else
                                    "e.g., 2 packs milk, 4 naans from local shop, Panadol strip..."
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("order_item_description_input"),
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // B. Upload Item Photos
                    Column {
                        Text(
                            text = Strings.get("upload_photos_label", currentLanguage),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("upload_photo_button")
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = Strings.get("add_photo_btn", currentLanguage),
                                    fontSize = 12.sp,
                                    fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                                )
                            }
                        }

                        // Photo previews
                        if (photoUris.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(photoUris) { dataUri ->
                                    Box(
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.LightGray)
                                    ) {
                                        val base64 = dataUri.substringAfter("base64,")
                                        val bytes = Base64.decode(base64, Base64.DEFAULT)
                                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                        if (bmp != null) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = "Selected photo",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                        // Remove button
                                        IconButton(
                                            onClick = { photoUris = photoUris.filter { it != dataUri } },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(22.dp)
                                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // C. Voice Request
                    Column {
                        Text(
                            text = Strings.get("voice_request_title", currentLanguage),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isRecording && recordedAudioData == null) {
                                Button(
                                    onClick = {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DastakSecondary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("record_voice_button")
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = Strings.get("record_voice_btn", currentLanguage),
                                        fontSize = 12.sp,
                                        fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                                    )
                                }
                            } else if (isRecording) {
                                Button(
                                    onClick = {
                                        voiceRecorder.stopRecording()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("stop_recording_button")
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = Strings.get("recording_active", currentLanguage),
                                        fontSize = 12.sp,
                                        fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                                    )
                                }
                            } else if (recordedAudioData != null) {
                                // Controls when recording is ready
                                FilledTonalButton(
                                    onClick = {
                                        if (isPlayingVoice) voiceRecorder.stopPlayback() else voiceRecorder.playRecording()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("play_recording_button")
                                ) {
                                    Icon(
                                        if (isPlayingVoice) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = Strings.get("play_voice_btn", currentLanguage),
                                        fontSize = 12.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = { voiceRecorder.clearRecording() },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("delete_recording_button")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    // D. Customer Contact
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            validationError = null
                        },
                        label = { Text(Strings.get("phone_number", currentLanguage)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_order_phone_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // E. Delivery Address
                    Text(
                        text = Strings.get("delivery_address", currentLanguage),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                        )
                    )

                    OutlinedTextField(
                        value = deliveryBlock,
                        onValueChange = {
                            deliveryBlock = it
                            validationError = null
                        },
                        label = { Text(if (currentLanguage == AppLanguage.URDU) "بلاک / Block" else "Block / بلاک") },
                        placeholder = { Text("e.g. Block A, B, C, Commercial...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_order_block_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = deliveryStreet,
                            onValueChange = {
                                deliveryStreet = it
                                validationError = null
                            },
                            label = { Text(Strings.get("street_label", currentLanguage)) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("customer_order_street_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = deliveryHouse,
                            onValueChange = {
                                deliveryHouse = it
                                validationError = null
                            },
                            label = { Text(Strings.get("house_label", currentLanguage)) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("customer_order_house_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = additionalInstructions,
                        onValueChange = { additionalInstructions = it },
                        label = { Text(Strings.get("address_instructions_label", currentLanguage)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_order_instructions_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Tariff & Post-Receipt Billing Policy
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = DastakSecondary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = Strings.get("transparent_pricing_title", currentLanguage),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = DastakSecondary,
                                        fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                                    )
                                )
                            }
                            Text(Strings.get("delivery_rate_info", currentLanguage), style = MaterialTheme.typography.bodySmall)
                            Text(Strings.get("service_charge_info", currentLanguage), style = MaterialTheme.typography.bodySmall)
                            Text(Strings.get("platform_fee_info", currentLanguage), style = MaterialTheme.typography.bodySmall)
                            Divider(modifier = Modifier.padding(vertical = 2.dp))
                            Text(
                                text = Strings.get("final_bill_notice", currentLanguage),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                            )
                        }
                    }

                    if (validationError != null) {
                        Text(
                            text = validationError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Button to proceed to Mandatory Location Confirmation Page
                    Button(
                        onClick = {
                            if (isSubmitting) return@Button
                            val hasText = itemDescription.trim().isNotBlank()
                            val hasVoice = !recordedAudioData.isNullOrBlank()
                            val hasPhotos = photoUris.isNotEmpty()

                            if (!hasText && !hasVoice && !hasPhotos) {
                                validationError = if (currentLanguage == AppLanguage.URDU)
                                    "آرڈر کے لیے کم از کم ایک طریقہ منتخب کریں: سامان لکھیں، آواز ریکارڈ کریں، یا تصویر لگائیں۔"
                                else
                                    "Please provide at least one input method: write description, record voice note, or upload photo."
                                return@Button
                            }
                            if (phone.trim().length < 7) {
                                validationError = if (currentLanguage == AppLanguage.URDU)
                                    "براہِ کرم رابطہ کے لیے درست موبائل نمبر درج کریں۔"
                                else
                                    "Please provide a valid contact phone number."
                                return@Button
                            }

                            validationError = null
                            isConfirmingLocation = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("customer_proceed_to_location_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary),
                        enabled = !isSubmitting
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Strings.get("proceed_to_location_btn", currentLanguage),
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialog
    if (confirmedOrderId != null) {
        AlertDialog(
            onDismissRequest = {
                confirmedOrderId = null
                onOrderCreated()
            },
            title = {
                Text(
                    text = Strings.get("order_submitted_success", currentLanguage),
                    fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                )
            },
            text = {
                Column {
                    Text(text = "Order ID: ${confirmedOrderId!!.take(8)}...")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (currentLanguage == AppLanguage.URDU)
                            "آپ کا آرڈر جہلم سٹی ہاؤسنگ کے دستیاب آن لائن رائیڈرز کو ارسال کر دیا گیا ہے۔"
                        else
                            "Your order has been broadcasted to online approved riders in Jhelum City Housing."
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmedOrderId = null
                        onOrderCreated()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary)
                ) {
                    Text(if (currentLanguage == AppLanguage.URDU) "ٹھیک ہے" else "OK")
                }
            }
        )
    }
}

@Composable
private fun CustomerOrdersContent(
    user: User,
    currentLanguage: AppLanguage,
    orderRepository: OrderRepository,
    showHistoryOnly: Boolean,
    onSwitchToHistory: () -> Unit
) {
    var allOrders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var userFeedbacks by remember { mutableStateOf<List<com.example.data.model.FeedbackItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var reviewingOrder by remember { mutableStateOf<Order?>(null) }
    var inspectingOrderDetails by remember { mutableStateOf<Order?>(null) }
    var feedbackOrder by remember { mutableStateOf<Order?>(null) }
    var feedbackRating by remember { mutableIntStateOf(5) }
    var feedbackComments by remember { mutableStateOf("") }
    var isSubmittingFeedback by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }
    var isReviewingAction by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(user.userId) {
        scope.launch {
            userFeedbacks = orderRepository.getUserFeedback()
        }
        orderRepository.observeCustomerOrders().collect { list ->
            allOrders = list
            isLoading = false
        }
    }

    // Filter active vs history
    val displayedOrders = remember(allOrders, showHistoryOnly) {
        if (showHistoryOnly) {
            allOrders.filter { it.orderStatus == "Delivered" || it.orderStatus == "Cancelled" }
        } else {
            allOrders.filter { it.orderStatus != "Delivered" && it.orderStatus != "Cancelled" }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = DastakPrimary)
        }
    } else if (displayedOrders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                text = if (showHistoryOnly)
                    Strings.get("no_history_orders", currentLanguage)
                else
                    Strings.get("no_active_orders", currentLanguage),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(displayedOrders) { order ->
                val hasGivenFeedback = userFeedbacks.any { it.orderId == order.id }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_order_card_${order.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                    "Bill Rejected", "Cancelled" -> MaterialTheme.colorScheme.error
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

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = order.itemDescription,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${order.deliveryBlock}, ${order.deliveryStreet}, ${order.deliveryHouseNumber}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )

                        // If bill is awaiting review: Display prominent action banner
                        if (order.orderStatus == "Awaiting Customer Bill Approval") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = Strings.get("bill_review_title", currentLanguage),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = DastakSecondary,
                                            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Item Purchase: PKR ${order.billAmount} | Total with Charges: PKR ${order.totalAmount}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { reviewingOrder = order },
                                        colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("open_bill_review_button")
                                    ) {
                                        Text(
                                            text = if (currentLanguage == AppLanguage.URDU) "رسید اور بل دیکھیں" else "Inspect Bill & Approve",
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { inspectingOrderDetails = order },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).testTag("view_order_details_${order.id}")
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(Strings.get("order_details_title", currentLanguage), fontSize = 12.sp)
                            }

                            // If order is delivered: Allow submitting feedback
                            if (order.orderStatus == "Delivered") {
                                Button(
                                    onClick = {
                                        if (!hasGivenFeedback) {
                                            feedbackOrder = order
                                            feedbackRating = 5
                                            feedbackComments = ""
                                        }
                                    },
                                    enabled = !hasGivenFeedback,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (hasGivenFeedback) Color(0xFF2E7D32) else DastakSecondary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("feedback_button_${order.id}")
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (hasGivenFeedback)
                                            Strings.get("feedback_status_submitted", currentLanguage)
                                        else
                                            Strings.get("feedback_status_pending", currentLanguage),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Bill Inspection & Review Modal
    if (reviewingOrder != null) {
        val o = reviewingOrder!!
        AlertDialog(
            onDismissRequest = { reviewingOrder = null },
            title = {
                Text(
                    text = Strings.get("bill_review_title", currentLanguage),
                    fontWeight = FontWeight.Bold,
                    fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (o.billPhotoUrl.isNotBlank()) {
                        Text(text = Strings.get("receipt_photo_label", currentLanguage), fontWeight = FontWeight.Bold)
                        val base64 = o.billPhotoUrl.substringAfter("base64,")
                        val bytes = try { Base64.decode(base64, Base64.DEFAULT) } catch (e: Exception) { null }
                        val bmp = if (bytes != null) BitmapFactory.decodeByteArray(bytes, 0, bytes.size) else null
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Shop Receipt",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    if (o.purchasedItemsDescription.isNotBlank()) {
                        Text(text = "Items: ${o.purchasedItemsDescription}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (o.substitutionsNotes.isNotBlank()) {
                        Text(text = "Substitutions: ${o.substitutionsNotes}", style = MaterialTheme.typography.bodySmall)
                    }

                    Divider()

                    // Full Tariff Breakdown
                    val serviceRateStr = if (o.billAmount < 300.0) "7% (< PKR 300)" else "5% (>= PKR 300)"
                    Text(text = "Purchase Bill: PKR ${String.format("%.2f", o.billAmount)}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Service Charge ($serviceRateStr): PKR ${String.format("%.2f", o.serviceCharge)}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Delivery Fee (PKR 70/km): PKR ${String.format("%.2f", o.deliveryCharge)}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Platform Fee: PKR ${String.format("%.2f", o.platformFee)}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "Final Total: PKR ${String.format("%.2f", o.totalAmount)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = DastakPrimary)
                    )

                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text(Strings.get("rejection_reason_label", currentLanguage)) },
                        placeholder = { Text("Required only if rejecting bill") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isReviewingAction = true
                        scope.launch {
                            orderRepository.reviewCustomerBill(o.id, isApproved = true)
                            isReviewingAction = false
                            reviewingOrder = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary),
                    enabled = !isReviewingAction,
                    modifier = Modifier.testTag("accept_bill_dialog_button")
                ) {
                    Text(Strings.get("accept_bill_btn", currentLanguage))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        isReviewingAction = true
                        scope.launch {
                            orderRepository.reviewCustomerBill(o.id, isApproved = false, rejectionReason = rejectReason)
                            isReviewingAction = false
                            reviewingOrder = null
                            rejectReason = ""
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    enabled = !isReviewingAction,
                    modifier = Modifier.testTag("reject_bill_dialog_button")
                ) {
                    Text(Strings.get("reject_bill_btn", currentLanguage))
                }
            }
        )
    }

    // Complete Order Inspection Modal
    if (inspectingOrderDetails != null) {
        val o = inspectingOrderDetails!!
        AlertDialog(
            onDismissRequest = { inspectingOrderDetails = null },
            title = {
                Text(
                    text = "${Strings.get("order_details_title", currentLanguage)} #${o.id.take(6).uppercase()}",
                    fontWeight = FontWeight.Bold,
                    fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "Status: ${Strings.getOrderStatusLabel(o.orderStatus, currentLanguage)}", fontWeight = FontWeight.Bold)
                    Text(text = "Description: ${o.itemDescription}")
                    Text(text = "Address: ${o.deliveryBlock}, ${o.deliveryStreet}, ${o.deliveryHouseNumber}")
                    if (o.additionalAddressInstructions.isNotBlank()) {
                        Text(text = "Instructions: ${o.additionalAddressInstructions}", style = MaterialTheme.typography.bodySmall)
                    }

                    if (o.itemPhotoUrls.isNotEmpty()) {
                        Text(text = "Item Photos:", fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(o.itemPhotoUrls) { photo ->
                                val base64 = photo.substringAfter("base64,")
                                val bytes = try { Base64.decode(base64, Base64.DEFAULT) } catch (e: Exception) { null }
                                val bmp = if (bytes != null) BitmapFactory.decodeByteArray(bytes, 0, bytes.size) else null
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }

                    if (o.billPhotoUrl.isNotBlank()) {
                        Text(text = "Merchant Receipt:", fontWeight = FontWeight.Bold)
                        val base64 = o.billPhotoUrl.substringAfter("base64,")
                        val bytes = try { Base64.decode(base64, Base64.DEFAULT) } catch (e: Exception) { null }
                        val bmp = if (bytes != null) BitmapFactory.decodeByteArray(bytes, 0, bytes.size) else null
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Receipt",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Divider()
                    if (o.billAmount > 0.0 && o.billPhotoUrl.isNotBlank()) {
                        val serviceRateStr = if (o.billAmount < 300.0) "7% (< PKR 300)" else "5% (>= PKR 300)"
                        Text(text = Strings.get("final_bill_label", currentLanguage), fontWeight = FontWeight.Bold)
                        Text(text = "Purchase Bill: PKR ${String.format("%.2f", o.billAmount)}")
                        Text(text = "Service Charge ($serviceRateStr): PKR ${String.format("%.2f", o.serviceCharge)}")
                        Text(text = "Delivery Fee (PKR 70/km): PKR ${String.format("%.2f", o.deliveryCharge)}")
                        Text(text = "Platform Fee: PKR ${String.format("%.2f", o.platformFee)}")
                        Text(
                            text = "Final Total: PKR ${String.format("%.2f", o.totalAmount)}",
                            fontWeight = FontWeight.Bold,
                            color = DastakPrimary
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.URDU)
                                    "حتمی بل اور رسید رائیڈر کی جانب سے سامان خریدنے کے بعد منسلک کی جائے گی۔"
                                else
                                    "Verified final bill and shop receipt will be provided after the rider purchases the items at the shop.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    if (o.deliveredAt != null) {
                        Text(
                            text = "${Strings.get("delivery_completed_at", currentLanguage)}: ${o.deliveredAt.toDate()}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { inspectingOrderDetails = null },
                    colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary)
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Feedback Submission Dialog
    if (feedbackOrder != null) {
        val o = feedbackOrder!!
        AlertDialog(
            onDismissRequest = { feedbackOrder = null },
            title = {
                Text(
                    text = Strings.get("feedback_dialog_title", currentLanguage),
                    fontWeight = FontWeight.Bold,
                    fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Order #${o.id.take(6).uppercase()}")
                    Text(text = Strings.get("rating_label", currentLanguage), fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..5).forEach { star ->
                            IconButton(onClick = { feedbackRating = star }) {
                                Icon(
                                    if (star <= feedbackRating) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "$star stars",
                                    tint = if (star <= feedbackRating) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = feedbackComments,
                        onValueChange = { feedbackComments = it },
                        label = { Text(Strings.get("comments_label", currentLanguage)) },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("feedback_comments_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmittingFeedback = true
                        scope.launch {
                            val res = orderRepository.submitFeedback(
                                rating = feedbackRating,
                                comments = feedbackComments.trim(),
                                orderId = o.id,
                                riderId = o.assignedRiderId
                            )
                            isSubmittingFeedback = false
                            if (res.isSuccess) {
                                userFeedbacks = orderRepository.getUserFeedback()
                                feedbackOrder = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary),
                    enabled = !isSubmittingFeedback,
                    modifier = Modifier.testTag("submit_feedback_confirm_btn")
                ) {
                    Text(Strings.get("btn_submit_feedback", currentLanguage))
                }
            },
            dismissButton = {
                TextButton(onClick = { feedbackOrder = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CustomerComplaintsContent(
    user: User,
    currentLanguage: AppLanguage,
    orderRepository: OrderRepository
) {
    val context = LocalContext.current
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategoryKey by remember { mutableStateOf(Strings.COMPLAINT_CATEGORIES.first()) }
    var selectedOrderId by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var userOrders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var userComplaints by remember { mutableStateOf<List<Complaint>>(emptyList()) }
    var isLoadingComplaints by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(user.userId) {
        userOrders = orderRepository.getCustomerOrders()
        userComplaints = orderRepository.getUserComplaints()
        isLoadingComplaints = false
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Helpline Quick Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Strings.get("help_center_title", currentLanguage),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                            )
                        )
                        Text(
                            text = Strings.get("dastak_hotline", currentLanguage),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        )
                    }
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${Strings.DASTAK_PHONE}")
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("complaints_call_helpline_btn")
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.URDU) "کال کریں" else "Call",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Complaint Submission Form
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.URDU) "شکایت درج کریں" else "Submit a Complaint",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                        )
                    )

                    // Category Selection Chips
                    Text(
                        text = Strings.get("category_label", currentLanguage),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(Strings.COMPLAINT_CATEGORIES) { catKey ->
                            val isSelected = selectedCategoryKey == catKey
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategoryKey = catKey },
                                label = {
                                    Text(
                                        text = Strings.get(catKey, currentLanguage),
                                        fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null,
                                        fontSize = 12.sp
                                    )
                                }
                            )
                        }
                    }

                    // Optional Order Selection
                    if (userOrders.isNotEmpty()) {
                        Text(
                            text = Strings.get("select_order_optional", currentLanguage),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedOrderId.isBlank(),
                                    onClick = { selectedOrderId = "" },
                                    label = { Text(if (currentLanguage == AppLanguage.URDU) "کوئی نہیں" else "None") }
                                )
                            }
                            items(userOrders) { ord ->
                                val isSelected = selectedOrderId == ord.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedOrderId = if (isSelected) "" else ord.id },
                                    label = {
                                        Text("#${ord.id.take(6).uppercase()} (${ord.itemDescription.take(12)}...)")
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text(if (currentLanguage == AppLanguage.URDU) "عنوان" else "Subject") },
                        modifier = Modifier.fillMaxWidth().testTag("complaint_subject_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(if (currentLanguage == AppLanguage.URDU) "تفصیل" else "Description") },
                        modifier = Modifier.fillMaxWidth().testTag("complaint_desc_input"),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (message != null) {
                        Text(text = message!!, color = DastakPrimary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (subject.isBlank() || description.isBlank()) return@Button
                            isSubmitting = true
                            scope.launch {
                                val catName = Strings.get(selectedCategoryKey, AppLanguage.ENGLISH)
                                val res = orderRepository.submitComplaint(
                                    Complaint(
                                        userId = user.userId,
                                        userRole = "customer",
                                        category = catName,
                                        orderId = selectedOrderId,
                                        subject = subject.trim(),
                                        description = description.trim(),
                                        status = "Open"
                                    )
                                )
                                isSubmitting = false
                                if (res.isSuccess) {
                                    subject = ""
                                    description = ""
                                    selectedOrderId = ""
                                    message = if (currentLanguage == AppLanguage.URDU) "شکایت درج ہو گئی! انتظامیہ جلد جائزہ لے گی۔" else "Complaint submitted! Admin will review shortly."
                                    userComplaints = orderRepository.getUserComplaints()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("submit_complaint_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary),
                        enabled = !isSubmitting
                    ) {
                        Text(if (currentLanguage == AppLanguage.URDU) "شکایت بھیجیں" else "Submit Complaint")
                    }
                }
            }
        }

        // List of Previously Submitted Complaints
        item {
            Text(
                text = if (currentLanguage == AppLanguage.URDU) "آپ کی درج شدہ شکایات" else "Your Submitted Complaints",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                )
            )
        }

        if (isLoadingComplaints) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DastakPrimary, modifier = Modifier.size(28.dp))
                }
            }
        } else if (userComplaints.isEmpty()) {
            item {
                Text(
                    text = Strings.get("no_complaints_history", currentLanguage),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        } else {
            items(userComplaints) { comp ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("user_complaint_item_${comp.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = comp.subject,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Badge(
                                containerColor = when (comp.status.lowercase()) {
                                    "resolved" -> Color(0xFF2E7D32)
                                    "in progress", "in_progress", "in_review" -> DastakSecondary
                                    else -> Color(0xFF0288D1)
                                }
                            ) {
                                Text(
                                    text = Strings.getComplaintStatusLabel(comp.status, currentLanguage),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 11.sp,
                                    fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                                )
                            }
                        }

                        if (comp.category.isNotBlank()) {
                            Text(
                                text = "Category: ${comp.category}",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            )
                        }

                        if (comp.orderId.isNotBlank()) {
                            Text(
                                text = "Related Order: #${comp.orderId.take(6).uppercase()}",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }

                        Text(
                            text = comp.description,
                            style = MaterialTheme.typography.bodySmall
                        )

                        if (comp.adminResponse.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE8F5E9),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = Strings.get("admin_reply_label", currentLanguage),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = comp.adminResponse,
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF1B5E20))
                                    )
                                    if (comp.respondedAt != null) {
                                        Text(
                                            text = "${comp.respondedAt.toDate()}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = Color(0xFF2E7D32))
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

@Composable
private fun CustomerProfileContent(
    user: User,
    currentLanguage: AppLanguage,
    userRepository: UserRepository,
    onLanguageChange: (AppLanguage) -> Unit
) {
    var name by remember { mutableStateOf(user.name) }
    var phone by remember { mutableStateOf(user.phone) }
    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = Strings.get("nav_profile", currentLanguage),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Text(
                    text = "Email: ${user.email}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(Strings.get("full_name", currentLanguage)) },
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(Strings.get("phone_number", currentLanguage)) },
                    modifier = Modifier.fillMaxWidth().testTag("profile_phone_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = Strings.get("language_selector", currentLanguage),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                LanguageSelector(
                    currentLanguage = currentLanguage,
                    onLanguageChange = { newLang ->
                        onLanguageChange(newLang)
                        scope.launch {
                            userRepository.updateLanguage(newLang.code)
                        }
                    }
                )

                if (message != null) {
                    Text(text = message!!, color = DastakPrimary, style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = {
                        isSaving = true
                        scope.launch {
                            val res = userRepository.updateProfile(name, phone, currentLanguage.code)
                            isSaving = false
                            if (res.isSuccess) {
                                message = if (currentLanguage == AppLanguage.URDU) "تبدیلیاں محفوظ ہو گئیں!" else "Profile updated successfully!"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_profile_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary),
                    enabled = !isSaving
                ) {
                    Text(if (currentLanguage == AppLanguage.URDU) "محفوظ کریں" else "Save Changes")
                }
            }
        }
    }
}

@Composable
private fun CustomerFeedbackContent(
    user: User,
    currentLanguage: AppLanguage,
    orderRepository: OrderRepository
) {
    var userFeedbacks by remember { mutableStateOf<List<com.example.data.model.FeedbackItem>>(emptyList()) }
    var customerOrders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedOrderToRate by remember { mutableStateOf<Order?>(null) }
    var ratingValue by remember { mutableIntStateOf(5) }
    var commentsValue by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadData() {
        scope.launch {
            isLoading = true
            userFeedbacks = orderRepository.getUserFeedback()
            customerOrders = orderRepository.getCustomerOrders()
            isLoading = false
        }
    }

    LaunchedEffect(user.userId) {
        loadData()
    }

    val deliveredOrders = remember(customerOrders) {
        customerOrders.filter { it.orderStatus == "Delivered" }
    }

    val eligibleOrders = remember(deliveredOrders, userFeedbacks) {
        deliveredOrders.filter { ord -> userFeedbacks.none { fb -> fb.orderId == ord.id } }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = Strings.get("nav_feedback", currentLanguage),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = DastakPrimary,
                    fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                )
            )
        }

        if (successMsg != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = successMsg!!,
                        color = Color(0xFF2E7D32),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        if (isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DastakPrimary)
                }
            }
        } else {
            // Section 1: Orders Eligible for Feedback
            item {
                Text(
                    text = if (currentLanguage == AppLanguage.URDU) "ترسیل شدہ آرڈرز پر رائے دیں" else "Eligible Delivered Orders for Feedback",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (eligibleOrders.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = Strings.get("no_eligible_feedback_orders", currentLanguage),
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(eligibleOrders) { ord ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Order #${ord.id.take(6).uppercase()}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Badge(containerColor = Color(0xFF2E7D32)) {
                                    Text("Delivered", color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp)
                                }
                            }
                            Text(text = ord.itemDescription, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "Total Bill: PKR ${String.format("%.2f", ord.totalAmount)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            if (ord.deliveredAt != null) {
                                Text(
                                    text = "Delivered on: ${ord.deliveredAt.toDate()}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                            Button(
                                onClick = {
                                    selectedOrderToRate = ord
                                    ratingValue = 5
                                    commentsValue = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("rate_order_btn_${ord.id}")
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Strings.get("feedback_status_pending", currentLanguage))
                            }
                        }
                    }
                }
            }

            // Section 2: Submitted Feedback History
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (currentLanguage == AppLanguage.URDU) "آپ کی سابقہ فیڈ بیک" else "Your Submitted Feedback History",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (userFeedbacks.isEmpty()) {
                item {
                    Text(
                        text = if (currentLanguage == AppLanguage.URDU) "ابھی تک کوئی فیڈ بیک جمع نہیں کروائی گئی۔" else "No feedback submitted yet.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            } else {
                items(userFeedbacks) { fb ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                (1..5).forEach { star ->
                                    Icon(
                                        imageVector = if (star <= fb.rating) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = null,
                                        tint = if (star <= fb.rating) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                if (fb.orderId.isNotBlank()) {
                                    Badge(containerColor = DastakSecondary, contentColor = Color.White) {
                                        Text("Order #${fb.orderId.take(6).uppercase()}", fontSize = 10.sp)
                                    }
                                }
                            }
                            if (fb.comments.isNotBlank()) {
                                Text(text = fb.comments, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (fb.createdAt != null) {
                                Text(
                                    text = "${fb.createdAt.toDate()}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog to Rate an Eligible Order
    if (selectedOrderToRate != null) {
        val ord = selectedOrderToRate!!
        AlertDialog(
            onDismissRequest = { selectedOrderToRate = null },
            title = {
                Text(
                    text = "${Strings.get("feedback_dialog_title", currentLanguage)} #${ord.id.take(6).uppercase()}",
                    fontWeight = FontWeight.Bold,
                    fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = ord.itemDescription, style = MaterialTheme.typography.bodySmall)
                    Text(text = Strings.get("rating_label", currentLanguage), fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..5).forEach { star ->
                            IconButton(onClick = { ratingValue = star }) {
                                Icon(
                                    if (star <= ratingValue) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "$star stars",
                                    tint = if (star <= ratingValue) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = commentsValue,
                        onValueChange = { commentsValue = it },
                        label = { Text(Strings.get("comments_label", currentLanguage)) },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("order_feedback_comments_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmitting = true
                        scope.launch {
                            val res = orderRepository.submitFeedback(
                                rating = ratingValue,
                                comments = commentsValue.trim(),
                                orderId = ord.id,
                                riderId = ord.assignedRiderId
                            )
                            isSubmitting = false
                            if (res.isSuccess) {
                                selectedOrderToRate = null
                                successMsg = Strings.get("feedback_submitted_success", currentLanguage)
                                loadData()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary),
                    enabled = !isSubmitting,
                    modifier = Modifier.testTag("submit_order_feedback_btn")
                ) {
                    Text(Strings.get("btn_submit_feedback", currentLanguage))
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedOrderToRate = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
