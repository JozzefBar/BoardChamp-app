package com.example.boardchamp

import android.app.AlertDialog
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

class SecondActivity : AppCompatActivity() {

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
        setContentView(R.layout.second_activity)

        // Získanie názvu hry z Intent
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

                // Aktualizovať časy s novým dátumom
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
                    // Nastavenie dátumu z vybraného gameDate
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
                    // Ak už máme end time a je menší ako start time, automaticky ho posunieme na ďalší deň
                    endTime?.let { endT ->
                        if (endT.get(Calendar.HOUR_OF_DAY) < hourOfDay ||
                            (endT.get(Calendar.HOUR_OF_DAY) == hourOfDay && endT.get(Calendar.MINUTE) <= minute)) {
                            endT.add(Calendar.DAY_OF_MONTH, 1)
                            btnEndTime.text = "${formatTime(endT)} (+1 day)"
                        }
                    }
                } else {
                    endTime = selectedTime
                    // Ak je end time menší ako start time, automaticky ho posunieme na ďalší deň
                    startTime?.let { startT ->
                        if (hourOfDay < startT.get(Calendar.HOUR_OF_DAY) ||
                            (hourOfDay == startT.get(Calendar.HOUR_OF_DAY) && minute <= startT.get(Calendar.MINUTE))) {
                            selectedTime.add(Calendar.DAY_OF_MONTH, 1)
                            btnEndTime.text = "${formatTime(selectedTime)} (+1 day)"
                        } else {
                            btnEndTime.text = formatTime(selectedTime)
                        }
                    } ?: run {
                        btnEndTime.text = formatTime(selectedTime)
                    }
                    endTime = selectedTime
                }

                calculateDuration()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()
    }

    private fun updateTimesWithDate() {
        // Ak už máme nastavené časy, aktualizujme ich s novým dátumom
        startTime?.let { time ->
            val originalHour = time.get(Calendar.HOUR_OF_DAY)
            val originalMinute = time.get(Calendar.MINUTE)
            time.set(Calendar.YEAR, gameDate!!.get(Calendar.YEAR))
            time.set(Calendar.MONTH, gameDate!!.get(Calendar.MONTH))
            time.set(Calendar.DAY_OF_MONTH, gameDate!!.get(Calendar.DAY_OF_MONTH))
            btnStartTime.text = formatTime(time)
        }

        endTime?.let { time ->
            val originalHour = time.get(Calendar.HOUR_OF_DAY)
            val originalMinute = time.get(Calendar.MINUTE)
            time.set(Calendar.YEAR, gameDate!!.get(Calendar.YEAR))
            time.set(Calendar.MONTH, gameDate!!.get(Calendar.MONTH))
            time.set(Calendar.DAY_OF_MONTH, gameDate!!.get(Calendar.DAY_OF_MONTH))

            // Skontrolovať, či má byť end time na ďalší deň
            startTime?.let { startT ->
                if (originalHour < startT.get(Calendar.HOUR_OF_DAY) ||
                    (originalHour == startT.get(Calendar.HOUR_OF_DAY) && originalMinute <= startT.get(Calendar.MINUTE))) {
                    time.add(Calendar.DAY_OF_MONTH, 1)
                    btnEndTime.text = "${formatTime(time)} (+1 day)"
                } else {
                    btnEndTime.text = formatTime(time)
                }
            } ?: run {
                btnEndTime.text = formatTime(time)
            }
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

    private fun formatDateTime(calendar: Calendar): String {
        val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
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

                // Ak je end time na ďalší deň, pridáme info do duration
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

        // Najprv skryjeme placeholder ak je viditeľný
        if (tvNoPlayers.visibility == LinearLayout.VISIBLE) {
            tvNoPlayers.visibility = LinearLayout.GONE
        }

        // Teraz počítaj len skutočných hráčov (bez placeholder TextView)
        val actualPlayerCount = getActualPlayerCount()
        etPlayerName.hint = "Player ${actualPlayerCount + 1}"

        // Nastavenie odstránenia hráča
        btnRemovePlayer.setOnClickListener {
            playersContainer.removeView(playerView)
            updatePlayersVisibility()
        }

        playersContainer.addView(playerView)
    }

    private fun getActualPlayerCount(): Int {
        // Počítaj len View ktoré nie są tvNoPlayers TextView
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
        // Validácia údajov
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

        // Zbieranie údajov o hráčoch
        val players = mutableListOf<PlayerData>()
        for (i in 0 until playersContainer.childCount) {
            val playerView = playersContainer.getChildAt(i)

            // Preskočiť tvNoPlayers TextView
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

        // Vytvorenie session objektu
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

        // Uloženie do SharedPreferences
        saveSessionToHistory(session)

        // Zobrazenie úspešnej správy s detailmi
        val sessionInfo = StringBuilder().apply {
            appendLine("Game: ${session.gameName}")
            appendLine("Date: ${formatDate(session.gameDate)}")
            append("Time: ${formatTime(session.startTime)}")

            // Ak je end time na ďalší deň, ukážeme to
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

        // Načítaj existujúce sessions
        val existingSessions = loadSessionsFromHistory()
        val updatedSessions = existingSessions.toMutableList()

        // Pridaj novú session
        updatedSessions.add(session)

        // Konvertuj na JSON array
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

        // Ulož do SharedPreferences
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

    // Data classes pre uchovávanie údajov
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