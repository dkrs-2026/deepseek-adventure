package com.uusse.deepseekadventure.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/** 字号与间距遵循 Material 3，正文略放大便于长文阅读 */
val AppTypography = Typography(
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    titleMedium = TextStyle(fontSize = 16.sp),
    titleLarge = TextStyle(fontSize = 20.sp),
)
