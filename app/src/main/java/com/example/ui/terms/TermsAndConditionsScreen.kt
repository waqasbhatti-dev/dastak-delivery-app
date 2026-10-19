package com.example.ui.terms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TermsVersion
import com.example.data.tts.TtsManager
import com.example.localization.AppLanguage
import com.example.localization.Strings
import com.example.ui.components.LanguageSelector
import com.example.ui.theme.DastakPrimary
import com.example.ui.theme.DastakSecondary
import com.example.ui.theme.UrduFontFamily

@Composable
fun TermsAndConditionsScreen(
    terms: TermsVersion,
    currentLanguage: AppLanguage,
    ttsManager: TtsManager,
    onLanguageChange: (AppLanguage) -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    isSubmitting: Boolean,
    modifier: Modifier = Modifier
) {
    var hasAgreed by remember { mutableStateOf(false) }
    val isSpeaking by ttsManager.isSpeaking.collectAsState()
    val isUrduSupported by ttsManager.isUrduSupported.collectAsState()
    val ttsStatusMessage by ttsManager.statusMessage.collectAsState()
    val scrollState = rememberScrollState()

    val currentContent = if (currentLanguage == AppLanguage.URDU) {
        terms.contentUr.ifBlank { TermsVersion().contentUr }
    } else {
        terms.contentEn.ifBlank { TermsVersion().contentEn }
    }

    val currentTitle = if (currentLanguage == AppLanguage.URDU) {
        terms.titleUr
    } else {
        terms.titleEn
    }

    DisposableEffect(Unit) {
        onDispose {
            ttsManager.stop()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Checkbox(
                            checked = hasAgreed,
                            onCheckedChange = { hasAgreed = it },
                            modifier = Modifier.testTag("terms_agree_checkbox")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Strings.get("terms_checkbox", currentLanguage),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                ttsManager.stop()
                                onDecline()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("terms_decline_button"),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isSubmitting
                        ) {
                            Text(
                                text = Strings.get("btn_decline", currentLanguage),
                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                            )
                        }

                        Button(
                            onClick = {
                                ttsManager.stop()
                                onAccept()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("terms_accept_button"),
                            shape = RoundedCornerShape(12.dp),
                            enabled = hasAgreed && !isSubmitting,
                            colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = Strings.get("btn_accept_continue", currentLanguage),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Header with Language selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Badge(
                    containerColor = DastakPrimary.copy(alpha = 0.15f),
                    contentColor = DastakPrimary
                ) {
                    Text(
                        text = "Version ${terms.version}",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                LanguageSelector(
                    currentLanguage = currentLanguage,
                    onLanguageChange = { newLang ->
                        ttsManager.stop()
                        onLanguageChange(newLang)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = currentTitle,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = DastakPrimary,
                    fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                )
            )

            Text(
                text = Strings.get("terms_subtitle", currentLanguage),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                ),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Audio Playback Control Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Audio Playback",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Strings.get("terms_listen_label", currentLanguage),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                                )
                            )
                        }

                        if (isSpeaking) {
                            Text(
                                text = Strings.get("tts_speaking", currentLanguage),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = DastakPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Play Button
                        Button(
                            onClick = {
                                ttsManager.speak(currentContent, currentLanguage)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DastakPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tts_play_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = Strings.get("btn_play", currentLanguage),
                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                            )
                        }

                        // Pause Button
                        OutlinedButton(
                            onClick = { ttsManager.pause() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tts_pause_button"),
                            enabled = isSpeaking
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = Strings.get("btn_pause", currentLanguage),
                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                            )
                        }

                        // Stop Button
                        FilledTonalButton(
                            onClick = { ttsManager.stop() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tts_stop_button"),
                            enabled = isSpeaking
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = Strings.get("btn_stop", currentLanguage),
                                fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                            )
                        }
                    }

                    // Notice if Urdu voice is unsupported on device
                    AnimatedVisibility(visible = currentLanguage == AppLanguage.URDU && !isUrduSupported) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = Strings.get("tts_not_supported", currentLanguage),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontFamily = UrduFontFamily
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Written Terms Content
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = currentContent,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 26.sp,
                            fontFamily = if (currentLanguage == AppLanguage.URDU) UrduFontFamily else null
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
