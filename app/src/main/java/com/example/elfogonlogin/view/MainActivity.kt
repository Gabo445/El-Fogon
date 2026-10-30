package com.example.elfogonlogin.view

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.util.Patterns
import android.view.MotionEvent
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.elfogonlogin.R
import com.example.elfogonlogin.controller.AdminRepository
import com.example.elfogonlogin.controller.FirebaseErrorHelper
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val TAG = "ElFogon - Login"

class MainActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var cbRecordarme: CheckBox
    private lateinit var btnIniciarSesion: Button
    private lateinit var tvRegistrarse: TextView
    private lateinit var btnGoogleSignIn: Button

    private var passwordVisible = false

    // Para no mandar a la persona dos veces si el aviso de "ya entró" llega repetido.
    private var enrutando = false

    private lateinit var auth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        ajustarPorTeclado()
        inicializarVistas()
        configurarTogglePassword()
        configurarBotonLogin()
        configurarEnlaceRegistro()
        configurarBotonGoogle()
        checkUser()
    }

    override fun onResume() {
        super.onResume()
        auth.addAuthStateListener(authStateListener)
    }

    override fun onPause() {
        super.onPause()
        auth.removeAuthStateListener(authStateListener)
    }

    private fun ajustarPorTeclado() {
        val vistaRaiz = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(vistaRaiz) { view, insets ->
            val teclado = insets.getInsets(WindowInsetsCompat.Type.ime())
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(barras.left, barras.top, barras.right, maxOf(teclado.bottom, barras.bottom))
            insets
        }
    }

    private fun inicializarVistas() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        cbRecordarme = findViewById(R.id.cbRecordarme)
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion)
        tvRegistrarse = findViewById(R.id.tvRegistrarse)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
    }

    private fun configurarTogglePassword() {
        etPassword.setOnTouchListener { _, event ->
            val DRAWABLE_END = 2
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableWidth =
                    etPassword.compoundDrawables[DRAWABLE_END]?.bounds?.width() ?: 0
                if (event.rawX >= (etPassword.right - drawableWidth - etPassword.paddingEnd)) {
                    togglePasswordVisibility()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun togglePasswordVisibility() {
        passwordVisible = !passwordVisible
        if (passwordVisible) {
            etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            etPassword.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_lock, 0, R.drawable.ic_visibility_off, 0
            )
        } else {
            etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            etPassword.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_lock, 0, R.drawable.ic_visibility, 0
            )
        }
        etPassword.setSelection(etPassword.text?.length ?: 0)
    }

    private fun configurarBotonLogin() {
        btnIniciarSesion.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            when {
                email.isEmpty() || password.isEmpty() -> {
                    Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                }
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    Toast.makeText(this, "Ingresa un correo electrónico válido", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    login(email, password)
                }
            }
        }
    }

    private fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.i(TAG, "Login exitoso: $email (Recordarme: ${cbRecordarme.isChecked})")
                    Toast.makeText(this, "¡Bienvenido a El Fogón!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.i(TAG, "Intento de login fallido: $email")
                Toast.makeText(this, FirebaseErrorHelper.mensajeAmigable(exception), Toast.LENGTH_SHORT).show()
            }
    }

    private fun configurarBotonGoogle() {
        btnGoogleSignIn.setOnClickListener {
            iniciarSesionConGoogle()
        }
    }

    private fun iniciarSesionConGoogle() {
        val credentialManager = CredentialManager.create(this)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@MainActivity,
                    request = request
                )
                manejarCredencialGoogle(result.credential)
            } catch (e: GetCredentialException) {
                Log.i(TAG, "Google Sign-In cancelado o fallido: ${e.message}")
                Toast.makeText(this@MainActivity, "No se pudo iniciar sesión con Google", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun manejarCredencialGoogle(credential: Credential) {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

                auth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "¡Bienvenido a El Fogón!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, FirebaseErrorHelper.mensajeAmigable(exception), Toast.LENGTH_SHORT).show()
                    }
            } catch (e: GoogleIdTokenParsingException) {
                Toast.makeText(this, "No se pudo procesar la cuenta de Google", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Tipo de credencial no reconocido", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkUser() {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                entrarSegunTipoDeCuenta()
            }
        }
    }

    // Aquí pasan TODAS las entradas: al iniciar sesión con correo, con Google y
    // cuando la app se abre con la sesión ya iniciada. Se decide a dónde va la
    // persona: si su cuenta es de administrador (existe admins/{su uid} en
    // Firestore) entra directo al Panel de control; cualquier otra cuenta, al
    // Home de siempre. Si no se puede comprobar (por ejemplo, sin internet),
    // se trata como cliente.
    private fun entrarSegunTipoDeCuenta() {
        if (enrutando) return
        enrutando = true

        AdminRepository.esAdmin { esAdministrador ->
            if (isFinishing || isDestroyed) return@esAdmin

            val destino = if (esAdministrador) {
                PanelControlActivity::class.java
            } else {
                HomeActivity::class.java
            }
            startActivity(Intent(this, destino))
            finish()
        }
    }

    private fun configurarEnlaceRegistro() {
        tvRegistrarse.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}