package com.exclusivostars.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy

/** Qué formulario está activo en la pantalla de tabs. */
private enum class Tab { LOGIN, REGISTER }

class MainActivity : Activity() {

    private val main = Handler(Looper.getMainLooper())
    private var activeTab = Tab.LOGIN

    companion object {
        // Una sola CookieManager para toda la app: guarda la cookie de
        // sesión que devuelve Flask y la reusa en el resto de pantallas
        // mientras dure la sesión.
        val cookieManager: CookieManager by lazy {
            CookieManager(null, CookiePolicy.ACCEPT_ALL).also {
                CookieHandler.setDefault(it)
            }
        }
    }

    private lateinit var tabLogin: TextView
    private lateinit var tabRegister: TextView
    private lateinit var indicator: View
    private lateinit var loginGroup: ViewGroup
    private lateinit var registerGroup: ViewGroup
    private lateinit var errorBox: TextView
    private lateinit var progress: ProgressBar
    private lateinit var submitButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cookieManager // fuerza la inicialización (y el setDefault) apenas arranca la pantalla

        val logo = findViewById<ImageView>(R.id.logo_star)
        val brand = findViewById<TextView>(R.id.brand_name)
        tabLogin = findViewById(R.id.tab_login)
        tabRegister = findViewById(R.id.tab_register)
        indicator = findViewById(R.id.tab_indicator)
        loginGroup = findViewById(R.id.login_group)
        registerGroup = findViewById(R.id.register_group)
        errorBox = findViewById(R.id.error_box)
        progress = findViewById(R.id.progress)
        submitButton = findViewById(R.id.submit_button)

        tabLogin.setOnClickListener { switchTab(Tab.LOGIN) }
        tabRegister.setOnClickListener { switchTab(Tab.REGISTER) }
        submitButton.setOnClickListener { onSubmit() }

        // Entrada del logo: fade + escala, como una versión corta del splash de la web.
        logo.alpha = 0f
        logo.scaleX = 0.6f
        logo.scaleY = 0.6f
        brand.alpha = 0f
        logo.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(420)
            .setInterpolator(OvershootInterpolator(1.6f))
            .start()
        brand.animate().alpha(1f).setStartDelay(150).setDuration(300).start()

        // Los indicadores necesitan que las tabs ya estén medidas.
        tabLogin.post {
            indicator.layoutParams.width = tabLogin.width
            indicator.requestLayout()
            animateFieldsIn(loginGroup)
        }
    }

    private fun switchTab(tab: Tab) {
        if (tab == activeTab) return
        activeTab = tab

        val (fromGroup, toGroup, fromTab, toTab, indicatorTarget) = when (tab) {
            Tab.LOGIN -> ToTabState(registerGroup, loginGroup, tabRegister, tabLogin, tabLogin)
            Tab.REGISTER -> ToTabState(loginGroup, registerGroup, tabLogin, tabRegister, tabRegister)
        }

        errorBox.visibility = View.GONE

        // Indicador deslizante entre "Iniciar sesión" / "Crear cuenta".
        indicator.animate()
            .x(indicatorTarget.left.toFloat())
            .setDuration(250)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
        fromTab.animate().setDuration(200).start()
        toTab.setTextColor(resources.getColor(R.color.text_primary, theme))
        fromTab.setTextColor(resources.getColor(R.color.text_secondary, theme))

        // Crossfade entre los dos formularios.
        fromGroup.animate().alpha(0f).setDuration(150).withEndAction {
            fromGroup.visibility = View.GONE
        }.start()
        toGroup.visibility = View.VISIBLE
        toGroup.alpha = 0f
        toGroup.animate().alpha(1f).setDuration(200).setStartDelay(100).start()
        animateFieldsIn(toGroup)

        submitButton.text = if (tab == Tab.LOGIN) getString(R.string.login) else getString(R.string.register)
    }

    private data class ToTabState(
        val fromGroup: ViewGroup,
        val toGroup: ViewGroup,
        val fromTab: TextView,
        val toTab: TextView,
        val indicatorTarget: View,
    )

    /** Cascada fade-up de los campos de un grupo, como en el CSS de la web. */
    private fun animateFieldsIn(group: ViewGroup) {
        for (i in 0 until group.childCount) {
            val child = group.getChildAt(i)
            child.alpha = 0f
            child.translationY = 24f
            child.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay(i * 60L)
                .setDuration(260)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }

    private fun showError(message: String) {
        errorBox.text = message
        if (errorBox.visibility != View.VISIBLE) {
            errorBox.alpha = 0f
            errorBox.visibility = View.VISIBLE
            errorBox.animate().alpha(1f).setDuration(200).start()
        }
    }

    private fun onSubmit() {
        when (activeTab) {
            Tab.LOGIN -> onLogin()
            Tab.REGISTER -> onRegister()
        }
    }

    private fun onLogin() {
        val email = findViewById<EditText>(R.id.login_email).text.toString().trim()
        val password = findViewById<EditText>(R.id.login_password).text.toString()
        val remember = findViewById<CheckBox>(R.id.remember).isChecked

        if (email.isEmpty() || password.isEmpty()) {
            showError("Completa correo y contraseña.")
            return
        }

        setLoading(true)
        Thread {
            val result = AuthApi.login(email, password, remember)
            main.post {
                setLoading(false)
                if (result.ok) {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                } else {
                    showError(result.error ?: "Email o contraseña incorrectos.")
                }
            }
        }.start()
    }

    private fun onRegister() {
        val name = findViewById<EditText>(R.id.register_name).text.toString().trim()
        val email = findViewById<EditText>(R.id.register_email).text.toString().trim()
        val password = findViewById<EditText>(R.id.register_password).text.toString()
        val confirm = findViewById<EditText>(R.id.register_confirm).text.toString()

        // Mismas validaciones que auth/routes.py, reflejadas acá para no
        // gastar un request en errores obvios (el backend las repite igual).
        when {
            name.isEmpty() -> { showError("Falta tu nombre."); return }
            email.isEmpty() || !email.contains("@") -> { showError("Poné un email válido."); return }
            password.length < 8 -> { showError("La contraseña necesita al menos 8 caracteres."); return }
            password != confirm -> { showError("Las contraseñas no coinciden."); return }
        }

        setLoading(true)
        Thread {
            val result = AuthApi.register(name, email, password, confirm)
            main.post {
                setLoading(false)
                if (result.ok) {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                } else {
                    showError(result.error ?: "No se pudo crear la cuenta.")
                }
            }
        }.start()
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        submitButton.isEnabled = !loading
        tabLogin.isEnabled = !loading
        tabRegister.isEnabled = !loading
    }
}
