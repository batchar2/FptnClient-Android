/*
 * FPTN Android Client
 * Copyright (C) 2026  Skokov Stanislav, Enin Sergey
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
 *
 * Website: https://fptn.org
 */

package org.fptn.vpn.ui.perappvpn

import android.graphics.drawable.Drawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AppInfo(val packageName: String) {
    var label: String = ""
    var icon: Drawable? = null

    // Backed by Compose state (not a plain var) so toggling one app re-renders just its
    // own row's Switch, without relying on the list being rebuilt with a new AppInfo identity.
    var isAllowed: Boolean by mutableStateOf(false)
    var isDisallowed: Boolean by mutableStateOf(false)

    var isSystemApp: Boolean = false
    var isForcedExcluded: Boolean = false
}
