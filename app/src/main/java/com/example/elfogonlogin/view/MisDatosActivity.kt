package com.example.elfogonlogin.view

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.elfogonlogin.R
import com.example.elfogonlogin.controller.PerfilRepository
import com.google.firebase.auth.FirebaseAuth

// "Mis datos": aquí se edita el nombre y el teléfono. El correo se muestra
// pero no se puede cambiar (es el de la cuenta con la que entras).
class MisDatosActivity : AppCompatActivity() {

    private lateinit var ivBack: View
    private lateinit var etNombre: EditText
    private lateinit var tvCorreoFijo: TextView
    private lateinit var etTelefono: EditText
    private lateinit var tvErrorMisDatos: TextView
    private lateinit var btnGuardarDatos: TextView

    // No se deja guardar hasta que lleguen tus datos actuales, para no
    // guardar por accidente un formulario vacío encima de los tuyos.
    private var datosCargados = false
    private var guardando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_mis_datos)

        ajustarPorTeclado()
        inicializarVistas()
        configurarFormatoTelefono()
        configurarAcciones()
        cargarDatos()
    }

    // Cuando sale el teclado, se sube todo para que no tape los campos.
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
        tvCorreoFijo = findViewById(R.id.tvCorreoFijo)
        etTelefono = findViewById(R.id.etTelefono)
        tvErrorMisDatos = findViewById(R.id.tvErrorMisDatos)
        btnGuardarDatos = findViewById(R.id.btnGuardarDatos)

        // El correo es el de la cuenta: se muestra de una vez.
        tvCorreoFijo.text = FirebaseAuth.getInstance().currentUser?.email.orEmpty()
    }

    private fun configurarAcciones() {
        ivBack.setOnClickListener { finish() }
        btnGuardarDatos.setOnClickListener { intentarGuardar() }

        // El botón "Listo" del teclado, en el teléfono, también guarda.
        etTelefono.setOnEditorActionListener { _, accion, _ ->
            if (accion == EditorInfo.IME_ACTION_DONE) {
                intentarGuardar()
                true
            } else {
                false
            }
        }
    }

    // Va acomodando el teléfono mientras escribes: 7123 4567
    private fun configurarFormatoTelefono() {
        etTelefono.addTextChangedListener(object : TextWatcher {
            private var editando = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (editando || s == null) return
                editando = true
                val digitos = s.toString().filter { it.isDigit() }.take(8)
                val formateado = PerfilRepository.formatoTelefono(digitos)
                if (formateado != s.toString()) {
                    s.replace(0, s.length, formateado)
                }
                editando = false
            }
        })
    }

    // ---------------------------------------------------------------------
    // Cargar y guardar
    // ---------------------------------------------------------------------

    private fun cargarDatos() {
        actualizarBoton(habilitado = false, texto = "Cargando…")
        PerfilRepository.obtenerPerfil { perfil ->
            if (isFinishing || isDestroyed) return@obtenerPerfil

            etNombre.setText(perfil.nombre)
            etNombre.setSelection(etNombre.text.length)
            etTelefono.setText(PerfilRepository.formatoTelefono(perfil.telefono))

            datosCargados = true
            actualizarBoton(habilitado = true)
        }
    }

    private fun intentarGuardar() {
        if (guardando || !datosCargados) return

        ocultarTeclado()
        tvErrorMisDatos.visibility = View.GONE

        val nombre = etNombre.text.toString().trim()
        val telefono = etTelefono.text.toString().filter { it.isDigit() }

        if (nombre.length < 2) {
            mostrarError("Escribe tu nombre (mínimo 2 letras)", etNombre)
            return
        }
        // El teléfono es opcional: vacío está bien; si lo escribes, debe ser válido.
        if (telefono.isNotEmpty() && !PerfilRepository.telefonoValido(telefono)) {
            mostrarError("Escribe un celular de 8 dígitos que empiece con 6 o 7, o déjalo vacío", etTelefono)
            return
        }

        guardando = true
        actualizarBoton(habilitado = false, texto = "Guardando…")

        PerfilRepository.guardarDatos(nombre, telefono) { ok, error ->
            if (isFinishing || isDestroyed) return@guardarDatos

            guardando = false
            actualizarBoton(habilitado = true)

            if (ok) {
                Toast.makeText(this, "Datos guardados", Toast.LENGTH_SHORT).show()
                finish() // regresa al Perfil, que se actualiza solo
            } else {
                mostrarError("No se pudieron guardar tus datos: ${error ?: "error desconocido"}", null)
            }
        }
    }

    private fun mostrarError(mensaje: String, campo: EditText?) {
        tvErrorMisDatos.text = mensaje
        tvErrorMisDatos.visibility = View.VISIBLE
        campo?.requestFocus()
    }

    private fun actualizarBoton(habilitado: Boolean, texto: String = "Guardar cambios") {
        btnGuardarDatos.text = texto
        btnGuardarDatos.isEnabled = habilitado
        btnGuardarDatos.alpha = if (habilitado) 1f else 0.6f
    }

    private fun ocultarTeclado() {
        val vista = currentFocus ?: return
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(vista.windowToken, 0)
    }
}