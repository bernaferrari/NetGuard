package eu.faircode.netguard.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

private val BaseTypography = Typography()

val Typography =
    Typography(
        headlineLarge = BaseTypography.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = BaseTypography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        headlineSmall = BaseTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.Medium),
        titleSmall = BaseTypography.titleSmall.copy(fontWeight = FontWeight.Medium),
        bodyMedium = BaseTypography.bodyMedium.copy(fontWeight = FontWeight.Normal),
        labelLarge = BaseTypography.labelLarge.copy(fontWeight = FontWeight.Medium),
        labelMedium = BaseTypography.labelMedium.copy(fontWeight = FontWeight.Medium),
        labelSmall = BaseTypography.labelSmall.copy(fontWeight = FontWeight.Medium),
    )
