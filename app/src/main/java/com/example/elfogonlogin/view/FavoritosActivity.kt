package com.example.elfogonlogin.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.elfogonlogin.R
import com.example.elfogonlogin.controller.CarritoRepository
import com.example.elfogonlogin.controller.DetalleProducto
import com.example.elfogonlogin.controller.DetalleProductoRepository
import com.example.elfogonlogin.controller.FavoritosRepository
import com.example.elfogonlogin.controller.ItemCarrito
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale

// "Favoritos": los productos que marcaste con el corazón, el más reciente
// arriba. Se actualiza solo: si quitas uno aquí (o desde el detalle de otro
// producto), la lista cambia al instante. Tocar una fila abre el detalle del
// producto; "Agregar" lo manda al carrito con su tamaño base; el corazón rojo
// lo quita de favoritos.
class FavoritosActivity : AppCompatActivity() {

    private lateinit var ivBack: View
    private lateinit var tvCargandoFavoritos: View
    private lateinit var scrollFavoritos: View
    private lateinit var contenedorFavoritos: ViewGroup
    private lateinit var layoutSinFavoritos: View
    private lateinit var btnIrAlMenuFavoritos: View

    private var listenerFavoritos: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_favoritos)

        ajustarBarrasDelSistema()
        inicializarVistas()
        configurarAcciones()
    }

    override fun onResume() {
        super.onResume()
        listenerFavoritos = FavoritosRepository.escucharFavoritos { ids -> mostrarFavoritos(ids) }
    }

    override fun onPause() {
        super.onPause()
        listenerFavoritos?.remove()
    }

    private fun ajustarBarrasDelSistema() {
        val vistaRaiz = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(vistaRaiz) { view, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
    }

    private fun inicializarVistas() {
        ivBack = findViewById(R.id.ivBack)
        tvCargandoFavoritos = findViewById(R.id.tvCargandoFavoritos)
        scrollFavoritos = findViewById(R.id.scrollFavoritos)
        contenedorFavoritos = findViewById(R.id.contenedorFavoritos)
        layoutSinFavoritos = findViewById(R.id.layoutSinFavoritos)
        btnIrAlMenuFavoritos = findViewById(R.id.btnIrAlMenuFavoritos)
    }

    private fun configurarAcciones() {
        ivBack.setOnClickListener { finish() }

        btnIrAlMenuFavoritos.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
    }

    // ---------------------------------------------------------------------
    // La lista
    // ---------------------------------------------------------------------

    private fun mostrarFavoritos(ids: List<String>) {
        tvCargandoFavoritos.visibility = View.GONE
        contenedorFavoritos.removeAllViews()

        // Si algún id ya no existe en el catálogo, simplemente no se muestra.
        val productos = ids.mapNotNull { DetalleProductoRepository.obtener(it) }

        if (productos.isEmpty()) {
            scrollFavoritos.visibility = View.GONE
            layoutSinFavoritos.visibility = View.VISIBLE
            return
        }

        layoutSinFavoritos.visibility = View.GONE
        scrollFavoritos.visibility = View.VISIBLE

        for (detalle in productos) {
            contenedorFavoritos.addView(crearFilaFavorito(detalle))
        }
    }

    private fun crearFilaFavorito(detalle: DetalleProducto): View {
        val fila = LayoutInflater.from(this).inflate(R.layout.item_favorito, contenedorFavoritos, false)

        fila.findViewById<ImageView>(R.id.ivImagenFavorito).setImageResource(detalle.imagenResId)
        fila.findViewById<TextView>(R.id.tvNombreFavorito).text = detalle.nombre
        fila.findViewById<TextView>(R.id.tvDescripcionFavorito).text = detalle.descripcion
        // El precio base es el del primer tamaño, el mismo que se ve al abrir el producto.
        fila.findViewById<TextView>(R.id.tvPrecioFavorito).text =
            String.format(Locale.US, "$%.2f", detalle.tamanos.first().precio)

        // Tocar la fila abre el detalle para personalizarlo.
        fila.setOnClickListener {
            val intent = Intent(this, DetalleProductoActivity::class.java)
            intent.putExtra(DetalleProductoActivity.EXTRA_PRODUCTO_ID, detalle.id)
            startActivity(intent)
        }

        fila.findViewById<View>(R.id.btnAgregarFavorito).setOnClickListener { agregarAlCarrito(detalle) }
        fila.findViewById<View>(R.id.btnQuitarFavorito).setOnClickListener { quitarDeFavoritos(detalle) }

        return fila
    }

    // Agrega con el tamaño base y sin extras (igual que el botón "Agregar" del
    // Menú). Para personalizar hay que abrir el producto.
    private fun agregarAlCarrito(detalle: DetalleProducto) {
        val tamanoBase = detalle.tamanos.first()
        CarritoRepository.agregarItem(
            ItemCarrito(
                nombreProducto = detalle.nombre,
                detalleId = detalle.id,
                nombreTamano = tamanoBase.nombre,
                precioTamano = tamanoBase.precio,
                extrasSeleccionados = emptyList()
            )
        )
        Toast.makeText(this, "Agregado al carrito: ${detalle.nombre}", Toast.LENGTH_SHORT).show()
    }

    // Al quitarlo, la fila desaparece sola: la lista se vuelve a armar cuando
    // Firestore avisa del cambio.
    private fun quitarDeFavoritos(detalle: DetalleProducto) {
        FavoritosRepository.marcar(detalle.id, false) { ok, error ->
            if (!ok && !isFinishing && !isDestroyed) {
                Toast.makeText(this, "No se pudo quitar: ${error ?: "error desconocido"}", Toast.LENGTH_LONG).show()
            }
        }
        Toast.makeText(this, "Quitado de favoritos", Toast.LENGTH_SHORT).show()
    }
}