package com.filantrop.pvnclient.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filantrop.pvnclient.auth.domain.AuthInteractor
import com.filantrop.pvnclient.core.common.Result
import com.filantrop.pvnclient.core.common.asResult
import com.filantrop.pvnclient.core.model.UserData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    authInteractor: AuthInteractor,
) : ViewModel() {
    val uiState: StateFlow<AuthActivityUiState> =
        authInteractor.userData
            .asResult()
            .map { result ->
                when (result) {
                    is Result.Error -> AuthActivityUiState.Login
                    is Result.Loading -> AuthActivityUiState.Loading
                    is Result.Success -> AuthActivityUiState.Main(result.data)
                }
            }.stateIn(
                scope = viewModelScope,
                initialValue = AuthActivityUiState.Loading,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            )

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

sealed interface AuthActivityUiState {
    data object Loading : AuthActivityUiState

    data object Login : AuthActivityUiState

    data class Main(
        val userData: UserData,
    ) : AuthActivityUiState

    /**
     * Returns `true` if the state wasn't loaded yet and it should keep showing the splash screen.
     */
    fun shouldKeepSplashScreen() = this is Loading

    /**
     * Returns `true` if the dynamic color is disabled.
     */
    val shouldDisableDynamicTheming: Boolean get() = true

    /**
     * Returns `true` if the Android theme should be used.
     */
    val shouldUseAndroidTheme: Boolean get() = false

    /**
     * Returns `true` if dark theme should be used.
     */
    fun shouldUseDarkTheme(isSystemDarkTheme: Boolean) = isSystemDarkTheme
}
