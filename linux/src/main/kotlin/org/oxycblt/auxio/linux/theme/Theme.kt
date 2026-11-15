/*
 * Copyright (c) 2026 Auxio Project
 * Theme.kt is part of Auxio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package org.oxycblt.auxio.linux.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import java.nio.file.Path
import kotlin.math.roundToInt

/** Where the colour scheme comes from. */
enum class ThemeMode(val label: String, val description: String) {
    DESKTOP("Desktop colours", "Use the accent colour and appearance your desktop reports"),
    MATUGEN("Matugen", "Generate the scheme from your wallpaper with matugen"),
    CUSTOM("Deep customization", "Choose the scheme's hue and chroma yourself"),
}

/** Everything the theme needs from the user, stored in the settings file. */
data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.DESKTOP,
    val hue: Float = 258f,
    val chroma: Float = 48f,
    val wallpaper: String = "",
    val type: SchemeType = SchemeType.TONAL_SPOT,
) {
    /** The colour the custom scheme is built around. */
    val seed: Color
        get() =
            Color.hsl(
                hue = hue.mod(360f),
                saturation = chroma.coerceIn(0f, 100f) / 100f,
                lightness = 0.5f,
            )
}

/**
 * Turns a [ThemeSettings] into a Material 3 scheme.
 *
 * matugen does the colour science when it is installed, for wallpapers, desktop accents and custom
 * seeds alike; the built-in generator takes over when it is not, so the port never falls back to
 * stock purple for want of a binary.
 */
object Themes {
    fun scheme(settings: ThemeSettings): ColorScheme =
        scheme(settings, Desktop.appearance(), Desktop.wallpaper())

    internal fun scheme(
        settings: ThemeSettings,
        appearance: Desktop.Appearance,
        wallpaper: Path?,
    ): ColorScheme {
        val accent = appearance.accent ?: Desktop.DEFAULT_ACCENT

        return when (settings.mode) {
            ThemeMode.DESKTOP -> fromSeed(accent, settings)
            ThemeMode.CUSTOM -> fromSeed(settings.seed, settings)
            ThemeMode.MATUGEN -> {
                val image =
                    settings.wallpaper.takeIf { it.isNotBlank() }?.let(Path::of) ?: wallpaper
                val roles = image?.let { Matugen.fromImage(it, settings.type.flag) }
                roles?.let(::schemeOf) ?: fromSeed(accent, settings)
            }
        }
    }

    /** The scheme matugen builds from [seed], or the built-in approximation when it is absent. */
    private fun fromSeed(seed: Color, settings: ThemeSettings): ColorScheme =
        (if (Matugen.isAvailable()) Matugen.fromColor(seed, settings.type.flag) else null)?.let(
            ::schemeOf
        ) ?: Approximation.scheme(seed)

    /** Reads matugen's snake_case roles into Compose's camelCase scheme. */
    internal fun schemeOf(roles: Map<String, String>): ColorScheme {
        val base = darkColorScheme()

        fun role(name: String, fallback: Color): Color = roles[name]?.let(::colorOf) ?: fallback

        return base.copy(
            primary = role("primary", base.primary),
            onPrimary = role("on_primary", base.onPrimary),
            primaryContainer = role("primary_container", base.primaryContainer),
            onPrimaryContainer = role("on_primary_container", base.onPrimaryContainer),
            inversePrimary = role("inverse_primary", base.inversePrimary),
            secondary = role("secondary", base.secondary),
            onSecondary = role("on_secondary", base.onSecondary),
            secondaryContainer = role("secondary_container", base.secondaryContainer),
            onSecondaryContainer = role("on_secondary_container", base.onSecondaryContainer),
            tertiary = role("tertiary", base.tertiary),
            onTertiary = role("on_tertiary", base.onTertiary),
            tertiaryContainer = role("tertiary_container", base.tertiaryContainer),
            onTertiaryContainer = role("on_tertiary_container", base.onTertiaryContainer),
            error = role("error", base.error),
            onError = role("on_error", base.onError),
            errorContainer = role("error_container", base.errorContainer),
            onErrorContainer = role("on_error_container", base.onErrorContainer),
            background = role("background", base.background),
            onBackground = role("on_background", base.onBackground),
            surface = role("surface", base.surface),
            onSurface = role("on_surface", base.onSurface),
            surfaceVariant = role("surface_variant", base.surfaceVariant),
            onSurfaceVariant = role("on_surface_variant", base.onSurfaceVariant),
            surfaceTint = role("primary", base.surfaceTint),
            inverseSurface = role("inverse_surface", base.inverseSurface),
            inverseOnSurface = role("inverse_on_surface", base.inverseOnSurface),
            outline = role("outline", base.outline),
            outlineVariant = role("outline_variant", base.outlineVariant),
            scrim = role("scrim", base.scrim),
        )
    }

