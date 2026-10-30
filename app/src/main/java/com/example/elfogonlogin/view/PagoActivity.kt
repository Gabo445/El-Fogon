package com.example.elfogonlogin.view

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.elfogonlogin.R
import com.example.elfogonlogin.controller.CarritoRepository
import com.example.elfogonlogin.controller.CodigoPromo
import com.example.elfogonlogin.controller.DireccionRepository
import com.example.elfogonlogin.controller.PedidoRepository
import com.example.elfogonlogin.controller.PerfilRepository
import com.example.elfogonlogin.model.ResumenPedido
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import java.util.Calendar
import java.util.Locale
import kotlin.collections.iterator

// Las 4 formas de pago que muestra el diseño.
enum class MetodoPago(val nombre: String) {
    TARJETA("Tarjeta de crédito / débito"),
    EFECTIVO("Pago en efectivo"),
    TIGO_MONEY("Tigo Money"),
    CHIVO_WALLET("Chivo Wallet")
}

// Pantalla de Pago (paso 3 de 4: Dirección → Entrega → Pago → Confirmar).
// Se abre desde el Carrito, que ya comprobó que hay dirección guardada.
// Al elegir un método se abre su formulario aquí mismo, y "Confirmar pago"
// valida los datos, guarda el pedido en Firestore y lo confirma sin salir de
// esta pantalla. Al terminar te lleva a la pestaña Pedidos.
class PagoActivity : AppCompatActivity() {

    companion object {
        // El Carrito manda aquí el código promocional que tenía aplicado.
        const val EXTRA_CODIGO = "codigoPromo"
        const val EXTRA_TIPO_DESCUENTO = "tipoDescuentoPromo"
        const val EXTRA_VALOR_DESCUENTO = "valorDescuentoPromo"
    }

    // Todo lo que le pertenece a un método de pago.
    private class FilaMetodo(
        val tarjeta: View,       // el recuadro completo (para cambiarle el fondo)
        val encabezado: View,    // la parte de arriba, que se toca para elegir el método
        val radio: RadioButton,
        val formulario: View?    // los campos que se abren al elegirlo (efectivo no tiene)
    )

    private lateinit var ivBack: View
    private lateinit var ivCart: FrameLayout
    private lateinit var tvCartBadge: TextView
    private lateinit var contenedorContenido: ViewGroup

    private lateinit var rowTarjeta: View
    private lateinit var rowEfectivo: View
    private lateinit var rowTigoMoney: View
    private lateinit var rowChivoWallet: View
    private lateinit var headerTarjeta: View
    private lateinit var headerEfectivo: View
    private lateinit var headerTigoMoney: View
    private lateinit var headerChivoWallet: View
    private lateinit var rbTarjeta: RadioButton
    private lateinit var rbEfectivo: RadioButton
    private lateinit var rbTigoMoney: RadioButton
    private lateinit var rbChivoWallet: RadioButton

    private lateinit var formTarjeta: View
    private lateinit var formTigoMoney: View
    private lateinit var formChivoWallet: View

    private lateinit var etNumeroTarjeta: EditText
    private lateinit var etNombreTitular: EditText
    private lateinit var etVencimiento: EditText
    private lateinit var etCvv: EditText
    private lateinit var tvErrorTarjeta: TextView
    private lateinit var etCelularTigo: EditText
    private lateinit var tvErrorTigo: TextView
    private lateinit var etCelularChivo: EditText
    private lateinit var tvErrorChivo: TextView

    private lateinit var tvSubtotal: TextView
    private lateinit var rowDescuento: View
    private lateinit var tvEtiquetaDescuento: TextView
    private lateinit var tvDescuento: TextView
    private lateinit var tvCostoEntrega: TextView
    private lateinit var tvImpuestos: TextView
    private lateinit var tvTotalPagar: TextView

    private lateinit var btnUsarPuntos: View
    private lateinit var btnConfirmarPago: View
    private lateinit var tvTotalBoton: TextView

    private lateinit var navInicio: View
    private lateinit var navMenu: View
    private lateinit var navCarrito: View
    private lateinit var navPedidos: View
    private lateinit var navPerfil: View

    private var codigoPromo: CodigoPromo? = null
    private var metodoSeleccionado = MetodoPago.TARJETA

