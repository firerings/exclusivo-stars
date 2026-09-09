package com.exclusivostars.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.exclusivostars.app.MediaflixApp
import com.exclusivostars.app.R
import com.exclusivostars.app.ui.auth.MainActivity
import com.exclusivostars.app.ui.common.ComingSoonFragment
import com.exclusivostars.app.ui.peliculas.PeliculasFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Punto de entrada real después del login/registro. Una sola barra
 * superior (logo + título de sección + logout) y un bottom nav que
 * cambia qué Fragment se ve en fragment_container — nada de Activities
 * sueltas por sección, así el logout y el header no se repiten ni hay
 * que reconstruirlos en cada pantalla nueva.
 */
class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val sectionTitle = findViewById<TextView>(R.id.section_title)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

        findViewById<ImageButton>(R.id.logout_button).setOnClickListener {
            MediaflixApp.cookieManager.cookieStore.removeAll()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        bottomNav.setOnItemSelectedListener { item ->
            val (fragment, title) = when (item.itemId) {
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
            bottomNav.selectedItemId = R.id.nav_peliculas
        }
    }

    private fun mostrarFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
