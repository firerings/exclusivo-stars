package com.exclusivostars.app.ui.inicio

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
 * la app tomando las primeras películas con fondo_url. El orden "con
 * logo primero" (mismo criterio que _construir_hero en la web) lo
 * decide InicioFragment antes de llamar a submitList; este adapter solo
 * pinta lo que recibe.
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

        Glide.with(holder.fondo)
            .load(pelicula.fondoUrl ?: pelicula.posterUrl)
            .centerCrop()
            .into(holder.fondo)

        // Logo transparente (como Netflix) en vez de título en texto
        // plano cuando la película tiene logo.png — mismo criterio que
        // el hero de la web (tiene_logo).
        if (pelicula.logoUrl != null) {
            holder.titulo.visibility = View.GONE
            holder.logo.visibility = View.VISIBLE
            Glide.with(holder.logo)
                .load(pelicula.logoUrl)
                .fitCenter()
                .into(holder.logo)
        } else {
            holder.logo.visibility = View.GONE
            holder.titulo.visibility = View.VISIBLE
            holder.titulo.text = pelicula.titulo
        }

        // Meta (año · duración · Película), mismo orden que #hero-meta en
        // la web — "Película" siempre presente, año/duración solo si hay dato.
        val partesMeta = buildList {
            pelicula.anio?.let { add(it) }
            pelicula.duracion?.let { add(it) }
            add("Película")
        }
        holder.meta.text = partesMeta.joinToString(" · ")

        holder.sinopsis.text = pelicula.sinopsis
            ?: "Descubre esta película de tu biblioteca en una experiencia de streaming."

        // Reproducir y + Mi lista apuntan los dos a la ficha de la
        // película — mismo criterio que la web, donde "+ Mi lista"
        // todavía no es una lista real, solo otro link a hero.url.
        holder.itemView.setOnClickListener { onClick(pelicula) }
        holder.botonReproducir.setOnClickListener { onClick(pelicula) }
        holder.botonMiLista.setOnClickListener { onClick(pelicula) }
    }

    override fun getItemCount(): Int = items.size

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val fondo: ImageView = view.findViewById(R.id.fondo)
        val titulo: TextView = view.findViewById(R.id.titulo)
        val logo: ImageView = view.findViewById(R.id.logo)
        val meta: TextView = view.findViewById(R.id.meta)
        val sinopsis: TextView = view.findViewById(R.id.sinopsis)
        val botonReproducir: Button = view.findViewById(R.id.boton_reproducir)
        val botonMiLista: Button = view.findViewById(R.id.boton_mi_lista)
    }
}
