package com.filantrop.pvnclient.auth.ui.di

import com.filantrop.pvnclient.auth.data.di.AuthDataModule
import com.filantrop.pvnclient.auth.domain.di.AuthDomainModule
import org.koin.dsl.module
//import org.koin.ksp.generated.module

val authModule = module {
//    includes(AuthDomainModule.module, AuthDataModule.module)
}
