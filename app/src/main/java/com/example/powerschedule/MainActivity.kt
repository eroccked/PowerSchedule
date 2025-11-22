package com.example.powerschedule

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.net.URL

class MainActivity : AppCompatActivity() {


    private lateinit var queueInput: EditText
    private lateinit var showButton: Button
    private lateinit var errorText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        queueInput = findViewById(R.id.queueInput)
        showButton = findViewById(R.id.showScheduleButton)
        errorText = findViewById(R.id.errorText)
        progressBar = findViewById(R.id.progressBar)


        showButton.setOnClickListener {
            val queue = queueInput.text.toString().trim()

            if (queue.isEmpty()) {
                showError("Введіть чергу!")
                return@setOnClickListener
            }

            if (!isValidQueue(queue)) {
                showError("Невірний формат! Приклад: 5.2")
                return@setOnClickListener
            }

            fetchSchedule(queue)
        }
    }

    private fun isValidQueue(queue: String): Boolean {
        val regex = Regex("^\\d+\\.\\d+$")
        return regex.matches(queue)
    }

    private fun fetchSchedule(queue: String) {
        progressBar.visibility = View.VISIBLE
        showButton.isEnabled = false
        errorText.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://be-svitlo.oe.if.ua/schedule-by-queue?queue=$queue"

                val jsonString = URL(url).readText()

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    showButton.isEnabled = true

                    val intent = Intent(this@MainActivity, ScheduleActivity::class.java)
                    intent.putExtra("queue", queue)
                    intent.putExtra("json", jsonString)
                    startActivity(intent)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    showButton.isEnabled = true
                    showError("Помилка завантаження: ${e.message}")
                }
            }
        }
    }
    
    private fun showError(message: String) {
        errorText.text = "❌ $message"
        errorText.visibility = View.VISIBLE
    }
}