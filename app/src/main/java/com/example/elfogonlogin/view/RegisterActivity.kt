package com.example.elfogonlogin.view

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
import com.example.elfogonlogin.R
import com.example.elfogonlogin.controller.FirebaseErrorHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

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

    // Referencia a Firebase Authentication
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

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
                else -> {
                    registrar(nombre, email, password)
                }
            }
        }
    }

    // Crea la cuenta en Firebase Authentication y le guarda el nombre en el
    // perfil (para que el Home pueda mostrar "Bienvenido, [nombre]").
    // IMPORTANTE: aquí sí hacemos finish() al terminar. Antes no lo hacíamos
    // porque queríamos que solo checkUser() (en MainActivity) navegara a
    // Home, pero esa pantalla deja de escuchar la sesión en cuanto
    // RegisterActivity queda al frente (onPause se dispara ahí). Sin este
    // finish(), la cuenta se creaba bien pero la app se quedaba trabada en
    // esta pantalla. Al hacer finish(), MainActivity vuelve a primer plano,
    // su listener se reactiva y ahí sí nos manda al Home.
    private fun registrar(nombre: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.i(TAG, "Usuario registrado: $email")

                    val actualizarPerfil = UserProfileChangeRequest.Builder()
                        .setDisplayName(nombre)
                        .build()

                    auth.currentUser?.updateProfile(actualizarPerfil)
                        ?.addOnCompleteListener {
                            Toast.makeText(this, "Cuenta creada. ¡Bienvenido a El Fogón!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    // Nota: telefono y direccion todavía no se guardan en
                    // ningún lado. Firebase Authentication solo tiene campo
                    // para el nombre (displayName); para telefono/direccion
                    // necesitaríamos sumar Cloud Firestore, que es aparte.
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, FirebaseErrorHelper.mensajeAmigable(exception), Toast.LENGTH_SHORT).show()
            }
    }

    private fun configurarEnlaceLogin() {
        tvIniciaSesion.setOnClickListener { finish() }
    }

    private fun configurarBotonAtras() {
        ivBack.setOnClickListener { finish() }
    }
}