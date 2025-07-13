package org.fptn.vpn.auth.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.fptn.vpn.core.model.UserData

interface AuthInteractor {
    val userData: Flow<UserData>

    suspend fun loginWithToken(token: String)

    suspend fun logout()
}

class AuthInteractorImpl(
    private val authRepository: AuthRepository,
) : AuthInteractor {
    override val userData: Flow<UserData> =
        authRepository.token.map {
            requireNotNull(it) { "Token is null" }
            UserData(it)
        }

    override suspend fun loginWithToken(token: String) = authRepository.loginWithToken(token)

    override suspend fun logout() = authRepository.logout()
}
