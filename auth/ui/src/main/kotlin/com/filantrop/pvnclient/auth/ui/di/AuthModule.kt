package com.filantrop.pvnclient.auth.ui.di

import com.filantrop.pvnclient.auth.data.di.authDataModule
import com.filantrop.pvnclient.auth.domain.di.authDomainModule
import com.filantrop.pvnclient.auth.ui.AuthViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule =
    module {
        viewModel { AuthViewModel(get()) }
    }

val authModule =
    module {
        includes(viewModelModule, authDataModule, authDomainModule)
    }
