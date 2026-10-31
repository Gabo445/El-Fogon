Anderson Ruben Portillo Alfaro PA250105 100%
Dinana Guadalupe Rivera Navas RN250337 100%
Nahum de Jesus Flores Giron FG250084 100%
Ana Ruth Lopez Lima LL250088 100%


# 🔥 El Fogón

Aplicación Android de pedidos de comida a domicilio para El Salvador. Los clientes piden desde el celular y el administrador recibe y gestiona los pedidos en un panel de control en tiempo real.

Hecha en **Kotlin** con **Firebase** (Authentication y Cloud Firestore). No usa servidor propio ni plan de pago.

---

## 🔑 Credenciales de prueba

### Usuario administrador

Estas son las credenciales del usuario administrador (entra directo al **Panel de control**):

- **Correo:** `aportilloalfaro@gmail.com`
- **Contraseña:** `Messiento12345`

### Usuario cliente

No hay una cuenta de cliente fija. Usted puede crear la suya ahora mismo: en la pantalla de inicio de sesión toque **Registrate**, llene sus datos y listo. También puede entrar con **Continuar con Google**.

---

##  Qué hace la app

### Para el cliente
- Registro e inicio de sesión con correo y contraseña, o con Google.
- Menú con **66 productos** en 7 categorías: Hamburguesas, Pizzas, Tacos, Burritos, Tortas, Postres y Bebidas, con buscador.
- Detalle del producto con tamaños y extras para personalizar el pedido.
- Favoritos, guardados en la cuenta (siguen ahí aunque cambie de celular).
- Carrito con **código promocional** (por ejemplo, `BIENVENIDO10`).
- **Dirección de entrega con GPS** y zonas de cobertura. Puede guardar varias direcciones.
- Pago con Tarjeta, Efectivo, Tigo Money o Chivo Wallet (los pagos son de prueba: no se cobra dinero real).
- Total calculado con costo de entrega de $2.00 e IVA del 13%.
- **Seguimiento del pedido en tiempo real**: pendiente → recibido → preparando → en camino → entregado, con la hora de cada paso.
- Perfil con foto, teléfono, método de pago preferido y notificaciones.
- Privacidad (cambiar contraseña y eliminar la cuenta) y sección de Ayuda.

### Para el administrador
- Su cuenta entra directo al **Panel de control**.
- Ve **todos los pedidos en tiempo real**, en pestañas: Nuevos, En curso y Entregados.
- Ve el nombre, la foto y el teléfono del cliente (esto solo lo ve el administrador), con botones para **Llamar**, abrir **WhatsApp** y **Cómo llegar** en Google Maps.
- Un botón grande avanza el pedido al siguiente estado y el cliente lo ve al instante.
- Suena y vibra cuando llega un pedido nuevo, y la pantalla no se apaga mientras el panel está abierto.

> El sonido y la vibración funcionan solo con el panel abierto. Con la app cerrada no llegan alertas, porque eso requiere notificaciones push (plan de pago de Firebase).

---

##  Tecnologías

- Kotlin y Android Views (XML)
- Firebase Authentication y Cloud Firestore
- Credential Manager + Google ID (inicio de sesión con Google)
- Kotlin Coroutines
- ViewPager2 (banner de promociones)
- Google Play Services Location (GPS)
- minSdk 24 · targetSdk 36

---

##  Estructura del proyecto (MVC)

```
app/src/main/java/com/example/elfogonlogin/
├── model/          Datos de la app
│   ├── Producto.kt
│   └── ResumenPedido.kt
├── controller/     Lógica y conexión con Firebase
│   ├── AdminRepository.kt
│   ├── CarritoRepository.kt
│   ├── CodigoPromoRepository.kt
│   ├── CuentaRepository.kt
│   ├── DetalleProductoRepository.kt
│   ├── DireccionRepository.kt
│   ├── FavoritosRepository.kt
│   ├── FirebaseErrorHelper.kt
│   ├── FotoPerfil.kt
│   ├── PedidoRepository.kt
│   ├── PerfilRepository.kt
│   └── ProductosRepository.kt
└── view/           Pantallas
    ├── MainActivity.kt                (inicio de sesión)
    ├── RegisterActivity.kt
    ├── HomeActivity.kt
    ├── MenuActivity.kt
    ├── DetalleProductoActivity.kt
    ├── CarritoActivity.kt
    ├── DireccionEntregaActivity.kt
    ├── PagoActivity.kt
    ├── PedidosActivity.kt
    ├── PerfilActivity.kt
    ├── MisDatosActivity.kt
    ├── DireccionesGuardadasActivity.kt
    ├── FavoritosActivity.kt
    ├── AyudaActivity.kt
    ├── PrivacidadActivity.kt
    ├── PanelControlActivity.kt        (administrador)
    └── PromoBannerAdapter.kt
```

Los diseños (XML) están en `app/src/main/res/layout`.

---

##  Datos en Firestore

| Colección | Qué guarda |
|---|---|
| `usuarios/{uid}` | Perfil, foto, favoritos y dirección de cada cliente (solo su dueño lo lee) |
| `pedidos` | Todos los pedidos, con su estado y la hora de cada paso |
| `codigosPromocionales/{CODIGO}` | Códigos de descuento (`tipoDescuento`: `porcentaje` o `fijo`, y `valor`) |
| `contadores/pedidos` | Lleva el número del siguiente pedido |
| `admins/{uid}` | Si existe el documento con el UID de una cuenta, esa cuenta es administrador |

### Cómo crear otro administrador
1. En Firebase Authentication, cree la cuenta (o regístrela desde la app).
2. Copie el **UID** de esa cuenta.
3. En Firestore, cree la colección `admins`, con un documento cuyo **ID sea ese UID** y un campo `nombre` (texto).

---

##  Cómo ejecutar el proyecto

1. Clone el repositorio y ábralo con **Android Studio**.
2. Coloque su archivo `google-services.json` (de su proyecto de Firebase) dentro de la carpeta `app/`.
3. En Firebase active **Authentication** (correo/contraseña y Google) y **Cloud Firestore**.
4. Presione **Sync Project with Gradle Files** y luego **Run ▶**.

---

##  Notas

- Los pagos son simulados: la app no cobra dinero real.
- Las horas se muestran siempre en horario de El Salvador.
