package com.saarthi.app.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.saarthi.app.network.ApiClient
import com.saarthi.app.network.ApiService
import com.saarthi.app.data.UserSession
import kotlinx.coroutines.*

class AdviceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        askAdvice()
    }

    private fun askAdvice() {

        val session = UserSession(this)

        val data = mapOf(
            "device_id" to session.getDeviceId(),
            "situation" to "I am confused about career"
        )

        val api = ApiClient.client
            .create(ApiService::class.java)

        CoroutineScope(Dispatchers.IO).launch {

            val res = api.getAdvice(
                session.getToken(),
                data
            )

            withContext(Dispatchers.Main) {

                if (res.isSuccessful) {
                    Toast.makeText(
                        this@AdviceActivity,
                        res.body()?.guidance,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}