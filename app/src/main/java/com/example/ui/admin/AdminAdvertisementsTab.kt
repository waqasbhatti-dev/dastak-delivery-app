package com.example.ui.admin

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.PricingCalculator
import com.example.data.model.Advertisement
import com.example.data.repository.AdvertisementRepository
import com.example.localization.AppLanguage
import com.example.localization.Strings
import com.example.ui.ad.AdvertisementMediaViewer
import com.example.ui.theme.DastakGreen
import com.example.ui.theme.DastakPrimary
import com.example.ui.theme.UrduFontFamily
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAdvertisementsTab(
    advertisementRepository: AdvertisementRepository,
    currentLanguage: AppLanguage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val adsList by advertisementRepository.observeAllAdvertisementsForAdmin().collectAsState(initial = emptyList())

    var selectedFilter by remember { mutableStateOf("ALL") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingAd by remember { mutableStateOf<Advertisement?>(null) }
    var previewAd by remember { mutableStateOf<Advertisement?>(null) }
    var deletingAdId by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isOperating by remember { mutableStateOf(false) }

    val filteredAds = remember(adsList, selectedFilter) {
        when (selectedFilter) {
            "ACTIVE" -> adsList.filter { it.isActive && it.status == "PUBLISHED" }
            "INACTIVE" -> adsList.filter { !it.isActive }
            "DRAFT" -> adsList.filter { it.status == "DRAFT" }
            "PAUSED" -> adsList.filter { it.status == "PAUSED" }
            else -> adsList
        }
    }

    val totalViews = remember(adsList) { adsList.sumOf { it.viewsCount } }
    val totalClicks = remember(adsList) { adsList.sumOf { it.clicksCount } }
    val activeCount = remember(adsList) { adsList.count { it.isActive && it.status == "PUBLISHED" } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header & Create Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = Strings.get("ad_management_title", currentLanguage),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                        )
                    )
                    Text(
                        text = Strings.get("locality_name", currentLanguage),
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("admin_create_ad_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Strings.get("ad_create_new", currentLanguage),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                    )
                }
            }
        }

        // Metrics Overview Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetricBox(
                        title = Strings.get("ad_status_published", currentLanguage),
                        value = activeCount.toString(),
                        icon = Icons.Default.Campaign,
                        iconColor = DastakGreen
                    )
                    Divider(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    MetricBox(
                        title = Strings.get("ad_views", currentLanguage),
                        value = totalViews.toString(),
                        icon = Icons.Default.Visibility,
                        iconColor = DastakPrimary
                    )
                    Divider(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    MetricBox(
                        title = Strings.get("ad_clicks", currentLanguage),
                        value = totalClicks.toString(),
                        icon = Icons.Default.PhoneCallback,
                        iconColor = Color(0xFFF57C00)
                    )
                }
            }
        }

        // Filter Chips Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("All (${adsList.size})") }
                )
                FilterChip(
                    selected = selectedFilter == "ACTIVE",
                    onClick = { selectedFilter = "ACTIVE" },
                    label = { Text("Active ($activeCount)") }
                )
                FilterChip(
                    selected = selectedFilter == "PAUSED",
                    onClick = { selectedFilter = "PAUSED" },
                    label = { Text("Paused") }
                )
                FilterChip(
                    selected = selectedFilter == "DRAFT",
                    onClick = { selectedFilter = "DRAFT" },
                    label = { Text("Draft") }
                )
            }
        }

        // Status banner
        statusMessage?.let { msg ->
            item {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DastakPrimary.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = DastakPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = msg, style = MaterialTheme.typography.bodySmall, color = DastakPrimary, modifier = Modifier.weight(1f))
                        IconButton(onClick = { statusMessage = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // Ads List
        if (filteredAds.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Strings.get("ad_no_active", currentLanguage),
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        } else {
            items(filteredAds, key = { it.id }) { ad ->
                AdminAdItemCard(
                    ad = ad,
                    currentLanguage = currentLanguage,
                    onPreview = { previewAd = ad },
                    onEdit = { editingAd = ad },
                    onToggleActive = { newActive ->
                        scope.launch {
                            isOperating = true
                            val res = advertisementRepository.toggleAdActive(ad.id, newActive)
                            isOperating = false
                            statusMessage = if (res.isSuccess) "Ad updated successfully" else "Failed to update ad"
                        }
                    },
                    onToggleStatus = { newStatus ->
                        scope.launch {
                            isOperating = true
                            val res = advertisementRepository.updateAdStatus(ad.id, newStatus)
                            isOperating = false
                            statusMessage = if (res.isSuccess) "Ad status updated to $newStatus" else "Failed to update status"
                        }
                    },
                    onDelete = { deletingAdId = ad.id }
                )
            }
        }
    }

    // Create / Edit Dialog
    if (showCreateDialog || editingAd != null) {
        val isEditing = (editingAd != null)
        val initialAd = editingAd ?: Advertisement(
            phone = AdvertisementRepository.DEFAULT_DASTAK_PHONE,
            category = "Delivery & Logistics"
        )

        AdminAdEditorDialog(
            initialAd = initialAd,
            isEditing = isEditing,
            currentLanguage = currentLanguage,
            advertisementRepository = advertisementRepository,
            onDismiss = {
                showCreateDialog = false
                editingAd = null
            },
            onSave = { adToSave ->
                scope.launch {
                    isOperating = true
                    val res = if (isEditing) {
                        advertisementRepository.updateAdvertisement(adToSave)
                    } else {
                        val createRes = advertisementRepository.createAdvertisement(adToSave)
                        if (createRes.isSuccess) Result.success(Unit) else Result.failure(createRes.exceptionOrNull()!!)
                    }
                    isOperating = false
                    if (res.isSuccess) {
                        statusMessage = "Advertisement saved successfully!"
                        showCreateDialog = false
                        editingAd = null
                    } else {
                        statusMessage = "Error saving ad: ${res.exceptionOrNull()?.message}"
                    }
                }
            }
        )
    }

    // Preview Dialog
    previewAd?.let { ad ->
        Dialog(
            onDismissRequest = { previewAd = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Strings.get("ad_preview", currentLanguage),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = { previewAd = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (ad.mediaUrl.isNotBlank()) {
                        AdvertisementMediaViewer(
                            ad = ad,
                            height = 200,
                            allowVideoPlayback = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Text(
                        text = ad.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                    )

                    if (ad.offer.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "★ ${ad.offer}",
                            color = DastakGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    if (ad.price != null && ad.price > 0.0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = PricingCalculator.formatPkr(ad.price),
                            color = DastakPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }

                    if (ad.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = ad.description,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Phone: ${ad.phone}",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        FilledTonalButton(
                            onClick = {},
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = DastakPrimary, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Strings.get("ad_call_now", currentLanguage))
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    deletingAdId?.let { adId ->
        AlertDialog(
            onDismissRequest = { deletingAdId = null },
            title = {
                Text(
                    text = Strings.get("ad_delete", currentLanguage),
                    fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                )
            },
            text = {
                Text(
                    text = Strings.get("ad_delete_confirm", currentLanguage),
                    fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isOperating = true
                            val res = advertisementRepository.deleteAdvertisement(adId)
                            isOperating = false
                            deletingAdId = null
                            statusMessage = if (res.isSuccess) "Ad deleted" else "Could not delete ad"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (currentLanguage == AppLanguage.URDU) "حذف کریں" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingAdId = null }) {
                    Text(if (currentLanguage == AppLanguage.URDU) "منسوخ" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
        Text(text = title, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
    }
}

@Composable
private fun AdminAdItemCard(
    ad: Advertisement,
    currentLanguage: AppLanguage,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onToggleStatus: (String) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Media thumbnail & info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Compact Media thumbnail
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .clickable { onPreview() }
                ) {
                    AdvertisementMediaViewer(
                        ad = ad,
                        height = 90,
                        allowVideoPlayback = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = ad.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                        )

                        // Status badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (ad.status) {
                                "PUBLISHED" -> DastakGreen.copy(alpha = 0.15f)
                                "PAUSED" -> Color(0xFFF57C00).copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            }
                        ) {
                            Text(
                                text = ad.status,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (ad.status) {
                                    "PUBLISHED" -> DastakGreen
                                    "PAUSED" -> Color(0xFFF57C00)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "${ad.businessName.ifBlank { "Dastak" }} • ${ad.category}",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        maxLines = 1
                    )

                    if (ad.offer.isNotBlank()) {
                        Text(
                            text = "Offer: ${ad.offer}",
                            style = MaterialTheme.typography.labelSmall.copy(color = DastakGreen, fontWeight = FontWeight.SemiBold),
                            maxLines = 1
                        )
                    }

                    if (ad.price != null && ad.price > 0.0) {
                        Text(
                            text = PricingCalculator.formatPkr(ad.price),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = DastakPrimary)
                        )
                    }

                    Text(
                        text = "Phone: ${ad.phone}",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics row: Views and Clicks
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = DastakPrimary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${ad.viewsCount} views", style = MaterialTheme.typography.labelSmall)

                    Spacer(modifier = Modifier.width(12.dp))

                    Icon(Icons.Default.PhoneCallback, contentDescription = null, tint = Color(0xFFF57C00), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${ad.clicksCount} inquiries", style = MaterialTheme.typography.labelSmall)
                }

                // Action buttons: Preview, Quick Toggle, Edit, Delete
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPreview, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Visibility, contentDescription = "Preview", modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = {
                            if (ad.status == "PUBLISHED") {
                                onToggleStatus("PAUSED")
                            } else {
                                onToggleStatus("PUBLISHED")
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (ad.status == "PUBLISHED") Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (ad.status == "PUBLISHED") "Pause Ad" else "Publish Ad",
                            tint = if (ad.status == "PUBLISHED") Color(0xFFF57C00) else DastakGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminAdEditorDialog(
    initialAd: Advertisement,
    isEditing: Boolean,
    currentLanguage: AppLanguage,
    advertisementRepository: AdvertisementRepository,
    onDismiss: () -> Unit,
    onSave: (Advertisement) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(initialAd.title) }
    var businessName by remember { mutableStateOf(initialAd.businessName) }
    var category by remember { mutableStateOf(initialAd.category.ifBlank { "Delivery & Logistics" }) }
    var description by remember { mutableStateOf(initialAd.description) }
    var offer by remember { mutableStateOf(initialAd.offer) }
    var priceInput by remember { mutableStateOf(initialAd.price?.toString() ?: "") }
    var phone by remember { mutableStateOf(initialAd.phone.ifBlank { AdvertisementRepository.DEFAULT_DASTAK_PHONE }) }
    var mediaType by remember { mutableStateOf(initialAd.mediaType) } // "IMAGE" or "VIDEO"
    var mediaUrl by remember { mutableStateOf(initialAd.mediaUrl) }
    var posterUrl by remember { mutableStateOf(initialAd.posterUrl) }
    var isActive by remember { mutableStateOf(initialAd.isActive) }
    var status by remember { mutableStateOf(initialAd.status) } // "DRAFT", "PUBLISHED", "PAUSED"

    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableIntStateOf(0) }
    var uploadErrorMessage by remember { mutableStateOf<String?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val categories = listOf(
        "Delivery & Logistics",
        "Food & Groceries",
        "Home Services",
        "Healthcare & Pharmacy",
        "Shopping & Retail",
        "Local Promotions",
        "Other"
    )

    // Media Picker Launcher
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val isVideo = mediaType == "VIDEO"
            isUploading = true
            uploadProgress = 10
            uploadErrorMessage = null

            scope.launch {
                val res = advertisementRepository.uploadMedia(
                    context = context,
                    uri = uri,
                    isVideo = isVideo,
                    onProgress = { uploadProgress = it }
                )
                isUploading = false
                if (res.isSuccess) {
                    mediaUrl = res.getOrNull().orEmpty()
                } else {
                    uploadErrorMessage = res.exceptionOrNull()?.message ?: "Upload failed"
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Dialog Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) Strings.get("ad_edit", currentLanguage) else Strings.get("ad_create_new", currentLanguage),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Form Body
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Validation or Upload Errors
                    if (validationError != null || uploadErrorMessage != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = validationError ?: uploadErrorMessage.orEmpty(),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    // Title
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it; validationError = null },
                            label = { Text(Strings.get("ad_title", currentLanguage)) },
                            placeholder = { Text("e.g., Dastak Delivery Service / سپر گروسری سیل") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ad_title_input"),
                            singleLine = true
                        )
                    }

                    // Business Name & Category Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = businessName,
                                onValueChange = { businessName = it },
                                label = { Text(Strings.get("ad_business_name", currentLanguage)) },
                                modifier = Modifier.weight(1.2f),
                                singleLine = true
                            )

                            var categoryExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = category,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(Strings.get("ad_category", currentLanguage)) },
                                    trailingIcon = {
                                        IconButton(onClick = { categoryExpanded = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                DropdownMenu(
                                    expanded = categoryExpanded,
                                    onDismissRequest = { categoryExpanded = false }
                                ) {
                                    categories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat) },
                                            onClick = {
                                                category = cat
                                                categoryExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Description
                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it; validationError = null },
                            label = { Text(Strings.get("ad_description", currentLanguage)) },
                            placeholder = { Text("Promotional details, service scope in Jhelum City Housing...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )
                    }

                    // Offer & Price
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = offer,
                                onValueChange = { offer = it },
                                label = { Text(Strings.get("ad_offer", currentLanguage)) },
                                placeholder = { Text("e.g., 20% OFF / Free Delivery") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = priceInput,
                                onValueChange = { priceInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text(Strings.get("ad_price", currentLanguage)) },
                                placeholder = { Text("500") },
                                modifier = Modifier.weight(0.8f),
                                singleLine = true
                            )
                        }
                    }

                    // Contact Phone
                    item {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text(Strings.get("ad_phone", currentLanguage)) },
                            placeholder = { Text("03049457463") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ad_phone_input"),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = DastakPrimary)
                            }
                        )
                    }

                    // Media Type Selection: Image vs Video
                    item {
                        Column {
                            Text(
                                text = Strings.get("ad_media_type", currentLanguage),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FilterChip(
                                    selected = mediaType == "IMAGE",
                                    onClick = { mediaType = "IMAGE" },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(Strings.get("ad_media_image", currentLanguage))
                                        }
                                    }
                                )
                                FilterChip(
                                    selected = mediaType == "VIDEO",
                                    onClick = { mediaType = "VIDEO" },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(Strings.get("ad_media_video", currentLanguage))
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Media Upload / Device Picker & Progress
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = Strings.get("ad_media_source", currentLanguage),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            val type = if (mediaType == "VIDEO") {
                                                ActivityResultContracts.PickVisualMedia.VideoOnly
                                            } else {
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            }
                                            mediaPickerLauncher.launch(PickVisualMediaRequest(type))
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(Strings.get("ad_upload_device", currentLanguage), fontSize = 12.sp)
                                    }
                                }

                                if (isUploading) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(Strings.get("ad_uploading", currentLanguage), fontSize = 11.sp)
                                            Text("$uploadProgress%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { uploadProgress / 100f },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = mediaUrl,
                                    onValueChange = { mediaUrl = it },
                                    label = { Text(Strings.get("ad_media_url_input", currentLanguage)) },
                                    placeholder = { Text("https://... or uploaded download URL") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                if (mediaType == "VIDEO") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = posterUrl,
                                        onValueChange = { posterUrl = it },
                                        label = { Text(Strings.get("ad_poster_url", currentLanguage)) },
                                        placeholder = { Text("https://... image poster URL") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }

                                // In-editor Preview if mediaUrl exists
                                if (mediaUrl.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Preview:", style = MaterialTheme.typography.labelSmall)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    AdvertisementMediaViewer(
                                        ad = initialAd.copy(
                                            mediaUrl = mediaUrl,
                                            mediaType = mediaType,
                                            posterUrl = posterUrl
                                        ),
                                        height = 120,
                                        allowVideoPlayback = false,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // Status & Active Toggle
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = Strings.get("ad_active_toggle", currentLanguage),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Visible in customer carousel",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                            Switch(
                                checked = isActive,
                                onCheckedChange = { isActive = it }
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = status == "PUBLISHED",
                                onClick = {
                                    status = "PUBLISHED"
                                    isActive = true
                                },
                                label = { Text(Strings.get("ad_status_published", currentLanguage)) }
                            )
                            FilterChip(
                                selected = status == "PAUSED",
                                onClick = {
                                    status = "PAUSED"
                                    isActive = false
                                },
                                label = { Text(Strings.get("ad_status_paused", currentLanguage)) }
                            )
                            FilterChip(
                                selected = status == "DRAFT",
                                onClick = {
                                    status = "DRAFT"
                                    isActive = false
                                },
                                label = { Text(Strings.get("ad_status_draft", currentLanguage)) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (currentLanguage == AppLanguage.URDU) "منسوخ" else "Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                validationError = Strings.get("ad_validation_title", currentLanguage)
                                return@Button
                            }
                            if (description.isBlank()) {
                                validationError = Strings.get("ad_validation_desc", currentLanguage)
                                return@Button
                            }
                            if (phone.isBlank()) {
                                validationError = Strings.get("ad_validation_phone", currentLanguage)
                                return@Button
                            }

                            if (isUploading) {
                                validationError = if (currentLanguage == AppLanguage.URDU)
                                    "میڈیا اپ لوڈ ہو رہا ہے، براہِ کرم مکمل ہونے کا انتظار فرمائیں۔"
                                else
                                    "Please wait for media upload to finish before saving."
                                return@Button
                            }
                            if ((status == "PUBLISHED" || isActive) && mediaUrl.isBlank()) {
                                validationError = if (currentLanguage == AppLanguage.URDU)
                                    "اشتہار شائع کرنے کے لیے تصویر یا ویڈیو منتخب کر کے اپ لوڈ کرنا لازمی ہے۔"
                                else
                                    "Uploading media (Image or Video) is mandatory to publish an advertisement."
                                return@Button
                            }
                            val cleanMedia = mediaUrl.trim()
                            if (cleanMedia.startsWith("content://") || cleanMedia.startsWith("file://")) {
                                validationError = if (currentLanguage == AppLanguage.URDU)
                                    "مقامی فائل پاتھ شائع نہیں ہو سکتا۔ براہِ کرم کلاؤڈ اسٹوریج پر اپ لوڈ کریں۔"
                                else
                                    "Local device file paths cannot be published. Please upload media to cloud storage."
                                return@Button
                            }
                            val cleanPoster = posterUrl.trim()
                            if (cleanPoster.startsWith("content://") || cleanPoster.startsWith("file://")) {
                                validationError = "Poster URL cannot be a local phone file path."
                                return@Button
                            }

                            val adToSave = initialAd.copy(
                                title = title.trim(),
                                businessName = businessName.trim(),
                                category = category.trim(),
                                description = description.trim(),
                                offer = offer.trim(),
                                price = priceInput.toDoubleOrNull(),
                                phone = phone.trim(),
                                mediaUrl = cleanMedia,
                                mediaType = mediaType,
                                posterUrl = cleanPoster,
                                isActive = isActive,
                                status = status
                            )
                            onSave(adToSave)
                        },
                        enabled = !isUploading,
                        colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEditing) Strings.get("ad_save_changes", currentLanguage) else Strings.get("ad_publish_now", currentLanguage),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
