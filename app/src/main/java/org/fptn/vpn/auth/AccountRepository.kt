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

import android.content.Context
import com.elvishew.xlog.XLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.fptn.vpn.database.AppDatabase
import org.fptn.vpn.services.tile.FptnTileService
import org.fptn.vpn.utils.SharedPrefUtils
import org.fptn.vpn.utils.token.TokenUtils
import org.fptn.vpn.vpnclient.exception.PVNClientException
import java.security.MessageDigest

private const val TAG = "AccountRepository"
private const val PREFS_NAME = "account_prefs"
private const val APPLIED_TOKEN_HASH_KEY = "applied_token_sha256"
private const val REFRESH_TIMEOUT_MS = 15_000L

/**
 * Glue between [AccountAuth] and the existing token pipeline: the account's Firestore token is
 * parsed with [TokenUtils] and stored in the same server table a pasted token goes to.
 */
class AccountRepository(context: Context) {

    private val appContext = context.applicationContext
    private val auth = AccountAuthProvider.get()
    private val serverDao = AppDatabase.getInstance(appContext).serverDAO()
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val isAvailable: Boolean get() = auth.isAvailable
    val currentEmail: String? get() = auth.currentEmail

    /**
     * Signs in and replaces the server list with the one from the account's token. If the
     * account has no usable token yet, the session is dropped so the app state stays consistent
     * (no servers == logged out) and the error is rethrown.
     */
    @Throws(AccountAuthException::class, PVNClientException::class)
    suspend fun signInAndLoadServers(email: String, password: String) {
        auth.signIn(email, password)
        try {
            applyToken(auth.fetchServerToken(), keepSelection = false)
        } catch (e: Exception) {
            signOut()
            throw e
        }
    }

    /** Creates the account. The backend fills `users/{uid}.token`, so there are no servers yet. */
    @Throws(AccountAuthException::class)
    suspend fun signUp(email: String, password: String) {
        auth.signUp(email, password)
        // Sign-up leaves Firebase signed in; drop it until the token exists (see signIn above).
        auth.signOut()
    }

    @Throws(AccountAuthException::class)
    suspend fun sendPasswordReset(email: String) = auth.sendPasswordReset(email)

    /** Re-downloads the account's token and applies it right away (the "update token" button). */
    @Throws(AccountAuthException::class, PVNClientException::class)
    suspend fun refreshServers() {
        applyToken(auth.fetchServerToken(), keepSelection = true)
    }

    /**
     * Fire-and-forget refresh at app start. Never blocks the UI and never fails loudly: when
     * Firebase is unreachable the servers already stored keep working.
     */
    fun refreshServersInBackground() {
        if (!auth.isAvailable || auth.currentEmail == null) return
        backgroundScope.launch {
            try {
                withTimeout(REFRESH_TIMEOUT_MS) {
                    val token = auth.fetchServerToken()
                    if (FptnTileService.getServiceStateMutableLiveData().value?.isActiveState == true) {
                        XLog.tag(TAG).i("VPN is active, token refresh postponed")
                    } else if (hash(token) != prefs.getString(APPLIED_TOKEN_HASH_KEY, null)) {
                        applyToken(token, keepSelection = true)
                        XLog.tag(TAG).i("Servers updated from account token")
                    }
                }
            } catch (e: Exception) {
                XLog.tag(TAG).w("Background token refresh failed: %s", e.message)
            }
        }
    }

    fun signOut() {
        auth.signOut()
        prefs.edit().remove(APPLIED_TOKEN_HASH_KEY).apply()
    }

    private suspend fun applyToken(token: String, keepSelection: Boolean) {
        val servers = TokenUtils.parseToken(token)
        withContext(Dispatchers.IO) {
            val previouslySelected = if (keepSelection) serverDao.getSelected() else null
            serverDao.deleteAndInsert(servers)
            previouslySelected?.let { old ->
                serverDao.getServerList()
                    .firstOrNull { it.name == old.name && it.isCensured == old.isCensured }
                    ?.let { serverDao.setSelected(it.id) }
            }
            SharedPrefUtils.saveTokenUpdatedDate(appContext, System.currentTimeMillis())
            prefs.edit().putString(APPLIED_TOKEN_HASH_KEY, hash(token)).apply()
        }
    }

    private fun hash(token: String): String = MessageDigest.getInstance("SHA-256")
        .digest(token.trim().toByteArray())
        .joinToString("") { "%02x".format(it) }

    private companion object {
        val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
