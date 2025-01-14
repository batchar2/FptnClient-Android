package com.filantrop.pvnclient.auth.domain

interface AuthRepository {

    fun login(token: String)
    fun logout()
}
