package com.example.elfogonlogin.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.elfogonlogin.R
import com.example.elfogonlogin.controller.CarritoRepository
import com.example.elfogonlogin.controller.CodigoPromo
import com.example.elfogonlogin.controller.CodigoPromoRepository
import com.example.elfogonlogin.controller.DireccionRepository
import com.example.elfogonlogin.controller.ItemCarrito
import com.example.elfogonlogin.model.ResumenPedido
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale

class CarritoActivity : AppCompatActivity() {

    private lateinit var ivBack: View
    private lateinit var ivCart: FrameLayout
    private lateinit var tvCartBadge: TextView

    private lateinit var rowDireccion: View
    private lateinit var tvDireccionGuardada: TextView
    private lateinit var tvTiempoEstimado: TextView
    private lateinit var tvVerEnMapa: View
    private lateinit var layoutCarritoVacio: View
    private lateinit var layoutCarritoConItems: View
    private lateinit var contenedorItems: LinearLayout
    private lateinit var btnIrAlMenu: View

    private lateinit var etCodigoPromocional: EditText
    private lateinit var btnAplicarCodigo: View
    private lateinit var tvMensajeCodigo: TextView

    private lateinit var tvSubtotal: TextView
    private lateinit var rowDescuento: View
    private lateinit var tvDescuento: TextView
    private lateinit var tvCostoEntrega: TextView
    private lateinit var tvImpuestos: TextView
    private lateinit var tvTotal: TextView
    private lateinit var btnProcederPago: View

    // Se queda solo mientras estés en esta pantalla (no se guarda) — si
    // sales del Carrito y vuelves a entrar, hay que aplicarlo de nuevo.
    private var codigoPromoAplicado: CodigoPromo? = null

    private lateinit var navInicio: View
    private lateinit var navMenu: View
    private lateinit var navCarrito: View
    private lateinit var navPedidos: View
    private lateinit var navPerfil: View

