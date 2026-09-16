package com.exclusivostars.app.ui.series

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.exclusivostars.app.R
import com.exclusivostars.app.model.Serie
import com.exclusivostars.app.util.Etiqueta

/** Mismo patrón exacto que PeliculasAdapter, con "Serie" fijo como tipo. */
class SeriesAdapter(
    private val onClick: (Serie) -> Unit,
) : RecyclerView.Adapter<SeriesAdapter.VH>() {

    private val items = mutableListOf<Serie>()

    fun submitList(nuevas: List<Serie>) {
        items.clear()
        items.addAll(nuevas)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pelicula, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val serie = items[position]
        holder.titulo.text = serie.titulo
        holder.etiqueta.text = Etiqueta.formatear(serie.pais, serie.anio, "Serie")

        Glide.with(holder.poster)
            .load(serie.posterUrl)
            .placeholder(R.drawable.poster_placeholder_background)
            .error(R.drawable.poster_placeholder_background)
            .centerCrop()
            .into(holder.poster)

        holder.itemView.setOnClickListener { onClick(serie) }
    }

    override fun getItemCount(): Int = items.size

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val poster: ImageView = view.findViewById(R.id.poster)
        val etiqueta: TextView = view.findViewById(R.id.etiqueta)
        val titulo: TextView = view.findViewById(R.id.titulo)
    }
}
