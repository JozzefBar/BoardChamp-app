package com.example.boardchamp

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var historyContainer: LinearLayout
    private lateinit var tvNoHistory: TextView
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        try {
            initializeViews()
            setupClickListeners()
            loadAndDisplayHistory()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading history: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeViews() {
        historyContainer = findViewById(R.id.historyContainer)
        tvNoHistory = findViewById(R.id.tvNoHistory)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }
    }

    private fun loadAndDisplayHistory() {
        try {
            val sessions = loadSessionsFromHistory()

            if (sessions.isEmpty()) {
                tvNoHistory.visibility = LinearLayout.VISIBLE
                historyContainer.visibility = LinearLayout.GONE
            } else {
                tvNoHistory.visibility = LinearLayout.GONE
                historyContainer.visibility = LinearLayout.VISIBLE

                // Zoradenie podľa dátumu (najnovšie navrchu)
                val sortedSessions = sessions.sortedByDescending { it.gameDate.timeInMillis }

                // Zoskupenie podľa dátumu
                val groupedSessions = sortedSessions.groupBy { formatDate(it.gameDate) }

                groupedSessions.forEach { (date, sessionsForDate) ->
                    addDateHeader(date)
                    sessionsForDate.forEach { session ->
                        addSessionCard(session)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error displaying history: ${e.message}", Toast.LENGTH_SHORT).show()
            tvNoHistory.visibility = LinearLayout.VISIBLE
            historyContainer.visibility = LinearLayout.GONE
        }
    }

    private fun addDateHeader(date: String) {
        try {
            val inflater = LayoutInflater.from(this)
            val headerView = inflater.inflate(R.layout.date_header_item, historyContainer, false)

            val tvDate = headerView.findViewById<TextView>(R.id.tvDateHeader)
            tvDate.text = date

            historyContainer.addView(headerView)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addSessionCard(session: GameSession) {
        try {
            val inflater = LayoutInflater.from(this)
            val sessionView = inflater.inflate(R.layout.session_history_item, historyContainer, false)

            val tvGameName = sessionView.findViewById<TextView>(R.id.tvGameName)
            val tvTimeRange = sessionView.findViewById<TextView>(R.id.tvTimeRange)
            val tvDuration = sessionView.findViewById<TextView>(R.id.tvDuration)
            val tvPlayers = sessionView.findViewById<TextView>(R.id.tvPlayers)
            val tvNotes = sessionView.findViewById<TextView>(R.id.tvNotes)
            val btnDelete = sessionView.findViewById<ImageButton>(R.id.btnDeleteSession)

            // Nastavenie údajov
            tvGameName.text = session.gameName

            // Time range s check pre midnight crossing
            val dayDifference = session.endTime.get(Calendar.DAY_OF_YEAR) - session.startTime.get(Calendar.DAY_OF_YEAR)
            val timeRange = if (dayDifference > 0) {
                "${formatTime(session.startTime)} - ${formatTime(session.endTime)} (+1 day)"
            } else {
                "${formatTime(session.startTime)} - ${formatTime(session.endTime)}"
            }
            tvTimeRange.text = timeRange

            // Duration calculation
            val durationMillis = session.endTime.timeInMillis - session.startTime.timeInMillis
            val hours = durationMillis / (1000 * 60 * 60)
            val minutes = (durationMillis % (1000 * 60 * 60)) / (1000 * 60)
            tvDuration.text = "Duration: ${hours}h ${minutes}m"

            // Players info
            val playersText = if (session.players.isNotEmpty()) {
                session.players.joinToString(", ") { "${it.name} (${it.score}pts)" }
            } else {
                "No players recorded"
            }
            tvPlayers.text = "Players: $playersText"

            // Notes
            if (session.notes.isNotEmpty()) {
                tvNotes.text = "Notes: ${session.notes}"
                tvNotes.visibility = LinearLayout.VISIBLE
            } else {
                tvNotes.visibility = LinearLayout.GONE
            }

            // Delete button
            btnDelete.setOnClickListener {
                deleteSession(session.id)
            }

            historyContainer.addView(sessionView)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun deleteSession(sessionId: Long) {
        try {
            val sessions = loadSessionsFromHistory().toMutableList()
            val initialSize = sessions.size
            sessions.removeAll { it.id == sessionId }

            if (sessions.size < initialSize) {
                saveSessionsToHistory(sessions)

                // Refresh display
                historyContainer.removeAllViews()
                loadAndDisplayHistory()

                Toast.makeText(this, "Session deleted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Session not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error deleting session: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSessionsFromHistory(): List<GameSession> {
        val sessions = mutableListOf<GameSession>()

        try {
            val sharedPref = getSharedPreferences("game_data", MODE_PRIVATE)
            val json = sharedPref.getString("game_sessions", null)

            if (json != null && json.isNotEmpty()) {
                val jsonArray = JSONArray(json)
                for (i in 0 until jsonArray.length()) {
                    try {
                        val sessionJson = jsonArray.getJSONObject(i)

                        // Parse players
                        val players = mutableListOf<PlayerData>()
                        if (sessionJson.has("players")) {
                            val playersArray = sessionJson.getJSONArray("players")
                            for (j in 0 until playersArray.length()) {
                                try {
                                    val playerJson = playersArray.getJSONObject(j)
                                    players.add(
                                        PlayerData(
                                            name = playerJson.optString("name", "Unknown"),
                                            score = playerJson.optInt("score", 0),
                                            position = playerJson.optInt("position", 0)
                                        )
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }

                        // Create session
                        val session = GameSession(
                            id = sessionJson.optLong("id", System.currentTimeMillis()),
                            gameName = sessionJson.optString("gameName", "Unknown Game"),
                            gameDate = Calendar.getInstance().apply {
                                timeInMillis = sessionJson.optLong("gameDate", System.currentTimeMillis())
                            },
                            startTime = Calendar.getInstance().apply {
                                timeInMillis = sessionJson.optLong("startTime", System.currentTimeMillis())
                            },
                            endTime = Calendar.getInstance().apply {
                                timeInMillis = sessionJson.optLong("endTime", System.currentTimeMillis())
                            },
                            players = players,
                            notes = sessionJson.optString("notes", "")
                        )
                        sessions.add(session)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Pokračuj s ďalšími sessions aj keď jedna je poškodená
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return sessions
    }

    private fun saveSessionsToHistory(sessions: List<GameSession>) {
        try {
            val sharedPref = getSharedPreferences("game_data", MODE_PRIVATE)
            val editor = sharedPref.edit()

            // Konvertuj na JSON array
            val jsonArray = JSONArray()
            sessions.forEach { gameSession ->
                try {
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
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Ulož do SharedPreferences
            editor.putString("game_sessions", jsonArray.toString())
            editor.apply()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error saving sessions: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatDate(calendar: Calendar): String {
        return try {
            val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            format.format(calendar.time)
        } catch (e: Exception) {
            "Invalid Date"
        }
    }

    private fun formatTime(calendar: Calendar): String {
        return try {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            format.format(calendar.time)
        } catch (e: Exception) {
            "Invalid Time"
        }
    }

    // Data classes (same as SecondActivity)
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