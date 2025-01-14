package com.filantrop.pvnclient.auth.data

import com.filantrop.pvnclient.auth.domain.AuthRepository
import org.koin.core.annotation.Single

@Single(binds = [AuthRepository::class])
class AuthRepositoryImpl: AuthRepository {
    override fun login(token: String) {
        TODO("Not yet implemented")
    }

    override fun logout() {
        TODO("Not yet implemented")
    }
}
