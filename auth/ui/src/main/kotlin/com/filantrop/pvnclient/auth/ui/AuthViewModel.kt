package com.filantrop.pvnclient.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filantrop.pvnclient.auth.domain.AuthInteractor
import com.filantrop.pvnclient.core.model.UserData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AuthViewModel(
    authInteractor: AuthInteractor,
) : ViewModel() {
    val uiState: StateFlow<AuthActivityUiState> =
        authInteractor.userData
            .map {
                AuthActivityUiState.Success(it)
            }.catch { AuthActivityUiState.Login }
            .stateIn(
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

    data class Success(
        val userData: UserData,
    ) : AuthActivityUiState
}
