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

package org.fptn.vpn.auth

import com.elvishew.xlog.XLog
import com.google.firebase.auth.FirebaseAuth

/** `gms` flavor: Firebase-backed accounts, unless Firebase isn't initialized for this build. */
object AccountAuthProvider {
    private val instance: AccountAuth by lazy {
        try {
            FirebaseAccountAuth(FirebaseAuth.getInstance())
        } catch (e: IllegalStateException) {
            // No default FirebaseApp: the google-services plugin wasn't applied to this build.
            XLog.tag("AccountAuthProvider").w("Firebase is not initialized, accounts disabled: %s", e.message)
            UnavailableAccountAuth
        }
    }

    fun get(): AccountAuth = instance
}
