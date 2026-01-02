package com.manuelbena.synkron.presentation.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.SignInButton
import com.manuelbena.synkron.databinding.ActivityLoginBinding
import com.manuelbena.synkron.presentation.activitys.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    // Lanzador del resultado de Google
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.handleSignInResult(result.data)
        } else {
            Toast.makeText(this, "Inicio cancelado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeState()
    }

    private fun setupUI() {
        binding.btnGoogleSignIn.setSize(SignInButton.SIZE_WIDE)
        binding.btnGoogleSignIn.setOnClickListener {
            launchGoogleSignIn()
        }
    }

    private fun launchGoogleSignIn() {
        val intent = viewModel.getSignInIntent()
        googleSignInLauncher.launch(intent)
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.loginState.collect { state ->
                when (state) {
                    is LoginState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.btnGoogleSignIn.visibility = View.GONE
                    }
                    is LoginState.Idle -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnGoogleSignIn.visibility = View.VISIBLE
                    }
                    is LoginState.Success -> {
                        // ¡ÉXITO! Navegamos a la MainActivity
                        navigateToHome()
                    }
                    is LoginState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnGoogleSignIn.visibility = View.VISIBLE
                        Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java)
        // Limpiamos la pila para que si le da "Atrás" no vuelva al login
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}