package com.example.ui.customer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.PricingCalculator
import com.example.data.location.RoadRoutingService
import com.example.localization.AppLanguage
import com.example.localization.Strings
import com.example.ui.components.LanguageSelector
import com.example.ui.theme.DastakPrimary
import com.example.ui.theme.DastakSecondary
import com.example.ui.theme.UrduFontFamily
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerLocationScreen(
    initialBlock: String = "Block A",
    initialStreet: String = "",
    initialHouse: String = "",
    initialInstructions: String = "",
    estimatedItemCost: Double = 0.0,
    currentLanguage: AppLanguage,
    centralLatitude: Double = RoadRoutingService.DEFAULT_CENTRAL_LATITUDE,
    centralLongitude: Double = RoadRoutingService.DEFAULT_CENTRAL_LONGITUDE,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit,
    onLocationConfirmed: (
        confirmedLat: Double,
        confirmedLng: Double,
        distanceKm: Double,
        deliveryCharge: Double,
        serviceFee: Double,
        platformFee: Double,
        total: Double,
        block: String,
        street: String,
        house: String,
        instructions: String,
        paymentMethod: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Customer GPS coordinates - starts at 0.0 to guarantee real customer confirmation
    var customerLat by remember { mutableDoubleStateOf(0.0) }
    var customerLng by remember { mutableDoubleStateOf(0.0) }
    var hasConfirmedGps by remember { mutableStateOf(false) }

    // Map viewport zoom and center
    var mapZoom by remember { mutableFloatStateOf(1.0f) }

    // Address fields
    var deliveryBlock by remember { mutableStateOf(initialBlock) }
    var deliveryStreet by remember { mutableStateOf(initialStreet) }
    var deliveryHouse by remember { mutableStateOf(initialHouse) }
    var additionalInstructions by remember { mutableStateOf(initialInstructions) }

    // Routing calculation & pricing
    var isCalculatingRoute by remember { mutableStateOf(false) }
    var routeError by remember { mutableStateOf<String?>(null) }
    var calculatedRoute by remember { mutableStateOf<RoadRoutingService.RouteResult?>(null) }

    // Payment method
    var selectedPaymentMethod by remember { mutableStateOf("cash_on_delivery") }

    // Validation / GPS status
    var gpsStatusMessage by remember { mutableStateOf<String?>(null) }
    var isLocating by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Function to acquire device GPS coordinates
    @SuppressLint("MissingPermission")
    fun requestDeviceLocation() {
        isLocating = true
        gpsStatusMessage = null
        validationError = null

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc: Location? ->
                isLocating = false
                if (loc != null && loc.latitude != 0.0 && loc.longitude != 0.0) {
                    customerLat = Math.round(loc.latitude * 1000000.0) / 1000000.0
                    customerLng = Math.round(loc.longitude * 1000000.0) / 1000000.0
                    hasConfirmedGps = true
                    calculatedRoute = null // Reset route to recalculate
                    gpsStatusMessage = if (currentLanguage == AppLanguage.URDU)
                        "جی پی ایس لوکیشن کامیابی سے حاصل ہو گئی۔"
                    else
                        "GPS location acquired successfully."
                } else {
                    // Try lastLocation fallback
                    fusedClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                        if (lastLoc != null && lastLoc.latitude != 0.0 && lastLoc.longitude != 0.0) {
                            customerLat = Math.round(lastLoc.latitude * 1000000.0) / 1000000.0
                            customerLng = Math.round(lastLoc.longitude * 1000000.0) / 1000000.0
                            hasConfirmedGps = true
                            calculatedRoute = null
                        } else {
                            hasConfirmedGps = false
                            customerLat = 0.0
                            customerLng = 0.0
                            calculatedRoute = null
                            gpsStatusMessage = Strings.get("location_gps_unavailable", currentLanguage)
                        }
                    }
                }
            }
            .addOnFailureListener {
                isLocating = false
                gpsStatusMessage = Strings.get("location_gps_unavailable", currentLanguage)
            }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            requestDeviceLocation()
        } else {
            gpsStatusMessage = Strings.get("location_permission_needed", currentLanguage)
        }
    }

    fun startLocationFlow() {
        val fineCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fineCheck == PackageManager.PERMISSION_GRANTED || coarseCheck == PackageManager.PERMISSION_GRANTED) {
            requestDeviceLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    val blockOptions = listOf("Block A", "Block B", "Block C", "Block D", "Commercial Area", "Main Boulevard")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = Strings.get("confirm_location_title", currentLanguage),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                            )
                        )
                        Text(
                            text = "Jhelum City Housing (جہلم سٹی ہاؤسنگ)",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("location_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    LanguageSelector(
                        currentLanguage = currentLanguage,
                        onLanguageChange = onLanguageChange,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: GPS Action Button & Status
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = DastakPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.URDU) "موجودہ لوکیشن کا تعین" else "Acquire Current Location",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                            )
                        )
                    }

                    Text(
                        text = if (currentLanguage == AppLanguage.URDU)
                            "ڈیلیوری فیس 70 روپے فی کلومیٹر کے حساب سے مرکزی گودام سے آپ کے گھر تک سڑک کے اصل فاصلے پر ہوگی۔"
                        else
                            "Delivery fee is calculated at PKR 70/km using the actual road route from the fixed central warehouse to your doorstep.",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                    )

                    Button(
                        onClick = { startLocationFlow() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("get_current_location_button"),
                        enabled = !isLocating
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (currentLanguage == AppLanguage.URDU) "لوکیشن تلاش کی جا رہی ہے..." else "Detecting GPS Location...")
                        } else {
                            Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Strings.get("btn_get_current_location", currentLanguage),
                                fontWeight = FontWeight.Bold,
                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                            )
                        }
                    }

                    if (gpsStatusMessage != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (hasConfirmedGps) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (hasConfirmedGps) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = gpsStatusMessage!!,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (hasConfirmedGps) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                ),
                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                            )
                        }
                    }

                    if (!hasConfirmedGps) {
                        OutlinedButton(
                            onClick = { startLocationFlow() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("retry_gps_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Strings.get("btn_retry_gps", currentLanguage))
                        }
                    }
                }
            }

            // Section 2: Interactive Map with Adjustable Pin
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (currentLanguage == AppLanguage.URDU) "ڈیلیوری میپ (جی پی ایس لاک)" else "Delivery Route Map",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                            )
                            Text(
                                text = if (hasConfirmedGps)
                                    (if (currentLanguage == AppLanguage.URDU) "پن جی پی ایس لوکیشن پر محفوظ اور لاک ہے" else "Pin is locked to captured GPS coordinates")
                                else
                                    (if (currentLanguage == AppLanguage.URDU) "موجودہ لوکیشن حاصل کرنے کے لیے اوپر والا بٹن دبائیں" else "Tap 'Get My Current Location' above to lock pin"),
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                            )
                        }

                        // Zoom Controls
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { mapZoom = (mapZoom + 0.2f).coerceAtMost(2.5f) },
                                modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Text("+", fontWeight = FontWeight.Bold)
                            }
                            IconButton(
                                onClick = { mapZoom = (mapZoom - 0.2f).coerceAtLeast(0.6f) },
                                modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Text("-", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Map Canvas (Locked pin - customer cannot drag, move or tap to reposition)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE8ECEF))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .testTag("interactive_map_canvas")
                    ) {
                        Canvas(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val w = size.width
                            val h = size.height
                            val center = Offset(w / 2f, h / 2f)

                            // 1. Draw background road network grid
                            val gridColor = Color(0xFFD0D7DE)
                            val roadColor = Color(0xFFFFFFFF)
                            val boulevardColor = Color(0xFFFFE082)

                            // Main roads / grid lines
                            drawLine(color = boulevardColor, start = Offset(0f, h * 0.5f), end = Offset(w, h * 0.5f), strokeWidth = 10f * mapZoom)
                            drawLine(color = boulevardColor, start = Offset(w * 0.45f, 0f), end = Offset(w * 0.45f, h), strokeWidth = 8f * mapZoom)

                            for (i in 1..5) {
                                val y = (h * i) / 6f
                                drawLine(color = roadColor, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 4f * mapZoom)
                            }
                            for (i in 1..5) {
                                val x = (w * i) / 6f
                                drawLine(color = roadColor, start = Offset(x, 0f), end = Offset(x, h), strokeWidth = 4f * mapZoom)
                            }

                            // 2. Fixed Central Point (Warehouse Hub)
                            // Position on canvas: slightly offset to represent warehouse location
                            val centralOffset = Offset(w * 0.35f, h * 0.55f)
                            drawCircle(color = DastakPrimary.copy(alpha = 0.25f), radius = 24f * mapZoom, center = centralOffset)
                            drawCircle(color = DastakPrimary, radius = 10f * mapZoom, center = centralOffset)
                            drawCircle(color = Color.White, radius = 4f * mapZoom, center = centralOffset)

                            // 3. Customer Delivery Pin
                            if (customerLat != 0.0 && customerLng != 0.0) {
                                val dLat = customerLat - centralLatitude
                                val dLng = customerLng - centralLongitude

                                val custX = centralOffset.x + (dLng / 0.015f * (w / 2f) * mapZoom).toFloat()
                                val custY = centralOffset.y - (dLat / 0.015f * (h / 2f) * mapZoom).toFloat()
                                val custOffset = Offset(
                                    custX.coerceIn(20f, w - 20f),
                                    custY.coerceIn(20f, h - 20f)
                                )

                                // Route line between Central Point and Customer Pin
                                drawLine(
                                    color = DastakSecondary,
                                    start = centralOffset,
                                    end = custOffset,
                                    strokeWidth = 4f * mapZoom,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                                )

                                // Customer Pin circle & icon
                                drawCircle(color = Color(0xFFD32F2F).copy(alpha = 0.3f), radius = 26f * mapZoom, center = custOffset)
                                drawCircle(color = Color(0xFFD32F2F), radius = 12f * mapZoom, center = custOffset)
                                drawCircle(color = Color.White, radius = 5f * mapZoom, center = custOffset)
                            }
                        }

                        // Map Legend Overlays
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(DastakPrimary, CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Warehouse Hub", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(Color(0xFFD32F2F), CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (hasConfirmedGps) "Your Delivery Pin" else "No Pin Set Yet",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // D-Pad Fine Tuning Controls
                    if (hasConfirmedGps) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = String.format(Locale.US, "Lat: %.6f, Lng: %.6f", customerLat, customerLng),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (currentLanguage == AppLanguage.URDU) "پن کو باریک ایڈجسٹ کریں" else "Fine-tune Pin Position",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }

                            // Directional Nudge Buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        customerLat = Math.round((customerLat + 0.0003) * 1000000.0) / 1000000.0
                                        calculatedRoute = null
                                    },
                                    modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "North", modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        customerLat = Math.round((customerLat - 0.0003) * 1000000.0) / 1000000.0
                                        calculatedRoute = null
                                    },
                                    modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "South", modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        customerLng = Math.round((customerLng - 0.0003) * 1000000.0) / 1000000.0
                                        calculatedRoute = null
                                    },
                                    modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                ) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "West", modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        customerLng = Math.round((customerLng + 0.0003) * 1000000.0) / 1000000.0
                                        calculatedRoute = null
                                    },
                                    modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                ) {
                                    Icon(Icons.Default.ArrowForward, contentDescription = "East", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Address Block, Street, House & Instructions
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (currentLanguage == AppLanguage.URDU) "گھر کا پتہ اور اضافی رہنمائی" else "Doorstep Address & Directions",
                        style = MaterialTheme.typography.titleSmall.copy(
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
                        placeholder = { Text("e.g. Block A, B, C, Rose, Commercial...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("location_block_input"),
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
                                .testTag("location_street_input"),
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
                                .testTag("location_house_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = additionalInstructions,
                        onValueChange = { additionalInstructions = it },
                        label = { Text(Strings.get("address_instructions_label", currentLanguage)) },
                        placeholder = { Text(if (currentLanguage == AppLanguage.URDU) "قریبی مسجد، پارک، یا واٹر فلٹریشن پلانٹ" else "Near park, mosque, or landmark") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("location_instructions_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Section 4: Route Calculation & Continue
            if (routeError != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Strings.get("route_error_title", currentLanguage),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            text = routeError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(
                            onClick = {
                                routeError = null
                                scope.launch {
                                    isCalculatingRoute = true
                                    val res = RoadRoutingService.calculateRoadDistance(
                                        originLat = centralLatitude,
                                        originLng = centralLongitude,
                                        destLat = customerLat,
                                        destLng = customerLng
                                    )
                                    isCalculatingRoute = false
                                    if (res.isSuccess) {
                                        calculatedRoute = res.getOrThrow()
                                    } else {
                                        routeError = Strings.get("route_error_desc", currentLanguage)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("retry_route_button")
                        ) {
                            Text(Strings.get("btn_retry_route", currentLanguage))
                        }
                    }
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

            // Button to Confirm Location and Calculate Route
            if (calculatedRoute == null) {
                Button(
                    onClick = {
                        if (!hasConfirmedGps || customerLat == 0.0 || customerLng == 0.0) {
                            validationError = if (currentLanguage == AppLanguage.URDU)
                                "براہِ کرم پہلے 'میری موجودہ لوکیشن حاصل کریں' پر کلک کریں اور پن کی تصدیق فرمائیں۔"
                            else
                                "Please click 'Get My Current Location' to confirm your delivery pin first."
                            return@Button
                        }
                        if (deliveryStreet.isBlank() || deliveryHouse.isBlank()) {
                            validationError = if (currentLanguage == AppLanguage.URDU)
                                "براہِ کرم گلی نمبر اور مکان نمبر درج کریں۔"
                            else
                                "Please enter required street and house number."
                            return@Button
                        }

                        validationError = null
                        routeError = null
                        isCalculatingRoute = true

                        scope.launch {
                            val res = RoadRoutingService.calculateRoadDistance(
                                originLat = centralLatitude,
                                originLng = centralLongitude,
                                destLat = customerLat,
                                destLng = customerLng
                            )
                            isCalculatingRoute = false
                            if (res.isSuccess) {
                                calculatedRoute = res.getOrThrow()
                            } else {
                                val exc = res.exceptionOrNull()
                                routeError = exc?.message ?: Strings.get("route_error_desc", currentLanguage)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("confirm_location_continue_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary),
                    enabled = !isCalculatingRoute
                ) {
                    if (isCalculatingRoute) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Strings.get("route_calculating", currentLanguage))
                    } else {
                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Strings.get("btn_confirm_location_continue", currentLanguage),
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                        )
                    }
                }
            }

            // Section 5: Complete Transparent Price Breakdown
            if (calculatedRoute != null) {
                val route = calculatedRoute!!
                val distanceKm = route.distanceKm
                val deliveryCharge = PricingCalculator.calculateDeliveryCharge(distanceKm)
                val platformFee = PricingCalculator.PLATFORM_FEE

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = DastakSecondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Strings.get("price_breakdown_title", currentLanguage),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DastakSecondary,
                                    fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                                )
                            )
                        }

                        Divider()

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(Strings.get("road_distance_label", currentLanguage), style = MaterialTheme.typography.bodyMedium)
                            Text("$distanceKm km", fontWeight = FontWeight.Bold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(Strings.get("delivery_fee_label", currentLanguage), style = MaterialTheme.typography.bodyMedium)
                            Text("${PricingCalculator.formatPkr(deliveryCharge)} (PKR ${PricingCalculator.getDeliveryRatePerKm(distanceKm).toInt()}/km)", fontWeight = FontWeight.Bold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(Strings.get("platform_fee_label", currentLanguage), style = MaterialTheme.typography.bodyMedium)
                            Text("PKR 4.00", fontWeight = FontWeight.Medium)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = if (currentLanguage == AppLanguage.URDU) "سامان کی قیمت" else "Items Purchase Bill",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (currentLanguage == AppLanguage.URDU) "دکان کی رسید کے بعد" else "Pending Shop Receipt",
                                style = MaterialTheme.typography.bodySmall.copy(color = DastakSecondary, fontWeight = FontWeight.Bold)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = Strings.get("service_fee_label", currentLanguage),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (currentLanguage == AppLanguage.URDU) "5% (یا 7% اگر 300 سے کم ہو)" else "5% (or 7% if < PKR 300)",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }

                        Divider()

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (currentLanguage == AppLanguage.URDU) "مکمل بل رائیڈر کی رسید کے بعد" else "Complete Bill Available After Shopping",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = DastakPrimary)
                                )
                                Text(
                                    text = Strings.get("final_bill_notice", currentLanguage),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                                )
                            }
                        }

                        // Payment Method Selection
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = Strings.get("payment_method_label", currentLanguage),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedPaymentMethod == "cash_on_delivery",
                                onClick = { selectedPaymentMethod = "cash_on_delivery" },
                                label = { Text(Strings.get("payment_cod", currentLanguage), fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = selectedPaymentMethod == "easypaisa_qr",
                                onClick = { selectedPaymentMethod = "easypaisa_qr" },
                                label = { Text(Strings.get("payment_easypaisa", currentLanguage), fontSize = 11.sp) }
                            )
                        }

                        if (selectedPaymentMethod == "easypaisa_qr") {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "EasyPaisa Mobile Account: 03049457463",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Account Title: Dastak Delivery Jhelum",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = if (currentLanguage == AppLanguage.URDU)
                                            "آرڈر ملنے پر آپ ادائیگی کا اسکرین شاٹ اپ لوڈ کر سکتے ہیں جسے ایڈمن تصدیق کرے گا۔"
                                        else
                                            "You can upload the payment receipt screenshot from active orders for admin verification.",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Final Order Submission
                        Button(
                            onClick = {
                                onLocationConfirmed(
                                    customerLat,
                                    customerLng,
                                    distanceKm,
                                    deliveryCharge,
                                    0.0,
                                    platformFee,
                                    0.0,
                                    deliveryBlock.trim().ifBlank { "General" },
                                    deliveryStreet.trim().ifBlank { "-" },
                                    deliveryHouse.trim().ifBlank { "-" },
                                    additionalInstructions.trim(),
                                    selectedPaymentMethod
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("final_submit_order_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Strings.get("btn_submit_order", currentLanguage),
                                fontWeight = FontWeight.Bold,
                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                            )
                        }
                    }
                }
            }
        }
    }
}
