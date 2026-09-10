package com.exclusivostars.app.ui.inicio

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.exclusivostars.app.R

/** Mismo patrón que PeliculasAdapter, pero genérico vía FilaItem para que
 *  una misma clase sirva tanto para la fila de Películas como la de Series
 *  (y cualquier fila nueva que se sume después). */
class FilaAdapter : RecyclerView.Adapter<FilaAdapter.VH>() {

    private val items = mutableListOf<FilaItem>()

    fun submitList(nuevos: List<FilaItem>) {
        items.clear()
        items.addAll(nuevos)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_fila_poster, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.titulo.text = item.titulo
        holder.anio.text = listOfNotNull(item.subtitulo, item.tipo).joinToString(" · ")

        Glide.with(holder.poster)
            .load(item.posterUrl)
            .placeholder(R.drawable.poster_placeholder_background)
            .error(R.drawable.poster_placeholder_background)
            .centerCrop()
            .into(holder.poster)

        holder.itemView.setOnClickListener { item.onClick() }
    }

    override fun getItemCount(): Int = items.size

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val poster: ImageView = view.findViewById(R.id.poster)
        val titulo: TextView = view.findViewById(R.id.titulo)
        val anio: TextView = view.findViewById(R.id.anio)
    }
}
