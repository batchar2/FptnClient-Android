package com.filantrop.pvnclient.di

import com.filantrop.pvnclient.auth.ui.di.authModule
import com.filantrop.pvnclient.core.persistent.di.persistentModule
import org.koin.core.module.Module
import org.koin.dsl.module

val appModule: Module =
    module {
        includes(
            persistentModule,
            authModule,
        )
    }
