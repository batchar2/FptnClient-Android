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

/**
 * Email/password account whose server list is delivered as an FPTN token string.
 *
 * The real implementation (Firebase Auth + Firestore) exists only in the `gms` flavor; the
 * `foss` flavor gets [UnavailableAccountAuth] so it stays free of Google code. Pick the
 * instance through `AccountAuthProvider.get()`, which each flavor defines.
 */
interface AccountAuth {
    /** `false` when accounts can't be used at all (foss build, Firebase not configured). */
    val isAvailable: Boolean

    /** Email of the signed-in account, or `null` when nobody is signed in. */
    val currentEmail: String?

    @Throws(AccountAuthException::class)
    suspend fun signIn(email: String, password: String)

    @Throws(AccountAuthException::class)
    suspend fun signUp(email: String, password: String)

    @Throws(AccountAuthException::class)
    suspend fun sendPasswordReset(email: String)

    /** Fetches the account's `fptn:`/`fptnb:` token from the server (never from a local cache). */
    @Throws(AccountAuthException::class)
    suspend fun fetchServerToken(): String

    fun signOut()
}

class AccountAuthException(
    val reason: Reason,
    cause: Throwable? = null,
) : Exception(reason.name, cause) {
    enum class Reason {
        NOT_AVAILABLE,
        NOT_SIGNED_IN,
        INVALID_EMAIL,
        INVALID_CREDENTIALS,
        EMAIL_ALREADY_IN_USE,
        WEAK_PASSWORD,
        TOO_MANY_REQUESTS,
        NETWORK,

        /** Signed in, but `users/{uid}.token` is missing or empty (e.g. right after sign-up). */
        NO_TOKEN,
        UNKNOWN,
    }
}

object UnavailableAccountAuth : AccountAuth {
    override val isAvailable = false
    override val currentEmail: String? = null

    override suspend fun signIn(email: String, password: String) = unavailable()

    override suspend fun signUp(email: String, password: String) = unavailable()

    override suspend fun sendPasswordReset(email: String) = unavailable()

    override suspend fun fetchServerToken(): String = unavailable()

    override fun signOut() = Unit

    private fun unavailable(): Nothing = throw AccountAuthException(AccountAuthException.Reason.NOT_AVAILABLE)
}
