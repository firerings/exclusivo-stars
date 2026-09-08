package com.exclusivostars.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button

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
