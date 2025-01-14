package com.filantrop.pvnclient.auth.domain

import org.koin.core.annotation.Single

interface AuthInteractor {

    fun login(token: String)
    fun logout()
}

@Single(binds = [AuthInteractor::class])
class AuthInteractorImpl: AuthInteractor {
    override fun login(token: String) {
        TODO("Not yet implemented")
    }

    override fun logout() {
        TODO("Not yet implemented")
    }

}