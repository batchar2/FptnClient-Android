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
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.suspendCancellableCoroutine
import org.fptn.vpn.auth.AccountAuthException.Reason
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "FirebaseAccountAuth"
private const val USERS_COLLECTION = "users"
private const val TOKEN_FIELD = "token"

/**
 * Firebase Auth (email/password) + Firestore. The token is read from `users/{uid}.token`;
 * the document is written by the backend only — Firestore rules must forbid client writes.
 */
internal class FirebaseAccountAuth(private val auth: FirebaseAuth) : AccountAuth {

    override val isAvailable = true

    override val currentEmail: String?
        get() = auth.currentUser?.email

    override suspend fun signIn(email: String, password: String) {
        mapErrors { auth.signInWithEmailAndPassword(email, password).await() }
    }

    override suspend fun signUp(email: String, password: String) {
        mapErrors { auth.createUserWithEmailAndPassword(email, password).await() }
    }

    override suspend fun sendPasswordReset(email: String) {
        mapErrors { auth.sendPasswordResetEmail(email).await() }
    }

    override suspend fun fetchServerToken(): String {
        val uid = auth.currentUser?.uid ?: throw AccountAuthException(Reason.NOT_SIGNED_IN)
        val snapshot = mapErrors {
            FirebaseFirestore.getInstance()
                .collection(USERS_COLLECTION)
                .document(uid)
                .get(Source.SERVER)
                .await()
        }
        val token = snapshot.getString(TOKEN_FIELD)
        if (token.isNullOrBlank()) {
            XLog.tag(TAG).w("No token in Firestore for the signed-in user [exists=%b]", snapshot.exists())
            throw AccountAuthException(Reason.NO_TOKEN)
        }
        return token
    }

    override fun signOut() = auth.signOut()

    private suspend fun <T> mapErrors(block: suspend () -> T): T = try {
        block()
    } catch (e: AccountAuthException) {
        throw e
    } catch (e: Exception) {
        XLog.tag(TAG).e("Firebase call failed: %s: %s", e.javaClass.simpleName, e.message)
        throw AccountAuthException(reasonOf(e), e)
    }

    private fun reasonOf(e: Exception): Reason = when (e) {
        // Weak-password is a subclass of invalid-credentials, so it must be checked first.
        is FirebaseAuthWeakPasswordException -> Reason.WEAK_PASSWORD
        is FirebaseAuthInvalidCredentialsException ->
            if (e.errorCode == "ERROR_INVALID_EMAIL") Reason.INVALID_EMAIL else Reason.INVALID_CREDENTIALS
        is FirebaseAuthInvalidUserException -> Reason.INVALID_CREDENTIALS
        is FirebaseAuthUserCollisionException -> Reason.EMAIL_ALREADY_IN_USE
        is FirebaseTooManyRequestsException -> Reason.TOO_MANY_REQUESTS
        is FirebaseNetworkException -> Reason.NETWORK
        is FirebaseFirestoreException -> when (e.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            -> Reason.NETWORK
            FirebaseFirestoreException.Code.NOT_FOUND -> Reason.NO_TOKEN
            else -> Reason.UNKNOWN
        }
        else -> Reason.UNKNOWN
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        val error = task.exception
        when {
            error != null -> cont.resumeWithException(error)
            task.isCanceled -> cont.cancel()
            else -> cont.resume(task.result)
        }
    }
}
