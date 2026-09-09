package com.exclusivostars.app.ui.inicio

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.exclusivostars.app.R
import com.exclusivostars.app.model.Pelicula

/**
 * ViewPager2 (que por debajo es un RecyclerView) con las candidatas a
 * hero — mismo criterio de imagen que el hero de la web (fondo >
 * tarjeta > poster, ver fondo_url en api_peliculas). No hay endpoint
 * propio de "hero" en el backend: se arman las candidatas del lado de
 * la app tomando las primeras películas con fondo_url, sin el
 * ordenamiento "con logo primero" que hace _construir_hero() en la web
 * porque la app todavía no recibe tiene_logo — se puede sumar después
 * si hace falta.
 */
class HeroAdapter(
    private val onClick: (Pelicula) -> Unit,
) : RecyclerView.Adapter<HeroAdapter.VH>() {

    private val items = mutableListOf<Pelicula>()

    fun submitList(nuevas: List<Pelicula>) {
        items.clear()
        items.addAll(nuevas)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hero, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val pelicula = items[position]
        holder.titulo.text = pelicula.titulo

        Glide.with(holder.fondo)
            .load(pelicula.fondoUrl ?: pelicula.posterUrl)
            .centerCrop()
            .into(holder.fondo)

        holder.itemView.setOnClickListener { onClick(pelicula) }
    }

    override fun getItemCount(): Int = items.size

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val fondo: ImageView = view.findViewById(R.id.fondo)
        val titulo: TextView = view.findViewById(R.id.titulo)
    }
}
