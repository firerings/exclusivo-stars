package com.exclusivostars.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.exclusivostars.app.MediaflixApp
import com.exclusivostars.app.R
import com.exclusivostars.app.network.SessionApi
import com.exclusivostars.app.ui.auth.MainActivity
import com.exclusivostars.app.ui.buscar.BuscarActivity
import com.exclusivostars.app.ui.common.ComingSoonFragment
import com.exclusivostars.app.ui.inicio.InicioFragment
import com.exclusivostars.app.ui.peliculas.PeliculasFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Punto de entrada real después del login/registro. Una sola barra
 * superior (logo + título de sección + buscador + avatar) y un bottom
 * nav que cambia qué Fragment se ve en fragment_container — nada de
 * Activities sueltas por sección, así el header no se repite ni hay
 * que reconstruirlo en cada pantalla nueva.
 */
class HomeActivity : AppCompatActivity() {

    private val main = Handler(Looper.getMainLooper())
    private var nombreCuenta: String = ""
    private var emailCuenta: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val sectionTitle = findViewById<TextView>(R.id.section_title)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        val avatarButton = findViewById<TextView>(R.id.avatar_button)

        findViewById<ImageButton>(R.id.search_button).setOnClickListener {
            startActivity(Intent(this, BuscarActivity::class.java))
        }

        avatarButton.setOnClickListener { mostrarDialogoCuenta() }
        cargarCuenta(avatarButton)

        bottomNav.setOnItemSelectedListener { item ->
            val (fragment, title) = when (item.itemId) {
                R.id.nav_inicio -> InicioFragment() to getString(R.string.nav_inicio)
                R.id.nav_peliculas -> PeliculasFragment() to getString(R.string.nav_peliculas)
                R.id.nav_series -> ComingSoonFragment.nuevaInstancia(getString(R.string.proximamente_series)) to getString(R.string.nav_series)
                R.id.nav_reels -> ComingSoonFragment.nuevaInstancia(getString(R.string.proximamente_reels)) to getString(R.string.nav_reels)
                R.id.nav_musica -> ComingSoonFragment.nuevaInstancia(getString(R.string.proximamente_musica)) to getString(R.string.nav_musica)
                else -> return@setOnItemSelectedListener false
            }
            sectionTitle.text = title
            mostrarFragment(fragment)
            true
        }

        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_inicio
        }
    }

    private fun mostrarFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    /** GET /api/me — la sesión vive en la cookie, no en un objeto de
     * usuario guardado del lado de la app, así que hay que pedirla
     * para saber qué inicial/nombre pintar en el avatar (mismo
     * criterio que _perfil_menu.html en la web). */
    private fun cargarCuenta(avatarButton: TextView) {
        Thread {
            val result = SessionApi.me()
            main.post {
                if (isFinishing) return@post
                if (result is SessionApi.Result.Ok) {
                    nombreCuenta = result.nombre
                    emailCuenta = result.email
                    val inicial = (result.nombre.ifBlank { result.email }).take(1).uppercase()
                    avatarButton.text = inicial
                }
                // Si falla (sin red, 401, etc.) el avatar queda sin
                // inicial pero sigue abriendo el diálogo — mejor eso
                // que trabar el header por un dato que no es crítico.
            }
        }.start()
    }

    private fun mostrarDialogoCuenta() {
        val vista = layoutInflater.inflate(R.layout.dialog_perfil, null)
        vista.findViewById<TextView>(R.id.perfil_nombre).text = nombreCuenta.ifBlank { getString(R.string.cuenta) }
        vista.findViewById<TextView>(R.id.perfil_email).text = emailCuenta

        val dialogo = AlertDialog.Builder(this)
            .setView(vista)
            .create()

        vista.findViewById<Button>(R.id.perfil_logout).setOnClickListener {
            dialogo.dismiss()
            MediaflixApp.cookieManager.cookieStore.removeAll()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        dialogo.show()
    }
}
