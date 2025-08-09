package com.example.boardchamp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditSessionActivity : AppCompatActivity() {

    private lateinit var tvGameDate: TextView
    private lateinit var btnStartTime: Button
    private lateinit var btnEndTime: Button
    private lateinit var tvDuration: TextView
    private lateinit var etNotes: EditText
    private lateinit var btnCancel: Button
    private lateinit var btnEdit: Button

    private var startHour = 0
    private var startMinute = 0
    private var endHour = 0
    private var endMinute = 0

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private var gameDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_session)

        tvGameDate = findViewById(R.id.tvGameDate)
        btnStartTime = findViewById(R.id.btnStartTime)
        btnEndTime = findViewById(R.id.btnEndTime)
        tvDuration = findViewById(R.id.tvDuration)
        etNotes = findViewById(R.id.etNotes)
        btnCancel = findViewById(R.id.btnCancel)
        btnEdit = findViewById(R.id.btnEdit)

        loadSessionData()

        tvGameDate.setOnClickListener {
            showDatePicker()
        }

        btnStartTime.setOnClickListener {
            showTimePicker(isStartTime = true)
        }

        btnEndTime.setOnClickListener {
            showTimePicker(isStartTime = false)
        }

        btnCancel.setOnClickListener {
            finish()
        }

        btnEdit.setOnClickListener {
            saveSession()
        }
    }

    private fun loadSessionData() {
        val intent = intent
        if (intent != null && intent.hasExtra("session_id")) {
            val gameDateMillis = intent.getLongExtra("game_date", System.currentTimeMillis())
            val startTimeMillis = intent.getLongExtra("start_time", System.currentTimeMillis())
            val endTimeMillis = intent.getLongExtra("end_time", System.currentTimeMillis())
            val notes = intent.getStringExtra("notes") ?: ""

            gameDate = Calendar.getInstance().apply { timeInMillis = gameDateMillis }
            tvGameDate.text = dateFormat.format(gameDate.time)

            startHour = Calendar.getInstance().apply { timeInMillis = startTimeMillis }.get(Calendar.HOUR_OF_DAY)
            startMinute = Calendar.getInstance().apply { timeInMillis = startTimeMillis }.get(Calendar.MINUTE)
            endHour = Calendar.getInstance().apply { timeInMillis = endTimeMillis }.get(Calendar.HOUR_OF_DAY)
            endMinute = Calendar.getInstance().apply { timeInMillis = endTimeMillis }.get(Calendar.MINUTE)

            btnStartTime.text = String.format("%02d:%02d", startHour, startMinute)
            btnEndTime.text = String.format("%02d:%02d", endHour, endMinute)

            etNotes.setText(notes)

            updateDuration()
        } else {
            gameDate = Calendar.getInstance()
            tvGameDate.text = dateFormat.format(gameDate.time)

            startHour = 14
            startMinute = 0
            endHour = 15
            endMinute = 30

            btnStartTime.text = String.format("%02d:%02d", startHour, startMinute)
            btnEndTime.text = String.format("%02d:%02d", endHour, endMinute)

            etNotes.setText("")

            updateDuration()
        }
    }

    private fun showDatePicker() {
        val year = gameDate.get(Calendar.YEAR)
        val month = gameDate.get(Calendar.MONTH)
        val day = gameDate.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, y, m, d ->
            gameDate.set(Calendar.YEAR, y)
            gameDate.set(Calendar.MONTH, m)
            gameDate.set(Calendar.DAY_OF_MONTH, d)
            tvGameDate.text = dateFormat.format(gameDate.time)
        }, year, month, day).show()
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val hour = if (isStartTime) startHour else endHour
        val minute = if (isStartTime) startMinute else endMinute

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            if (isStartTime) {
                startHour = selectedHour
                startMinute = selectedMinute
                btnStartTime.text = String.format("%02d:%02d", startHour, startMinute)
            } else {
                endHour = selectedHour
                endMinute = selectedMinute
                btnEndTime.text = String.format("%02d:%02d", endHour, endMinute)
            }
            updateDuration()
        }, hour, minute, true).show()
    }

    private fun updateDuration() {
        val startInMinutes = startHour * 60 + startMinute
        val endInMinutes = endHour * 60 + endMinute
        val durationMinutes = if (endInMinutes >= startInMinutes) {
            endInMinutes - startInMinutes
        } else {
            0
        }

        val hours = durationMinutes / 60
        val minutes = durationMinutes % 60

        tvDuration.text = "Duration: %02d:%02d".format(hours, minutes)
    }

    private fun saveSession() {
        val notes = etNotes.text.toString()
        val resultIntent = Intent()
        val sessionId = intent.extras?.getLong("session_id") ?: System.currentTimeMillis()
        resultIntent.putExtra("session_id", sessionId)
        resultIntent.putExtra("game_name", "Game Name")
        resultIntent.putExtra("game_date", gameDate.timeInMillis)
        resultIntent.putExtra("start_time", Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMinute)
        }.timeInMillis)
        resultIntent.putExtra("end_time", Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, endHour)
            set(Calendar.MINUTE, endMinute)
        }.timeInMillis)
        resultIntent.putExtra("notes", notes)
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}