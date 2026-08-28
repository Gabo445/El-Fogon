package com.example.elfogonlogin

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.util.Patterns
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

private val TAG = "ElFogon - Registro"

class RegisterActivity : AppCompatActivity() {

    private lateinit var ivBack: ImageView
    private lateinit var etNombre: EditText
    private lateinit var etEmail: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etDireccion: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnCrearCuenta: Button
    private lateinit var tvIniciaSesion: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        ajustarPorTeclado()
        inicializarVistas()
        configurarTogglePassword(etPassword)
        configurarTogglePassword(etConfirmPassword)
        configurarBotonRegistro()
        configurarEnlaceLogin()
        configurarBotonAtras()
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
        ivBack = findViewById(R.id.ivBack)
        etNombre = findViewById(R.id.etNombre)
        etEmail = findViewById(R.id.etEmail)
        etTelefono = findViewById(R.id.etTelefono)
        etDireccion = findViewById(R.id.etDireccion)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnCrearCuenta = findViewById(R.id.btnCrearCuenta)
        tvIniciaSesion = findViewById(R.id.tvIniciaSesion)
    }

    // Se reutiliza para el campo de contraseña y el de confirmar contraseña
    private fun configurarTogglePassword(campo: EditText) {
        var visible = false
        campo.setOnTouchListener { _, event ->
            val DRAWABLE_END = 2
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableWidth = campo.compoundDrawables[DRAWABLE_END]?.bounds?.width() ?: 0
                if (event.rawX >= (campo.right - drawableWidth - campo.paddingEnd)) {
                    visible = !visible
                    if (visible) {
                        campo.transformationMethod = HideReturnsTransformationMethod.getInstance()
                        campo.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_lock, 0, R.drawable.ic_visibility_off, 0
                        )
                    } else {
                        campo.transformationMethod = PasswordTransformationMethod.getInstance()
                        campo.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_lock, 0, R.drawable.ic_visibility, 0
                        )
                    }
                    campo.setSelection(campo.text?.length ?: 0)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun configurarBotonRegistro() {
        btnCrearCuenta.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val telefono = etTelefono.text.toString().trim()
            val direccion = etDireccion.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            when {
                nombre.isEmpty() || email.isEmpty() || telefono.isEmpty() ||
                        direccion.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() -> {
                    Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                }
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    Toast.makeText(this, "Ingresa un correo electrónico válido", Toast.LENGTH_SHORT).show()
                }
                password.length < 6 -> {
                    Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                }
                password != confirmPassword -> {
                    Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                }
                UsuariosRepository.existeUsuario(email) -> {
                    Toast.makeText(this, "Ya existe una cuenta con ese correo", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    UsuariosRepository.registrarUsuario(email, password)
                    Log.i(TAG, "Usuario registrado: $email")
                    Toast.makeText(this, "Cuenta creada. Ya puedes iniciar sesión", Toast.LENGTH_SHORT).show()
                    finish() // Regresa al login
                }
            }
        }
    }

    private fun configurarEnlaceLogin() {
        tvIniciaSesion.setOnClickListener { finish() }
    }

    private fun configurarBotonAtras() {
        ivBack.setOnClickListener { finish() }
    }
}