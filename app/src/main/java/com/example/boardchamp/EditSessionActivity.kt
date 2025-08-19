package com.example.boardchamp

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditSessionActivity : AppCompatActivity() {

    private lateinit var tvGameDate: TextView
    private lateinit var btnStartTime: Button
    private lateinit var btnEndTime: Button
    private lateinit var tvDuration: TextView
    private lateinit var playersContainer: LinearLayout
    private lateinit var etNotes: EditText
    private lateinit var btnCancel: Button
    private lateinit var btnEdit: Button

    private var startTime: Calendar? = null
    private var endTime: Calendar? = null

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private var gameDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_session)

        tvGameDate = findViewById(R.id.tvGameDate)
        btnStartTime = findViewById(R.id.btnStartTime)
        btnEndTime = findViewById(R.id.btnEndTime)
        tvDuration = findViewById(R.id.tvDuration)
        playersContainer = findViewById(R.id.playersContainer)
        etNotes = findViewById(R.id.etNotes)
        btnCancel = findViewById(R.id.btnCancel)
        btnEdit = findViewById(R.id.btnEdit)

        loadSessionData()

        tvGameDate.setOnClickListener {
            // Date editing is completely disabled
            Toast.makeText(this, "Date cannot be changed", Toast.LENGTH_SHORT).show()
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
        val gameDateMillis = intent.getLongExtra("game_date", System.currentTimeMillis())
        val startTimeMillis = intent.getLongExtra("start_time", System.currentTimeMillis())
        val endTimeMillis = intent.getLongExtra("end_time", System.currentTimeMillis())
        val notes = intent.getStringExtra("notes") ?: ""
        val playersJson = intent.getStringExtra("players") ?: ""

        gameDate = Calendar.getInstance().apply { timeInMillis = gameDateMillis }
        tvGameDate.text = dateFormat.format(gameDate.time)

        startTime = Calendar.getInstance().apply { timeInMillis = startTimeMillis }
        endTime = Calendar.getInstance().apply { timeInMillis = endTimeMillis }

        btnStartTime.text = formatTime(startTime!!)
        updateEndTimeDisplay()

        etNotes.setText(notes)

        // Load and display players
        loadPlayers(playersJson)

        calculateDuration()
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(this, { _, hourOfDay, minute ->
            val selectedTime = Calendar.getInstance().apply {
                // Set the date from the selected gameDate
                set(Calendar.YEAR, gameDate.get(Calendar.YEAR))
                set(Calendar.MONTH, gameDate.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, gameDate.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (isStartTime) {
                startTime = selectedTime
                btnStartTime.text = formatTime(selectedTime)
                // If we already have an end time, we need to check it
                endTime?.let {
                    updateEndTimeDisplay()
                }
            } else {
                // For end time we set the time without modifying the day
                endTime = selectedTime
                updateEndTimeDisplay()
            }

            calculateDuration()
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
    }

    private fun updateEndTimeDisplay() {
        endTime?.let { endT ->
            startTime?.let { startT ->
                // Reset end time to original day
                endT.set(Calendar.YEAR, gameDate.get(Calendar.YEAR))
                endT.set(Calendar.MONTH, gameDate.get(Calendar.MONTH))
                endT.set(Calendar.DAY_OF_MONTH, gameDate.get(Calendar.DAY_OF_MONTH))

                val endHour = endT.get(Calendar.HOUR_OF_DAY)
                val endMinute = endT.get(Calendar.MINUTE)
                val startHour = startT.get(Calendar.HOUR_OF_DAY)
                val startMinute = startT.get(Calendar.MINUTE)

                // Compare times and determine if the end time should be for the next day
                val isNextDay = endHour < startHour || (endHour == startHour && endMinute <= startMinute)

                if (isNextDay) {
                    endT.add(Calendar.DAY_OF_MONTH, 1)
                    btnEndTime.text = "${formatTime(endT)} (+1 day)"
                } else {
                    btnEndTime.text = formatTime(endT)
                }
            } ?: run {
                btnEndTime.text = formatTime(endT)
            }
        }
    }

    private fun formatTime(calendar: Calendar): String {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(calendar.time)
    }

    private fun calculateDuration() {
        if (startTime != null && endTime != null) {
            val durationMillis = endTime!!.timeInMillis - startTime!!.timeInMillis

            if (durationMillis < 0) {
                tvDuration.text = "Duration: Invalid (calculation error)"
            } else {
                val hours = durationMillis / (1000 * 60 * 60)
                val minutes = (durationMillis % (1000 * 60 * 60)) / (1000 * 60)

                // If the end time is for the next day, we add the info to the duration
                val dayDifference = endTime!!.get(Calendar.DAY_OF_YEAR) - startTime!!.get(Calendar.DAY_OF_YEAR)
                val durationText = if (dayDifference > 0) {
                    "Duration: ${hours}h ${minutes}m (crosses midnight)"
                } else {
                    "Duration: ${hours}h ${minutes}m"
                }

                tvDuration.text = durationText
            }
        }
    }

    private fun saveSession() {
        val notes = etNotes.text.toString()
        val resultIntent = Intent()
        val sessionId = intent.extras?.getLong("session_id") ?: System.currentTimeMillis()
        resultIntent.putExtra("session_id", sessionId)
        resultIntent.putExtra("game_date", gameDate.timeInMillis)
        resultIntent.putExtra("start_time", startTime?.timeInMillis ?: System.currentTimeMillis())
        resultIntent.putExtra("end_time", endTime?.timeInMillis ?: System.currentTimeMillis())
        resultIntent.putExtra("notes", notes)

        // Collect updated players data
        val playersJson = collectPlayersData()?: return // Validation failed, stay in edit mode
        resultIntent.putExtra("players", playersJson)

        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun loadPlayers(playersJson: String) {
        if (playersJson.isEmpty()) {
            return
        }

        try {
            val jsonArray = JSONArray(playersJson)
            for (i in 0 until jsonArray.length()) {
                val playerJson = jsonArray.getJSONObject(i)
                val name = playerJson.getString("name")
                val score = playerJson.getInt("score")
                val position = playerJson.getInt("position")

                addPlayerCard(name, score, position)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addPlayerCard(name: String, score: Int, position: Int) {
        val inflater = LayoutInflater.from(this)
        val playerView = inflater.inflate(R.layout.player_item, playersContainer, false)

        val etPlayerName = playerView.findViewById<EditText>(R.id.etPlayerName)
        val etPlayerScore = playerView.findViewById<EditText>(R.id.etPlayerScore)
        val etPlayerPosition = playerView.findViewById<EditText>(R.id.etPlayerPosition)
        val btnRemovePlayer = playerView.findViewById<ImageButton>(R.id.btnRemovePlayer)

        // Set player data
        etPlayerName.setText(name)
        etPlayerName.isEnabled = false // Make name non-editable
        etPlayerName.isFocusable = false
        etPlayerScore.setText(score.toString())
        etPlayerPosition.setText(position.toString())

        // Hide remove button since we're editing existing session
        btnRemovePlayer.visibility = android.view.View.GONE

        playersContainer.addView(playerView)
    }

    private fun collectPlayersData(): String? {
        val jsonArray = JSONArray()

        for (i in 0 until playersContainer.childCount) {
            val playerView = playersContainer.getChildAt(i)

            val etPlayerName = playerView.findViewById<EditText>(R.id.etPlayerName)
            val etPlayerScore = playerView.findViewById<EditText>(R.id.etPlayerScore)
            val etPlayerPosition = playerView.findViewById<EditText>(R.id.etPlayerPosition)

            val name = etPlayerName.text.toString().trim()
            val score = etPlayerScore.text.toString().toIntOrNull() ?: 0
            val position = etPlayerPosition.text.toString().toIntOrNull() ?: 1

            if (position > playersContainer.childCount) {
                Toast.makeText(this, "The $name's position is greater than the total number of players.", Toast.LENGTH_LONG).show()
                return null
            }

            if(position == 0){
                Toast.makeText(this, "The $name's position is zero.", Toast.LENGTH_SHORT).show()
                return null
            }

            if (name.isNotEmpty()) {
                val playerJson = JSONObject().apply {
                    put("name", name)
                    put("score", score)
                    put("position", position)
                }
                jsonArray.put(playerJson)
            }
        }

        return jsonArray.toString()
    }
}