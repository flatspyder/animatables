package com.singleoriginapps.animatables.catalog

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF465D32), onPrimary = Color.White,
    primaryContainer = Color(0xFFDEEDC4), onPrimaryContainer = Color(0xFF263617),
    secondary = Color(0xFF695078), secondaryContainer = Color(0xFFEEDDF5),
    onSecondaryContainer = Color(0xFF33203E),
    tertiary = Color(0xFF805137), tertiaryContainer = Color(0xFFFFDBC7),
    background = Color(0xFFF8F8F2), onBackground = Color(0xFF20241C),
    surface = Color(0xFFF8F8F2), onSurface = Color(0xFF20241C),
    surfaceContainer = Color(0xFFEEEEE6),
    surfaceContainerLow = Color(0xFFF0F1E9), surfaceContainerLowest = Color.White,
    onSurfaceVariant = Color(0xFF555C4E), outlineVariant = Color(0xFFD9DDCF),
)
private val Dark = darkColorScheme(
    primary = Color(0xFFBCD59C), onPrimary = Color(0xFF253B15),
    primaryContainer = Color(0xFF344C24), onPrimaryContainer = Color(0xFFDEEDC4),
    secondary = Color(0xFFD8BAE5), secondaryContainer = Color(0xFF4D365D),
    onSecondaryContainer = Color(0xFFEEDDF5),
    background = Color(0xFF13160F), surface = Color(0xFF13160F),
    onBackground = Color(0xFFE4E8DA), onSurface = Color(0xFFE4E8DA),
    surfaceContainerLow = Color(0xFF1B2017), surfaceContainer = Color(0xFF23281E),
    surfaceContainerLowest = Color(0xFF0E120B), onSurfaceVariant = Color(0xFFC0C8B5),
    outlineVariant = Color(0xFF414A37),
)

@Composable
internal fun CatalogTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    MaterialExpressiveTheme(colorScheme = if (dark) Dark else Light, content = content)
}
