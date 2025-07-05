package org.fptn.vpn.auth.data.di

import org.fptn.vpn.auth.data.AuthRepositoryImpl
import org.fptn.vpn.auth.domain.AuthRepository
import org.koin.dsl.module

val authDataModule =
    module {
        single<AuthRepository> { AuthRepositoryImpl(get()) }
    }
