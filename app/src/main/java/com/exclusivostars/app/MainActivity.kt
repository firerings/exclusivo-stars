package com.exclusivostars.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import java.io.OutputStreamWriter
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MainActivity : Activity() {

    private val main = Handler(Looper.getMainLooper())

    companion object {
        // Una sola CookieManager para toda la app: guarda la cookie de
        // sesión que devuelve Flask (login/CSRF) y la reusa en el
        // resto de pantallas mientras dure la sesión.
        val cookieManager: CookieManager by lazy {
            CookieManager(null, CookiePolicy.ACCEPT_ALL).also {
                CookieHandler.setDefault(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cookieManager // fuerza la inicialización (y el setDefault) apenas arranca la pantalla

        val email = findViewById<EditText>(R.id.username)
        val password = findViewById<EditText>(R.id.password)
        val remember = findViewById<CheckBox>(R.id.remember)
        val errorBox = findViewById<TextView>(R.id.error_box)
        val progress = findViewById<ProgressBar>(R.id.progress)
        val loginButton = findViewById<Button>(R.id.login_button)

        loginButton.setOnClickListener {
            val emailValue = email.text.toString().trim()
            val passwordValue = password.text.toString()

            if (emailValue.isEmpty() || passwordValue.isEmpty()) {
                showError(errorBox, "Completa correo y contraseña.")
                return@setOnClickListener
            }

            errorBox.visibility = View.GONE
            progress.visibility = View.VISIBLE
            loginButton.isEnabled = false

            Thread {
                val error = doLogin(emailValue, passwordValue, remember.isChecked)
                main.post {
                    progress.visibility = View.GONE
                    loginButton.isEnabled = true
                    if (error == null) {
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    } else {
                        showError(errorBox, error)
                    }
                }
            }.start()
        }

        findViewById<Button>(R.id.register_button).setOnClickListener {
            Toast.makeText(this, "Registro próximamente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showError(errorBox: TextView, message: String) {
        errorBox.text = message
        errorBox.visibility = View.VISIBLE
    }

    /** Corre en background. Devuelve null si el login fue exitoso, o un mensaje de error. */
    private fun doLogin(email: String, password: String, remember: Boolean): String? {
        return try {
            // 1) GET /login: la cookie de sesión inicial + el csrf_token
            //    que exige el formulario (mismo que usa el navegador).
            val getConn = URL(Config.BASE_URL + "/login").openConnection() as HttpURLConnection
            getConn.connectTimeout = 8000
            getConn.readTimeout = 8000
            val html = getConn.inputStream.bufferedReader().readText()
            getConn.disconnect()

            val csrf = Regex("name=\"csrf_token\" value=\"([^\"]+)\"").find(html)?.groupValues?.get(1)
                ?: return "El servidor respondió pero no encontré el csrf_token. ¿Es la URL correcta?"

            // 2) POST /login con los mismos campos que el form web.
            val postConn = URL(Config.BASE_URL + "/login").openConnection() as HttpURLConnection
            postConn.requestMethod = "POST"
            postConn.instanceFollowRedirects = false
            postConn.doOutput = true
            postConn.connectTimeout = 8000
            postConn.readTimeout = 8000
            postConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

            val body = buildString {
                append("csrf_token=").append(URLEncoder.encode(csrf, "UTF-8"))
                append("&email=").append(URLEncoder.encode(email, "UTF-8"))
                append("&password=").append(URLEncoder.encode(password, "UTF-8"))
                if (remember) append("&remember=on")
            }
            OutputStreamWriter(postConn.outputStream).use { it.write(body) }

            val code = postConn.responseCode
            if (code == 302) {
                // Flask solo redirige a "/" cuando el login fue exitoso.
                postConn.disconnect()
                return null
            }

            val errorHtml = (postConn.errorStream ?: postConn.inputStream).bufferedReader().readText()
            postConn.disconnect()

            Regex("error-box\">⚠ ([^<]+)<").find(errorHtml)?.groupValues?.get(1)?.trim()
                ?: "Email o contraseña incorrectos."
        } catch (e: Exception) {
            "No se pudo conectar con ${Config.BASE_URL}. ¿Está corriendo el servidor?"
        }
    }
}
