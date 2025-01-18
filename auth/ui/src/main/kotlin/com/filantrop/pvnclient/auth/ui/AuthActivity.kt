package com.filantrop.pvnclient.auth.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class AuthActivity : ComponentActivity() {
    private val viewModel: AuthViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState
                    .collect {
                        when (it) {
                            AuthActivityUiState.Loading -> splashScreen.setKeepOnScreenCondition { true }
                            AuthActivityUiState.Login -> System.out.println("moggot login")
                            is AuthActivityUiState.Success -> {
                            }
                        }
                    }
            }
        }
    }
}
