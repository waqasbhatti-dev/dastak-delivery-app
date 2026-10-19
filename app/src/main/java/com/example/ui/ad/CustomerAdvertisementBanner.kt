package com.example.ui.ad

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.data.PricingCalculator
import com.example.data.model.Advertisement
import com.example.data.repository.AdvertisementRepository
import com.example.localization.AppLanguage
import com.example.localization.Strings
import com.example.ui.theme.DastakGreen
import com.example.ui.theme.DastakPrimary
import com.example.ui.theme.UrduFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CustomerAdvertisementBanner(
    advertisementRepository: AdvertisementRepository,
    currentLanguage: AppLanguage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val adsList by advertisementRepository.observeActiveAdvertisements().collectAsState(initial = emptyList())

    // If there are no ads, hide gracefully
    if (adsList.isEmpty()) {
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isUserInteracting by remember { mutableStateOf(false) }

    // Auto-advance every 7 seconds when there are multiple ads and user is not touching it
    LaunchedEffect(adsList.size, isUserInteracting) {
        if (adsList.size > 1 && !isUserInteracting) {
            while (true) {
                delay(7000L)
                currentIndex = (currentIndex + 1) % adsList.size
            }
        }
    }

    val currentAd = adsList.getOrNull(currentIndex % adsList.size) ?: return

    // Record view in background
    LaunchedEffect(currentAd.id) {
        advertisementRepository.recordView(currentAd.id)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("customer_ad_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Category, Sponsor Badge, and Navigation arrows if multiple ads
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = Strings.get("ad_badge_featured", currentLanguage),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                            )
                        },
                        icon = {
                            Icon(
                                Icons.Default.Campaign,
                                contentDescription = null,
                                tint = DastakPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = DastakPrimary.copy(alpha = 0.12f),
                            labelColor = DastakPrimary
                        ),
                        border = null,
                        modifier = Modifier.height(26.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = currentAd.businessName.ifBlank { "Dastak" },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (adsList.size > 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                isUserInteracting = true
                                currentIndex = if (currentIndex > 0) currentIndex - 1 else adsList.size - 1
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Ad",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = "${(currentIndex % adsList.size) + 1}/${adsList.size}",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        IconButton(
                            onClick = {
                                isUserInteracting = true
                                currentIndex = (currentIndex + 1) % adsList.size
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Ad",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Animated Media & Details Section
            AnimatedContent(
                targetState = currentAd,
                transitionSpec = {
                    slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
                },
                label = "AdCarouselContent"
            ) { targetAd ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Media Viewer (Image or Short Video with Muted Autoplay & Play/Pause controls)
                    if (targetAd.mediaUrl.isNotBlank()) {
                        AdvertisementMediaViewer(
                            ad = targetAd,
                            height = 170,
                            allowVideoPlayback = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Ad Title & Offer Banner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = targetAd.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (targetAd.offer.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DastakGreen.copy(alpha = 0.15f),
                                contentColor = DastakGreen
                            ) {
                                Text(
                                    text = targetAd.offer,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                                )
                            }
                        }
                    }

                    // Price Tag (if provided)
                    if (targetAd.price != null && targetAd.price > 0.0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Sell,
                                contentDescription = null,
                                tint = DastakPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (currentLanguage == AppLanguage.URDU)
                                    PricingCalculator.formatPkrUrdu(targetAd.price)
                                else
                                    PricingCalculator.formatPkr(targetAd.price),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DastakPrimary
                                )
                            )
                        }
                    }

                    // Description text
                    if (targetAd.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = targetAd.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                            ),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Contact & Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (targetAd.phone.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = targetAd.phone,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            FilledTonalButton(
                                onClick = {
                                    scope.launch {
                                        advertisementRepository.recordClick(targetAd.id)
                                    }
                                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${targetAd.phone}")
                                    }
                                    context.startActivity(dialIntent)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = DastakPrimary,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("ad_call_button")
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = Strings.get("ad_call_now", currentLanguage),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = Strings.get("locality_name", currentLanguage),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            // Dots indicator for carousel
            if (adsList.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    adsList.forEachIndexed { idx, _ ->
                        val isSelected = (idx == currentIndex % adsList.size)
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (isSelected) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) DastakPrimary else MaterialTheme.colorScheme.outlineVariant
                                )
                                .clickable {
                                    isUserInteracting = true
                                    currentIndex = idx
                                }
                        )
                    }
                }
            }
        }
    }
}
