package com.example.boardchamp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class SessionActivity : AppCompatActivity() {

    private lateinit var gameTitle: TextView
    private lateinit var btnSelectDate: Button
    private lateinit var btnStartTime: Button
    private lateinit var btnEndTime: Button
    private lateinit var tvDuration: TextView
    private lateinit var btnAddPlayer: Button
    private lateinit var playersContainer: LinearLayout
    private lateinit var tvNoPlayers: TextView
    private lateinit var etNotes: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private var gameDate: Calendar? = null
    private var startTime: Calendar? = null
    private var endTime: Calendar? = null
    private var gameName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.session_activity)

        // Getting the game name from an Intent
        gameName = intent.getStringExtra("GAME_NAME") ?: "Unknown Game"

        initializeViews()
        setupClickListeners()
        updateGameTitle()
    }

    private fun initializeViews() {
        gameTitle = findViewById(R.id.gameTitle)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        btnStartTime = findViewById(R.id.btnStartTime)
        btnEndTime = findViewById(R.id.btnEndTime)
        tvDuration = findViewById(R.id.tvDuration)
        btnAddPlayer = findViewById(R.id.btnAddPlayer)
        playersContainer = findViewById(R.id.playersContainer)
        tvNoPlayers = findViewById(R.id.tvNoPlayers)
        etNotes = findViewById(R.id.etNotes)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
    }

    private fun setupClickListeners() {
        btnSelectDate.setOnClickListener { showDatePickerDialog() }
        btnStartTime.setOnClickListener { showTimePickerDialog(true) }
        btnEndTime.setOnClickListener { showTimePickerDialog(false) }
        btnAddPlayer.setOnClickListener { addNewPlayer() }
        btnSave.setOnClickListener { saveGameSession() }
        btnCancel.setOnClickListener { finish() }
    }

    private fun updateGameTitle() {
        gameTitle.text = "$gameName - New Session"
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                gameDate = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                btnSelectDate.text = formatDate(gameDate!!)

                //Update times with new date
                updateTimesWithDate()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun showTimePickerDialog(isStartTime: Boolean) {
        if (gameDate == null) {
            Toast.makeText(this, "Please select a date first", Toast.LENGTH_SHORT).show()
            return
        }

        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val selectedTime = Calendar.getInstance().apply {
                    // Setting the date from the selected gameDate
                    set(Calendar.YEAR, gameDate!!.get(Calendar.YEAR))
                    set(Calendar.MONTH, gameDate!!.get(Calendar.MONTH))
                    set(Calendar.DAY_OF_MONTH, gameDate!!.get(Calendar.DAY_OF_MONTH))
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
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()
    }

    private fun updateEndTimeDisplay() {
        endTime?.let { endT ->
            startTime?.let { startT ->
                //Reset end time to original day
                endT.set(Calendar.YEAR, gameDate!!.get(Calendar.YEAR))
                endT.set(Calendar.MONTH, gameDate!!.get(Calendar.MONTH))
                endT.set(Calendar.DAY_OF_MONTH, gameDate!!.get(Calendar.DAY_OF_MONTH))

                val endHour = endT.get(Calendar.HOUR_OF_DAY)
                val endMinute = endT.get(Calendar.MINUTE)
                val startHour = startT.get(Calendar.HOUR_OF_DAY)
                val startMinute = startT.get(Calendar.MINUTE)

                //Compare times and determine if the end time should be for the next day
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

    private fun updateTimesWithDate() {
        // If we already have times set, let's update them with the new date
        startTime?.let { time ->
            val originalHour = time.get(Calendar.HOUR_OF_DAY)
            val originalMinute = time.get(Calendar.MINUTE)

            time.set(Calendar.YEAR, gameDate!!.get(Calendar.YEAR))
            time.set(Calendar.MONTH, gameDate!!.get(Calendar.MONTH))
            time.set(Calendar.DAY_OF_MONTH, gameDate!!.get(Calendar.DAY_OF_MONTH))
            time.set(Calendar.HOUR_OF_DAY, originalHour)
            time.set(Calendar.MINUTE, originalMinute)

            btnStartTime.text = formatTime(time)
        }

        endTime?.let {
            updateEndTimeDisplay()
        }

        calculateDuration()
    }

    private fun formatDate(calendar: Calendar): String {
        val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return format.format(calendar.time)
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
                tvDuration.setTextColor(resources.getColor(android.R.color.holo_red_dark))
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
                tvDuration.setTextColor(resources.getColor(android.R.color.holo_green_dark))
            }
        }
    }

    private fun addNewPlayer() {
        val inflater = LayoutInflater.from(this)
        val playerView = inflater.inflate(R.layout.player_item, playersContainer, false)

        val etPlayerName = playerView.findViewById<EditText>(R.id.etPlayerName)
        val btnRemovePlayer = playerView.findViewById<ImageButton>(R.id.btnRemovePlayer)

        // First, we hide the placeholder if it is visible.
        if (tvNoPlayers.visibility == LinearLayout.VISIBLE) {
            tvNoPlayers.visibility = LinearLayout.GONE
        }

        //Now count only real players (without placeholder TextView)
        val actualPlayerCount = getActualPlayerCount()
        etPlayerName.hint = "Player ${actualPlayerCount + 1}"

        //Player removal settings
        btnRemovePlayer.setOnClickListener {
            playersContainer.removeView(playerView)
            updatePlayersVisibility()
            updatePlayerHints()
        }

        playersContainer.addView(playerView)
    }

    private fun updatePlayerHints() {
        var playerNumber = 1
        for (i in 0 until playersContainer.childCount) {
            val child = playersContainer.getChildAt(i)

            // Skip tvNoPlayers TextView
            if (child == tvNoPlayers) continue

            val etPlayerName = child.findViewById<EditText>(R.id.etPlayerName)
            // If EditText is empty, update hint
            if (etPlayerName.text.toString().trim().isEmpty()) {
                etPlayerName.hint = "Player $playerNumber"
            }
            playerNumber++
        }
    }

    private fun getActualPlayerCount(): Int {
        //Only count Views that are not tvNoPlayers TextView
        var count = 0
        for (i in 0 until playersContainer.childCount) {
            val child = playersContainer.getChildAt(i)
            if (child != tvNoPlayers) {
                count++
            }
        }
        return count
    }

    private fun updatePlayersVisibility() {
        val actualPlayerCount = getActualPlayerCount()
        tvNoPlayers.visibility = if (actualPlayerCount == 0) {
            LinearLayout.VISIBLE
        } else {
            LinearLayout.GONE
        }
    }

    private fun saveGameSession() {
        // Data validation
        if (gameDate == null) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            return
        }

        if (startTime == null || endTime == null) {
            Toast.makeText(this, "Please set start and end time", Toast.LENGTH_SHORT).show()
            return
        }

        if (getActualPlayerCount() == 0) {
            Toast.makeText(this, "Please add at least one player", Toast.LENGTH_SHORT).show()
            return
        }

        // Collecting player data
        val players = mutableListOf<PlayerData>()
        for (i in 0 until playersContainer.childCount) {
            val playerView = playersContainer.getChildAt(i)

            //Skip tvNoPlayers TextView
            if (playerView == tvNoPlayers) continue

            val name = playerView.findViewById<EditText>(R.id.etPlayerName).text.toString().trim()
            val scoreText = playerView.findViewById<EditText>(R.id.etPlayerScore).text.toString()
            val positionText = playerView.findViewById<EditText>(R.id.etPlayerPosition).text.toString()

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter name for all players", Toast.LENGTH_SHORT).show()
                return
            }

            val score = scoreText.toIntOrNull() ?: 0
            val position = positionText.toIntOrNull() ?: (players.size + 1)

            players.add(PlayerData(name, score, position))
        }

        //Creating a session object
        val notes = etNotes.text.toString()
        val session = GameSession(
            id = System.currentTimeMillis(), // Unique ID based on timestamp
            gameName = gameName,
            gameDate = gameDate!!,
            startTime = startTime!!,
            endTime = endTime!!,
            players = players,
            notes = notes
        )

        //Saving to SharedPreferences
        saveSessionToHistory(session)

        // View success message with details
        val sessionInfo = StringBuilder().apply {
            appendLine("Game: ${session.gameName}")
            appendLine("Date: ${formatDate(session.gameDate)}")
            append("Time: ${formatTime(session.startTime)}")

            //If the end time is for the next day, we will show it.
            val dayDifference = session.endTime.get(Calendar.DAY_OF_YEAR) - session.startTime.get(Calendar.DAY_OF_YEAR)
            if (dayDifference > 0) {
                appendLine(" - ${formatTime(session.endTime)} (+1 day)")
            } else {
                appendLine(" - ${formatTime(session.endTime)}")
            }

            append("Players: ${session.players.size}")
        }.toString()

        Toast.makeText(this, "Game session saved to history!\n$sessionInfo", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun saveSessionToHistory(session: GameSession) {
        val sharedPref = getSharedPreferences("game_data", MODE_PRIVATE)
        val editor = sharedPref.edit()

        //Load existing sessions
        val existingSessions = loadSessionsFromHistory()
        val updatedSessions = existingSessions.toMutableList()

        //Add a new session
        updatedSessions.add(session)

        //Convert to JSON array
        val jsonArray = JSONArray()
        updatedSessions.forEach { gameSession ->
            val sessionJson = JSONObject().apply {
                put("id", gameSession.id)
                put("gameName", gameSession.gameName)
                put("gameDate", gameSession.gameDate.timeInMillis)
                put("startTime", gameSession.startTime.timeInMillis)
                put("endTime", gameSession.endTime.timeInMillis)
                put("notes", gameSession.notes)

                // Players array
                val playersArray = JSONArray()
                gameSession.players.forEach { player ->
                    val playerJson = JSONObject().apply {
                        put("name", player.name)
                        put("score", player.score)
                        put("position", player.position)
                    }
                    playersArray.put(playerJson)
                }
                put("players", playersArray)
            }
            jsonArray.put(sessionJson)
        }

        //Save to SharedPreferences
        editor.putString("game_sessions", jsonArray.toString())
        editor.apply()
    }

    private fun loadSessionsFromHistory(): List<GameSession> {
        val sharedPref = getSharedPreferences("game_data", MODE_PRIVATE)
        val json = sharedPref.getString("game_sessions", null)
        val sessions = mutableListOf<GameSession>()

        if (json != null) {
            try {
                val jsonArray = JSONArray(json)
                for (i in 0 until jsonArray.length()) {
                    val sessionJson = jsonArray.getJSONObject(i)

                    // Parse players
                    val players = mutableListOf<PlayerData>()
                    val playersArray = sessionJson.getJSONArray("players")
                    for (j in 0 until playersArray.length()) {
                        val playerJson = playersArray.getJSONObject(j)
                        players.add(
                            PlayerData(
                                name = playerJson.getString("name"),
                                score = playerJson.getInt("score"),
                                position = playerJson.getInt("position")
                            )
                        )
                    }

                    // Create session
                    val session = GameSession(
                        id = sessionJson.getLong("id"),
                        gameName = sessionJson.getString("gameName"),
                        gameDate = Calendar.getInstance().apply {
                            timeInMillis = sessionJson.getLong("gameDate")
                        },
                        startTime = Calendar.getInstance().apply {
                            timeInMillis = sessionJson.getLong("startTime")
                        },
                        endTime = Calendar.getInstance().apply {
                            timeInMillis = sessionJson.getLong("endTime")
                        },
                        players = players,
                        notes = sessionJson.getString("notes")
                    )
                    sessions.add(session)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return sessions
    }

    // Data classes for data storage
    data class PlayerData(val name: String, val score: Int, val position: Int)

    data class GameSession(
        val id: Long,
        val gameName: String,
        val gameDate: Calendar,
        val startTime: Calendar,
        val endTime: Calendar,
        val players: List<PlayerData>,
        val notes: String
    )
}