/*
 * Copyright (c) 2026 Auxio Project
 * ProcessLocale.kt is part of Auxio.
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
 
package org.oxycblt.auxio.linux.playback

import com.sun.jna.Library
import com.sun.jna.Native

/**
 * The process-wide C locale, as far as libmpv is concerned.
 *
 * libmpv parses numbers in the C locale and returns NULL from `mpv_create()` when `LC_NUMERIC` is
 * anything else. GTK and the jpackage launcher both call `setlocale(LC_ALL, "")` at startup, so a
 * desktop session with `LANG=en_US.UTF-8` breaks playback even though `java.util.Locale` is
 * irrelevant here. Kotlin has no way to call `setlocale` itself, hence JNA.
 */
internal object ProcessLocale {
    /** glibc and musl both number their categories with LC_NUMERIC first after LC_CTYPE. */
    private const val LC_NUMERIC = 1

    private interface LibC : Library {
        fun setlocale(category: Int, locale: String): String?
    }

    private val libc: LibC? by lazy {
        runCatching { Native.load("c", LibC::class.java) }.getOrNull()
    }

    /** Resets LC_NUMERIC to "C", returning whether the C library accepted it. */
    fun resetNumericToC(): Boolean = setNumeric("C")

    /** Sets LC_NUMERIC, returning whether the locale is available on this machine. */
    fun setNumeric(locale: String): Boolean =
        runCatching { libc?.setlocale(LC_NUMERIC, locale) }.getOrNull() != null
}
