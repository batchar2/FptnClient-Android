package com.filantrop.pvnclient.di

import org.fptn.vpn.di.appModule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.koin.test.verify.verify

@Category(CheckModulesTest::class)
class CheckModulesTest {
    @Test
    fun checkAllModules() {
        appModule.verify()
    }
}
