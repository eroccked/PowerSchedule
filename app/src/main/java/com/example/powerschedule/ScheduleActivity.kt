package com.example.powerschedule

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray

class ScheduleActivity : AppCompatActivity() {

    private lateinit var queueTitle: TextView
    private lateinit var dateText: TextView
    private lateinit var updatedText: TextView
    private lateinit var approvedText: TextView
    private lateinit var shutdownsContainer: LinearLayout
    private lateinit var totalTimeText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)


        val queue = intent.getStringExtra("queue") ?: "5.2"
        val jsonString = intent.getStringExtra("json") ?: ""

        queueTitle = findViewById(R.id.queueTitle)
        dateText = findViewById(R.id.dateText)
        updatedText = findViewById(R.id.updatedText)
        approvedText = findViewById(R.id.approvedText)
        shutdownsContainer = findViewById(R.id.shutdownsContainer)
        totalTimeText = findViewById(R.id.totalTimeText)

        val backButton = findViewById<Button>(R.id.backButton)
        val refreshButton = findViewById<Button>(R.id.refreshButton)

        queueTitle.text = "Черга $queue"

        parseAndDisplay(jsonString, queue)

        backButton.setOnClickListener {
            finish()
        }

        // Кнопка оновити
        refreshButton.setOnClickListener {
            recreate()
        }
    }

    private fun parseAndDisplay(jsonString: String, queue: String) {
        try {
            val jsonArray = JSONArray(jsonString)
            val scheduleObj = jsonArray.getJSONObject(0)

            // Дата
            val eventDate = scheduleObj.getString("eventDate")
            dateText.text = "📅 $eventDate"

            // Коли оновлено
            val createdAt = scheduleObj.getString("createdAt")
            updatedText.text = "Оновлено: $createdAt"

            // Коли затверджено
            val approved = scheduleObj.getString("scheduleApprovedSince")
            approvedText.text = "Затверджено з: $approved"

            // Отримуємо список відключень
            val queuesObj = scheduleObj.getJSONObject("queues")
            val shutdowns = queuesObj.getJSONArray(queue)

            var totalMinutes = 0
            for (i in 0 until shutdowns.length()) {
                val shutdown = shutdowns.getJSONObject(i)
                val from = shutdown.getString("from")
                val to = shutdown.getString("to")
                val shutdownHours = shutdown.getString("shutdownHours")

                addShutdownCard(shutdownHours, from, to)

                totalMinutes += calculateDuration(from, to)
            }

            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            totalTimeText.text = "📊 ВСЬОГО: $hours год $minutes хв без світла"

        } catch (e: Exception) {
            dateText.text = "❌ Помилка парсингу: ${e.message}"
        }
    }

    private fun addShutdownCard(shutdownHours: String, from: String, to: String) {
        val card = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(12))
            }
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            elevation = dpToPx(2).toFloat()
        }

        val timeText = TextView(this).apply {
            text = "🕐 $shutdownHours"
            textSize = 20f
            setTextColor(Color.parseColor("#F44336"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val duration = calculateDuration(from, to)
        val hours = duration / 60
        val minutes = duration % 60

        val durationText = TextView(this).apply {
            text = "   ($hours год $minutes хв)"
            textSize = 14f
            setTextColor(Color.parseColor("#757575"))
        }

        card.addView(timeText)
        card.addView(durationText)

        shutdownsContainer.addView(card)
    }

    private fun calculateDuration(from: String, to: String): Int {
        val fromParts = from.split(":")
        val toParts = to.split(":")

        val fromMinutes = fromParts[0].toInt() * 60 + fromParts[1].toInt()
        val toMinutes = toParts[0].toInt() * 60 + toParts[1].toInt()

        return toMinutes - fromMinutes
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}