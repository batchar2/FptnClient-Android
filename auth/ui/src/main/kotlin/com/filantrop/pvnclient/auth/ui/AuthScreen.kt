package com.filantrop.pvnclient.auth.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun AuthScreen(viewModel: AuthViewModel = koinViewModel()) {
    val uiState: AuthState by viewModel.uiState.collectAsStateWithLifecycle()
    AuthState(
        uiState,
    ) { viewModel.login(it) }
}

@Composable
fun AuthState(state: AuthState, onLoginClick: (token: String) -> Unit) {
    Scaffold(
        modifier = Modifier.navigationBarsPadding(),
    ) { padding: PaddingValues ->
        Text(modifier = Modifier.padding(padding), text = "Login")
    }
}