    // Se pone en true cuando el pedido ya se confirmó. Sirve para que, al
    // vaciar el carrito a propósito, la pantalla no crea que "se quedó sin
    // productos" y muestre el aviso de carrito vacío.
    private var pedidoConfirmado = false

    // Mientras se está guardando el pedido, el botón se bloquea para que un
    // doble toque no cree dos pedidos.
    private var guardandoPedido = false

    // Se pone en true cuando la persona toca un método de pago. Sirve para que el
    // método preferido del Perfil (que llega un instante después) no le cambie
    // lo que ya eligió.
    private var metodoEligidoPorUsuario = false

    // Los RadioButton no están en un RadioGroup porque cada uno vive dentro
    // de un recuadro distinto, así que la selección de "solo uno a la vez"
    // la manejamos nosotros aquí.
    private val filasMetodos = mutableMapOf<MetodoPago, FilaMetodo>()

    private var listenerCarrito: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_pago)

        ajustarPorTeclado()
        inicializarVistas()
        leerCodigoPromo()
        configurarFormatosDeCampos()
        configurarMetodosDePago()
        aplicarMetodoPreferido()
        configurarAcciones()
        configurarBottomNav()

        // El Carrito ya dejó los productos en memoria, así que los números
        // salen de una vez, sin esperar a que Firestore conteste.
        actualizarBadgeCarrito()
        actualizarResumen()
    }

    override fun onResume() {
        super.onResume()
        listenerCarrito = CarritoRepository.escucharCambios { items ->
            if (!pedidoConfirmado) {
                if (items.isEmpty()) {
                    // Sin productos no hay nada que pagar: regresamos al Carrito.
                    Toast.makeText(this, "Tu carrito está vacío", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    actualizarBadgeCarrito()
                    actualizarResumen()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        listenerCarrito?.remove()
    }

    // Igual que en el Carrito: cuando sale el teclado, se sube todo para que
    // no tape los campos que estás llenando.
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
        contenedorContenido = findViewById(R.id.contenedorContenido)

        rowTarjeta = findViewById(R.id.rowMetodoTarjeta)
        rowEfectivo = findViewById(R.id.rowMetodoEfectivo)
        rowTigoMoney = findViewById(R.id.rowMetodoTigoMoney)
        rowChivoWallet = findViewById(R.id.rowMetodoChivoWallet)
        headerTarjeta = findViewById(R.id.headerMetodoTarjeta)
        headerEfectivo = findViewById(R.id.headerMetodoEfectivo)
        headerTigoMoney = findViewById(R.id.headerMetodoTigoMoney)
        headerChivoWallet = findViewById(R.id.headerMetodoChivoWallet)
        rbTarjeta = findViewById(R.id.rbMetodoTarjeta)
        rbEfectivo = findViewById(R.id.rbMetodoEfectivo)
        rbTigoMoney = findViewById(R.id.rbMetodoTigoMoney)
        rbChivoWallet = findViewById(R.id.rbMetodoChivoWallet)

        formTarjeta = findViewById(R.id.formTarjeta)
        formTigoMoney = findViewById(R.id.formTigoMoney)
        formChivoWallet = findViewById(R.id.formChivoWallet)

        etNumeroTarjeta = findViewById(R.id.etNumeroTarjeta)
        etNombreTitular = findViewById(R.id.etNombreTitular)
        etVencimiento = findViewById(R.id.etVencimiento)
        etCvv = findViewById(R.id.etCvv)
        tvErrorTarjeta = findViewById(R.id.tvErrorTarjeta)
        etCelularTigo = findViewById(R.id.etCelularTigo)
        tvErrorTigo = findViewById(R.id.tvErrorTigo)
        etCelularChivo = findViewById(R.id.etCelularChivo)
        tvErrorChivo = findViewById(R.id.tvErrorChivo)

        tvSubtotal = findViewById(R.id.tvSubtotal)
        rowDescuento = findViewById(R.id.rowDescuento)
        tvEtiquetaDescuento = findViewById(R.id.tvEtiquetaDescuento)
        tvDescuento = findViewById(R.id.tvDescuento)
        tvCostoEntrega = findViewById(R.id.tvCostoEntrega)
        tvImpuestos = findViewById(R.id.tvImpuestos)
        tvTotalPagar = findViewById(R.id.tvTotalPagar)

        btnUsarPuntos = findViewById(R.id.btnUsarPuntos)
        btnConfirmarPago = findViewById(R.id.btnConfirmarPago)
        tvTotalBoton = findViewById(R.id.tvTotalBoton)

        navInicio = findViewById(R.id.navInicio)
        navMenu = findViewById(R.id.navMenu)
        navCarrito = findViewById(R.id.navCarrito)
        navPedidos = findViewById(R.id.navPedidos)
        navPerfil = findViewById(R.id.navPerfil)
    }

    // Reconstruye el código promocional con lo que mandó el Carrito. Si no
    // mandó nada (no había código aplicado), el pedido va sin descuento.
    private fun leerCodigoPromo() {
        val codigo = intent.getStringExtra(EXTRA_CODIGO)
        val tipo = intent.getStringExtra(EXTRA_TIPO_DESCUENTO)
        codigoPromo = if (codigo != null && tipo != null) {
            CodigoPromo(codigo, tipo, intent.getDoubleExtra(EXTRA_VALOR_DESCUENTO, 0.0))
        } else {
            null
        }
    }

    // ---------------------------------------------------------------------
    // Formato mientras escribes
    // ---------------------------------------------------------------------

    private fun configurarFormatosDeCampos() {
        // Tarjeta: 1234 5678 9012 3456
        aplicarFormato(etNumeroTarjeta, 16) { digitos -> digitos.chunked(4).joinToString(" ") }

        // Vencimiento: MM/AA
        aplicarFormato(etVencimiento, 4) { digitos ->
            if (digitos.length > 2) digitos.substring(0, 2) + "/" + digitos.substring(2) else digitos
        }

        // Celulares de El Salvador: 7123 4567
        aplicarFormato(etCelularTigo, 8) { digitos -> digitos.chunked(4).joinToString(" ") }
        aplicarFormato(etCelularChivo, 8) { digitos -> digitos.chunked(4).joinToString(" ") }
    }

    // Deja solo los números que escribes (hasta maxDigitos) y los vuelve a
    // acomodar con el formato que pida cada campo (espacios, la diagonal...).
    private fun aplicarFormato(campo: EditText, maxDigitos: Int, formato: (String) -> String) {
        campo.addTextChangedListener(object : TextWatcher {
            private var editando = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (editando || s == null) return
                editando = true
                val digitos = s.toString().filter { it.isDigit() }.take(maxDigitos)
                val textoFormateado = formato(digitos)
                if (textoFormateado != s.toString()) {
                    s.replace(0, s.length, textoFormateado)
                }
                editando = false
            }
        })
    }

    private fun soloDigitos(campo: EditText): String =
        campo.text.toString().filter { it.isDigit() }

    // ---------------------------------------------------------------------
    // Método de pago: elegir y abrir su formulario
    // ---------------------------------------------------------------------

    private fun configurarMetodosDePago() {
        filasMetodos[MetodoPago.TARJETA] = FilaMetodo(rowTarjeta, headerTarjeta, rbTarjeta, formTarjeta)
        filasMetodos[MetodoPago.EFECTIVO] = FilaMetodo(rowEfectivo, headerEfectivo, rbEfectivo, null)
        filasMetodos[MetodoPago.TIGO_MONEY] = FilaMetodo(rowTigoMoney, headerTigoMoney, rbTigoMoney, formTigoMoney)
        filasMetodos[MetodoPago.CHIVO_WALLET] = FilaMetodo(rowChivoWallet, headerChivoWallet, rbChivoWallet, formChivoWallet)

        for ((metodo, fila) in filasMetodos) {
            fila.encabezado.setOnClickListener {
                metodoEligidoPorUsuario = true
                seleccionarMetodo(metodo)
            }
        }

        seleccionarMetodo(metodoSeleccionado, animar = false)
    }

    // Si en el Perfil elegiste cómo vas a pagar, aquí te aparece ya marcado. Si no
    // has elegido ninguno, se queda la tarjeta como siempre. Puedes cambiarlo aquí.
    private fun aplicarMetodoPreferido() {
        PerfilRepository.obtenerPerfil { perfil ->
            val preferido = perfil.metodoPagoPreferido
            if (preferido != null && !metodoEligidoPorUsuario && pantallaActiva()) {
                seleccionarMetodo(preferido, animar = false)
            }
        }
    }

    // Marca el método elegido (borde rojo + fondo rosado + radio lleno), abre
    // su formulario y cierra los de los demás.
    private fun seleccionarMetodo(metodo: MetodoPago, animar: Boolean = true) {
        // Si tocas el que ya estaba elegido no hay nada que cambiar.
        if (animar && metodo == metodoSeleccionado) return

        if (animar) {
            ocultarTeclado()
            TransitionManager.beginDelayedTransition(contenedorContenido)
        }

        metodoSeleccionado = metodo
        ocultarErroresDeFormulario()

        for ((m, fila) in filasMetodos) {
            val elegido = m == metodo
            fila.radio.isChecked = elegido
            fila.tarjeta.setBackgroundResource(
                if (elegido) R.drawable.bg_pago_metodo_sel else R.drawable.bg_pago_metodo
            )
            fila.formulario?.visibility = if (elegido) View.VISIBLE else View.GONE
        }
    }

    private fun ocultarTeclado() {
        val vista = currentFocus ?: return
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(vista.windowToken, 0)
    }

    // ---------------------------------------------------------------------
    // Confirmar pago
    // ---------------------------------------------------------------------

    private fun configurarAcciones() {
        ivBack.setOnClickListener { finish() }
        ivCart.setOnClickListener { irACarrito() }

        btnUsarPuntos.setOnClickListener {
            Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show()
        }

        btnConfirmarPago.setOnClickListener { intentarConfirmarPago() }
    }

    // Revisa los datos del método elegido. Si algo está mal, muestra el
    // mensaje debajo del formulario y se queda aquí; si todo está bien,
    // guarda el pedido sin salir de esta pantalla.
    private fun intentarConfirmarPago() {
        if (guardandoPedido) return

        ocultarTeclado()
        ocultarErroresDeFormulario()

        val datosCorrectos = when (metodoSeleccionado) {
            MetodoPago.TARJETA -> validarTarjeta()
            MetodoPago.EFECTIVO -> true // no hay nada que llenar: se paga al recibir
            MetodoPago.TIGO_MONEY -> validarCelular(etCelularTigo, tvErrorTigo)
            MetodoPago.CHIVO_WALLET -> validarCelular(etCelularChivo, tvErrorChivo)
        }

        if (datosCorrectos) guardarPedido()
    }

    // OJO: los datos de la tarjeta solo se revisan aquí y se descartan, no se
    // guardan en ningún lado. Tampoco se usa el algoritmo de Luhn, así puedes
    // probar con cualquier número de 16 dígitos.
    private fun validarTarjeta(): Boolean {
        if (soloDigitos(etNumeroTarjeta).length != 16) {
            return marcarError(tvErrorTarjeta, etNumeroTarjeta, "El número de tarjeta debe tener 16 dígitos")
        }

        if (etNombreTitular.text.toString().trim().length < 3) {
            return marcarError(tvErrorTarjeta, etNombreTitular, "Escribe el nombre como aparece en la tarjeta")
        }

        val vencimiento = soloDigitos(etVencimiento)
        if (vencimiento.length != 4) {
            return marcarError(tvErrorTarjeta, etVencimiento, "Escribe el vencimiento como MM/AA, por ejemplo 08/28")
        }
        val mes = vencimiento.substring(0, 2).toInt()
        val anio = 2000 + vencimiento.substring(2, 4).toInt()
        if (mes !in 1..12) {
            return marcarError(tvErrorTarjeta, etVencimiento, "El mes del vencimiento no es válido (de 01 a 12)")
        }
        val hoy = Calendar.getInstance()
        val anioActual = hoy.get(Calendar.YEAR)
        val mesActual = hoy.get(Calendar.MONTH) + 1
        if (anio < anioActual || (anio == anioActual && mes < mesActual)) {
            return marcarError(tvErrorTarjeta, etVencimiento, "Esta tarjeta ya está vencida")
        }

        if (soloDigitos(etCvv).length != 3) {
            return marcarError(tvErrorTarjeta, etCvv, "El código de seguridad (CVV) tiene 3 dígitos")
        }

        return true
    }

    // Celulares de El Salvador: 8 dígitos, y los móviles empiezan con 6 o 7.
    private fun validarCelular(campo: EditText, tvError: TextView): Boolean {
        val numero = soloDigitos(campo)
        if (numero.length != 8 || (numero[0] != '6' && numero[0] != '7')) {
            return marcarError(tvError, campo, "Escribe tu número de celular de 8 dígitos (empieza con 6 o 7)")
        }
        return true
    }

    // Muestra el mensaje de error, deja el cursor en el campo con el
    // problema y devuelve false para que quien llamó sepa que algo falló.
    private fun marcarError(tvError: TextView, campo: EditText, mensaje: String): Boolean {
        tvError.text = mensaje
        tvError.visibility = View.VISIBLE
        campo.requestFocus()
        return false
    }

    private fun ocultarErroresDeFormulario() {
        tvErrorTarjeta.visibility = View.GONE
        tvErrorTigo.visibility = View.GONE
        tvErrorChivo.visibility = View.GONE
    }

    // El orden aquí es lo más importante:
    //   1) se leen tu nombre y teléfono, y se comprueba que haya dirección,
    //   2) se GUARDA el pedido en Firestore,
    //   3) solo si se guardó bien, se vacía el carrito,
    //   4) sale el aviso de "Pedido confirmado".
    // Si algo falla en el paso 1 o 2, el carrito no se toca y no se pierde nada.
    private fun guardarPedido() {
        // Una "fotografía" de lo que hay ahora, porque el carrito se vaciará.
        val items = CarritoRepository.obtenerItems().toList()
        if (items.isEmpty()) {
            Toast.makeText(this, "Tu carrito está vacío", Toast.LENGTH_SHORT).show()
            return
        }

        val resumen = ResumenPedido.calcular(CarritoRepository.calcularSubtotal(), codigoPromo)
        val codigo = codigoPromo
        val metodo = metodoSeleccionado
        val detalle = detallePago()
        val textoMetodo = textoMetodoParaAviso()

        bloquearBotonConfirmar(true)

        // Primero se leen el nombre y el teléfono del cliente para guardarlos
        // dentro del pedido: así el restaurante sabe quién pidió y a qué número
        // llamar, aunque después el cliente cambie su perfil.
        PerfilRepository.obtenerPerfil { perfil ->
            DireccionRepository.obtenerDireccion { direccion ->
                if (direccion == null) {
                    if (pantallaActiva()) {
                        bloquearBotonConfirmar(false)
                        mostrarAvisoSinDireccion()
                    }
                    return@obtenerDireccion
                }

                PedidoRepository.crearPedido(
                    items, resumen, codigo, metodo, detalle, direccion,
                    perfil.nombre, perfil.telefono
                ) { numero, error ->
                    if (numero != null) {
                        // Ya está guardado: ahora sí se puede vaciar el carrito. Se marca
                        // primero, para que la pantalla no crea que "se quedó sin productos".
                        pedidoConfirmado = true
                        CarritoRepository.vaciar()
                        if (pantallaActiva()) mostrarPedidoConfirmado(numero, resumen.total, textoMetodo)
                    } else if (pantallaActiva()) {
                        bloquearBotonConfirmar(false)
                        mostrarErrorAlGuardar(error)
                    }
                }
            }
        }
    }

    private fun pantallaActiva(): Boolean = !isFinishing && !isDestroyed

    private fun bloquearBotonConfirmar(bloquear: Boolean) {
        guardandoPedido = bloquear
        btnConfirmarPago.isEnabled = !bloquear
        btnConfirmarPago.alpha = if (bloquear) 0.6f else 1f
    }

    // Lo que se guarda en el pedido sobre el pago. OJO: del número de tarjeta
    // solo se guardan los últimos 4 dígitos; el número completo, el nombre del
    // titular, el vencimiento y el CVV NUNCA se guardan.
    private fun detallePago(): String = when (metodoSeleccionado) {
        MetodoPago.TARJETA -> "Tarjeta terminada en ${soloDigitos(etNumeroTarjeta).takeLast(4)}"
        MetodoPago.EFECTIVO -> "Pagas al recibir tu pedido"
        MetodoPago.TIGO_MONEY -> "Celular ${etCelularTigo.text}"
        MetodoPago.CHIVO_WALLET -> "Celular ${etCelularChivo.text}"
    }

    private fun textoMetodoParaAviso(): String = when (metodoSeleccionado) {
        MetodoPago.TARJETA -> "tarjeta terminada en ${soloDigitos(etNumeroTarjeta).takeLast(4)}"
        MetodoPago.EFECTIVO -> "efectivo (pagas al recibir tu pedido)"
        MetodoPago.TIGO_MONEY -> "Tigo Money (${etCelularTigo.text})"
        MetodoPago.CHIVO_WALLET -> "Chivo Wallet (${etCelularChivo.text})"
    }

    // La confirmación sale aquí mismo, como un aviso sobre esta pantalla.
    // OJO: por ahora no se cobra nada de verdad; el pedido queda guardado
    // como "pago de prueba". El cobro real llega más adelante.
    private fun mostrarPedidoConfirmado(numero: Long, total: Double, textoMetodo: String) {
        AlertDialog.Builder(this)
            .setTitle("¡Pedido enviado! 🎉")
            .setMessage(
                "Pedido ${PedidoRepository.formatoNumero(numero)}\n" +
                        "Método de pago: $textoMetodo\n" +
                        "Total: ${formatoPrecio(total)}\n\n" +
                        "El Fogón va a confirmar tu pedido en un momento. " +
                        "Puedes seguirlo en la pestaña Pedidos."
            )
            .setCancelable(false)
            .setPositiveButton("Listo") { _, _ -> terminarPedido() }
            .show()
    }

    private fun mostrarErrorAlGuardar(error: String?) {
        AlertDialog.Builder(this)
            .setTitle("No se pudo guardar tu pedido")
            .setMessage(
                "Tu carrito sigue igual, no se perdió nada. " +
                        "Revisa tu internet e inténtalo de nuevo.\n\n" +
                        "Detalle técnico: ${error ?: "desconocido"}"
            )
            .setPositiveButton("Entendido", null)
            .show()
    }

    // Igual que en el Carrito: sin dirección guardada no se puede confirmar.
    private fun mostrarAvisoSinDireccion() {
        AlertDialog.Builder(this)
            .setTitle("Falta tu dirección")
            .setMessage("Para confirmar tu pedido, primero pon tu dirección de entrega. Así confirmamos que llegamos a tu zona.")
            .setPositiveButton("Poner dirección") { _, _ ->
                startActivity(Intent(this, DireccionEntregaActivity::class.java))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Te lleva a la pestaña Pedidos, donde ya aparece tu pedido arriba de todo.
    // Se abre Inicio primero y Pedidos encima, para que al darle "atrás" desde
    // Pedidos regreses al Inicio (y no al Carrito, que ya quedó vacío).
    private fun terminarPedido() {
        val inicio = Intent(this, HomeActivity::class.java)
        inicio.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pedidos = Intent(this, PedidosActivity::class.java)
        startActivities(arrayOf(inicio, pedidos))
        finish()
    }

    // ---------------------------------------------------------------------
    // Resumen, badge y navegación
    // ---------------------------------------------------------------------

    private fun actualizarResumen() {
        val resumen = ResumenPedido.calcular(CarritoRepository.calcularSubtotal(), codigoPromo)

        tvSubtotal.text = formatoPrecio(resumen.subtotal)
        if (resumen.descuento > 0) {
            rowDescuento.visibility = View.VISIBLE
            tvEtiquetaDescuento.text = "Descuento (${codigoPromo?.codigo})"
            tvDescuento.text = "-" + formatoPrecio(resumen.descuento)
        } else {
            rowDescuento.visibility = View.GONE
        }
        tvCostoEntrega.text = formatoPrecio(resumen.entrega)
        tvImpuestos.text = formatoPrecio(resumen.impuestos)
        tvTotalPagar.text = formatoPrecio(resumen.total)
        tvTotalBoton.text = formatoPrecio(resumen.total)
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

    private fun irAMenu() {
        val intent = Intent(this, MenuActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    private fun irAPedidos() {
        val intent = Intent(this, PedidosActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    private fun irAPerfil() {
        val intent = Intent(this, PerfilActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    // Regresa al Carrito que quedó abierto debajo de esta pantalla (así el
    // código promocional que ya habías aplicado sigue ahí).
    private fun irACarrito() {
        val intent = Intent(this, CarritoActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    private fun configurarBottomNav() {
        navInicio.setOnClickListener { irAInicio() }
        navMenu.setOnClickListener { irAMenu() }
        navCarrito.setOnClickListener { irACarrito() }
        navPedidos.setOnClickListener { irAPedidos() }

        navPerfil.setOnClickListener { irAPerfil() }
    }
}