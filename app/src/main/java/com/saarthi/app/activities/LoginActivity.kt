package com.saarthi.app.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.saarthi.app.data.UserSession
import com.saarthi.app.ui.HackerDashboard   // ✅ IMPORT ADDED

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val session = UserSession(this)

        // ✅ Auto Login System
        if (session.isLoggedIn()) {

            // Already logged in → Open Hacker Panel
            goHackerDashboard()

        } else {

            // First time login (demo)
            session.saveSession("akash001", "demo_token")

            // Open Hacker Panel
            goHackerDashboard()
        }
    }

    // ✅ Open Hacker Dashboard
    private fun goHackerDashboard() {

        startActivity(
            Intent(this, HackerDashboard::class.java)
        )

        finish()
    }
}
