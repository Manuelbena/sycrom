package com.manuelbena.synkron.presentation.login

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val googleSignInClient: GoogleSignInClient,
    @ApplicationContext private val context: Context // Necesario para comprobar sesión existente
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Loading)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    init {
        checkCurrentSession()
    }

    // 1. Verificamos si el usuario ya entró antes (Auto-login)
    private fun checkCurrentSession() {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null && !account.isExpired()) {
            _loginState.value = LoginState.Success(account)
        } else {
            _loginState.value = LoginState.Idle // Esperando a que pulse el botón
        }
    }

    // Extensión simple para chequear expiración (opcional, Google suele manejarlo)
    private fun GoogleSignInAccount.isExpired() = false

    // 2. Obtener el Intent para lanzar la ventana de Google
    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    // 3. Procesar el resultado que vuelve de la Activity de Google
    fun handleSignInResult(data: Intent?) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                _loginState.value = LoginState.Success(account)
            } else {
                _loginState.value = LoginState.Error("No se pudo iniciar sesión")
            }
        } catch (e: ApiException) {
            _loginState.value = LoginState.Error("Error de Google: ${e.statusCode}")
        }
    }
}

sealed class LoginState {
    object Loading : LoginState() // Verificando sesión
    object Idle : LoginState()    // Mostrar botón
    data class Success(val account: GoogleSignInAccount) : LoginState()
    data class Error(val message: String) : LoginState()
}