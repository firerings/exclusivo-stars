package com.exclusivostars.app.ui.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.exclusivostars.app.R
import com.exclusivostars.app.ui.auth.MainActivity

class HomeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        findViewById<Button>(R.id.logout_button).setOnClickListener {
            MainActivity.cookieManager.cookieStore.removeAll()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
