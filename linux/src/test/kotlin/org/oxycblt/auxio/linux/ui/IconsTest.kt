/*
 * Copyright (c) 2026 Auxio Project
 * IconsTest.kt is part of Auxio.
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
 
package org.oxycblt.auxio.linux.ui

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The icon path data is written by hand, so a typo would produce an invisible icon rather than a
 * compile error. Parsing each one proves it contributed at least one path.
 */
class IconsTest {
    private val icons =
        listOf(
            AuxioIcons.PlayArrow,
            AuxioIcons.Pause,
            AuxioIcons.SkipNext,
            AuxioIcons.SkipPrevious,
            AuxioIcons.Settings,
            AuxioIcons.ArrowBack,
            AuxioIcons.MusicNote,
            AuxioIcons.Refresh,
        )

    @Test
    fun `every icon parses into at least one path`() {
        for (icon in icons) {
            assertTrue(icon.root.size > 0, "${icon.name} produced no drawing instructions")
        }
    }

    @Test
    fun `every icon uses the 24dp viewport material icons are drawn in`() {
        for (icon in icons) {
            assertTrue(icon.viewportWidth == 24f && icon.viewportHeight == 24f, icon.name)
        }
    }
}
