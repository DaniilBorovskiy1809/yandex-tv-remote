package dev.daniilborovskiy.yandextvremote.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val YandexRed = Color(0xFFFC3F1D)
val YandexRedContainer = Color(0xFFFFDAD2)
val YandexOnRedContainer = Color(0xFF3A0B00)
val YandexYellow = Color(0xFFFFDB4D)
val YandexYellowContainer = Color(0xFFFFE08A)

val LightColors = lightColorScheme(
    primary = YandexRed,
    onPrimary = Color.White,
    primaryContainer = YandexRedContainer,
    onPrimaryContainer = YandexOnRedContainer,
    secondary = Color(0xFF77574E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD2),
    onSecondaryContainer = Color(0xFF2C1510),
    tertiary = Color(0xFF6C5D00),
    onTertiary = Color.White,
    tertiaryContainer = YandexYellowContainer,
    onTertiaryContainer = Color(0xFF211B00),
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF231917),
    surface = Color(0xFFFFF8F6),
    onSurface = Color(0xFF231917),
    surfaceVariant = Color(0xFFF5DED8),
    onSurfaceVariant = Color(0xFF53433F),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF85736E),
    outlineVariant = Color(0xFFD8C2BC),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF392E2B),
    inverseOnSurface = Color(0xFFFFEDE9),
    inversePrimary = Color(0xFFFFB4A2),
    surfaceTint = YandexRed,
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4A2),
    onPrimary = Color(0xFF690000),
    primaryContainer = Color(0xFF8C1D00),
    onPrimaryContainer = YandexRedContainer,
    secondary = Color(0xFFE7BDB2),
    onSecondary = Color(0xFF442A23),
    secondaryContainer = Color(0xFF5D4038),
    onSecondaryContainer = Color(0xFFFFDAD2),
    tertiary = Color(0xFFDBC66A),
    onTertiary = Color(0xFF393000),
    tertiaryContainer = Color(0xFF524600),
    onTertiaryContainer = Color(0xFFFFE08A),
    background = Color(0xFF1A1110),
    onBackground = Color(0xFFF1DFDA),
    surface = Color(0xFF1A1110),
    onSurface = Color(0xFFF1DFDA),
    surfaceVariant = Color(0xFF53433F),
    onSurfaceVariant = Color(0xFFD8C2BC),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFFA08C87),
    outlineVariant = Color(0xFF53433F),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFF1DFDA),
    inverseOnSurface = Color(0xFF392E2B),
    inversePrimary = YandexRed,
    surfaceTint = Color(0xFFFFB4A2),
)
