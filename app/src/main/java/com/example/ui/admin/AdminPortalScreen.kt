package com.example.ui.admin

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PricingCalculator
import com.example.data.model.*
import com.example.data.repository.AdvertisementRepository
import com.example.data.repository.OrderRepository
import com.example.data.repository.RiderRepository
import com.example.data.repository.TermsRepository
import com.example.data.repository.UserRepository
import com.example.localization.AppLanguage
import com.example.localization.Strings
import com.example.ui.components.LanguageSelector
import com.example.ui.theme.DastakPrimary
import com.example.ui.theme.DastakSecondary
import com.example.ui.theme.UrduFontFamily
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.Locale

const val AUTHORIZED_ADMIN_EMAIL = "mwaqasbhatti1230@gmail.com"
val AUTHORIZED_ADMIN_EMAILS = setOf(AUTHORIZED_ADMIN_EMAIL, "mwaqasbhatti57570@gmail.com")
fun isAuthorizedAdmin(email: String?): Boolean =
    email != null && AUTHORIZED_ADMIN_EMAILS.any { it.equals(email, ignoreCase = true) }

enum class DateFilterType {
    TODAY,
    LAST_7_DAYS,
    LAST_30_DAYS,
    ALL_TIME
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPortalScreen(
    userEmail: String,
    userUid: String,
    currentLanguage: AppLanguage,
    userRepository: UserRepository,
    riderRepository: RiderRepository,
    orderRepository: OrderRepository,
    termsRepository: TermsRepository,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    advertisementRepository: AdvertisementRepository,
    modifier: Modifier = Modifier
) {
    val isAuthorized = isAuthorizedAdmin(userEmail)

    if (!isAuthorized) {
        AdminAccessDeniedScreen(
            currentEmail = userEmail,
            currentLanguage = currentLanguage,
            onLanguageChange = onLanguageChange,
            onLogout = onLogout
        )
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    val adminTabs = listOf(
        Pair("nav_overview", Icons.Default.Dashboard),
        Pair("admin_orders_title", Icons.Default.ShoppingBag),
        Pair("admin_ads_title", Icons.Default.Campaign),
        Pair("nav_applications", Icons.Default.AssignmentInd),
        Pair("nav_riders", Icons.Default.DirectionsBike),
        Pair("admin_complaints_title", Icons.Default.ReportProblem),
        Pair("admin_feedback_title", Icons.Default.Star),
        Pair("portal_customer", Icons.Default.People),
        Pair("nav_terms_mgmt", Icons.Default.Gavel)
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = Strings.get("portal_admin", currentLanguage),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DastakPrimary,
                                    fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge(containerColor = DastakPrimary, contentColor = Color.White) {
                                Text("Super Admin", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontSize = 10.sp)
                            }
                        }
                        Text(
                            text = AUTHORIZED_ADMIN_EMAIL,
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        modifier = Modifier.testTag("admin_logout_button")
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 12.dp,
                modifier = Modifier.fillMaxWidth().testTag("admin_portal_tab_row")
            ) {
                adminTabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(tab.second, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = Strings.get(tab.first, currentLanguage),
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                                )
                            }
                        },
                        modifier = Modifier.testTag("admin_tab_$index")
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> AdminOverviewTab(
                        userRepository = userRepository,
                        riderRepository = riderRepository,
                        orderRepository = orderRepository,
                        currentLanguage = currentLanguage
                    )
                    1 -> AdminOrdersTab(
                        orderRepository = orderRepository,
                        currentLanguage = currentLanguage
                    )
                    2 -> AdminAdvertisementsTab(
                        advertisementRepository = advertisementRepository,
                        currentLanguage = currentLanguage
                    )
                    3 -> AdminApplicationsTab(
                        riderRepository = riderRepository,
                        adminUid = userUid,
                        currentLanguage = currentLanguage
                    )
                    4 -> AdminApprovedRidersTab(
                        riderRepository = riderRepository,
                        orderRepository = orderRepository,
                        currentLanguage = currentLanguage
                    )
                    5 -> AdminComplaintsTab(
                        orderRepository = orderRepository,
                        currentLanguage = currentLanguage
                    )
                    6 -> AdminFeedbackTab(
                        orderRepository = orderRepository,
                        riderRepository = riderRepository,
                        currentLanguage = currentLanguage
                    )
                    7 -> AdminCustomersTab(
                        userRepository = userRepository,
                        currentLanguage = currentLanguage
                    )
                    8 -> AdminTermsManagementTab(
                        termsRepository = termsRepository,
                        currentLanguage = currentLanguage
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminAccessDeniedScreen(
    currentEmail: String,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                LanguageSelector(currentLanguage = currentLanguage, onLanguageChange = onLanguageChange)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = Strings.get("admin_access_denied", currentLanguage),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = Strings.get("admin_denied_desc", currentLanguage),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Signed in as: $currentEmail",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("admin_denied_logout_button")
            ) {
                Text(text = Strings.get("nav_logout", currentLanguage))
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 0: OVERVIEW & REPORTS WITH DATE FILTER
// -------------------------------------------------------------
@Composable
private fun AdminOverviewTab(
    userRepository: UserRepository,
    riderRepository: RiderRepository,
    orderRepository: OrderRepository,
    currentLanguage: AppLanguage
) {
    var allCustomers by remember { mutableStateOf<List<User>>(emptyList()) }
    var allApps by remember { mutableStateOf<List<RiderApplication>>(emptyList()) }
    var allRiders by remember { mutableStateOf<List<Rider>>(emptyList()) }
    var allOrders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var allComplaints by remember { mutableStateOf<List<Complaint>>(emptyList()) }
    var centralSettings by remember { mutableStateOf(AdminSettings()) }
    var isEditingCentralPoint by remember { mutableStateOf(false) }
    var inputLat by remember { mutableStateOf("32.9957231") }
    var inputLng by remember { mutableStateOf("73.6522227") }
    var inputLocality by remember { mutableStateOf("Jhelum City Housing") }
    var isLoading by remember { mutableStateOf(true) }
    var selectedDateFilter by remember { mutableStateOf(DateFilterType.ALL_TIME) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            allCustomers = userRepository.getAllCustomers()
            allApps = riderRepository.getAllApplications()
            allRiders = riderRepository.getAllApprovedRiders()
            allOrders = orderRepository.getAllOrdersForAdmin()
            allComplaints = orderRepository.getAllComplaintsForAdmin()
            val loadedSettings = orderRepository.getCentralSettings()
            centralSettings = loadedSettings
            inputLat = loadedSettings.centralLatitude.toString()
            inputLng = loadedSettings.centralLongitude.toString()
            inputLocality = loadedSettings.locality
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = DastakPrimary)
        }
    } else {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        val filteredOrders = remember(allOrders, selectedDateFilter) {
            when (selectedDateFilter) {
                DateFilterType.TODAY -> {
                    calendar.timeInMillis = now
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    val startOfDay = calendar.time
                    allOrders.filter { it.createdAt != null && it.createdAt.toDate().after(startOfDay) }
                }
                DateFilterType.LAST_7_DAYS -> {
                    val sevenDaysAgo = Date(now - (7L * 24 * 60 * 60 * 1000))
                    allOrders.filter { it.createdAt != null && it.createdAt.toDate().after(sevenDaysAgo) }
                }
                DateFilterType.LAST_30_DAYS -> {
                    val thirtyDaysAgo = Date(now - (30L * 24 * 60 * 60 * 1000))
                    allOrders.filter { it.createdAt != null && it.createdAt.toDate().after(thirtyDaysAgo) }
                }
                DateFilterType.ALL_TIME -> allOrders
            }
        }

        val totalCustomersCount = allCustomers.size
        val pendingAppsCount = allApps.count { it.status == "pending" }
        val approvedRidersCount = allRiders.count { it.status == "approved" }
        val onlineRidersCount = allRiders.count { it.isOnline }

        val totalOrdersCount = filteredOrders.size
        val pendingOrdersCount = filteredOrders.count { it.orderStatus == "Pending" }
        val activeOrdersCount = filteredOrders.count {
            it.orderStatus in listOf("Assigned", "Shopping", "Awaiting Customer Bill Approval", "Bill Rejected", "Bill Approved", "Out for Delivery")
        }
        val deliveredOrdersCount = filteredOrders.count { it.orderStatus == "Delivered" }
        val cancelledOrdersCount = filteredOrders.count { it.orderStatus == "Cancelled" }
        val pendingBillApprovalsCount = filteredOrders.count {
            it.orderStatus == "Awaiting Customer Bill Approval" || it.billApprovalStatus == "pending_approval"
        }
        val openComplaintsCount = allComplaints.count { it.status.lowercase() == "open" }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = Strings.get("locality_name", currentLanguage),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (currentLanguage == AppLanguage.URDU) "لائیو آپریشنز اور انتظامی رپورٹ" else "Live Operations & Executive Report",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }

            // Date Filters
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedDateFilter == DateFilterType.ALL_TIME,
                        onClick = { selectedDateFilter = DateFilterType.ALL_TIME },
                        label = { Text(if (currentLanguage == AppLanguage.URDU) "تمام تاریخ" else "All Time", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedDateFilter == DateFilterType.TODAY,
                        onClick = { selectedDateFilter = DateFilterType.TODAY },
                        label = { Text(if (currentLanguage == AppLanguage.URDU) "آج" else "Today", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedDateFilter == DateFilterType.LAST_7_DAYS,
                        onClick = { selectedDateFilter = DateFilterType.LAST_7_DAYS },
                        label = { Text(if (currentLanguage == AppLanguage.URDU) "7 ایام" else "7 Days", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedDateFilter == DateFilterType.LAST_30_DAYS,
                        onClick = { selectedDateFilter = DateFilterType.LAST_30_DAYS },
                        label = { Text(if (currentLanguage == AppLanguage.URDU) "30 ایام" else "30 Days", fontSize = 11.sp) }
                    )
                }
            }

            // Metric Cards Grid
            item {
                Text(text = if (currentLanguage == AppLanguage.URDU) "صارفین و رائیڈرز" else "Customers & Fleet", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminMetricCard(
                        title = if (currentLanguage == AppLanguage.URDU) "کل رجسٹرڈ صارفین" else "Registered Customers",
                        value = "$totalCustomersCount",
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    AdminMetricCard(
                        title = if (currentLanguage == AppLanguage.URDU) "منظور شدہ رائیڈرز" else "Approved Riders",
                        value = "$approvedRidersCount",
                        accentColor = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminMetricCard(
                        title = if (currentLanguage == AppLanguage.URDU) "آن لائن رائیڈرز" else "Online Riders",
                        value = "$onlineRidersCount",
                        accentColor = if (onlineRidersCount > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.weight(1f)
                    )
                    AdminMetricCard(
                        title = if (currentLanguage == AppLanguage.URDU) "زیرِ التوا درخواستیں" else "Pending Applications",
                        value = "$pendingAppsCount",
                        accentColor = DastakSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text(text = if (currentLanguage == AppLanguage.URDU) "آرڈرز کی تفاصیل" else "Order Operations", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminMetricCard(
                        title = if (currentLanguage == AppLanguage.URDU) "کل آرڈرز" else "Total Orders",
                        value = "$totalOrdersCount",
                        accentColor = Color(0xFF6B46C1),
                        modifier = Modifier.weight(1f)
                    )
                    AdminMetricCard(
                        title = if (currentLanguage == AppLanguage.URDU) "زیرِ التوا آرڈرز" else "Pending Orders",
                        value = "$pendingOrdersCount",
                        accentColor = DastakSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminMetricCard(
                        title = if (currentLanguage == AppLanguage.URDU) "فعال آرڈرز" else "Active Orders",
                        value = "$activeOrdersCount",
                        accentColor = Color(0xFF0288D1),
                        modifier = Modifier.weight(1f)
                    )
                    AdminMetricCard(
                        title = if (currentLanguage == AppLanguage.URDU) "مکمل شدہ ترسیل" else "Delivered Orders",
                        value = "$deliveredOrdersCount",
                        accentColor = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminMetricCard(
                        title = if (currentLanguage == AppLanguage.URDU) "منسوخ شدہ آرڈرز" else "Cancelled Orders",
                        value = "$cancelledOrdersCount",
                        accentColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    AdminMetricCard(
                        title = if (currentLanguage == AppLanguage.URDU) "زیرِ التوا بل منظوری" else "Pending Bill Approvals",
                        value = "$pendingBillApprovalsCount",
                        accentColor = DastakSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminMetricCard(
                        title = if (currentLanguage == AppLanguage.URDU) "کھلی شکایات" else "Open Complaints",
                        value = "$openComplaintsCount",
                        accentColor = if (openComplaintsCount > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Fixed Central Delivery Point Configuration Card (Requirement 4)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("admin_central_point_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationCity, contentDescription = null, tint = DastakPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (currentLanguage == AppLanguage.URDU) "مقررہ مرکزی ترسیلی مقام (Origin Point)" else "Fixed Central Delivery Point (Origin)",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                                    )
                                )
                            }
                            IconButton(onClick = { isEditingCentralPoint = !isEditingCentralPoint }) {
                                Icon(if (isEditingCentralPoint) Icons.Default.Close else Icons.Default.Edit, contentDescription = "Edit Central Point")
                            }
                        }

                        Text(
                            text = if (currentLanguage == AppLanguage.URDU)
                                "تمام آرڈرز کا فاصلہ اور کرایہ (70 روپے فی کلومیٹر) اس مرکزی مقام سے کسٹمر کی کنفرمڈ لوکیشن تک سڑک کے اصل فاصلے پر طے کیا جاتا ہے۔"
                            else
                                "All delivery distances and fees (PKR 70/km) are strictly measured from this fixed origin point to the customer's confirmed delivery location via actual road routing.",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                        )

                        if (!isEditingCentralPoint) {
                            Text(text = "Locality: ${centralSettings.locality}", fontWeight = FontWeight.Medium)
                            Text(text = "Latitude: ${centralSettings.centralLatitude} | Longitude: ${centralSettings.centralLongitude}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Delivery Fee Rate: PKR 70.00 / km (OSRM Road Network)", style = MaterialTheme.typography.bodySmall.copy(color = DastakPrimary, fontWeight = FontWeight.Bold))
                        } else {
                            OutlinedTextField(
                                value = inputLocality,
                                onValueChange = { inputLocality = it },
                                label = { Text("Locality") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = inputLat,
                                    onValueChange = { inputLat = it },
                                    label = { Text("Latitude") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = inputLng,
                                    onValueChange = { inputLng = it },
                                    label = { Text("Longitude") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            Button(
                                onClick = {
                                    val lat = inputLat.toDoubleOrNull() ?: 32.9957231
                                    val lng = inputLng.toDoubleOrNull() ?: 73.6522227
                                    val updated = centralSettings.copy(
                                        centralLatitude = lat,
                                        centralLongitude = lng,
                                        locality = inputLocality.trim()
                                    )
                                    scope.launch {
                                        orderRepository.updateCentralSettings(updated)
                                        centralSettings = updated
                                        isEditingCentralPoint = false
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary),
                                modifier = Modifier.fillMaxWidth().testTag("save_central_point_button")
                            ) {
                                Text(if (currentLanguage == AppLanguage.URDU) "تبدیلی محفوظ کریں" else "Save Central Point Configuration")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminMetricCard(title: String, value: String, accentColor: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = accentColor))
        }
    }
}

// -------------------------------------------------------------
// TAB 1: ORDERS MANAGEMENT
// -------------------------------------------------------------
@Composable
private fun AdminOrdersTab(
    orderRepository: OrderRepository,
    currentLanguage: AppLanguage
) {
    var allOrders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("All") }
    var inspectingOrder by remember { mutableStateOf<Order?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            allOrders = orderRepository.getAllOrdersForAdmin()
            isLoading = false
        }
    }

    val filteredOrders = remember(allOrders, searchQuery, selectedStatusFilter) {
        allOrders.filter { ord ->
            val matchesQuery = searchQuery.isBlank() ||
                ord.id.contains(searchQuery, ignoreCase = true) ||
                ord.customerName.contains(searchQuery, ignoreCase = true) ||
                ord.customerPhone.contains(searchQuery, ignoreCase = true) ||
                ord.itemDescription.contains(searchQuery, ignoreCase = true)

            val matchesStatus = when (selectedStatusFilter) {
                "All" -> true
                "Pending" -> ord.orderStatus == "Pending"
                "Active" -> ord.orderStatus in listOf("Assigned", "Shopping", "Awaiting Customer Bill Approval", "Bill Rejected", "Bill Approved", "Out for Delivery")
                "Delivered" -> ord.orderStatus == "Delivered"
                "Cancelled" -> ord.orderStatus == "Cancelled"
                else -> true
            }
            matchesQuery && matchesStatus
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text(if (currentLanguage == AppLanguage.URDU) "آرڈر آئی ڈی، نام، فون تلاش کریں" else "Search by Order ID, Resident, Phone") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().testTag("admin_orders_search_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val statuses = listOf("All", "Pending", "Active", "Delivered", "Cancelled")
            items(statuses) { st ->
                FilterChip(
                    selected = selectedStatusFilter == st,
                    onClick = { selectedStatusFilter = st },
                    label = { Text(st, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DastakPrimary)
            }
        } else if (filteredOrders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (currentLanguage == AppLanguage.URDU) "کوئی آرڈر نہیں ملا۔" else "No orders matching criteria.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredOrders) { ord ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().clickable { inspectingOrder = ord }.testTag("admin_order_card_${ord.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Order #${ord.id.take(6).uppercase()}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Badge(
                                    containerColor = when (ord.orderStatus) {
                                        "Delivered" -> Color(0xFF2E7D32)
                                        "Awaiting Customer Bill Approval" -> DastakSecondary
                                        "Out for Delivery" -> Color(0xFF0288D1)
                                        "Cancelled" -> MaterialTheme.colorScheme.error
                                        else -> DastakPrimary
                                    }
                                ) {
                                    Text(
                                        text = Strings.getOrderStatusLabel(ord.orderStatus, currentLanguage),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                                    )
                                }
                            }

                            Text(
                                text = "Customer: ${ord.customerName.ifBlank { "Resident" }} (${ord.customerPhone})",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Address: ${ord.deliveryBlock}, ${ord.deliveryStreet}, ${ord.deliveryHouseNumber}",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = "Item: ${ord.itemDescription}",
                                style = MaterialTheme.typography.bodySmall
                            )

                            if (ord.assignedRiderId.isNotBlank()) {
                                Text(
                                    text = "Assigned Rider ID: ${ord.assignedRiderId.take(8)}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = DastakPrimary)
                                )
                            } else {
                                Text(
                                    text = "Rider: Unassigned",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                                )
                            }

                            Divider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Total Bill: PKR ${String.format(Locale.US, "%.2f", ord.totalAmount)}", fontWeight = FontWeight.Bold, color = DastakPrimary)
                                Text(text = "Bill Status: ${ord.billApprovalStatus}", style = MaterialTheme.typography.bodySmall)
                            }

                            if (ord.deliveredAt != null) {
                                Text(
                                    text = "Delivered at: ${ord.deliveredAt.toDate()}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF2E7D32))
                                )
                            }

                            OutlinedButton(
                                onClick = { inspectingOrder = ord },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("inspect_order_btn_${ord.id}")
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (currentLanguage == AppLanguage.URDU) "آرڈر کی مکمل تفصیلات دیکھیں" else "Inspect Full Order & Breakdown")
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Full Order History & Pricing Breakdown Inspection
    if (inspectingOrder != null) {
        val o = inspectingOrder!!
        AlertDialog(
            onDismissRequest = { inspectingOrder = null },
            title = {
                Text(
                    text = "Order Details #${o.id.take(6).uppercase()}",
                    fontWeight = FontWeight.Bold
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
                    Text(text = "Customer: ${o.customerName} (${o.customerPhone})")
                    Text(text = "Delivery Address: ${o.deliveryBlock}, ${o.deliveryStreet}, ${o.deliveryHouseNumber}")
                    if (o.additionalAddressInstructions.isNotBlank()) {
                        Text(text = "Instructions: ${o.additionalAddressInstructions}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(text = "Item Request: ${o.itemDescription}")

                    if (o.createdAt != null) {
                        Text(text = "Placed: ${o.createdAt.toDate()}", style = MaterialTheme.typography.labelSmall)
                    }
                    if (o.riderAcceptedAt != null) {
                        Text(text = "Rider Accepted: ${o.riderAcceptedAt.toDate()}", style = MaterialTheme.typography.labelSmall)
                    }
                    if (o.deliveryStartedAt != null) {
                        Text(text = "Out for Delivery: ${o.deliveryStartedAt.toDate()}", style = MaterialTheme.typography.labelSmall)
                    }
                    if (o.deliveredAt != null) {
                        Text(text = "Delivered: ${o.deliveredAt.toDate()}", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold))
                    }

                    Divider()

                    // Authoritative Route & Coordinates
                    Text(text = "Origin & Destination Telemetry:", fontWeight = FontWeight.Bold, color = DastakPrimary)
                    Text(text = "Central Origin Point: Lat ${String.format(Locale.US, "%.6f", o.centralLatitude)}, Lng ${String.format(Locale.US, "%.6f", o.centralLongitude)} (Jhelum City Housing)", style = MaterialTheme.typography.bodySmall)
                    if (o.customerLatitude != 0.0) {
                        Text(text = "Confirmed Customer Pin: Lat ${String.format(Locale.US, "%.6f", o.customerLatitude)}, Lng ${String.format(Locale.US, "%.6f", o.customerLongitude)}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(text = "Road-Route Network Distance: ${o.deliveryDistanceKm} km", fontWeight = FontWeight.Medium)

                    Divider()

                    // Exact Pricing Breakdown (Prompt 7)
                    val serviceFeeRateLabel = if (o.billAmount > 0 && o.billAmount < 300.0) "7%" else "5%"
                    Text(text = "Authoritative Pricing Breakdown:", fontWeight = FontWeight.Bold, color = DastakPrimary)
                    Text(text = "Actual Item Bill: PKR ${String.format(Locale.US, "%.2f", o.billAmount)}")
                    Text(text = "Service Fee ($serviceFeeRateLabel): PKR ${String.format(Locale.US, "%.2f", o.serviceCharge)}")
                    Text(text = "Delivery Fee (${o.deliveryDistanceKm} km @ PKR 70/km): PKR ${String.format(Locale.US, "%.2f", o.deliveryCharge)}")
                    Text(text = "Platform Fee: PKR 4.00")
                    Text(
                        text = "Total Order Amount: PKR ${String.format(Locale.US, "%.2f", o.totalAmount)}",
                        fontWeight = FontWeight.Bold,
                        color = DastakPrimary
                    )

                    if (o.billPhotoUrl.isNotBlank() && o.billPhotoUrl.startsWith("data:image")) {
                        Text(text = "Rider Bill / Receipt Photo:", fontWeight = FontWeight.Bold)
                        val base64 = o.billPhotoUrl.substringAfter("base64,")
                        val bytes = try { Base64.decode(base64, Base64.DEFAULT) } catch (e: Exception) { null }
                        val bmp = if (bytes != null) BitmapFactory.decodeByteArray(bytes, 0, bytes.size) else null
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Receipt",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // EasyPaisa Payment Verification Workflow (Requirement 9)
                    Divider()
                    Text(text = "Payment Status & Verification:", fontWeight = FontWeight.Bold, color = DastakPrimary)
                    Text(text = "Method: ${o.paymentMethod.uppercase()} | Status: ${o.paymentStatus.uppercase()}")

                    if (o.paymentMethod == "easypaisa_qr") {
                        if (o.paymentAmountPaid > 0.0) {
                            Text(text = "Amount Paid: PKR ${String.format(Locale.US, "%.2f", o.paymentAmountPaid)}", fontWeight = FontWeight.Medium)
                        }
                        if (o.paymentScreenshotUrl.isNotBlank() && o.paymentScreenshotUrl.startsWith("data:image")) {
                            Text(text = "Customer Payment Screenshot:", fontWeight = FontWeight.Bold)
                            val base64 = o.paymentScreenshotUrl.substringAfter("base64,")
                            val bytes = try { Base64.decode(base64, Base64.DEFAULT) } catch (e: Exception) { null }
                            val bmp = if (bytes != null) BitmapFactory.decodeByteArray(bytes, 0, bytes.size) else null
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Payment Proof",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        if (o.paymentStatus == "payment_uploaded") {
                            var rejectReasonInput by remember { mutableStateOf("") }
                            OutlinedTextField(
                                value = rejectReasonInput,
                                onValueChange = { rejectReasonInput = it },
                                label = { Text("Rejection Reason (if rejecting)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            orderRepository.reviewPayment(o.id, isVerified = true)
                                            inspectingOrder = null
                                            allOrders = orderRepository.getAllOrdersForAdmin()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("admin_approve_payment_button")
                                ) {
                                    Text("Verify & Approve")
                                }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            orderRepository.reviewPayment(o.id, isVerified = false, rejectionReason = rejectReasonInput)
                                            inspectingOrder = null
                                            allOrders = orderRepository.getAllOrdersForAdmin()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("admin_reject_payment_button")
                                ) {
                                    Text("Reject Payment")
                                }
                            }
                        } else if (o.paymentStatus == "verified") {
                            Text(text = "Payment verified by Admin", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        } else if (o.paymentStatus == "rejected") {
                            Text(text = "Payment rejected: ${o.paymentRejectionReason}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { inspectingOrder = null }, colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary)) {
                    Text("Close")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// TAB 2: RIDER APPLICATIONS REVIEW (Pending, Approved, Rejected)
// -------------------------------------------------------------
@Composable
private fun AdminApplicationsTab(
    riderRepository: RiderRepository,
    adminUid: String,
    currentLanguage: AppLanguage
) {
    var applications by remember { mutableStateOf<List<RiderApplication>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedAppForReject by remember { mutableStateOf<RiderApplication?>(null) }
    var rejectReason by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            isLoading = true
            applications = riderRepository.getAllApplications()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    var selectedFilterTab by remember { mutableIntStateOf(0) } // 0: Pending, 1: Approved, 2: Rejected

    val filteredApps = remember(applications, selectedFilterTab) {
        when (selectedFilterTab) {
            0 -> applications.filter { it.status == "pending" }
            1 -> applications.filter { it.status == "approved" }
            2 -> applications.filter { it.status == "rejected" }
            else -> applications
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedFilterTab) {
            Tab(
                selected = selectedFilterTab == 0,
                onClick = { selectedFilterTab = 0 },
                text = { Text(Strings.get("admin_tab_pending", currentLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("admin_pending_apps_tab")
            )
            Tab(
                selected = selectedFilterTab == 1,
                onClick = { selectedFilterTab = 1 },
                text = { Text(Strings.get("admin_tab_approved", currentLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("admin_approved_apps_tab")
            )
            Tab(
                selected = selectedFilterTab == 2,
                onClick = { selectedFilterTab = 2 },
                text = { Text(Strings.get("admin_tab_rejected", currentLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("admin_rejected_apps_tab")
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DastakPrimary)
            }
        } else if (filteredApps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = if (currentLanguage == AppLanguage.URDU) "اس زمرے میں کوئی درخواست نہیں ہے۔" else "No applications in this category.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredApps) { app ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().testTag("admin_app_card_${app.userId}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = app.fullName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Badge(
                                    containerColor = when (app.status) {
                                        "approved" -> Color(0xFF2E7D32)
                                        "rejected" -> MaterialTheme.colorScheme.error
                                        else -> DastakSecondary
                                    }
                                ) {
                                    Text(
                                        text = app.status.uppercase(),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "CNIC: ${app.cnicNumber}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text(text = "Phone: ${app.phone} | Email: ${app.email}", style = MaterialTheme.typography.bodySmall)

                            if (app.submittedAt != null) {
                                Text(text = "Submitted: ${app.submittedAt.toDate()}", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            }
                            if (app.reviewedAt != null) {
                                Text(text = "Reviewed: ${app.reviewedAt.toDate()}", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            }

                            // CNIC Front & Back Image Display
                            if (app.cnicFrontImageUrl.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = Strings.get("cnic_front_label", currentLanguage), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(4.dp))
                                if (app.cnicFrontImageUrl.startsWith("data:image")) {
                                    val base64 = app.cnicFrontImageUrl.substringAfter("base64,")
                                    val bytes = try { Base64.decode(base64, Base64.DEFAULT) } catch (_: Exception) { null }
                                    val bmp = if (bytes != null) BitmapFactory.decodeByteArray(bytes, 0, bytes.size) else null
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "CNIC Front",
                                            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                } else {
                                    coil.compose.AsyncImage(
                                        model = app.cnicFrontImageUrl,
                                        contentDescription = "CNIC Front",
                                        modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }

                            if (app.cnicBackImageUrl.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = Strings.get("cnic_back_label", currentLanguage), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(4.dp))
                                if (app.cnicBackImageUrl.startsWith("data:image")) {
                                    val base64 = app.cnicBackImageUrl.substringAfter("base64,")
                                    val bytes = try { Base64.decode(base64, Base64.DEFAULT) } catch (_: Exception) { null }
                                    val bmp = if (bytes != null) BitmapFactory.decodeByteArray(bytes, 0, bytes.size) else null
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "CNIC Back",
                                            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                } else {
                                    coil.compose.AsyncImage(
                                        model = app.cnicBackImageUrl,
                                        contentDescription = "CNIC Back",
                                        modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }

                            if (app.status == "rejected" && app.rejectionReason.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "Rejection Reason: ${app.rejectionReason}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }

                            if (app.status == "pending") {
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                riderRepository.approveApplication(app, adminUid)
                                                refresh()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).testTag("approve_rider_btn_${app.userId}")
                                    ) {
                                        Text(Strings.get("approve_btn", currentLanguage))
                                    }

                                    OutlinedButton(
                                        onClick = { selectedAppForReject = app },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).testTag("reject_rider_btn_${app.userId}")
                                    ) {
                                        Text(Strings.get("reject_btn", currentLanguage))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedAppForReject != null) {
        AlertDialog(
            onDismissRequest = { selectedAppForReject = null },
            title = { Text(Strings.get("reject_btn", currentLanguage)) },
            text = {
                Column {
                    Text(text = "Specify reason for rejecting ${selectedAppForReject!!.fullName}:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text(Strings.get("reason_prompt", currentLanguage)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val app = selectedAppForReject!!
                        scope.launch {
                            riderRepository.rejectApplication(app.userId, rejectReason.ifBlank { "CNIC or contact details could not be verified" }, adminUid)
                            selectedAppForReject = null
                            rejectReason = ""
                            refresh()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Strings.get("reject_btn", currentLanguage))
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedAppForReject = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// TAB 3: APPROVED RIDERS & GENUINE RATINGS
// -------------------------------------------------------------
@Composable
private fun AdminApprovedRidersTab(
    riderRepository: RiderRepository,
    orderRepository: OrderRepository,
    currentLanguage: AppLanguage
) {
    var riders by remember { mutableStateOf<List<Rider>>(emptyList()) }
    var allFeedback by remember { mutableStateOf<List<FeedbackItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        riders = riderRepository.getAllApprovedRiders()
        allFeedback = orderRepository.getAllFeedbackForAdmin()
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = DastakPrimary)
        }
    } else if (riders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                text = if (currentLanguage == AppLanguage.URDU) "کوئی منظور شدہ رائیڈر موجود نہیں۔" else "No approved riders currently registered.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(riders) { rider ->
                val riderFeedbacks = allFeedback.filter { it.riderId == rider.userId }
                val avgRating = if (riderFeedbacks.isNotEmpty()) {
                    riderFeedbacks.map { it.rating }.average()
                } else null

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("admin_rider_card_${rider.userId}")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = rider.fullName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Badge(
                                containerColor = if (rider.isOnline) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                            ) {
                                Text(
                                    text = if (rider.isOnline) "ONLINE" else "OFFLINE",
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(text = "Phone: ${rider.phone} | Email: ${rider.email}", style = MaterialTheme.typography.bodySmall)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            if (avgRating != null) {
                                Text(
                                    text = String.format(Locale.US, "%.1f / 5.0 (%d reviews)", avgRating, riderFeedbacks.size),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                            } else {
                                Text(
                                    text = if (currentLanguage == AppLanguage.URDU) "کوئی ریٹنگ ریکارڈ نہیں" else "No ratings yet",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 4: COMPLAINTS MANAGEMENT
// -------------------------------------------------------------
@Composable
private fun AdminComplaintsTab(
    orderRepository: OrderRepository,
    currentLanguage: AppLanguage
) {
    var complaints by remember { mutableStateOf<List<Complaint>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilterStatus by remember { mutableStateOf("All") }
    var respondingComplaint by remember { mutableStateOf<Complaint?>(null) }
    var adminReplyText by remember { mutableStateOf("") }
    var newStatusSelection by remember { mutableStateOf("Resolved") }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            isLoading = true
            complaints = orderRepository.getAllComplaintsForAdmin()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    val filteredComplaints = remember(complaints, selectedFilterStatus) {
        when (selectedFilterStatus) {
            "All" -> complaints
            "Open" -> complaints.filter { it.status.lowercase() == "open" }
            "In Progress" -> complaints.filter { it.status.lowercase() in listOf("in progress", "in_progress", "in_review") }
            "Resolved" -> complaints.filter { it.status.lowercase() == "resolved" }
            else -> complaints
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val statusList = listOf("All", "Open", "In Progress", "Resolved")
            items(statusList) { st ->
                FilterChip(
                    selected = selectedFilterStatus == st,
                    onClick = { selectedFilterStatus = st },
                    label = { Text(st, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DastakPrimary)
            }
        } else if (filteredComplaints.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = if (currentLanguage == AppLanguage.URDU) "کوئی شکایت نہیں ہے۔" else "No complaints in this category.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredComplaints) { comp ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().testTag("admin_complaint_card_${comp.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = comp.subject, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Badge(
                                    containerColor = when (comp.status.lowercase()) {
                                        "resolved" -> Color(0xFF2E7D32)
                                        "in progress", "in_progress", "in_review" -> DastakSecondary
                                        else -> Color(0xFF0288D1)
                                    }
                                ) {
                                    Text(text = comp.status.uppercase(), color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp)
                                }
                            }

                            if (comp.category.isNotBlank()) {
                                Text(text = "Category: ${comp.category}", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary))
                            }
                            if (comp.orderId.isNotBlank()) {
                                Text(text = "Order: #${comp.orderId.take(6).uppercase()}", style = MaterialTheme.typography.labelSmall)
                            }
                            Text(text = comp.description, style = MaterialTheme.typography.bodySmall)

                            if (comp.adminResponse.isNotBlank()) {
                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE8F5E9), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(text = "Admin Reply: ${comp.adminResponse}", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF2E7D32)))
                                        if (comp.respondedAt != null) {
                                            Text(text = "Responded on: ${comp.respondedAt.toDate()}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    respondingComplaint = comp
                                    adminReplyText = comp.adminResponse
                                    newStatusSelection = if (comp.status.lowercase() == "open") "In Progress" else comp.status
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary),
                                modifier = Modifier.fillMaxWidth().testTag("respond_complaint_btn_${comp.id}")
                            ) {
                                Text(if (currentLanguage == AppLanguage.URDU) "جواب درج کریں / کیفیت تبدیل کریں" else "Add Response & Update Status")
                            }
                        }
                    }
                }
            }
        }
    }

    if (respondingComplaint != null) {
        val comp = respondingComplaint!!
        AlertDialog(
            onDismissRequest = { respondingComplaint = null },
            title = { Text("Manage Complaint #${comp.id.take(6)}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Subject: ${comp.subject}", fontWeight = FontWeight.Bold)
                    Text(text = comp.description, style = MaterialTheme.typography.bodySmall)

                    Text(text = "Change Status:", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Open", "In Progress", "Resolved").forEach { st ->
                            FilterChip(
                                selected = newStatusSelection == st,
                                onClick = { newStatusSelection = st },
                                label = { Text(st, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = adminReplyText,
                        onValueChange = { adminReplyText = it },
                        label = { Text("Administrator Response to Customer") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("admin_complaint_reply_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            orderRepository.updateComplaintStatus(comp.id, newStatusSelection, adminReplyText.trim())
                            respondingComplaint = null
                            refresh()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { respondingComplaint = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// TAB 5: CUSTOMER FEEDBACK & RATINGS REPORT
// -------------------------------------------------------------
@Composable
private fun AdminFeedbackTab(
    orderRepository: OrderRepository,
    riderRepository: RiderRepository,
    currentLanguage: AppLanguage
) {
    var allFeedback by remember { mutableStateOf<List<FeedbackItem>>(emptyList()) }
    var approvedRiders by remember { mutableStateOf<List<Rider>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedRatingFilter by remember { mutableIntStateOf(0) } // 0: All, 1..5

    LaunchedEffect(Unit) {
        allFeedback = orderRepository.getAllFeedbackForAdmin()
        approvedRiders = riderRepository.getAllApprovedRiders()
        isLoading = false
    }

    val filteredFeedback = remember(allFeedback, selectedRatingFilter) {
        if (selectedRatingFilter == 0) allFeedback
        else allFeedback.filter { it.rating == selectedRatingFilter }
    }

    val averageRating = remember(allFeedback) {
        if (allFeedback.isNotEmpty()) allFeedback.map { it.rating }.average() else 0.0
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Summary Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (currentLanguage == AppLanguage.URDU) "اوسط صارفین ریٹنگ" else "Average Overall Rating",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onPrimaryContainer)
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f / 5.0", averageRating),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    )
                }
                Badge(containerColor = DastakPrimary) {
                    Text("${allFeedback.size} Total Reviews", color = Color.White, modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Star Filters
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedRatingFilter == 0,
                    onClick = { selectedRatingFilter = 0 },
                    label = { Text("All Stars", fontSize = 12.sp) }
                )
            }
            (5 downTo 1).forEach { star ->
                item {
                    FilterChip(
                        selected = selectedRatingFilter == star,
                        onClick = { selectedRatingFilter = star },
                        label = { Text("$star ★", fontSize = 12.sp) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DastakPrimary)
            }
        } else if (filteredFeedback.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = if (currentLanguage == AppLanguage.URDU) "کوئی فیڈ بیک دستیاب نہیں ہے۔" else "No feedback entries found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredFeedback) { fb ->
                    val riderName = approvedRiders.find { it.userId == fb.riderId }?.fullName ?: fb.riderId.take(8)

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().testTag("admin_feedback_card_${fb.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row {
                                    (1..fb.rating).forEach {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                                    }
                                }
                                if (fb.orderId.isNotBlank()) {
                                    Badge(containerColor = DastakSecondary, contentColor = Color.White) {
                                        Text("Order #${fb.orderId.take(6).uppercase()}", fontSize = 10.sp)
                                    }
                                }
                            }

                            if (fb.riderId.isNotBlank()) {
                                Text(text = "Rider: $riderName", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = DastakPrimary))
                            }

                            if (fb.comments.isNotBlank()) {
                                Text(text = fb.comments, style = MaterialTheme.typography.bodyMedium)
                            }

                            if (fb.createdAt != null) {
                                Text(text = "Submitted: ${fb.createdAt.toDate()}", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 6: CUSTOMERS MANAGEMENT
// -------------------------------------------------------------
@Composable
private fun AdminCustomersTab(
    userRepository: UserRepository,
    currentLanguage: AppLanguage
) {
    var customers by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        customers = userRepository.getAllCustomers()
        isLoading = false
    }

    val filteredCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else customers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.phone.contains(searchQuery, ignoreCase = true) ||
            it.email.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text(if (currentLanguage == AppLanguage.URDU) "صارف تلاش کریں (نام، فون، ای میل)" else "Search Residents (Name, Phone, Email)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().testTag("admin_customer_search_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DastakPrimary)
            }
        } else if (filteredCustomers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = if (currentLanguage == AppLanguage.URDU) "کوئی صارف نہیں ملا۔" else "No residents matching criteria.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredCustomers) { cust ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().testTag("admin_customer_card_${cust.userId}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = cust.name.ifBlank { "Resident" }, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Badge(containerColor = Color(0xFF2E7D32)) {
                                    Text(cust.accountStatus.uppercase(), color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp)
                                }
                            }
                            Text(text = "Phone: ${cust.phone.ifBlank { "Not provided" }} | Email: ${cust.email}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Preferred Language: ${if (cust.preferredLanguage == "ur") "Urdu (اردو)" else "English"}", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            if (cust.createdAt != null) {
                                Text(text = "Joined: ${cust.createdAt.toDate()}", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 7: MANDATORY TERMS & CONDITIONS MANAGEMENT
// -------------------------------------------------------------
@Composable
private fun AdminTermsManagementTab(
    termsRepository: TermsRepository,
    currentLanguage: AppLanguage
) {
    var terms by remember { mutableStateOf<TermsVersion?>(null) }
    var version by remember { mutableStateOf("1.0") }
    var contentEn by remember { mutableStateOf("") }
    var contentUr by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        val t = termsRepository.getActiveTerms()
        terms = t
        version = t.version
        contentEn = t.contentEn
        contentUr = t.contentUr
    }

    if (terms == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = DastakPrimary)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = Strings.get("terms_edit_title", currentLanguage),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            OutlinedTextField(
                value = version,
                onValueChange = { version = it },
                label = { Text(Strings.get("terms_version_label", currentLanguage)) },
                modifier = Modifier.fillMaxWidth().testTag("terms_version_input"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = contentEn,
                onValueChange = { contentEn = it },
                label = { Text(Strings.get("terms_en_content", currentLanguage)) },
                modifier = Modifier.fillMaxWidth().testTag("terms_en_input"),
                minLines = 8,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = contentUr,
                onValueChange = { contentUr = it },
                label = { Text(Strings.get("terms_ur_content", currentLanguage)) },
                modifier = Modifier.fillMaxWidth().testTag("terms_ur_input"),
                minLines = 8,
                shape = RoundedCornerShape(12.dp)
            )

            if (message != null) {
                Text(text = message!!, color = DastakPrimary, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    isSaving = true
                    scope.launch {
                        val updated = TermsVersion(
                            id = "v1",
                            version = version,
                            titleEn = "Terms & Conditions - Dastak Delivery",
                            titleUr = "دستک ڈیلیوری سروس - شرائط و ضوابط",
                            contentEn = contentEn,
                            contentUr = contentUr,
                            isActive = true
                        )
                        val res = termsRepository.saveTermsAsAdmin(updated)
                        isSaving = false
                        if (res.isSuccess) {
                            message = if (currentLanguage == AppLanguage.URDU)
                                "نئی شرائط شائع ہو گئیں۔ تمام صارفین سے دوبارہ قبولیت طلب کی جائے گی۔"
                            else
                                "New terms published successfully. Users will be required to re-accept."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("publish_terms_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary),
                enabled = !isSaving
            ) {
                Text(Strings.get("btn_publish_terms", currentLanguage))
            }
        }
    }
}
