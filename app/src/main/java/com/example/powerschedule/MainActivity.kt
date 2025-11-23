package com.example.powerschedule

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var nameInput: EditText
    private lateinit var queueInput: EditText
    private lateinit var addQueueButton: Button
    private lateinit var errorText: TextView
    private lateinit var queuesContainer: LinearLayout
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nameInput = findViewById(R.id.nameInput)
        queueInput = findViewById(R.id.queueInput)
        addQueueButton = findViewById(R.id.addQueueButton)
        errorText = findViewById(R.id.errorText)
        queuesContainer = findViewById(R.id.queuesContainer)
        emptyText = findViewById(R.id.emptyText)

        loadQueues()

        addQueueButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val queue = queueInput.text.toString().trim()

            if (name.isEmpty()) {
                showError("❌ Введіть назву!", true)
                return@setOnClickListener
            }

            if (queue.isEmpty()) {
                showError("❌ Введіть чергу!", true)
                return@setOnClickListener
            }

            if (!isValidQueue(queue)) {
                showError("❌ Невірний формат черги! Приклад: 5.2", true)
                return@setOnClickListener
            }

            saveQueue(name, queue)

            nameInput.text.clear()
            queueInput.text.clear()

            loadQueues()

            showError("✅ Чергу \"$name\" додано!", false)
        }
    }

    override fun onResume() {
        super.onResume()
        loadQueues()
    }

    private fun isValidQueue(queue: String): Boolean {
        val regex = Regex("^\\d+\\.\\d+$")
        return regex.matches(queue)
    }

    private fun saveQueue(name: String, queue: String) {
        val prefs = getSharedPreferences("PowerSchedule", MODE_PRIVATE)
        val count = prefs.getInt("queue_count", 0)

        prefs.edit().apply {
            putString("queue_name_$count", name)
            putString("queue_value_$count", queue)
            putInt("queue_count", count + 1)
            apply()
        }
    }

    private fun loadQueues() {
        queuesContainer.removeAllViews()

        val prefs = getSharedPreferences("PowerSchedule", MODE_PRIVATE)
        val count = prefs.getInt("queue_count", 0)

        if (count == 0) {
            emptyText.visibility = View.VISIBLE
            return
        }

        emptyText.visibility = View.GONE

        for (i in 0 until count) {
            val name = prefs.getString("queue_name_$i", "") ?: ""
            val queue = prefs.getString("queue_value_$i", "") ?: ""

            if (name.isNotEmpty() && queue.isNotEmpty()) {
                addQueueCard(i, name, queue)
            }
        }
    }

    private fun addQueueCard(index: Int, name: String, queue: String) {
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
            elevation = dpToPx(4).toFloat()
        }

        val nameText = TextView(this).apply {
            text = "📍 $name"
            textSize = 20f
            setTextColor(Color.parseColor("#1976D2"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val queueText = TextView(this).apply {
            text = "Черга: $queue"
            textSize = 16f
            setTextColor(Color.parseColor("#424242"))
            setPadding(0, dpToPx(4), 0, dpToPx(12))
        }

        val buttonsLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
        }

        val openButton = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                dpToPx(48),
                1f
            ).apply {
                marginEnd = dpToPx(8)
            }
            text = "ПОКАЗАТИ"
            textSize = 14f
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                fetchSchedule(queue, name)
            }
        }

        val deleteButton = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                dpToPx(48),
                1f
            ).apply {
                marginStart = dpToPx(8)
            }
            text = "ВИДАЛИТИ"
            textSize = 14f
            setBackgroundColor(Color.parseColor("#F44336"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                confirmDelete(index, name)
            }
        }

        buttonsLayout.addView(openButton)
        buttonsLayout.addView(deleteButton)

        card.addView(nameText)
        card.addView(queueText)
        card.addView(buttonsLayout)

        queuesContainer.addView(card)
    }

    private fun confirmDelete(index: Int, name: String) {
        AlertDialog.Builder(this)
            .setTitle("Видалити чергу?")
            .setMessage("Ви впевнені що хочете видалити \"$name\"?")
            .setPositiveButton("Видалити") { _, _ ->
                deleteQueue(index)
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }

    private fun deleteQueue(index: Int) {
        val prefs = getSharedPreferences("PowerSchedule", MODE_PRIVATE)
        val count = prefs.getInt("queue_count", 0)

        prefs.edit().apply {
            for (i in index until count - 1) {
                val nextName = prefs.getString("queue_name_${i + 1}", "")
                val nextQueue = prefs.getString("queue_value_${i + 1}", "")
                putString("queue_name_$i", nextName)
                putString("queue_value_$i", nextQueue)
            }
            remove("queue_name_${count - 1}")
            remove("queue_value_${count - 1}")
            putInt("queue_count", count - 1)
            apply()
        }

        loadQueues()
    }

    private fun fetchSchedule(queue: String, name: String) {
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Завантаження...")
            .setMessage("Отримуємо графік для \"$name\"")
            .setCancelable(false)
            .create()

        progressDialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://be-svitlo.oe.if.ua/schedule-by-queue?queue=$queue"
                val jsonString = URL(url).readText()

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()

                    val intent = Intent(this@MainActivity, ScheduleActivity::class.java)
                    intent.putExtra("queue", queue)
                    intent.putExtra("name", name)
                    intent.putExtra("json", jsonString)
                    startActivity(intent)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    showError("❌ Помилка завантаження: ${e.message}", true)
                }
            }
        }
    }

    private fun showError(message: String, isError: Boolean) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
        errorText.setTextColor(
            if (isError) Color.parseColor("#F44336")
            else Color.parseColor("#4CAF50")
        )
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}