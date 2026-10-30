package com.example.elfogonlogin.controller

import com.example.elfogonlogin.R

// Una opción de tamaño o de extra: nombre + precio.
// En tamaños el precio es el precio TOTAL de ese tamaño (no se suma al base).
// En extras el precio SÍ se suma al total si el extra está marcado.
data class OpcionPersonalizacion(val nombre: String, val precio: Double)

data class DetalleProducto(
    val id: String,
    val nombre: String,
    val rating: Double,
    val numOpiniones: Int,
    val descripcion: String,
    val imagenResId: Int,
    val tamanos: List<OpcionPersonalizacion>,
    val extras: List<OpcionPersonalizacion>,
    val extrasMarcadosPorDefecto: Set<String> = emptySet()
)

object DetalleProductoRepository {

    private val productos = mapOf(
        "pizza" to DetalleProducto(
            id = "pizza",
            nombre = "Pizza Pepperoni",
            rating = 4.8,
            numOpiniones = 95,
            descripcion = "Salsa de tomate casera, mozzarella derretida y pepperoni en cada rebanada, horneada al momento en piedra.",
            imagenResId = R.drawable.producto_pizza,
            tamanos = listOf(
                OpcionPersonalizacion("Personal", 12.00),
                OpcionPersonalizacion("Mediana", 15.00),
                OpcionPersonalizacion("Familiar", 18.00)
            ),
            extras = listOf(
                OpcionPersonalizacion("Extra queso", 1.50),
                OpcionPersonalizacion("Orilla rellena", 2.00),
                OpcionPersonalizacion("Aceitunas", 1.00),
                OpcionPersonalizacion("Champiñones", 1.25)
            ),
            extrasMarcadosPorDefecto = setOf("Extra queso")
        ),

        "hamburguesa" to DetalleProducto(
            id = "hamburguesa",
            nombre = "Hamburguesa Clásica",
            rating = 4.7,
            numOpiniones = 120,
            descripcion = "Carne de res jugosa, lechuga fresca, tomate, cebolla, queso amarillo y nuestras salsas especiales en pan artesanal.",
            imagenResId = R.drawable.producto_hamburguesa,
            tamanos = listOf(
                OpcionPersonalizacion("Tradicional", 8.50),
                OpcionPersonalizacion("Doble carne", 10.50),
                OpcionPersonalizacion("Mega", 12.50)
            ),
            extras = listOf(
                OpcionPersonalizacion("Queso extra", 1.00),
                OpcionPersonalizacion("Tocino", 1.00),
                OpcionPersonalizacion("Aguacate", 1.25),
                OpcionPersonalizacion("Jalapeños", 0.75)
            ),
            extrasMarcadosPorDefecto = setOf("Queso extra", "Tocino")
        ),

        "tacos" to DetalleProducto(
            id = "tacos",
            nombre = "Tacos de Carne de Res",
            rating = 4.7,
            numOpiniones = 78,
            descripcion = "Carne de res asada a la parrilla, cebolla y cilantro fresco, servidos en tortilla de maíz recién hecha.",
            imagenResId = R.drawable.producto_tacos_res,
            tamanos = listOf(
                OpcionPersonalizacion("3 tacos", 4.50),
                OpcionPersonalizacion("5 tacos", 6.50),
                OpcionPersonalizacion("7 tacos", 8.50)
            ),
            extras = listOf(
                OpcionPersonalizacion("Extra carne", 1.50),
                OpcionPersonalizacion("Queso", 0.75),
                OpcionPersonalizacion("Guacamole", 1.00),
                OpcionPersonalizacion("Salsa extra", 0.50)
            ),
            extrasMarcadosPorDefecto = setOf("Extra carne")
        ),

        "burritos" to DetalleProducto(
            id = "burritos",
            nombre = "Burritos de Res 5 Capas",
            rating = 4.9,
            numOpiniones = 64,
            descripcion = "Carne de res, arroz, frijoles refritos, queso derretido y pico de gallo envueltos en tortilla de harina.",
            imagenResId = R.drawable.producto_burritos_5capas,
            tamanos = listOf(
                OpcionPersonalizacion("Sencillo", 4.50),
                OpcionPersonalizacion("Doble carne", 6.00),
                OpcionPersonalizacion("Especial", 7.50)
            ),
            extras = listOf(
                OpcionPersonalizacion("Extra carne", 1.50),
                OpcionPersonalizacion("Queso extra", 1.00),
                OpcionPersonalizacion("Guacamole", 1.00),
                OpcionPersonalizacion("Crema", 0.50)
            ),
            extrasMarcadosPorDefecto = setOf("Extra carne")
        ),

        "tortas" to DetalleProducto(
            id = "tortas",
            nombre = "Tortas de Birria",
            rating = 4.5,
            numOpiniones = 52,
            descripcion = "Birria de res deshebrada, queso derretido y consomé para remojar, en pan artesanal dorado a la plancha.",
            imagenResId = R.drawable.producto_tortas_birria,
            tamanos = listOf(
                OpcionPersonalizacion("Sencilla", 3.75),
                OpcionPersonalizacion("Doble carne", 5.25),
                OpcionPersonalizacion("Con consomé extra", 4.50)
            ),
            extras = listOf(
                OpcionPersonalizacion("Extra carne", 1.50),
                OpcionPersonalizacion("Consomé extra", 1.00),
                OpcionPersonalizacion("Queso", 0.75),
                OpcionPersonalizacion("Cebolla y cilantro", 0.25)
            ),
            extrasMarcadosPorDefecto = setOf("Extra carne")
        ),

        "tres_leches" to DetalleProducto(
            id = "tres_leches",
            nombre = "Tres Leches",
            rating = 5.0,
            numOpiniones = 110,
            descripcion = "Pastel esponjoso bañado en tres tipos de leche, cubierto con crema chantilly casera.",
            imagenResId = R.drawable.producto_tres_leches,
            tamanos = listOf(
                OpcionPersonalizacion("Rebanada", 3.00),
                OpcionPersonalizacion("Media rebanada", 1.75),
                OpcionPersonalizacion("Pastel completo", 15.00)
            ),
            extras = listOf(
                OpcionPersonalizacion("Extra crema chantilly", 0.75),
                OpcionPersonalizacion("Fresas", 1.00),
                OpcionPersonalizacion("Canela extra", 0.25),
                OpcionPersonalizacion("Nuez picada", 0.75)
            ),
            extrasMarcadosPorDefecto = setOf("Extra crema chantilly")
        ),

        // ---- Las 14 hamburguesas del Menú (categoría Hamburguesas) ----

        "clasica" to DetalleProducto(
            id = "clasica",
            nombre = "Clásica",
            rating = 4.7,
            numOpiniones = 120,
            descripcion = "Carne de res, lechuga, tomate, cebolla, queso amarillo y salsas de la casa.",
            imagenResId = R.drawable.producto_clasica,
            tamanos = listOf(
                OpcionPersonalizacion("Tradicional", 8.50),
                OpcionPersonalizacion("Doble carne", 10.50),
                OpcionPersonalizacion("Mega", 12.50)
            ),
            extras = listOf(
                OpcionPersonalizacion("Queso extra", 1.00),
                OpcionPersonalizacion("Tocino", 1.00),
                OpcionPersonalizacion("Aguacate", 1.25),
                OpcionPersonalizacion("Jalapeños", 0.75)
            ),
            extrasMarcadosPorDefecto = setOf("Queso extra")
        ),

        "bacon_cheese" to DetalleProducto(
            id = "bacon_cheese",
            nombre = "Bacon Cheese",
            rating = 4.8,
            numOpiniones = 96,
            descripcion = "Carne de res, tocino crujiente, queso cheddar derretido y cebolla caramelizada.",
            imagenResId = R.drawable.producto_bacon_cheese,
            tamanos = listOf(
                OpcionPersonalizacion("Tradicional", 9.50),
                OpcionPersonalizacion("Doble carne", 11.50),
                OpcionPersonalizacion("Mega", 13.50)
            ),
            extras = listOf(
                OpcionPersonalizacion("Tocino extra", 1.25),
                OpcionPersonalizacion("Queso extra", 1.00),
                OpcionPersonalizacion("Aguacate", 1.25),
                OpcionPersonalizacion("Cebolla caramelizada extra", 0.75)
            ),
            extrasMarcadosPorDefecto = setOf("Tocino extra")
        ),

        "bbq_ahumada" to DetalleProducto(
            id = "bbq_ahumada",
            nombre = "BBQ Ahumada",
            rating = 4.6,
            numOpiniones = 84,
            descripcion = "Carne de res, salsa BBQ casera, cebolla crispy, queso cheddar y tocino.",
            imagenResId = R.drawable.producto_bbq_ahumada,
            tamanos = listOf(
                OpcionPersonalizacion("Tradicional", 9.75),
                OpcionPersonalizacion("Doble carne", 11.75),
                OpcionPersonalizacion("Mega", 13.75)
            ),
            extras = listOf(
                OpcionPersonalizacion("Salsa BBQ extra", 0.75),
                OpcionPersonalizacion("Tocino", 1.00),
                OpcionPersonalizacion("Aros de cebolla extra", 1.00),
                OpcionPersonalizacion("Queso cheddar extra", 1.00)
            ),
            extrasMarcadosPorDefecto = setOf("Salsa BBQ extra")
        ),

        "doble_carne_queso" to DetalleProducto(
            id = "doble_carne_queso",
            nombre = "Doble Carne y Queso",
            rating = 4.9,
            numOpiniones = 110,
            descripcion = "Doble carne de res, doble queso americano y salsa especial de la casa.",
            imagenResId = R.drawable.producto_doble_carne_queso,
            tamanos = listOf(
                OpcionPersonalizacion("Tradicional (doble carne)", 11.00),
                OpcionPersonalizacion("Triple carne", 14.50),
                OpcionPersonalizacion("Mega", 16.50)
            ),
            extras = listOf(
                OpcionPersonalizacion("Queso extra", 1.00),
                OpcionPersonalizacion("Tocino", 1.25),
                OpcionPersonalizacion("Jalapeños", 0.75),
                OpcionPersonalizacion("Salsa especial extra", 0.50)
            ),
            extrasMarcadosPorDefecto = setOf("Queso extra")
        ),

        "picante" to DetalleProducto(
            id = "picante",
            nombre = "Picante",
            rating = 4.5,
            numOpiniones = 67,
            descripcion = "Carne de res, jalapeños, queso pepper jack, salsa picante y aros de cebolla.",
            imagenResId = R.drawable.producto_picante,
            tamanos = listOf(
                OpcionPersonalizacion("Tradicional", 9.00),
                OpcionPersonalizacion("Doble carne", 11.00),
                OpcionPersonalizacion("Mega Picante", 13.00)
            ),
            extras = listOf(
                OpcionPersonalizacion("Jalapeños extra", 0.75),
                OpcionPersonalizacion("Queso pepper jack extra", 1.00),
                OpcionPersonalizacion("Salsa picante extra", 0.50),
                OpcionPersonalizacion("Aros de cebolla", 1.00)
            ),
            extrasMarcadosPorDefecto = setOf("Jalapeños extra")
        ),

        "champinones" to DetalleProducto(
            id = "champinones",
            nombre = "De Champiñones",
            rating = 4.4,
            numOpiniones = 58,
            descripcion = "Carne de res, champiñones salteados, queso suizo y mayonesa de ajo.",
            imagenResId = R.drawable.producto_champinones,
            tamanos = listOf(
                OpcionPersonalizacion("Tradicional", 9.25),
                OpcionPersonalizacion("Doble carne", 11.25),
                OpcionPersonalizacion("Mega", 13.25)
            ),
            extras = listOf(
                OpcionPersonalizacion("Champiñones extra", 1.00),
                OpcionPersonalizacion("Queso suizo extra", 1.00),
                OpcionPersonalizacion("Tocino", 1.25),
                OpcionPersonalizacion("Mayonesa de ajo extra", 0.50)
            ),
            extrasMarcadosPorDefecto = setOf("Champiñones extra")
        ),

        "hawaiana" to DetalleProducto(
            id = "hawaiana",
            nombre = "Hawaiana",
            rating = 4.3,
            numOpiniones = 45,
            descripcion = "Carne de res, piña a la parrilla, jamón, queso y salsa teriyaki.",
            imagenResId = R.drawable.producto_hawaiana,
            tamanos = listOf(
                OpcionPersonalizacion("Tradicional", 9.50),
                OpcionPersonalizacion("Doble carne", 11.50),
                OpcionPersonalizacion("Mega", 13.50)
            ),
            extras = listOf(
                OpcionPersonalizacion("Piña extra", 0.75),
                OpcionPersonalizacion("Jamón extra", 1.25),
                OpcionPersonalizacion("Queso extra", 1.00),
                OpcionPersonalizacion("Salsa teriyaki extra", 0.50)
            ),
            extrasMarcadosPorDefecto = setOf("Piña extra")
        ),

        "el_fogon" to DetalleProducto(
            id = "el_fogon",
            nombre = "\"El Fogón\"",
            rating = 5.0,
            numOpiniones = 140,
            descripcion = "Doble carne, tocino, queso cheddar, huevo frito, aros de cebolla y salsa especial.",
            imagenResId = R.drawable.producto_el_fogon,
            tamanos = listOf(
                OpcionPersonalizacion("Tradicional", 12.00),
                OpcionPersonalizacion("Doble ración de carne", 15.00),
                OpcionPersonalizacion("Mega Fogón (todo doble)", 18.00)
            ),
            extras = listOf(
                OpcionPersonalizacion("Huevo extra", 1.00),
                OpcionPersonalizacion("Tocino extra", 1.25),
                OpcionPersonalizacion("Aros de cebolla extra", 1.00),
                OpcionPersonalizacion("Queso cheddar extra", 1.00)
            ),
            extrasMarcadosPorDefecto = setOf("Huevo extra", "Tocino extra")
        ),

        "texana" to DetalleProducto(
            id = "texana",
            nombre = "Texana",
            rating = 4.6,
            numOpiniones = 72,
            descripcion = "Carne de res, salsa BBQ ahumada, aros de cebolla crujientes y queso monterey jack.",
            imagenResId = R.drawable.producto_texana,
            tamanos = listOf(
                OpcionPersonalizacion("Tradicional", 9.75),
                OpcionPersonalizacion("Doble carne", 11.75),
                OpcionPersonalizacion("Mega", 13.75)
            ),
            extras = listOf(
                OpcionPersonalizacion("Aros de cebolla extra", 1.00),
                OpcionPersonalizacion("Queso monterey jack extra", 1.00),
                OpcionPersonalizacion("Salsa BBQ extra", 0.75),
                OpcionPersonalizacion("Tocino", 1.25)
            ),
            extrasMarcadosPorDefecto = setOf("Aros de cebolla extra")
        ),

        "criolla_salvadorena" to DetalleProducto(
            id = "criolla_salvadorena",
            nombre = "Criolla Salvadoreña",
            rating = 4.8,
            numOpiniones = 90,
            descripcion = "Carne de res, plátano frito, queso duro rallado y curtido.",
            imagenResId = R.drawable.producto_criolla_salvadorena,
            tamanos = listOf(
                OpcionPersonalizacion("Tradicional", 9.50),
                OpcionPersonalizacion("Doble carne", 11.50),
                OpcionPersonalizacion("Mega", 13.50)
            ),
            extras = listOf(
                OpcionPersonalizacion("Plátano extra", 1.00),
                OpcionPersonalizacion("Queso duro extra", 1.00),
                OpcionPersonalizacion("Curtido extra", 0.50),
                OpcionPersonalizacion("Salsa especial extra", 0.50)
            ),
            extrasMarcadosPorDefecto = setOf("Plátano extra")
        ),

        "cuatro_quesos" to DetalleProducto(
            id = "cuatro_quesos",
            nombre = "Cuatro Quesos",
            rating = 4.7,
            numOpiniones = 88,
            descripcion = "Carne de res bañada en mezcla de queso cheddar, mozzarella, parmesano y queso crema.",
            imagenResId = R.drawable.producto_cuatro_quesos,
            tamanos = listOf(
                OpcionPersonalizacion("Tradicional", 10.25),
                OpcionPersonalizacion("Doble carne", 12.25),
                OpcionPersonalizacion("Mega", 14.25)
            ),
            extras = listOf(
                OpcionPersonalizacion("Extra mezcla de quesos", 1.50),
                OpcionPersonalizacion("Tocino", 1.25),
                OpcionPersonalizacion("Jalapeños", 0.75),
                OpcionPersonalizacion("Champiñones", 1.00)
            ),
            extrasMarcadosPorDefecto = setOf("Extra mezcla de quesos")
        ),

        "suprema_pollo" to DetalleProducto(
            id = "suprema_pollo",
            nombre = "Suprema de Pollo",
            rating = 4.5,
            numOpiniones = 76,
            descripcion = "Pechuga de pollo a la parrilla, aguacate, tocino y mayonesa de chipotle.",
            imagenResId = R.drawable.producto_suprema_pollo,
            tamanos = listOf(
                OpcionPersonalizacion("Pechuga sencilla", 8.75),
                OpcionPersonalizacion("Doble pechuga", 11.25),
                OpcionPersonalizacion("Mega", 13.25)
            ),
            extras = listOf(
                OpcionPersonalizacion("Aguacate extra", 1.25),
                OpcionPersonalizacion("Tocino extra", 1.25),
                OpcionPersonalizacion("Mayonesa de chipotle extra", 0.50),
                OpcionPersonalizacion("Queso extra", 1.00)
            ),
            extrasMarcadosPorDefecto = setOf("Aguacate extra")
        ),

        "vegetariana" to DetalleProducto(
            id = "vegetariana",
            nombre = "Vegetariana",
            rating = 4.3,
            numOpiniones = 40,
            descripcion = "Carne de vegetales y granos, lechuga, tomate, aguacate y salsa especial (sin carne).",
            imagenResId = R.drawable.producto_vegetariana,
            tamanos = listOf(
                OpcionPersonalizacion("Tradicional", 8.50),
                OpcionPersonalizacion("Doble (patty vegetal doble)", 10.50),
                OpcionPersonalizacion("Mega", 12.50)
            ),
            extras = listOf(
                OpcionPersonalizacion("Aguacate extra", 1.25),
                OpcionPersonalizacion("Queso extra", 1.00),
                OpcionPersonalizacion("Champiñones extra", 1.00),
                OpcionPersonalizacion("Salsa especial extra", 0.50)
            ),
            extrasMarcadosPorDefecto = setOf("Aguacate extra")
        ),

        "costena_pescado" to DetalleProducto(
            id = "costena_pescado",
            nombre = "Costeña de Pescado",
            rating = 4.4,
            numOpiniones = 52,
            descripcion = "Filete de pescado empanizado, repollo curtido y salsa tártara casera.",
            imagenResId = R.drawable.producto_costena_pescado,
            tamanos = listOf(
                OpcionPersonalizacion("Filete sencillo", 9.25),
                OpcionPersonalizacion("Doble filete", 11.75),
                OpcionPersonalizacion("Mega", 13.75)
            ),
            extras = listOf(
                OpcionPersonalizacion("Salsa tártara extra", 0.50),
                OpcionPersonalizacion("Repollo curtido extra", 0.50),
                OpcionPersonalizacion("Queso extra", 1.00),
                OpcionPersonalizacion("Aguacate extra", 1.25)
            ),
            extrasMarcadosPorDefecto = setOf("Salsa tártara extra")
        ),

        // ---- Las 12 pizzas del Menú (categoría Pizzas) ----

        "pizza_pepperoni" to DetalleProducto(
            id = "pizza_pepperoni",
            nombre = "Pepperoni",
            rating = 4.8,
            numOpiniones = 95,
            descripcion = "Salsa de tomate, mozzarella y pepperoni clásico.",
            imagenResId = R.drawable.producto_pizza_pepperoni,
            tamanos = listOf(
                OpcionPersonalizacion("Personal", 12.00),
                OpcionPersonalizacion("Mediana", 15.00),
                OpcionPersonalizacion("Familiar", 18.00)
            ),
            extras = listOf(
                OpcionPersonalizacion("Extra queso", 1.50),
                OpcionPersonalizacion("Orilla rellena", 2.00),
                OpcionPersonalizacion("Aceitunas", 1.00),
                OpcionPersonalizacion("Champiñones", 1.25)
            ),
            extrasMarcadosPorDefecto = setOf("Extra queso")
        ),

        "pizza_hawaiana" to DetalleProducto(
            id = "pizza_hawaiana",
            nombre = "Hawaiana",
            rating = 4.4,
            numOpiniones = 58,
            descripcion = "Jamón, piña y queso mozzarella.",
            imagenResId = R.drawable.producto_pizza_hawaiana,
            tamanos = listOf(
                OpcionPersonalizacion("Personal", 12.50),
                OpcionPersonalizacion("Mediana", 15.50),
                OpcionPersonalizacion("Familiar", 18.50)
            ),
            extras = listOf(
                OpcionPersonalizacion("Piña extra", 1.00),
                OpcionPersonalizacion("Jamón extra", 1.50),
                OpcionPersonalizacion("Extra queso", 1.50),
                OpcionPersonalizacion("Orilla rellena", 2.00)
            ),
            extrasMarcadosPorDefecto = setOf("Piña extra")
        ),

        "pizza_mexicana" to DetalleProducto(
            id = "pizza_mexicana",
            nombre = "Mexicana",
            rating = 4.7,
            numOpiniones = 82,
            descripcion = "Chorizo, jalapeños, pico de gallo y queso.",
            imagenResId = R.drawable.producto_pizza_mexicana,
            tamanos = listOf(
                OpcionPersonalizacion("Personal", 13.00),
                OpcionPersonalizacion("Mediana", 16.00),
                OpcionPersonalizacion("Familiar", 19.00)
            ),
            extras = listOf(
                OpcionPersonalizacion("Jalapeños extra", 1.00),
                OpcionPersonalizacion("Chorizo extra", 1.75),
                OpcionPersonalizacion("Pico de gallo extra", 0.75),
                OpcionPersonalizacion("Extra queso", 1.50)
            ),
            extrasMarcadosPorDefecto = setOf("Jalapeños extra")
        ),

        "pizza_cuatro_quesos" to DetalleProducto(
            id = "pizza_cuatro_quesos",
            nombre = "Cuatro Quesos",
            rating = 4.8,
            numOpiniones = 90,
            descripcion = "Mezcla de mozzarella, parmesano, queso azul y queso crema.",
            imagenResId = R.drawable.producto_pizza_cuatro_quesos,
            tamanos = listOf(
                OpcionPersonalizacion("Personal", 13.50),
                OpcionPersonalizacion("Mediana", 16.50),
                OpcionPersonalizacion("Familiar", 19.50)
            ),
            extras = listOf(
                OpcionPersonalizacion("Extra mezcla de quesos", 2.00),
                OpcionPersonalizacion("Orilla rellena", 2.00),
                OpcionPersonalizacion("Champiñones", 1.25),
                OpcionPersonalizacion("Tocino", 1.50)
            ),
            extrasMarcadosPorDefecto = setOf("Extra mezcla de quesos")
        ),

        "pizza_vegetariana" to DetalleProducto(
            id = "pizza_vegetariana",
            nombre = "Vegetariana",
            rating = 4.3,
            numOpiniones = 42,
            descripcion = "Pimientos, champiñones, cebolla morada, aceitunas y tomate (sin carne).",
            imagenResId = R.drawable.producto_pizza_vegetariana,
            tamanos = listOf(
                OpcionPersonalizacion("Personal", 12.00),
                OpcionPersonalizacion("Mediana", 15.00),
                OpcionPersonalizacion("Familiar", 18.00)
            ),
            extras = listOf(
                OpcionPersonalizacion("Champiñones extra", 1.25),
                OpcionPersonalizacion("Aceitunas extra", 1.00),
                OpcionPersonalizacion("Pimientos extra", 1.00),
                OpcionPersonalizacion("Extra queso", 1.50)
            ),
            extrasMarcadosPorDefecto = setOf("Champiñones extra")
        ),

        "pizza_bbq_pollo" to DetalleProducto(
            id = "pizza_bbq_pollo",
            nombre = "BBQ de Pollo",
            rating = 4.6,
            numOpiniones = 70,
            descripcion = "Pollo a la parrilla, salsa BBQ, cebolla morada y cilantro.",
            imagenResId = R.drawable.producto_pizza_bbq_pollo,
            tamanos = listOf(
                OpcionPersonalizacion("Personal", 13.50),
                OpcionPersonalizacion("Mediana", 16.50),
                OpcionPersonalizacion("Familiar", 19.50)
            ),
            extras = listOf(
                OpcionPersonalizacion("Pollo extra", 1.75),
                OpcionPersonalizacion("Salsa BBQ extra", 0.75),
                OpcionPersonalizacion("Cebolla morada extra", 0.50),
                OpcionPersonalizacion("Extra queso", 1.50)
            ),
            extrasMarcadosPorDefecto = setOf("Pollo extra")
        ),

        "pizza_suprema_fogon" to DetalleProducto(
            id = "pizza_suprema_fogon",
            nombre = "Suprema \"El Fogón\"",
            rating = 5.0,
            numOpiniones = 130,
            descripcion = "Pepperoni, carne molida, champiñones, pimientos y cebolla — la más cargada.",
            imagenResId = R.drawable.producto_pizza_suprema_fogon,
            tamanos = listOf(
                OpcionPersonalizacion("Personal", 14.50),
                OpcionPersonalizacion("Mediana", 17.50),
                OpcionPersonalizacion("Familiar", 20.50)
            ),
            extras = listOf(
                OpcionPersonalizacion("Extra carne molida", 1.75),
                OpcionPersonalizacion("Pepperoni extra", 1.50),
                OpcionPersonalizacion("Champiñones extra", 1.25),
                OpcionPersonalizacion("Extra queso", 1.50)
            ),
            extrasMarcadosPorDefecto = setOf("Extra carne molida", "Pepperoni extra")
        ),

        "pizza_margarita" to DetalleProducto(
            id = "pizza_margarita",
            nombre = "Margarita",
            rating = 4.5,
            numOpiniones = 60,
            descripcion = "Tomate fresco, albahaca y mozzarella — la más sencilla y clásica italiana.",
            imagenResId = R.drawable.producto_pizza_margarita,
            tamanos = listOf(
                OpcionPersonalizacion("Personal", 11.50),
                OpcionPersonalizacion("Mediana", 14.50),
                OpcionPersonalizacion("Familiar", 17.50)
            ),
            extras = listOf(
                OpcionPersonalizacion("Albahaca extra", 0.50),
                OpcionPersonalizacion("Extra queso", 1.50),
                OpcionPersonalizacion("Tomate extra", 0.75),
                OpcionPersonalizacion("Orilla rellena", 2.00)
            ),
            extrasMarcadosPorDefecto = setOf("Albahaca extra")
        ),

        "pizza_carnivora" to DetalleProducto(
            id = "pizza_carnivora",
            nombre = "Carnívora",
            rating = 4.9,
            numOpiniones = 105,
            descripcion = "Pepperoni, tocino, chorizo y jamón — para los que aman la carne.",
            imagenResId = R.drawable.producto_pizza_carnivora,
            tamanos = listOf(
                OpcionPersonalizacion("Personal", 14.00),
                OpcionPersonalizacion("Mediana", 17.00),
                OpcionPersonalizacion("Familiar", 20.00)
            ),
            extras = listOf(
                OpcionPersonalizacion("Tocino extra", 1.50),
                OpcionPersonalizacion("Chorizo extra", 1.75),
                OpcionPersonalizacion("Jamón extra", 1.50),
                OpcionPersonalizacion("Extra queso", 1.50)
            ),
            extrasMarcadosPorDefecto = setOf("Tocino extra")
        ),

        "pizza_camarones" to DetalleProducto(
            id = "pizza_camarones",
            nombre = "De Camarones",
            rating = 4.6,
            numOpiniones = 48,
            descripcion = "Camarones, ajo, mozzarella y un toque de limón.",
            imagenResId = R.drawable.producto_pizza_camarones,
            tamanos = listOf(
                OpcionPersonalizacion("Personal", 15.00),
                OpcionPersonalizacion("Mediana", 18.00),
                OpcionPersonalizacion("Familiar", 21.00)
            ),
            extras = listOf(
                OpcionPersonalizacion("Camarones extra", 2.50),
                OpcionPersonalizacion("Ajo extra", 0.50),
                OpcionPersonalizacion("Extra queso", 1.50),
                OpcionPersonalizacion("Orilla rellena", 2.00)
            ),
            extrasMarcadosPorDefecto = setOf("Camarones extra")
        ),

        "pizza_champinones_tocino" to DetalleProducto(
            id = "pizza_champinones_tocino",
            nombre = "Champiñones y Tocino",
            rating = 4.5,
            numOpiniones = 55,
            descripcion = "Champiñones salteados, tocino crujiente y queso mozzarella.",
            imagenResId = R.drawable.producto_pizza_champinones_tocino,
            tamanos = listOf(
                OpcionPersonalizacion("Personal", 12.50),
                OpcionPersonalizacion("Mediana", 15.50),
                OpcionPersonalizacion("Familiar", 18.50)
            ),
            extras = listOf(
                OpcionPersonalizacion("Champiñones extra", 1.25),
                OpcionPersonalizacion("Tocino extra", 1.50),
                OpcionPersonalizacion("Extra queso", 1.50),
                OpcionPersonalizacion("Orilla rellena", 2.00)
            ),
            extrasMarcadosPorDefecto = setOf("Tocino extra")
        ),

        "pizza_diabla_picante" to DetalleProducto(
            id = "pizza_diabla_picante",
            nombre = "Diabla Picante",
            rating = 4.4,
            numOpiniones = 50,
            descripcion = "Salami picante, jalapeños y salsa picante de la casa.",
            imagenResId = R.drawable.producto_pizza_diabla_picante,
            tamanos = listOf(
                OpcionPersonalizacion("Personal", 13.00),
                OpcionPersonalizacion("Mediana", 16.00),
                OpcionPersonalizacion("Familiar", 19.00)
            ),
            extras = listOf(
                OpcionPersonalizacion("Jalapeños extra", 1.00),
                OpcionPersonalizacion("Salsa picante extra", 0.75),
                OpcionPersonalizacion("Salami extra", 1.75),
                OpcionPersonalizacion("Extra queso", 1.50)
            ),
            extrasMarcadosPorDefecto = setOf("Salsa picante extra")
        ),

        // ---- Los 12 tacos del Menú (categoría Tacos) ----
        // Nota: el "tamaño" aquí es la cantidad de tacos por orden, ya que
        // es como se piden de verdad (4 / 6 / 8 tacos).

        "taco_al_pastor" to DetalleProducto(
            id = "taco_al_pastor",
            nombre = "Al Pastor",
            rating = 4.8,
            numOpiniones = 100,
            descripcion = "Carne de cerdo marinada en achiote, piña asada, cebolla y cilantro.",
            imagenResId = R.drawable.taco_al_pastor,
            tamanos = listOf(
                OpcionPersonalizacion("4 tacos", 4.75),
                OpcionPersonalizacion("6 tacos", 7.00),
                OpcionPersonalizacion("8 tacos", 9.00)
            ),
            extras = listOf(
                OpcionPersonalizacion("Piña extra", 0.50),
                OpcionPersonalizacion("Cebolla y cilantro extra", 0.25),
                OpcionPersonalizacion("Salsa extra", 0.25),
                OpcionPersonalizacion("Queso extra", 0.75)
            ),
            extrasMarcadosPorDefecto = setOf("Piña extra")
        ),

        "taco_carne_asada" to DetalleProducto(
            id = "taco_carne_asada",
            nombre = "Carne Asada",
            rating = 4.7,
            numOpiniones = 88,
            descripcion = "Carne de res asada a la parrilla, cebolla y cilantro fresco.",
            imagenResId = R.drawable.taco_carne_asada,
            tamanos = listOf(
                OpcionPersonalizacion("4 tacos", 4.50),
                OpcionPersonalizacion("6 tacos", 6.75),
                OpcionPersonalizacion("8 tacos", 8.75)
            ),
            extras = listOf(
                OpcionPersonalizacion("Extra carne", 1.00),
                OpcionPersonalizacion("Guacamole", 0.75),
                OpcionPersonalizacion("Queso", 0.50),
                OpcionPersonalizacion("Salsa extra", 0.25)
            ),
            extrasMarcadosPorDefecto = setOf("Extra carne")
        ),

        "taco_carnitas" to DetalleProducto(
            id = "taco_carnitas",
            nombre = "Carnitas",
            rating = 4.7,
            numOpiniones = 75,
            descripcion = "Cerdo cocido a fuego lento y deshebrado, con cebolla y cilantro.",
            imagenResId = R.drawable.taco_carnitas,
            tamanos = listOf(
                OpcionPersonalizacion("4 tacos", 4.75),
                OpcionPersonalizacion("6 tacos", 7.00),
                OpcionPersonalizacion("8 tacos", 9.00)
            ),
            extras = listOf(
                OpcionPersonalizacion("Extra carne", 1.00),
                OpcionPersonalizacion("Guacamole", 0.75),
                OpcionPersonalizacion("Cebolla y cilantro extra", 0.25),
                OpcionPersonalizacion("Salsa extra", 0.25)
            ),
            extrasMarcadosPorDefecto = setOf("Guacamole")
        ),

        "taco_suadero" to DetalleProducto(
            id = "taco_suadero",
            nombre = "Suadero",
            rating = 4.6,
            numOpiniones = 62,
            descripcion = "Carne de res suave y jugosa, al estilo tradicional de taquería.",
            imagenResId = R.drawable.taco_suadero,
            tamanos = listOf(
                OpcionPersonalizacion("4 tacos", 4.75),
                OpcionPersonalizacion("6 tacos", 7.00),
                OpcionPersonalizacion("8 tacos", 9.00)
            ),
            extras = listOf(
                OpcionPersonalizacion("Extra carne", 1.00),
                OpcionPersonalizacion("Guacamole", 0.75),
                OpcionPersonalizacion("Queso", 0.50),
                OpcionPersonalizacion("Cebolla y cilantro extra", 0.25)
            ),
            extrasMarcadosPorDefecto = setOf("Extra carne")
        ),

        "taco_birria" to DetalleProducto(
            id = "taco_birria",
            nombre = "De Birria",
            rating = 4.9,
            numOpiniones = 115,
            descripcion = "Birria de res deshebrada con queso derretido, servidos con consomé para remojar.",
            imagenResId = R.drawable.taco_birria,
            tamanos = listOf(
                OpcionPersonalizacion("4 tacos", 5.25),
                OpcionPersonalizacion("6 tacos", 7.75),
                OpcionPersonalizacion("8 tacos", 10.00)
            ),
            extras = listOf(
                OpcionPersonalizacion("Consomé extra", 0.75),
                OpcionPersonalizacion("Queso extra", 0.75),
                OpcionPersonalizacion("Extra carne", 1.00),
                OpcionPersonalizacion("Cebolla y cilantro extra", 0.25)
            ),
            extrasMarcadosPorDefecto = setOf("Consomé extra", "Queso extra")
        ),

        "taco_pescado" to DetalleProducto(
            id = "taco_pescado",
            nombre = "De Pescado",
            rating = 4.5,
            numOpiniones = 54,
            descripcion = "Pescado empanizado, repollo curtido y salsa cremosa, estilo Baja.",
            imagenResId = R.drawable.taco_pescado,
            tamanos = listOf(
                OpcionPersonalizacion("4 tacos", 5.00),
                OpcionPersonalizacion("6 tacos", 7.50),
                OpcionPersonalizacion("8 tacos", 9.75)
            ),
            extras = listOf(
                OpcionPersonalizacion("Salsa cremosa extra", 0.50),
                OpcionPersonalizacion("Repollo extra", 0.25),
                OpcionPersonalizacion("Aguacate", 0.75),
                OpcionPersonalizacion("Pico de gallo", 0.50)
            ),
            extrasMarcadosPorDefecto = setOf("Salsa cremosa extra")
        ),

        "taco_camaron" to DetalleProducto(
            id = "taco_camaron",
            nombre = "De Camarón",
            rating = 4.6,
            numOpiniones = 48,
            descripcion = "Camarones a la parrilla, repollo morado y salsa chipotle.",
            imagenResId = R.drawable.taco_camaron,
            tamanos = listOf(
                OpcionPersonalizacion("4 tacos", 5.50),
                OpcionPersonalizacion("6 tacos", 8.25),
                OpcionPersonalizacion("8 tacos", 10.75)
            ),
            extras = listOf(
                OpcionPersonalizacion("Salsa chipotle extra", 0.50),
                OpcionPersonalizacion("Extra camarones", 1.25),
                OpcionPersonalizacion("Repollo extra", 0.25),
                OpcionPersonalizacion("Aguacate", 0.75)
            ),
            extrasMarcadosPorDefecto = setOf("Salsa chipotle extra")
        ),

        "taco_chorizo_papa" to DetalleProducto(
            id = "taco_chorizo_papa",
            nombre = "De Chorizo con Papa",
            rating = 4.4,
            numOpiniones = 40,
            descripcion = "Chorizo frito con papa en cuadritos y cilantro.",
            imagenResId = R.drawable.taco_chorizo_papa,
            tamanos = listOf(
                OpcionPersonalizacion("4 tacos", 4.25),
                OpcionPersonalizacion("6 tacos", 6.25),
                OpcionPersonalizacion("8 tacos", 8.25)
            ),
            extras = listOf(
                OpcionPersonalizacion("Extra chorizo", 0.75),
                OpcionPersonalizacion("Queso extra", 0.75),
                OpcionPersonalizacion("Salsa extra", 0.25),
                OpcionPersonalizacion("Aguacate", 0.75)
            ),
            extrasMarcadosPorDefecto = setOf("Extra chorizo")
        ),

        "taco_frito_pollo" to DetalleProducto(
            id = "taco_frito_pollo",
            nombre = "Fritos de Pollo",
            rating = 4.5,
            numOpiniones = 66,
            descripcion = "Tacos enrollados y fritos, rellenos de pollo deshebrado, cubiertos de repollo y salsa de tomate.",
            imagenResId = R.drawable.taco_frito_pollo,
            tamanos = listOf(
                OpcionPersonalizacion("4 tacos", 4.00),
                OpcionPersonalizacion("6 tacos", 6.00),
                OpcionPersonalizacion("8 tacos", 7.75)
            ),
            extras = listOf(
                OpcionPersonalizacion("Crema extra", 0.50),
                OpcionPersonalizacion("Queso extra", 0.75),
                OpcionPersonalizacion("Repollo extra", 0.25),
                OpcionPersonalizacion("Salsa extra", 0.25)
            ),
            extrasMarcadosPorDefecto = setOf("Crema extra")
        ),

        "taco_frito_res" to DetalleProducto(
            id = "taco_frito_res",
            nombre = "Fritos de Res",
            rating = 4.6,
            numOpiniones = 70,
            descripcion = "Tacos enrollados y fritos, rellenos de carne de res deshebrada, cubiertos de repollo y salsa de tomate.",
            imagenResId = R.drawable.taco_frito_res,
            tamanos = listOf(
                OpcionPersonalizacion("4 tacos", 4.25),
                OpcionPersonalizacion("6 tacos", 6.25),
                OpcionPersonalizacion("8 tacos", 8.25)
            ),
            extras = listOf(
                OpcionPersonalizacion("Crema extra", 0.50),
                OpcionPersonalizacion("Queso extra", 0.75),
                OpcionPersonalizacion("Repollo extra", 0.25),
                OpcionPersonalizacion("Salsa extra", 0.25)
            ),
            extrasMarcadosPorDefecto = setOf("Crema extra")
        ),

        "taco_frito_queso" to DetalleProducto(
            id = "taco_frito_queso",
            nombre = "Fritos de Queso",
            rating = 4.3,
            numOpiniones = 35,
            descripcion = "Tacos enrollados y fritos, rellenos de queso, cubiertos de repollo y salsa de tomate.",
            imagenResId = R.drawable.taco_frito_queso,
            tamanos = listOf(
                OpcionPersonalizacion("4 tacos", 3.75),
                OpcionPersonalizacion("6 tacos", 5.50),
                OpcionPersonalizacion("8 tacos", 7.25)
            ),
            extras = listOf(
                OpcionPersonalizacion("Extra queso", 0.75),
                OpcionPersonalizacion("Crema extra", 0.50),
                OpcionPersonalizacion("Repollo extra", 0.25),
                OpcionPersonalizacion("Salsa extra", 0.25)
            ),
            extrasMarcadosPorDefecto = setOf("Extra queso")
        ),

        "taco_dorado_mixto" to DetalleProducto(
            id = "taco_dorado_mixto",
            nombre = "Dorados Mixtos",
            rating = 4.7,
            numOpiniones = 80,
            descripcion = "Tacos fritos rellenos de pollo y carne de res, cubiertos de repollo, crema y salsa de tomate.",
            imagenResId = R.drawable.taco_dorado_mixto,
            tamanos = listOf(
                OpcionPersonalizacion("4 tacos", 4.50),
                OpcionPersonalizacion("6 tacos", 6.75),
                OpcionPersonalizacion("8 tacos", 8.75)
            ),
            extras = listOf(
                OpcionPersonalizacion("Crema extra", 0.50),
                OpcionPersonalizacion("Queso extra", 0.75),
                OpcionPersonalizacion("Repollo extra", 0.25),
                OpcionPersonalizacion("Salsa extra", 0.25)
            ),
            extrasMarcadosPorDefecto = setOf("Crema extra")
        ),

        // ---- Las 12 bebidas del Menú (categoría Bebidas) ----
        // Nota: el "tamaño" aquí es la presentación: 16 oz (la normal) o
        // 18 oz (un poco más grande, a otro precio), como lo pidió el usuario.

        "bebida_gaseosa_cola" to DetalleProducto(
            id = "bebida_gaseosa_cola",
            nombre = "Gaseosa de Cola",
            rating = 4.6,
            numOpiniones = 60,
            descripcion = "Gaseosa sabor cola, servida bien fría. Presentación de 16 oz.",
            imagenResId = R.drawable.bebida_gaseosa_cola,
            tamanos = listOf(
                OpcionPersonalizacion("16 oz", 1.50),
                OpcionPersonalizacion("18 oz", 2.00)
            ),
            extras = listOf(
                OpcionPersonalizacion("Rodaja de limón", 0.10)
            ),
            extrasMarcadosPorDefecto = emptySet()
        ),

        "bebida_gaseosa_cola_light" to DetalleProducto(
            id = "bebida_gaseosa_cola_light",
            nombre = "Gaseosa de Cola Light",
            rating = 4.3,
            numOpiniones = 35,
            descripcion = "Gaseosa sabor cola sin azúcar, servida bien fría. Presentación de 16 oz.",
            imagenResId = R.drawable.bebida_gaseosa_cola_light,
            tamanos = listOf(
                OpcionPersonalizacion("16 oz", 1.50),
                OpcionPersonalizacion("18 oz", 2.00)
            ),
            extras = listOf(
                OpcionPersonalizacion("Rodaja de limón", 0.10)
            ),
            extrasMarcadosPorDefecto = emptySet()
        ),

        "bebida_gaseosa_lima_limon" to DetalleProducto(
            id = "bebida_gaseosa_lima_limon",
            nombre = "Gaseosa Lima-Limón",
            rating = 4.4,
            numOpiniones = 40,
            descripcion = "Gaseosa sabor lima-limón, servida bien fría. Presentación de 16 oz.",
            imagenResId = R.drawable.bebida_gaseosa_lima_limon,
            tamanos = listOf(
                OpcionPersonalizacion("16 oz", 1.50),
                OpcionPersonalizacion("18 oz", 2.00)
            ),
            extras = listOf(
                OpcionPersonalizacion("Rodaja de limón extra", 0.10)
            ),
            extrasMarcadosPorDefecto = emptySet()
        ),

        "bebida_gaseosa_naranja" to DetalleProducto(
            id = "bebida_gaseosa_naranja",
            nombre = "Gaseosa de Naranja",
            rating = 4.5,
            numOpiniones = 45,
            descripcion = "Gaseosa sabor naranja, servida bien fría. Presentación de 16 oz.",
            imagenResId = R.drawable.bebida_gaseosa_naranja,
            tamanos = listOf(
                OpcionPersonalizacion("16 oz", 1.50),
                OpcionPersonalizacion("18 oz", 2.00)
            ),
            extras = listOf(
                OpcionPersonalizacion("Hielo extra", 0.10)
            ),
            extrasMarcadosPorDefecto = emptySet()
        ),

        "bebida_horchata" to DetalleProducto(
            id = "bebida_horchata",
            nombre = "Horchata",
            rating = 4.8,
            numOpiniones = 85,
            descripcion = "Bebida tradicional de arroz, canela y vainilla. Presentación de 16 oz.",
            imagenResId = R.drawable.bebida_horchata,
            tamanos = listOf(
                OpcionPersonalizacion("16 oz", 1.75),
                OpcionPersonalizacion("18 oz", 2.25)
            ),
            extras = listOf(
                OpcionPersonalizacion("Canela extra", 0.10)
            ),
            extrasMarcadosPorDefecto = emptySet()
        ),

        "bebida_tamarindo" to DetalleProducto(
            id = "bebida_tamarindo",
            nombre = "Refresco de Tamarindo",
            rating = 4.7,
            numOpiniones = 70,
            descripcion = "Refresco natural hecho de tamarindo. Presentación de 16 oz.",
            imagenResId = R.drawable.bebida_tamarindo,
            tamanos = listOf(
                OpcionPersonalizacion("16 oz", 1.75),
                OpcionPersonalizacion("18 oz", 2.25)
            ),
            extras = listOf(
                OpcionPersonalizacion("Hielo extra", 0.10)
            ),
            extrasMarcadosPorDefecto = emptySet()
        ),

        "bebida_jamaica" to DetalleProducto(
            id = "bebida_jamaica",
            nombre = "Refresco de Jamaica",
            rating = 4.6,
            numOpiniones = 58,
            descripcion = "Refresco natural de flor de jamaica (hibisco). Presentación de 16 oz.",
            imagenResId = R.drawable.bebida_jamaica,
            tamanos = listOf(
                OpcionPersonalizacion("16 oz", 1.75),
                OpcionPersonalizacion("18 oz", 2.25)
            ),
            extras = listOf(
                OpcionPersonalizacion("Hielo extra", 0.10)
            ),
            extrasMarcadosPorDefecto = emptySet()
        ),

        "bebida_limonada" to DetalleProducto(
            id = "bebida_limonada",
            nombre = "Limonada",
            rating = 4.7,
            numOpiniones = 65,
            descripcion = "Limonada clásica, fresca y natural. Presentación de 16 oz.",
            imagenResId = R.drawable.bebida_limonada,
            tamanos = listOf(
                OpcionPersonalizacion("16 oz", 1.50),
                OpcionPersonalizacion("18 oz", 2.00)
            ),
            extras = listOf(
                OpcionPersonalizacion("Rodaja de limón extra", 0.10)
            ),
            extrasMarcadosPorDefecto = emptySet()
        ),

        "bebida_limonada_hierbabuena" to DetalleProducto(
            id = "bebida_limonada_hierbabuena",
            nombre = "Limonada con Hierbabuena",
            rating = 4.6,
            numOpiniones = 50,
            descripcion = "Limonada con un toque de menta fresca. Presentación de 16 oz.",
            imagenResId = R.drawable.bebida_limonada_hierbabuena,
            tamanos = listOf(
                OpcionPersonalizacion("16 oz", 1.75),
                OpcionPersonalizacion("18 oz", 2.25)
            ),
            extras = listOf(
                OpcionPersonalizacion("Hierbabuena extra", 0.10)
            ),
            extrasMarcadosPorDefecto = emptySet()
        ),

        "bebida_agua_pina" to DetalleProducto(
            id = "bebida_agua_pina",
            nombre = "Agua Fresca de Piña",
            rating = 4.5,
            numOpiniones = 42,
            descripcion = "Agua fresca natural de piña. Presentación de 16 oz.",
            imagenResId = R.drawable.bebida_agua_pina,
            tamanos = listOf(
                OpcionPersonalizacion("16 oz", 1.75),
                OpcionPersonalizacion("18 oz", 2.25)
            ),
            extras = listOf(
                OpcionPersonalizacion("Trozo de piña extra", 0.10)
            ),
            extrasMarcadosPorDefecto = emptySet()
        ),

        "bebida_te_helado" to DetalleProducto(
            id = "bebida_te_helado",
            nombre = "Té Helado",
            rating = 4.4,
            numOpiniones = 38,
            descripcion = "Té negro helado, ligeramente endulzado. Presentación de 16 oz.",
            imagenResId = R.drawable.bebida_te_helado,
            tamanos = listOf(
                OpcionPersonalizacion("16 oz", 1.50),
                OpcionPersonalizacion("18 oz", 2.00)
            ),
            extras = listOf(
                OpcionPersonalizacion("Rodaja de limón extra", 0.10)
            ),
            extrasMarcadosPorDefecto = emptySet()
        ),

        "bebida_agua_embotellada" to DetalleProducto(
            id = "bebida_agua_embotellada",
            nombre = "Agua Embotellada",
            rating = 4.2,
            numOpiniones = 20,
            descripcion = "Agua pura sin gas. Presentación de 16 oz.",
            imagenResId = R.drawable.bebida_agua_embotellada,
            tamanos = listOf(
                OpcionPersonalizacion("16 oz", 1.00),
                OpcionPersonalizacion("18 oz", 1.50)
            ),
            extras = emptyList(),
            extrasMarcadosPorDefecto = emptySet()
        ),

        // ---- Los 6 postres del Menú (categoría Postres) ----

        "postre_tres_leches" to DetalleProducto(
            id = "postre_tres_leches",
            nombre = "Tres Leches",
            rating = 4.9,
            numOpiniones = 98,
            descripcion = "Pastel esponjoso bañado en tres tipos de leche, cubierto con crema chantilly casera.",
            imagenResId = R.drawable.postre_tres_leches,
            tamanos = listOf(
                OpcionPersonalizacion("Rebanada", 3.00),
                OpcionPersonalizacion("Rebanada grande", 4.25),
                OpcionPersonalizacion("Pastel completo (8 porciones)", 18.00)
            ),
            extras = listOf(
                OpcionPersonalizacion("Extra crema chantilly", 0.75),
                OpcionPersonalizacion("Canela extra", 0.25),
                OpcionPersonalizacion("Fresas", 1.00),
                OpcionPersonalizacion("Nuez picada", 0.75)
            ),
            extrasMarcadosPorDefecto = setOf("Extra crema chantilly")
        ),

        "postre_flan_caramelo" to DetalleProducto(
            id = "postre_flan_caramelo",
            nombre = "Flan de Caramelo",
            rating = 4.7,
            numOpiniones = 72,
            descripcion = "Flan casero suave, bañado en salsa de caramelo.",
            imagenResId = R.drawable.postre_flan_caramelo,
            tamanos = listOf(
                OpcionPersonalizacion("Individual", 2.75),
                OpcionPersonalizacion("Doble porción", 4.75)
            ),
            extras = listOf(
                OpcionPersonalizacion("Caramelo extra", 0.50),
                OpcionPersonalizacion("Crema batida extra", 0.50),
                OpcionPersonalizacion("Fresas", 1.00)
            ),
            extrasMarcadosPorDefecto = setOf("Caramelo extra")
        ),

        "postre_pastel_chocolate" to DetalleProducto(
            id = "postre_pastel_chocolate",
            nombre = "Pastel de Chocolate",
            rating = 4.8,
            numOpiniones = 90,
            descripcion = "Pastel húmedo de chocolate con ganache y chispas de chocolate.",
            imagenResId = R.drawable.postre_pastel_chocolate,
            tamanos = listOf(
                OpcionPersonalizacion("Rebanada", 3.25),
                OpcionPersonalizacion("Rebanada grande", 4.50),
                OpcionPersonalizacion("Pastel completo (8 porciones)", 20.00)
            ),
            extras = listOf(
                OpcionPersonalizacion("Ganache extra", 0.75),
                OpcionPersonalizacion("Fresas", 1.00),
                OpcionPersonalizacion("Helado de vainilla", 1.25)
            ),
            extrasMarcadosPorDefecto = setOf("Ganache extra")
        ),

        "postre_churros_chocolate" to DetalleProducto(
            id = "postre_churros_chocolate",
            nombre = "Churros con Chocolate",
            rating = 4.6,
            numOpiniones = 65,
            descripcion = "Churros crujientes espolvoreados con azúcar y canela, con salsa de chocolate para remojar.",
            imagenResId = R.drawable.postre_churros_chocolate,
            tamanos = listOf(
                OpcionPersonalizacion("4 churros", 2.50),
                OpcionPersonalizacion("8 churros", 4.50)
            ),
            extras = listOf(
                OpcionPersonalizacion("Chocolate extra", 0.50),
                OpcionPersonalizacion("Canela extra", 0.25),
                OpcionPersonalizacion("Dulce de leche extra", 0.75)
            ),
            extrasMarcadosPorDefecto = setOf("Chocolate extra")
        ),

        "postre_cheesecake_fresa" to DetalleProducto(
            id = "postre_cheesecake_fresa",
            nombre = "Cheesecake de Fresa",
            rating = 4.8,
            numOpiniones = 85,
            descripcion = "Cheesecake cremoso con cobertura de fresas frescas y mermelada.",
            imagenResId = R.drawable.postre_cheesecake_fresa,
            tamanos = listOf(
                OpcionPersonalizacion("Rebanada", 3.50),
                OpcionPersonalizacion("Rebanada grande", 4.75),
                OpcionPersonalizacion("Pastel completo (8 porciones)", 22.00)
            ),
            extras = listOf(
                OpcionPersonalizacion("Fresas extra", 1.00),
                OpcionPersonalizacion("Mermelada extra", 0.50),
                OpcionPersonalizacion("Crema batida extra", 0.50)
            ),
            extrasMarcadosPorDefecto = setOf("Fresas extra")
        ),

        "postre_semita_pina" to DetalleProducto(
            id = "postre_semita_pina",
            nombre = "Semita de Piña",
            rating = 4.5,
            numOpiniones = 48,
            descripcion = "Pan dulce tradicional salvadoreño relleno de mermelada de piña.",
            imagenResId = R.drawable.postre_semita_pina,
            tamanos = listOf(
                OpcionPersonalizacion("1 pieza", 2.00),
                OpcionPersonalizacion("3 piezas", 5.00)
            ),
            extras = listOf(
                OpcionPersonalizacion("Mermelada extra", 0.50)
            ),
            extrasMarcadosPorDefecto = emptySet()
        ),
        // ---- Los 5 burritos del Menú (categoría Burritos) ----

        "burrito_res_5capas" to DetalleProducto(
            id = "burrito_res_5capas",
            nombre = "Burrito de Res 5 Capas",
            rating = 4.8,
            numOpiniones = 70,
            descripcion = "Carne de res, arroz, frijoles refritos, queso derretido y pico de gallo.",
            imagenResId = R.drawable.burrito_res_5capas,
            tamanos = listOf(
                OpcionPersonalizacion("Sencillo", 4.50),
                OpcionPersonalizacion("Doble carne", 6.00),
                OpcionPersonalizacion("Especial", 7.50)
            ),
            extras = listOf(
                OpcionPersonalizacion("Extra carne", 1.50),
                OpcionPersonalizacion("Queso extra", 1.00),
                OpcionPersonalizacion("Guacamole", 1.00),
                OpcionPersonalizacion("Crema", 0.50)
            ),
            extrasMarcadosPorDefecto = setOf("Extra carne")
        ),

        "burrito_pollo" to DetalleProducto(
            id = "burrito_pollo",
            nombre = "Burrito de Pollo",
            rating = 4.6,
            numOpiniones = 55,
            descripcion = "Pollo a la parrilla, arroz, frijoles, queso y pico de gallo.",
            imagenResId = R.drawable.burrito_pollo,
            tamanos = listOf(
                OpcionPersonalizacion("Sencillo", 4.25),
                OpcionPersonalizacion("Doble pollo", 5.75),
                OpcionPersonalizacion("Especial", 7.25)
            ),
            extras = listOf(
                OpcionPersonalizacion("Extra pollo", 1.25),
                OpcionPersonalizacion("Queso extra", 1.00),
                OpcionPersonalizacion("Guacamole", 1.00),
                OpcionPersonalizacion("Pico de gallo extra", 0.50)
            ),
            extrasMarcadosPorDefecto = setOf("Extra pollo")
        ),

        "burrito_carnitas" to DetalleProducto(
            id = "burrito_carnitas",
            nombre = "Burrito de Carnitas",
            rating = 4.7,
            numOpiniones = 62,
            descripcion = "Cerdo deshebrado, arroz, frijoles, queso y salsa verde.",
            imagenResId = R.drawable.burrito_carnitas,
            tamanos = listOf(
                OpcionPersonalizacion("Sencillo", 4.50),
                OpcionPersonalizacion("Doble carne", 6.00),
                OpcionPersonalizacion("Especial", 7.50)
            ),
            extras = listOf(
                OpcionPersonalizacion("Extra carne", 1.50),
                OpcionPersonalizacion("Queso extra", 1.00),
                OpcionPersonalizacion("Salsa verde extra", 0.50),
                OpcionPersonalizacion("Crema", 0.50)
            ),
            extrasMarcadosPorDefecto = setOf("Extra carne")
        ),

        "burrito_vegetariano" to DetalleProducto(
            id = "burrito_vegetariano",
            nombre = "Burrito Vegetariano",
            rating = 4.3,
            numOpiniones = 38,
            descripcion = "Arroz, frijoles, queso, pico de gallo, lechuga y guacamole (sin carne).",
            imagenResId = R.drawable.burrito_vegetariano,
            tamanos = listOf(
                OpcionPersonalizacion("Sencillo", 4.00),
                OpcionPersonalizacion("Doble porción", 5.25),
                OpcionPersonalizacion("Especial", 6.50)
            ),
            extras = listOf(
                OpcionPersonalizacion("Guacamole extra", 1.00),
                OpcionPersonalizacion("Queso extra", 1.00),
                OpcionPersonalizacion("Frijoles extra", 0.75),
                OpcionPersonalizacion("Pico de gallo extra", 0.50)
            ),
            extrasMarcadosPorDefecto = setOf("Guacamole extra")
        ),

        "burrito_supremo_fogon" to DetalleProducto(
            id = "burrito_supremo_fogon",
            nombre = "Burrito Supremo \"El Fogón\"",
            rating = 4.9,
            numOpiniones = 95,
            descripcion = "Doble carne de res, tocino, arroz, frijoles, queso derretido y guacamole — el más cargado.",
            imagenResId = R.drawable.burrito_supremo_fogon,
            tamanos = listOf(
                OpcionPersonalizacion("Sencillo", 5.50),
                OpcionPersonalizacion("Extra grande", 7.50),
                OpcionPersonalizacion("Especial Mega", 9.50)
            ),
            extras = listOf(
                OpcionPersonalizacion("Tocino extra", 1.25),
                OpcionPersonalizacion("Extra carne", 1.50),
                OpcionPersonalizacion("Queso extra", 1.00),
                OpcionPersonalizacion("Guacamole extra", 1.00)
            ),
            extrasMarcadosPorDefecto = setOf("Tocino extra")
        ),
        // ---- Las 5 tortas del Menú (categoría Tortas) ----

        "torta_birria" to DetalleProducto(
            id = "torta_birria",
            nombre = "Torta de Birria",
            rating = 4.8,
            numOpiniones = 85,
            descripcion = "Birria de res deshebrada con queso derretido y consomé para remojar.",
            imagenResId = R.drawable.torta_birria,
            tamanos = listOf(
                OpcionPersonalizacion("Sencilla", 3.75),
                OpcionPersonalizacion("Doble carne", 5.25),
                OpcionPersonalizacion("Con consomé extra", 4.50)
            ),
            extras = listOf(
                OpcionPersonalizacion("Extra carne", 1.50),
                OpcionPersonalizacion("Consomé extra", 1.00),
                OpcionPersonalizacion("Queso", 0.75),
                OpcionPersonalizacion("Cebolla y cilantro", 0.25)
            ),
            extrasMarcadosPorDefecto = setOf("Extra carne")
        ),

        "torta_milanesa" to DetalleProducto(
            id = "torta_milanesa",
            nombre = "Torta de Milanesa",
            rating = 4.7,
            numOpiniones = 68,
            descripcion = "Milanesa de res empanizada, lechuga, tomate, aguacate y mayonesa.",
            imagenResId = R.drawable.torta_milanesa,
            tamanos = listOf(
                OpcionPersonalizacion("Sencilla", 4.00),
                OpcionPersonalizacion("Doble milanesa", 6.00),
                OpcionPersonalizacion("Especial", 6.75)
            ),
            extras = listOf(
                OpcionPersonalizacion("Extra milanesa", 1.50),
                OpcionPersonalizacion("Aguacate extra", 0.75),
                OpcionPersonalizacion("Queso extra", 0.75),
                OpcionPersonalizacion("Mayonesa extra", 0.25)
            ),
            extrasMarcadosPorDefecto = setOf("Extra milanesa")
        ),

        "torta_jamon_queso" to DetalleProducto(
            id = "torta_jamon_queso",
            nombre = "Torta de Jamón y Queso",
            rating = 4.4,
            numOpiniones = 42,
            descripcion = "Jamón, queso amarillo, lechuga, tomate y mayonesa.",
            imagenResId = R.drawable.torta_jamon_queso,
            tamanos = listOf(
                OpcionPersonalizacion("Sencilla", 3.00),
                OpcionPersonalizacion("Doble jamón y queso", 4.25),
                OpcionPersonalizacion("Especial", 4.75)
            ),
            extras = listOf(
                OpcionPersonalizacion("Jamón extra", 1.00),
                OpcionPersonalizacion("Queso extra", 0.75),
                OpcionPersonalizacion("Aguacate", 0.75),
                OpcionPersonalizacion("Mayonesa extra", 0.25)
            ),
            extrasMarcadosPorDefecto = setOf("Jamón extra")
        ),

        "torta_pierna" to DetalleProducto(
            id = "torta_pierna",
            nombre = "Torta de Pierna",
            rating = 4.6,
            numOpiniones = 58,
            descripcion = "Pierna de cerdo adobada, frijoles refritos, lechuga y aguacate.",
            imagenResId = R.drawable.torta_pierna,
            tamanos = listOf(
                OpcionPersonalizacion("Sencilla", 3.75),
                OpcionPersonalizacion("Doble porción", 5.25),
                OpcionPersonalizacion("Especial", 5.75)
            ),
            extras = listOf(
                OpcionPersonalizacion("Extra pierna", 1.50),
                OpcionPersonalizacion("Frijoles extra", 0.50),
                OpcionPersonalizacion("Aguacate extra", 0.75),
                OpcionPersonalizacion("Queso", 0.75)
            ),
            extrasMarcadosPorDefecto = setOf("Extra pierna")
        ),

        "torta_cubana_fogon" to DetalleProducto(
            id = "torta_cubana_fogon",
            nombre = "Torta Cubana \"El Fogón\"",
            rating = 4.9,
            numOpiniones = 100,
            descripcion = "Milanesa, jamón, queso, chorizo y huevo — la más cargada de todas.",
            imagenResId = R.drawable.torta_cubana_fogon,
            tamanos = listOf(
                OpcionPersonalizacion("Sencilla", 4.50),
                OpcionPersonalizacion("Extra grande", 6.00),
                OpcionPersonalizacion("Mega", 7.50)
            ),
            extras = listOf(
                OpcionPersonalizacion("Huevo extra", 1.00),
                OpcionPersonalizacion("Extra queso", 1.00),
                OpcionPersonalizacion("Chorizo extra", 1.25),
                OpcionPersonalizacion("Jamón extra", 1.00)
            ),
            extrasMarcadosPorDefecto = setOf("Huevo extra")
        )
    )

    fun obtener(id: String): DetalleProducto? = productos[id]
}