    private var listenerCarrito: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_carrito)

        ajustarPorTeclado()
        inicializarVistas()
        configurarAcciones()
        configurarCodigoPromocional()
        configurarBottomNav()
        configurarBotonAtras()
    }

    override fun onResume() {
        super.onResume()
        listenerCarrito = CarritoRepository.escucharCambios {
            actualizarBadgeCarrito()
            actualizarListaCarrito()
        }
        cargarDireccionGuardada()
    }

    // Se pide una sola vez (no en tiempo real como el carrito) porque la
    // dirección no cambia seguido — pero se vuelve a pedir cada vez que
    // regresas a esta pantalla, por si la acabas de guardar/editar/borrar.
    private fun cargarDireccionGuardada() {
        DireccionRepository.obtenerDireccion { direccion ->
            if (direccion != null) {
                tvDireccionGuardada.text = "${direccion.zona} — ${direccion.direccionEspecifica}"
                tvTiempoEstimado.text = "Entrega estimada: " +
                        (DireccionRepository.tiemposEstimadosPorZona[direccion.zona] ?: "30 - 40 min")

                if (direccion.latitud != null && direccion.longitud != null) {
                    tvVerEnMapa.visibility = View.VISIBLE
                    tvVerEnMapa.setOnClickListener {
                        val uri = Uri.parse(
                            "https://www.google.com/maps/search/?api=1&query=${direccion.latitud},${direccion.longitud}"
                        )
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                    }
                } else {
                    tvVerEnMapa.visibility = View.GONE
                }
            } else {
                tvDireccionGuardada.text = "Pon tu dirección"
                tvTiempoEstimado.text = "Entrega estimada: 30 - 40 min"
                tvVerEnMapa.visibility = View.GONE
            }
        }
    }

    override fun onPause() {
        super.onPause()
        listenerCarrito?.remove()
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
        ivBack = findViewById(R.id.ivBack)
        ivCart = findViewById(R.id.ivCart)
        tvCartBadge = findViewById(R.id.tvCartBadge)

        rowDireccion = findViewById(R.id.rowDireccion)
        tvDireccionGuardada = findViewById(R.id.tvDireccionGuardada)
        tvTiempoEstimado = findViewById(R.id.tvTiempoEstimado)
        tvVerEnMapa = findViewById(R.id.tvVerEnMapa)
        layoutCarritoVacio = findViewById(R.id.layoutCarritoVacio)
        layoutCarritoConItems = findViewById(R.id.layoutCarritoConItems)
        contenedorItems = findViewById(R.id.contenedorItems)
        btnIrAlMenu = findViewById(R.id.btnIrAlMenu)

        etCodigoPromocional = findViewById(R.id.etCodigoPromocional)
        btnAplicarCodigo = findViewById(R.id.btnAplicarCodigo)
        tvMensajeCodigo = findViewById(R.id.tvMensajeCodigo)

        tvSubtotal = findViewById(R.id.tvSubtotal)
        rowDescuento = findViewById(R.id.rowDescuento)
        tvDescuento = findViewById(R.id.tvDescuento)
        tvCostoEntrega = findViewById(R.id.tvCostoEntrega)
        tvImpuestos = findViewById(R.id.tvImpuestos)
        tvTotal = findViewById(R.id.tvTotal)
        btnProcederPago = findViewById(R.id.btnProcederPago)

        navInicio = findViewById(R.id.navInicio)
        navMenu = findViewById(R.id.navMenu)
        navCarrito = findViewById(R.id.navCarrito)
        navPedidos = findViewById(R.id.navPedidos)
        navPerfil = findViewById(R.id.navPerfil)
    }

    private fun configurarAcciones() {
        rowDireccion.setOnClickListener {
            startActivity(Intent(this, DireccionEntregaActivity::class.java))
        }
        btnProcederPago.setOnClickListener { intentarProcederAlPago() }
        btnIrAlMenu.setOnClickListener { irAMenu() }
        ivCart.setOnClickListener { /* ya estamos en el Carrito */ }
    }

    // Sin dirección guardada no se puede pasar al pago. Se vuelve a
    // consultar Firestore en el momento del clic (en vez de confiar en lo
    // que ya se cargó), así no hay forma de que un dato viejo o que aún no
    // llegó deje pasar a alguien sin dirección.
    private fun intentarProcederAlPago() {
        DireccionRepository.obtenerDireccion { direccion ->
            // Si saliste de la pantalla mientras Firestore contestaba, no
            // hay nada que mostrar (abrir un aviso aquí podría fallar).
            if (isFinishing || isDestroyed) return@obtenerDireccion

            if (direccion == null) {
                mostrarAvisoSinDireccion()
            } else {
                irAPago()
            }
        }
    }

    // Le pasa a la pantalla de Pago el código promocional que esté aplicado
    // (si hay uno) para que ahí el resumen muestre exactamente el mismo
    // descuento y total que ves aquí.
    private fun irAPago() {
        val intent = Intent(this, PagoActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        codigoPromoAplicado?.let { codigo ->
            intent.putExtra(PagoActivity.EXTRA_CODIGO, codigo.codigo)
            intent.putExtra(PagoActivity.EXTRA_TIPO_DESCUENTO, codigo.tipoDescuento)
            intent.putExtra(PagoActivity.EXTRA_VALOR_DESCUENTO, codigo.valor)
        }
        startActivity(intent)
    }

    private fun mostrarAvisoSinDireccion() {
        AlertDialog.Builder(this)
            .setTitle("Falta tu dirección")
            .setMessage("Para continuar con el pago, primero pon tu dirección de entrega. Así confirmamos que llegamos a tu zona.")
            .setPositiveButton("Poner dirección") { _, _ ->
                startActivity(Intent(this, DireccionEntregaActivity::class.java))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Busca el código tal cual en Firestore. Si el documento existe, lo
    // aplica; si no existe (o ya lo borraste desde Firestore Console),
    // muestra "Código no válido".
    private fun configurarCodigoPromocional() {
        btnAplicarCodigo.setOnClickListener {
            val texto = etCodigoPromocional.text.toString()
            if (texto.isBlank()) {
                mostrarMensajeCodigo("Escribe un código", esValido = false)
                return@setOnClickListener
            }

            CodigoPromoRepository.validarCodigo(texto) { codigo ->
                if (codigo != null) {
                    codigoPromoAplicado = codigo
                    val textoDescuento = if (codigo.tipoDescuento == "porcentaje") {
                        "${codigo.valor.toInt()}% de descuento"
                    } else {
                        "${formatoPrecio(codigo.valor)} de descuento"
                    }
                    mostrarMensajeCodigo("✓ Código ${codigo.codigo} aplicado: $textoDescuento", esValido = true)
                } else {
                    codigoPromoAplicado = null
                    mostrarMensajeCodigo("Código no válido", esValido = false)
                }
                actualizarResumen()
            }
        }
    }

    private fun mostrarMensajeCodigo(texto: String, esValido: Boolean) {
        tvMensajeCodigo.text = texto
        tvMensajeCodigo.setTextColor(getColor(if (esValido) R.color.fogonRed else R.color.fogonGrayText))
        tvMensajeCodigo.visibility = View.VISIBLE
    }

    private fun irAMenu() {
        val intent = Intent(this, MenuActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    // Reconstruye toda la lista del carrito desde CarritoRepository. Se
    // llama en onResume porque el carrito pudo haber cambiado en otra
    // pantalla (agregar desde Home, Menú o el Detalle).
    private fun actualizarListaCarrito() {
        val items = CarritoRepository.obtenerItems()
        contenedorItems.removeAllViews()

        if (items.isEmpty()) {
            layoutCarritoVacio.visibility = View.VISIBLE
            layoutCarritoConItems.visibility = View.GONE
            btnProcederPago.visibility = View.GONE
            return
        }

        layoutCarritoVacio.visibility = View.GONE
        layoutCarritoConItems.visibility = View.VISIBLE
        btnProcederPago.visibility = View.VISIBLE

        for (item in items) {
            contenedorItems.addView(crearFilaCarrito(item))
        }

        actualizarResumen()
    }

    private fun crearFilaCarrito(item: ItemCarrito): View {
        val fila = LayoutInflater.from(this).inflate(R.layout.item_carrito, contenedorItems, false)
        val ivImagen = fila.findViewById<ImageView>(R.id.ivImagenCarrito)
        val tvNombre = fila.findViewById<TextView>(R.id.tvNombreCarrito)
        val tvPrecio = fila.findViewById<TextView>(R.id.tvPrecioCarrito)
        val tvTamano = fila.findViewById<TextView>(R.id.tvTamanoCarrito)
        val tvExtras = fila.findViewById<TextView>(R.id.tvExtrasCarrito)
        val tvCantidad = fila.findViewById<TextView>(R.id.tvCantidadCarrito)
        val btnMenos = fila.findViewById<TextView>(R.id.btnMenosCarrito)
        val btnMas = fila.findViewById<TextView>(R.id.btnMasCarrito)
        val btnEliminar = fila.findViewById<TextView>(R.id.btnEliminarCarrito)

        ivImagen.setImageResource(item.imagenResId())
        tvNombre.text = item.nombreProducto
        tvPrecio.text = formatoPrecio(item.subtotalLinea())
        tvTamano.text = item.nombreTamano
        tvExtras.text = if (item.extrasSeleccionados.isEmpty()) {
            "Sin extras"
        } else {
            "Extras: " + item.extrasSeleccionados.joinToString(", ") { it.nombre }
        }
        tvCantidad.text = item.cantidad.toString()

        btnMenos.setOnClickListener {
            CarritoRepository.actualizarCantidad(item.idDocumento, item.cantidad - 1)
            actualizarBadgeCarrito()
            actualizarListaCarrito()
        }
        btnMas.setOnClickListener {
            CarritoRepository.actualizarCantidad(item.idDocumento, item.cantidad + 1)
            actualizarBadgeCarrito()
            actualizarListaCarrito()
        }
        btnEliminar.setOnClickListener {
            CarritoRepository.quitarItem(item.idDocumento)
            actualizarBadgeCarrito()
            actualizarListaCarrito()
        }

        return fila
    }

    private fun actualizarResumen() {
        val resumen = ResumenPedido.calcular(CarritoRepository.calcularSubtotal(), codigoPromoAplicado)

        tvSubtotal.text = formatoPrecio(resumen.subtotal)
        if (resumen.descuento > 0) {
            rowDescuento.visibility = View.VISIBLE
            tvDescuento.text = "-" + formatoPrecio(resumen.descuento)
        } else {
            rowDescuento.visibility = View.GONE
        }
        tvCostoEntrega.text = formatoPrecio(resumen.entrega)
        tvImpuestos.text = formatoPrecio(resumen.impuestos)
        tvTotal.text = formatoPrecio(resumen.total)
    }

    private fun formatoPrecio(precio: Double): String = String.format(Locale.US, "$%.2f", precio)

    private fun actualizarBadgeCarrito() {
        val total = CarritoRepository.obtenerTotalItems()
        if (total > 0) {
            tvCartBadge.visibility = View.VISIBLE
            tvCartBadge.text = total.toString()
        } else {
            tvCartBadge.visibility = View.GONE
        }
    }

    private fun irAInicio() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    private fun irAPedidos() {
        val intent = Intent(this, PedidosActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    private fun configurarBottomNav() {
        navInicio.setOnClickListener { irAInicio() }
        navMenu.setOnClickListener { irAMenu() }
        navCarrito.setOnClickListener { /* ya estamos en Carrito */ }

        navPedidos.setOnClickListener { irAPedidos() }

        navPerfil.setOnClickListener {
            Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun configurarBotonAtras() {
        ivBack.setOnClickListener { finish() }
    }
}