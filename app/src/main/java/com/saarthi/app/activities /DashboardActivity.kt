package com.saarthi.app.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Direct Advice screen for now
        startActivity(
            Intent(this, AdviceActivity::class.java)
        )
    }
}