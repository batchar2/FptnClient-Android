package org.fptn.vpn.auth.ui

import org.fptn.vpn.auth.data.di.authDataModule
import org.fptn.vpn.auth.domain.di.authDomainModule
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule =
    module {
        viewModel { AuthViewModel() }
    }

val authModule =
    module {
        includes(viewModelModule, authDataModule, authDomainModule)
    }
