package com.exclusivostars.app.ui.peliculas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.exclusivostars.app.R
import com.exclusivostars.app.model.Pelicula

class PeliculasAdapter(
    private val onClick: (Pelicula) -> Unit,
) : RecyclerView.Adapter<PeliculasAdapter.VH>() {

    private val items = mutableListOf<Pelicula>()

    fun submitList(nuevas: List<Pelicula>) {
        items.clear()
        items.addAll(nuevas)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pelicula, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val pelicula = items[position]
        holder.titulo.text = pelicula.titulo
        holder.anio.text = listOfNotNull(pelicula.anio, "Película").joinToString(" · ")

        Glide.with(holder.poster)
            .load(pelicula.posterUrl)
            .placeholder(R.drawable.poster_placeholder_background)
            .error(R.drawable.poster_placeholder_background)
            .centerCrop()
            .into(holder.poster)

        holder.itemView.setOnClickListener { onClick(pelicula) }
    }

    override fun getItemCount(): Int = items.size

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val poster: ImageView = view.findViewById(R.id.poster)
        val titulo: TextView = view.findViewById(R.id.titulo)
        val anio: TextView = view.findViewById(R.id.anio)
    }
}
