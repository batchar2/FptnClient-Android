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

package org.fptn.vpn.ui.login

import android.content.Context
import org.fptn.vpn.R
import org.fptn.vpn.auth.AccountAuthException
import org.fptn.vpn.auth.AccountAuthException.Reason
import org.fptn.vpn.utils.ResourcesUtils.getStringResourceByName
import org.fptn.vpn.vpnclient.exception.ErrorCode
import org.fptn.vpn.vpnclient.exception.PVNClientException

/** User-facing text for a failure of token parsing or of an account (Firebase) call. */
fun loginErrorMessage(context: Context, e: Exception): String = when {
    e is AccountAuthException -> context.getString(e.reason.messageRes())
    e is PVNClientException && e.errorCode != ErrorCode.UNKNOWN_ERROR ->
        getStringResourceByName(context, e.errorCode.value) ?: e.message.orEmpty()
    else -> e.message.orEmpty()
}

private fun Reason.messageRes(): Int = when (this) {
    Reason.NOT_AVAILABLE -> R.string.auth_error_not_available
    Reason.NOT_SIGNED_IN -> R.string.auth_error_not_signed_in
    Reason.INVALID_EMAIL -> R.string.auth_error_invalid_email
    Reason.INVALID_CREDENTIALS -> R.string.auth_error_invalid_credentials
    Reason.EMAIL_ALREADY_IN_USE -> R.string.auth_error_email_in_use
    Reason.WEAK_PASSWORD -> R.string.auth_error_weak_password
    Reason.TOO_MANY_REQUESTS -> R.string.auth_error_too_many_requests
    Reason.NETWORK -> R.string.auth_error_network
    Reason.NO_TOKEN -> R.string.auth_error_no_token
    Reason.UNKNOWN -> R.string.auth_error_unknown
}