    internal fun colorOf(hex: String): Color? = Desktop.parseHex(hex)

    /**
     * Stands in for matugen when it is not installed: the seed's hue carries the primary and
     * containers, chroma decides how far the greys lean into it, and every on-colour is set by
     * luminance rather than assumed, so a bright seed does not produce unreadable text.
     */
    internal object Approximation {
        private const val SURFACE_LIGHTNESS = 0.07f

        fun scheme(seed: Color): ColorScheme {
            val hsl = seed.toHsl()
            val hue = hsl[0]
            val chroma = hsl[1].coerceIn(0f, 1f)

            fun tone(lightness: Float, saturation: Float): Color =
                Color.hsl(hue, (chroma * saturation).coerceIn(0f, 1f), lightness)

            val surface = tone(SURFACE_LIGHTNESS, 0.08f)
            val surfaceVariant = tone(0.17f, 0.10f)
            val onSurface = tone(0.93f, 0.04f)
            val onSurfaceVariant = tone(0.78f, 0.07f)
            val primary = tone(0.76f, 0.55f)
            val primaryContainer = tone(0.30f, 0.45f)
            val secondary = tone(0.72f, 0.30f)
            val secondaryContainer = tone(0.28f, 0.24f)
            val tertiaryHue = (hue + 60f).mod(360f)
            val tertiary = Color.hsl(tertiaryHue, chroma * 0.45f, 0.74f)
            val tertiaryContainer = Color.hsl(tertiaryHue, chroma * 0.35f, 0.29f)

            return darkColorScheme(
                primary = primary,
                onPrimary = contrasting(primary, dark = 0.12f, light = 0.95f),
                primaryContainer = primaryContainer,
                onPrimaryContainer = contrasting(primaryContainer, dark = 0.92f, light = 0.10f),
                inversePrimary = tone(0.42f, 0.50f),
                secondary = secondary,
                onSecondary = contrasting(secondary, dark = 0.12f, light = 0.95f),
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = contrasting(secondaryContainer, dark = 0.92f, light = 0.10f),
                tertiary = tertiary,
                onTertiary = contrasting(tertiary, dark = 0.12f, light = 0.95f),
                tertiaryContainer = tertiaryContainer,
                onTertiaryContainer = contrasting(tertiaryContainer, dark = 0.92f, light = 0.10f),
                error = Color.hsl(6f, 0.72f, 0.74f),
                onError = Color.hsl(6f, 0.85f, 0.13f),
                errorContainer = Color.hsl(6f, 0.50f, 0.30f),
                onErrorContainer = Color.hsl(6f, 0.75f, 0.90f),
                background = surface,
                onBackground = onSurface,
                surface = surface,
                onSurface = onSurface,
                surfaceVariant = surfaceVariant,
                onSurfaceVariant = onSurfaceVariant,
                inverseSurface = onSurface,
                inverseOnSurface = surface,
                outline = tone(0.55f, 0.08f),
                outlineVariant = tone(0.30f, 0.10f),
                scrim = Color.Black,
            )
        }

        /** Dark text on a light fill, light text on a dark one, so labels stay readable. */
        private fun contrasting(fill: Color, dark: Float, light: Float): Color =
            Color.hsl(fill.toHsl()[0], 0.25f, if (fill.luminance() > 0.45f) dark else light)
    }
}

/** `#rrggbb`, for naming a detected colour in the settings. */
fun Color.toHexString(): String {
    fun channel(value: Float): Int = (value.coerceIn(0f, 1f) * 255f).roundToInt()
    return "#%02x%02x%02x".format(channel(red), channel(green), channel(blue))
}

/** Hue, saturation and lightness, the three numbers the approximation works in. */
internal fun Color.toHsl(): FloatArray {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val lightness = (max + min) / 2f
    if (max == min) return floatArrayOf(0f, 0f, lightness)

    val delta = max - min
    val saturation = if (lightness > 0.5f) delta / (2f - max - min) else delta / (max + min)
    val hue =
        when (max) {
            red -> 60f * (((green - blue) / delta).mod(6f))
            green -> 60f * ((blue - red) / delta + 2f)
            else -> 60f * ((red - green) / delta + 4f)
        }
    return floatArrayOf(hue.mod(360f), saturation.coerceIn(0f, 1f), lightness)
}
