package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.AppLanguage
import com.example.ui.theme.DastakPrimary
import com.example.ui.theme.UrduFontFamily

@Composable
fun LanguageSelector(
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag("language_selector"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = "Language",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(20.dp)
            )

            LanguageOptionButton(
                title = "English",
                isSelected = currentLanguage == AppLanguage.ENGLISH,
                onClick = { onLanguageChange(AppLanguage.ENGLISH) },
                testTag = "lang_en_button"
            )

            Spacer(modifier = Modifier.width(4.dp))

            LanguageOptionButton(
                title = "اردو",
                isSelected = currentLanguage == AppLanguage.URDU,
                isUrdu = true,
                onClick = { onLanguageChange(AppLanguage.URDU) },
                testTag = "lang_ur_button"
            )
        }
    }
}

@Composable
private fun LanguageOptionButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isUrdu: Boolean = false,
    testTag: String = ""
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) DastakPrimary else Color.Transparent,
        label = "btn_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "btn_text"
    )

    Box(
        modifier = modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = if (isUrdu) 16.sp else 14.sp,
            fontFamily = if (isUrdu) UrduFontFamily else null
        )
    }
}
