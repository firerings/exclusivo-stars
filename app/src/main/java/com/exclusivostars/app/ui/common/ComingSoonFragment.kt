package com.exclusivostars.app.ui.common

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.exclusivostars.app.R

/**
 * Placeholder para pestañas del bottom nav que todavía no tienen
 * pantalla propia. Nada de datos ni de red acá — reemplazar por el
 * Fragment real de la sección (SeriesFragment, ReelsFragment,
 * MusicaFragment) cuando exista, sin tocar HomeActivity más que el
 * `when` que decide qué Fragment mostrar por cada ítem del bottom nav.
 */
class ComingSoonFragment : Fragment(R.layout.fragment_coming_soon) {

    companion object {
        private const val ARG_MENSAJE = "mensaje"

        fun nuevaInstancia(mensaje: String) = ComingSoonFragment().apply {
            arguments = Bundle().apply { putString(ARG_MENSAJE, mensaje) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.coming_soon_message).text =
            arguments?.getString(ARG_MENSAJE).orEmpty()
    }
}
