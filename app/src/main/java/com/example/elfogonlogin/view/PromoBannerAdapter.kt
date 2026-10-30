package com.example.elfogonlogin.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.elfogonlogin.R

data class Promocion(val titulo: String, val subtitulo: String, val imagenResId: Int)

class PromoBannerAdapter(
    private val promociones: List<Promocion>
) : RecyclerView.Adapter<PromoBannerAdapter.PromoViewHolder>() {

    inner class PromoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fondo: ImageView = itemView.findViewById(R.id.vFondoPromo)
        val titulo: TextView = itemView.findViewById(R.id.tvPromoTitulo)
        val subtitulo: TextView = itemView.findViewById(R.id.tvPromoSubtitulo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PromoViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_promo_banner, parent, false)
        return PromoViewHolder(vista)
    }

    override fun onBindViewHolder(holder: PromoViewHolder, position: Int) {
        val promo = promociones[position]
        holder.titulo.text = promo.titulo
        holder.subtitulo.text = promo.subtitulo
        holder.fondo.setImageResource(promo.imagenResId)
    }

    override fun getItemCount(): Int = promociones.size
}