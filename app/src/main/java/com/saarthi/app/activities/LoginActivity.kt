package com.saarthi.app.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.saarthi.app.data.UserSession

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val session = UserSession(this)

        // Auto login
        if (session.isLoggedIn()) {
            goDashboard()
        } else {
            session.saveSession("akash001", "demo_token")
            goDashboard()
        }
    }

    private fun goDashboard() {
        startActivity(
            Intent(this, DashboardActivity::class.java)
        )
        finish()
    }
}