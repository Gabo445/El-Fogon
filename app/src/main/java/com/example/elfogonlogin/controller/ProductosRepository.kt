package com.example.elfogonlogin.controller

import com.example.elfogonlogin.R
import com.example.elfogonlogin.model.Producto

object ProductosRepository {

    private val productos = listOf(
        Producto(
            1,
            "Hamburguesas",
            "Clásica",
            "Carne de res, lechuga, tomate, cebolla, queso amarillo y salsas de la casa.",
            8.50,
            R.drawable.producto_clasica,
            "clasica"
        ),
        Producto(
            2,
            "Hamburguesas",
            "Bacon Cheese",
            "Carne de res, tocino crujiente, queso cheddar derretido y cebolla caramelizada.",
            9.50,
            R.drawable.producto_bacon_cheese,
            "bacon_cheese"
        ),
        Producto(
            3,
            "Hamburguesas",
            "BBQ Ahumada",
            "Carne de res, salsa BBQ casera, cebolla crispy, queso cheddar y tocino.",
            9.75,
            R.drawable.producto_bbq_ahumada,
            "bbq_ahumada"
        ),
        Producto(
            4,
            "Hamburguesas",
            "Doble Carne y Queso",
            "Doble carne de res, doble queso americano y salsa especial de la casa.",
            11.00,
            R.drawable.producto_doble_carne_queso,
            "doble_carne_queso"
        ),
        Producto(
            5,
            "Hamburguesas",
            "Picante",
            "Carne de res, jalapeños, queso pepper jack, salsa picante y aros de cebolla.",
            9.00,
            R.drawable.producto_picante,
            "picante"
        ),
        Producto(
            6,
            "Hamburguesas",
            "De Champiñones",
            "Carne de res, champiñones salteados, queso suizo y mayonesa de ajo.",
            9.25,
            R.drawable.producto_champinones,
            "champinones"
        ),
        Producto(
            7,
            "Hamburguesas",
            "Hawaiana",
            "Carne de res, piña a la parrilla, jamón, queso y salsa teriyaki.",
            9.50,
            R.drawable.producto_hawaiana,
            "hawaiana"
        ),
        Producto(
            8,
            "Hamburguesas",
            "\"El Fogón\"",
            "Doble carne, tocino, queso cheddar, huevo frito, aros de cebolla y salsa especial.",
            12.00,
            R.drawable.producto_el_fogon,
            "el_fogon"
        ),
        Producto(
            9,
            "Hamburguesas",
            "Texana",
            "Carne de res, salsa BBQ ahumada, aros de cebolla crujientes y queso monterey jack.",
            9.75,
            R.drawable.producto_texana,
            "texana"
        ),
        Producto(
            10,
            "Hamburguesas",
            "Criolla Salvadoreña",
            "Carne de res, plátano frito, queso duro rallado y curtido.",
            9.50,
            R.drawable.producto_criolla_salvadorena,
            "criolla_salvadorena"
        ),
        Producto(
            11,
            "Hamburguesas",
            "Cuatro Quesos",
            "Carne de res bañada en mezcla de queso cheddar, mozzarella, parmesano y queso crema.",
            10.25,
            R.drawable.producto_cuatro_quesos,
            "cuatro_quesos"
        ),
        Producto(
            12,
            "Hamburguesas",
            "Suprema de Pollo",
            "Pechuga de pollo a la parrilla, aguacate, tocino y mayonesa de chipotle.",
            8.75,
            R.drawable.producto_suprema_pollo,
            "suprema_pollo"
        ),
        Producto(
            13,
            "Hamburguesas",
            "Vegetariana",
            "Carne de vegetales y granos, lechuga, tomate, aguacate y salsa especial (sin carne).",
            8.50,
            R.drawable.producto_vegetariana,
            "vegetariana"
        ),
        Producto(
            14,
            "Hamburguesas",
            "Costeña de Pescado",
            "Filete de pescado empanizado, repollo curtido y salsa tártara casera.",
            9.25,
            R.drawable.producto_costena_pescado,
            "costena_pescado"
        ),

        Producto(
            15,
            "Pizzas",
            "Pepperoni",
            "Salsa de tomate, mozzarella y pepperoni clásico.",
            12.00,
            R.drawable.producto_pizza_pepperoni,
            "pizza_pepperoni"
        ),
        Producto(
            16,
            "Pizzas",
            "Hawaiana",
            "Jamón, piña y queso mozzarella.",
            12.50,
            R.drawable.producto_pizza_hawaiana,
            "pizza_hawaiana"
        ),
        Producto(
            17,
            "Pizzas",
            "Mexicana",
            "Chorizo, jalapeños, pico de gallo y queso.",
            13.00,
            R.drawable.producto_pizza_mexicana,
            "pizza_mexicana"
        ),
        Producto(
            18,
            "Pizzas",
            "Cuatro Quesos",
            "Mezcla de mozzarella, parmesano, queso azul y queso crema.",
            13.50,
            R.drawable.producto_pizza_cuatro_quesos,
            "pizza_cuatro_quesos"
        ),
        Producto(
            19,
            "Pizzas",
            "Vegetariana",
            "Pimientos, champiñones, cebolla morada, aceitunas y tomate (sin carne).",
            12.00,
            R.drawable.producto_pizza_vegetariana,
            "pizza_vegetariana"
        ),
        Producto(
            20,
            "Pizzas",
            "BBQ de Pollo",
            "Pollo a la parrilla, salsa BBQ, cebolla morada y cilantro.",
            13.50,
            R.drawable.producto_pizza_bbq_pollo,
            "pizza_bbq_pollo"
        ),
        Producto(
            21,
            "Pizzas",
            "Suprema \"El Fogón\"",
            "Pepperoni, carne molida, champiñones, pimientos y cebolla — la más cargada.",
            14.50,
            R.drawable.producto_pizza_suprema_fogon,
            "pizza_suprema_fogon"
        ),
        Producto(
            22,
            "Pizzas",
            "Margarita",
            "Tomate fresco, albahaca y mozzarella — la más sencilla y clásica italiana.",
            11.50,
            R.drawable.producto_pizza_margarita,
            "pizza_margarita"
        ),
        Producto(
            23,
            "Pizzas",
            "Carnívora",
            "Pepperoni, tocino, chorizo y jamón — para los que aman la carne.",
            14.00,
            R.drawable.producto_pizza_carnivora,
            "pizza_carnivora"
        ),
        Producto(
            24,
            "Pizzas",
            "De Camarones",
            "Camarones, ajo, mozzarella y un toque de limón.",
            15.00,
            R.drawable.producto_pizza_camarones,
            "pizza_camarones"
        ),
        Producto(
            25,
            "Pizzas",
            "Champiñones y Tocino",
            "Champiñones salteados, tocino crujiente y queso mozzarella.",
            12.50,
            R.drawable.producto_pizza_champinones_tocino,
            "pizza_champinones_tocino"
        ),
        Producto(
            26,
            "Pizzas",
            "Diabla Picante",
            "Salami picante, jalapeños y salsa picante de la casa.",
            13.00,
            R.drawable.producto_pizza_diabla_picante,
            "pizza_diabla_picante"
        ),

        Producto(
            27,
            "Tacos",
            "Al Pastor",
            "4 tacos de carne de cerdo marinada en achiote, piña asada, cebolla y cilantro.",
            4.75,
            R.drawable.taco_al_pastor,
            "taco_al_pastor"
        ),
        Producto(
            28,
            "Tacos",
            "Carne Asada",
            "4 tacos de carne de res asada a la parrilla, cebolla y cilantro fresco.",
            4.50,
            R.drawable.taco_carne_asada,
            "taco_carne_asada"
        ),
        Producto(
            29,
            "Tacos",
            "Carnitas",
            "4 tacos de cerdo cocido a fuego lento y deshebrado, con cebolla y cilantro.",
            4.75,
            R.drawable.taco_carnitas,
            "taco_carnitas"
        ),
        Producto(
            30,
            "Tacos",
            "Suadero",
            "4 tacos de carne de res suave y jugosa, al estilo tradicional de taquería.",
            4.75,
            R.drawable.taco_suadero,
            "taco_suadero"
        ),
        Producto(
            31,
            "Tacos",
            "De Birria",
            "4 tacos de birria de res deshebrada con queso derretido, servidos con consomé para remojar.",
            5.25,
            R.drawable.taco_birria,
            "taco_birria"
        ),
        Producto(
            32,
            "Tacos",
            "De Pescado",
            "4 tacos de pescado empanizado, repollo curtido y salsa cremosa, estilo Baja.",
            5.00,
            R.drawable.taco_pescado,
            "taco_pescado"
        ),
        Producto(
            33,
            "Tacos",
            "De Camarón",
            "4 tacos de camarones a la parrilla, repollo morado y salsa chipotle.",
            5.50,
            R.drawable.taco_camaron,
            "taco_camaron"
        ),
        Producto(
            34,
            "Tacos",
            "De Chorizo con Papa",
            "4 tacos de chorizo frito con papa en cuadritos y cilantro.",
            4.25,
            R.drawable.taco_chorizo_papa,
            "taco_chorizo_papa"
        ),
        Producto(
            35,
            "Tacos",
            "Fritos de Pollo",
            "4 tacos enrollados y fritos, rellenos de pollo deshebrado, cubiertos de repollo y salsa de tomate.",
            4.00,
            R.drawable.taco_frito_pollo,
            "taco_frito_pollo"
        ),
        Producto(
            36,
            "Tacos",
            "Fritos de Res",
            "4 tacos enrollados y fritos, rellenos de carne de res deshebrada, cubiertos de repollo y salsa de tomate.",
            4.25,
            R.drawable.taco_frito_res,
            "taco_frito_res"
        ),
        Producto(
            37,
            "Tacos",
            "Fritos de Queso",
            "4 tacos enrollados y fritos, rellenos de queso, cubiertos de repollo y salsa de tomate.",
            3.75,
            R.drawable.taco_frito_queso,
            "taco_frito_queso"
        ),
        Producto(
            38,
            "Tacos",
            "Dorados Mixtos",
            "4 tacos fritos rellenos de pollo y carne de res, cubiertos de repollo, crema y salsa de tomate.",
            4.50,
            R.drawable.taco_dorado_mixto,
            "taco_dorado_mixto"
        ),

        Producto(
            39,
            "Bebidas",
            "Gaseosa de Cola",
            "Gaseosa sabor cola, servida bien fría. Presentación de 16 oz.",
            1.50,
            R.drawable.bebida_gaseosa_cola,
            "bebida_gaseosa_cola"
        ),
        Producto(
            40,
            "Bebidas",
            "Gaseosa de Cola Light",
            "Gaseosa sabor cola sin azúcar, servida bien fría. Presentación de 16 oz.",
            1.50,
            R.drawable.bebida_gaseosa_cola_light,
            "bebida_gaseosa_cola_light"
        ),
        Producto(
            41,
            "Bebidas",
            "Gaseosa Lima-Limón",
            "Gaseosa sabor lima-limón, servida bien fría. Presentación de 16 oz.",
            1.50,
            R.drawable.bebida_gaseosa_lima_limon,
            "bebida_gaseosa_lima_limon"
        ),
        Producto(
            42,
            "Bebidas",
            "Gaseosa de Naranja",
            "Gaseosa sabor naranja, servida bien fría. Presentación de 16 oz.",
            1.50,
            R.drawable.bebida_gaseosa_naranja,
            "bebida_gaseosa_naranja"
        ),
        Producto(
            43,
            "Bebidas",
            "Horchata",
            "Bebida tradicional de arroz, canela y vainilla. Presentación de 16 oz.",
            1.75,
            R.drawable.bebida_horchata,
            "bebida_horchata"
        ),
        Producto(
            44,
            "Bebidas",
            "Refresco de Tamarindo",
            "Refresco natural hecho de tamarindo. Presentación de 16 oz.",
            1.75,
            R.drawable.bebida_tamarindo,
            "bebida_tamarindo"
        ),
        Producto(
            45,
            "Bebidas",
            "Refresco de Jamaica",
            "Refresco natural de flor de jamaica (hibisco). Presentación de 16 oz.",
            1.75,
            R.drawable.bebida_jamaica,
            "bebida_jamaica"
        ),
        Producto(
            46,
            "Bebidas",
            "Limonada",
            "Limonada clásica, fresca y natural. Presentación de 16 oz.",
            1.50,
            R.drawable.bebida_limonada,
            "bebida_limonada"
        ),
        Producto(
            47,
            "Bebidas",
            "Limonada con Hierbabuena",
            "Limonada con un toque de menta fresca. Presentación de 16 oz.",
            1.75,
            R.drawable.bebida_limonada_hierbabuena,
            "bebida_limonada_hierbabuena"
        ),
        Producto(
            48,
            "Bebidas",
            "Agua Fresca de Piña",
            "Agua fresca natural de piña. Presentación de 16 oz.",
            1.75,
            R.drawable.bebida_agua_pina,
            "bebida_agua_pina"
        ),
        Producto(
            49,
            "Bebidas",
            "Té Helado",
            "Té negro helado, ligeramente endulzado. Presentación de 16 oz.",
            1.50,
            R.drawable.bebida_te_helado,
            "bebida_te_helado"
        ),
        Producto(
            50,
            "Bebidas",
            "Agua Embotellada",
            "Agua pura sin gas. Presentación de 16 oz.",
            1.00,
            R.drawable.bebida_agua_embotellada,
            "bebida_agua_embotellada"
        ),

        Producto(
            51,
            "Postres",
            "Tres Leches",
            "Pastel esponjoso bañado en tres tipos de leche, cubierto con crema chantilly casera.",
            3.00,
            R.drawable.postre_tres_leches,
            "postre_tres_leches"
        ),
        Producto(
            52,
            "Postres",
            "Flan de Caramelo",
            "Flan casero suave, bañado en salsa de caramelo.",
            2.75,
            R.drawable.postre_flan_caramelo,
            "postre_flan_caramelo"
        ),
        Producto(
            53,
            "Postres",
            "Pastel de Chocolate",
            "Pastel húmedo de chocolate con ganache y chispas de chocolate.",
            3.25,
            R.drawable.postre_pastel_chocolate,
            "postre_pastel_chocolate"
        ),
        Producto(
            54,
            "Postres",
            "Churros con Chocolate",
            "Churros crujientes espolvoreados con azúcar y canela, con salsa de chocolate para remojar.",
            2.50,
            R.drawable.postre_churros_chocolate,
            "postre_churros_chocolate"
        ),
        Producto(
            55,
            "Postres",
            "Cheesecake de Fresa",
            "Cheesecake cremoso con cobertura de fresas frescas y mermelada.",
            3.50,
            R.drawable.postre_cheesecake_fresa,
            "postre_cheesecake_fresa"
        ),
        Producto(
            56,
            "Postres",
            "Semita de Piña",
            "Pan dulce tradicional salvadoreño relleno de mermelada de piña.",
            2.00,
            R.drawable.postre_semita_pina,
            "postre_semita_pina"
        ),

        Producto(
            57,
            "Burritos",
            "Burrito de Res 5 Capas",
            "Carne de res, arroz, frijoles refritos, queso derretido y pico de gallo.",
            4.50,
            R.drawable.burrito_res_5capas,
            "burrito_res_5capas"
        ),
        Producto(
            58,
            "Burritos",
            "Burrito de Pollo",
            "Pollo a la parrilla, arroz, frijoles, queso y pico de gallo.",
            4.25,
            R.drawable.burrito_pollo,
            "burrito_pollo"
        ),
        Producto(
            59,
            "Burritos",
            "Burrito de Carnitas",
            "Cerdo deshebrado, arroz, frijoles, queso y salsa verde.",
            4.50,
            R.drawable.burrito_carnitas,
            "burrito_carnitas"
        ),
        Producto(
            60,
            "Burritos",
            "Burrito Vegetariano",
            "Arroz, frijoles, queso, pico de gallo, lechuga y guacamole (sin carne).",
            4.00,
            R.drawable.burrito_vegetariano,
            "burrito_vegetariano"
        ),
        Producto(
            61,
            "Burritos",
            "Burrito Supremo \"El Fogón\"",
            "Doble carne de res, tocino, arroz, frijoles, queso derretido y guacamole — el más cargado.",
            5.50,
            R.drawable.burrito_supremo_fogon,
            "burrito_supremo_fogon"
        ),

        Producto(
            62,
            "Tortas",
            "Torta de Birria",
            "Birria de res deshebrada con queso derretido y consomé para remojar.",
            3.75,
            R.drawable.torta_birria,
            "torta_birria"
        ),
        Producto(
            63,
            "Tortas",
            "Torta de Milanesa",
            "Milanesa de res empanizada, lechuga, tomate, aguacate y mayonesa.",
            4.00,
            R.drawable.torta_milanesa,
            "torta_milanesa"
        ),
        Producto(
            64,
            "Tortas",
            "Torta de Jamón y Queso",
            "Jamón, queso amarillo, lechuga, tomate y mayonesa.",
            3.00,
            R.drawable.torta_jamon_queso,
            "torta_jamon_queso"
        ),
        Producto(
            65,
            "Tortas",
            "Torta de Pierna",
            "Pierna de cerdo adobada, frijoles refritos, lechuga y aguacate.",
            3.75,
            R.drawable.torta_pierna,
            "torta_pierna"
        ),
        Producto(
            66,
            "Tortas",
            "Torta Cubana \"El Fogón\"",
            "Milanesa, jamón, queso, chorizo y huevo — la más cargada de todas.",
            4.50,
            R.drawable.torta_cubana_fogon,
            "torta_cubana_fogon"
        )



        // Burritos y Tortas se agregan igual, cuando decidan el menú de cada categoría.
    )

    fun porCategoria(categoria: String): List<Producto> =
        productos.filter { it.categoria == categoria }

    fun buscar(categoria: String, consulta: String): List<Producto> {
        if (consulta.isBlank()) return porCategoria(categoria)
        val texto = consulta.trim().lowercase()
        return porCategoria(categoria).filter {
            it.nombre.lowercase().contains(texto) || it.descripcion.lowercase().contains(texto)
        }
    }
}