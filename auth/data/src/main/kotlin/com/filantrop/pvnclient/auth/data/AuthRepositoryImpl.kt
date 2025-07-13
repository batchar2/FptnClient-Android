package org.fptn.vpn.auth.data

import kotlinx.coroutines.flow.Flow
import org.fptn.vpn.auth.domain.AuthRepository
import org.fptn.vpn.core.persistent.PreferenceStore

class AuthRepositoryImpl(
    private val preferenceStore: PreferenceStore,
) : AuthRepository {
    override val token: Flow<String?> = preferenceStore.token

    override suspend fun loginWithToken(token: String) = preferenceStore.updateToken(token)

    override suspend fun logout() = preferenceStore.clearAllData()
}
