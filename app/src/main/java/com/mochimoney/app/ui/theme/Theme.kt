package com.mochimoney.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.mochimoney.app.R

private val LightColors = lightColorScheme(
    primary = MochiMint,
    onPrimary = MochiSurface,
    primaryContainer = ColorSchemeTokens.MintContainer,
    onPrimaryContainer = MochiInk,
    secondary = MochiRose,
    onSecondary = MochiSurface,
    secondaryContainer = ColorSchemeTokens.RoseContainer,
    onSecondaryContainer = MochiInk,
    tertiary = MochiSky,
    onTertiary = MochiSurface,
    tertiaryContainer = ColorSchemeTokens.SkyContainer,
    onTertiaryContainer = MochiInk,
    background = MochiCream,
    onBackground = MochiInk,
    surface = MochiSurface,
    onSurface = MochiInk,
    surfaceVariant = ColorSchemeTokens.SurfaceVariant,
    onSurfaceVariant = ColorSchemeTokens.OnSurfaceVariant,
    outline = MochiOutline,
    outlineVariant = ColorSchemeTokens.OutlineVariant,
    error = ColorSchemeTokens.Error,
)

private val DarkColors = darkColorScheme(
    primary = ColorSchemeTokens.MintDark,
    onPrimary = ColorSchemeTokens.DarkInk,
    primaryContainer = ColorSchemeTokens.MintDarkContainer,
    onPrimaryContainer = ColorSchemeTokens.LightInk,
    secondary = ColorSchemeTokens.RoseDark,
    onSecondary = ColorSchemeTokens.DarkInk,
    secondaryContainer = ColorSchemeTokens.RoseDarkContainer,
    onSecondaryContainer = ColorSchemeTokens.LightInk,
    tertiary = ColorSchemeTokens.SkyDark,
    onTertiary = ColorSchemeTokens.DarkInk,
    tertiaryContainer = ColorSchemeTokens.SkyDarkContainer,
    onTertiaryContainer = ColorSchemeTokens.LightInk,
    background = ColorSchemeTokens.DarkBackground,
    onBackground = ColorSchemeTokens.LightInk,
    surface = ColorSchemeTokens.DarkSurface,
    onSurface = ColorSchemeTokens.LightInk,
    surfaceVariant = ColorSchemeTokens.DarkSurfaceVariant,
    onSurfaceVariant = ColorSchemeTokens.DarkMuted,
    outline = ColorSchemeTokens.DarkOutline,
    outlineVariant = ColorSchemeTokens.DarkOutlineVariant,
    error = ColorSchemeTokens.ErrorDark,
)

private val Fredoka = FontFamily(
    Font(R.font.fredoka, FontWeight.Light),
    Font(R.font.fredoka, FontWeight.Normal),
    Font(R.font.fredoka, FontWeight.Medium),
    Font(R.font.fredoka, FontWeight.SemiBold),
    Font(R.font.fredoka, FontWeight.Bold),
)

private val KawaiiDisplay = Fredoka
private val KawaiiText = Fredoka

val MochiTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = KawaiiDisplay,
        fontWeight = FontWeight.Black,
        fontSize = 38.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.5.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = KawaiiDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.4.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = KawaiiDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.3.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = KawaiiText,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.2.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = KawaiiText,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = KawaiiText,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = KawaiiText,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.4.sp,
    ),
)

private val MochiShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun MochiMoneyTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MochiTypography,
        shapes = MochiShapes,
        content = content,
    )
}

private object ColorSchemeTokens {
    val MintContainer = androidx.compose.ui.graphics.Color(0xFFDFF8E8)
    val RoseContainer = androidx.compose.ui.graphics.Color(0xFFFFDFEA)
    val SkyContainer = androidx.compose.ui.graphics.Color(0xFFDDECFF)
    val SurfaceVariant = androidx.compose.ui.graphics.Color(0xFFFFF0DE)
    val OnSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF645C52)
    val OutlineVariant = androidx.compose.ui.graphics.Color(0xFFF2E6D8)
    val Error = androidx.compose.ui.graphics.Color(0xFFBA1A1A)

    val DarkInk = androidx.compose.ui.graphics.Color(0xFF14201B)
    val LightInk = androidx.compose.ui.graphics.Color(0xFFFFF6ED)
    val DarkBackground = androidx.compose.ui.graphics.Color(0xFF211B24)
    val DarkSurface = androidx.compose.ui.graphics.Color(0xFF2B2430)
    val DarkSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF413747)
    val DarkMuted = androidx.compose.ui.graphics.Color(0xFFE3D3C8)
    val DarkOutline = androidx.compose.ui.graphics.Color(0xFF796B78)
    val DarkOutlineVariant = androidx.compose.ui.graphics.Color(0xFF4F4352)
    val MintDark = androidx.compose.ui.graphics.Color(0xFF86E7BF)
    val MintDarkContainer = androidx.compose.ui.graphics.Color(0xFF1E5B49)
    val RoseDark = androidx.compose.ui.graphics.Color(0xFFFFB4C4)
    val RoseDarkContainer = androidx.compose.ui.graphics.Color(0xFF82354A)
    val SkyDark = androidx.compose.ui.graphics.Color(0xFFB6D0FF)
    val SkyDarkContainer = androidx.compose.ui.graphics.Color(0xFF28518C)
    val ErrorDark = androidx.compose.ui.graphics.Color(0xFFFFB4AB)
}
