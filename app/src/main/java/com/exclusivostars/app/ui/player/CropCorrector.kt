package com.exclusivostars.app.ui.player

import android.graphics.Matrix
import android.view.TextureView
import androidx.media3.ui.PlayerView
import com.exclusivostars.app.model.CropBarras

/**
 * Replica exacta de la corrección de crop de `player.js` (web), pero
 * con `TextureView.setTransform(Matrix)` en vez de CSS `transform`.
 *
 * Fórmula (idéntica a la web):
 *   scaleX = 1 / (1 - left - right)
 *   scaleY = 1 / (1 - top - bottom)
 *   shiftX% = (left - right) / 2   (relativo al ancho del propio frame)
 *   shiftY% = (top - bottom) / 2   (relativo al alto del propio frame)
 *
 * CSS aplica `scale(...) translate(...)`, que compone como
 * "trasladar primero (en unidades del frame original), después
 * escalar alrededor del centro". En Matrix de Android eso es
 * setScale(centrado) + preTranslate (preTranslate mete la traslación
 * ANTES de la escala ya existente en la matriz, mismo orden que CSS).
 *
 * Requiere que el PlayerView tenga `app:surface_type="texture_view"`
 * — es el único surface type de Media3 que soporta Matrix.
 */
object CropCorrector {

    /**
     * Aplica (o remueve) la corrección sobre el TextureView interno del
     * PlayerView. Hay que llamarla de nuevo cada vez que el tamaño del
     * TextureView cambia (primer layout, rotación, resize del
     * contenedor) porque el shift en píxeles depende de ese tamaño —
     * por eso se engancha a un OnLayoutChangeListener en vez de
     * aplicarse una sola vez.
     */
    fun instalar(playerView: PlayerView, crop: CropBarras?) {
        val textureView = playerView.videoSurfaceView as? TextureView ?: return

        fun aplicar() {
            val w = textureView.width.toFloat()
            val h = textureView.height.toFloat()
            if (w <= 0f || h <= 0f) return
            if (crop == null || crop.esNulo()) {
                textureView.setTransform(Matrix())
                return
            }
            val scaleX = 1f / (1f - crop.left - crop.right)
            val scaleY = 1f / (1f - crop.top - crop.bottom)
            val shiftX = ((crop.left - crop.right) / 2f) * w
            val shiftY = ((crop.top - crop.bottom) / 2f) * h

            val matrix = Matrix()
            matrix.setScale(scaleX, scaleY, w / 2f, h / 2f)
            matrix.preTranslate(shiftX, shiftY)
            textureView.setTransform(matrix)
        }

        aplicar()
        textureView.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) aplicar()
        }
    }
}
