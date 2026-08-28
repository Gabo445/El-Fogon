package com.example.elfogonlogin

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

private val TAG = "ElFogon - Login"

class MainActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var cbRecordarme: CheckBox
    private lateinit var btnIniciarSesion: Button
    private lateinit var tvRegistrarse: TextView

    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ajustarPorTeclado()
        inicializarVistas()
        configurarTogglePassword()
        configurarBotonLogin()
        configurarEnlaceRegistro()
    }

    // Deja espacio abajo cuando aparece el teclado, para que no tape los campos
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
                UsuariosRepository.validarCredenciales(email, password) -> {
                    Log.i(TAG, "Login exitoso: $email (Recordarme: ${cbRecordarme.isChecked})")
                    Toast.makeText(this, "¡Bienvenido a El Fogón!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }
                else -> {
                    Log.i(TAG, "Intento de login fallido: $email")
                    Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun configurarEnlaceRegistro() {
        tvRegistrarse.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}