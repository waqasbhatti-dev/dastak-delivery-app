package com.example.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.localization.AppLanguage
import com.example.localization.Strings
import com.example.ui.components.LanguageSelector
import com.example.ui.theme.DastakPrimary
import com.example.ui.theme.DastakSecondary
import com.example.ui.theme.UrduFontFamily

@Composable
fun WelcomeScreen(
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onPortalSelected: (portalRole: String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.hero_delivery),
                    contentDescription = "Dastak Delivery Hero",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                            )
                        )
                )

                // Language toggle in hero overlay
                LanguageSelector(
                    currentLanguage = currentLanguage,
                    onLanguageChange = onLanguageChange,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Branding Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Strings.get("app_name", currentLanguage),
                    style = MaterialTheme.typography.displayLarge.copy(
                        color = DastakPrimary,
                        fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                    ),
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = Strings.get("locality_name", currentLanguage),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = Strings.get("tagline", currentLanguage),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = Strings.get("welcome_desc", currentLanguage),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Portals Title
            Text(
                text = if (currentLanguage == AppLanguage.URDU) "لاگ ان کے لیے پورٹل منتخب کریں" else "Select Login Portal",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    color = DastakPrimary,
                    modifier = Modifier
                        .padding(24.dp)
                        .size(48.dp)
                )
                Text(
                    text = Strings.get("signing_in", currentLanguage),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                )
            } else {
                // Portal 1: Customer
                PortalCard(
                    title = Strings.get("portal_customer", currentLanguage),
                    description = Strings.get("portal_customer_desc", currentLanguage),
                    icon = Icons.Default.ShoppingBag,
                    accentColor = DastakPrimary,
                    buttonLabel = Strings.get("sign_in_google", currentLanguage),
                    onClick = { onPortalSelected("customer") },
                    currentLanguage = currentLanguage,
                    testTag = "portal_customer_card"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Portal 2: Rider
                PortalCard(
                    title = Strings.get("portal_rider", currentLanguage),
                    description = Strings.get("portal_rider_desc", currentLanguage),
                    icon = Icons.Default.DirectionsBike,
                    accentColor = DastakSecondary,
                    buttonLabel = if (currentLanguage == AppLanguage.URDU) "رائیڈر سائن ان / درخواست دیں" else "Rider Sign In / Apply",
                    onClick = { onPortalSelected("rider") },
                    currentLanguage = currentLanguage,
                    testTag = "portal_rider_card"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Portal 3: Admin
                PortalCard(
                    title = Strings.get("portal_admin", currentLanguage),
                    description = Strings.get("portal_admin_desc", currentLanguage),
                    icon = Icons.Default.AdminPanelSettings,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    buttonLabel = if (currentLanguage == AppLanguage.URDU) "ایڈمن لاگ ان" else "Admin Login",
                    onClick = { onPortalSelected("admin") },
                    currentLanguage = currentLanguage,
                    testTag = "portal_admin_card"
                )
            }
        }
    }
}

@Composable
private fun PortalCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    buttonLabel: String,
    onClick: () -> Unit,
    currentLanguage: AppLanguage,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                        )
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text(
                    text = buttonLabel,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                )
            }
        }
    }
}
