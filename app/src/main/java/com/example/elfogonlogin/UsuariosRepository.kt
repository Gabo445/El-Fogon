package com.example.elfogonlogin

// Repositorio en memoria: mientras la app siga abierta, aquí se guardan
// los usuarios. Al cerrar la app por completo, esta lista se reinicia
// porque no se está usando base de datos ni almacenamiento persistente.
object UsuariosRepository {

    private val usuarios = mutableMapOf(
        "admin@elfogon.com" to "admin123",
        "usuario@elfogon.com" to "usuario123"
    )

    fun validarCredenciales(email: String, password: String): Boolean {
        return usuarios[email] == password
    }

    fun existeUsuario(email: String): Boolean {
        return usuarios.containsKey(email)
    }

    fun registrarUsuario(email: String, password: String) {
        usuarios[email] = password
    }
}