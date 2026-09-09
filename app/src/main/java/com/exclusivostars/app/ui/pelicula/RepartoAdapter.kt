package com.exclusivostars.app.ui.pelicula

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.exclusivostars.app.R
import com.exclusivostars.app.model.Actor

class RepartoAdapter(private val reparto: List<Actor>) : RecyclerView.Adapter<RepartoAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_actor, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val actor = reparto[position]
        holder.nombre.text = actor.nombre

        if (actor.fotoUrl != null) {
            Glide.with(holder.foto)
                .load(actor.fotoUrl)
                .placeholder(R.drawable.actor_foto_placeholder_background)
                .error(R.drawable.actor_foto_placeholder_background)
                .centerCrop()
                .into(holder.foto)
        } else {
            // Sin perfil/foto todavía en personas/ — mismo criterio que
            // pelicula.html: placeholder liso, sin romper el layout.
            holder.foto.setImageDrawable(null)
            holder.foto.setBackgroundResource(R.drawable.actor_foto_placeholder_background)
        }
    }

    override fun getItemCount(): Int = reparto.size

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val foto: ImageView = view.findViewById(R.id.foto)
        val nombre: TextView = view.findViewById(R.id.nombre)
    }
}
