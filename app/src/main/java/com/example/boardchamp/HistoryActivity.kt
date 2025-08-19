package com.example.boardchamp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

                historyContainer.removeAllViews()
                val sortedSessions = sessions.sortedByDescending { it.startTime.timeInMillis }
                val groupedSessions = sortedSessions.groupBy { formatDate(it.startTime) }

                groupedSessions.forEach { (date, sessionsForDate) ->
                    addDateHeader(date)
                    val dailySorted = sessionsForDate.sortedByDescending{ it.startTime.timeInMillis }

                    dailySorted.forEach { session ->
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
            val playersListContainer = sessionView.findViewById<LinearLayout>(R.id.playersListContainer)
            val tvNotes = sessionView.findViewById<TextView>(R.id.tvNotes)
            val btnDelete = sessionView.findViewById<ImageButton>(R.id.btnDeleteSession)
            val btnEdit = sessionView.findViewById<ImageButton>(R.id.btnEditSession)

            tvGameName.text = session.gameName

            val dayDifference = session.endTime.get(Calendar.DAY_OF_YEAR) - session.startTime.get(Calendar.DAY_OF_YEAR)
            val timeRange = if (dayDifference > 0) {
                "${formatTime(session.startTime)} - ${formatTime(session.endTime)} (+1 day)"
            } else {
                "${formatTime(session.startTime)} - ${formatTime(session.endTime)}"
            }
            tvTimeRange.text = timeRange

            val durationMillis = session.endTime.timeInMillis - session.startTime.timeInMillis
            val hours = durationMillis / (1000 * 60 * 60)
            val minutes = (durationMillis % (1000 * 60 * 60)) / (1000 * 60)
            tvDuration.text = "Duration: ${hours}h ${minutes}m"

            // We will fill the player list with a new layout
            populatePlayersInCard(playersListContainer, session.players)

            if (session.notes.isNotEmpty()) {
                tvNotes.text = "Notes: ${session.notes}"
                tvNotes.visibility = View.VISIBLE
            } else {
                tvNotes.visibility = View.GONE
            }

            btnDelete.setOnClickListener {
                confirmDelete(session.id)
            }

            btnEdit.setOnClickListener {
                val intent = Intent(this, EditSessionActivity::class.java).apply {
                    putExtra("session_id", session.id)
                    putExtra("game_name", session.gameName)
                    putExtra("game_date", session.gameDate.timeInMillis)
                    putExtra("start_time", session.startTime.timeInMillis)
                    putExtra("end_time", session.endTime.timeInMillis)
                    putExtra("notes", session.notes)
                    val playersJson = JSONArray().apply {
                        session.players.forEach { player ->
                            put(JSONObject().apply {
                                put("name", player.name)
                                put("score", player.score)
                                put("position", player.position)
                            })
                        }
                    }
                    putExtra("players", playersJson.toString())
                }
                startActivityForResult(intent, 1)
            }

            historyContainer.addView(sessionView)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun populatePlayersInCard(playersContainer: LinearLayout, players: List<PlayerData>) {
        playersContainer.removeAllViews()

        if (players.isEmpty()) {
            val noPlayersView = TextView(this).apply {
                text = "No players recorded"
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@HistoryActivity, android.R.color.darker_gray))
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(8), 0, dpToPx(8))
            }
            playersContainer.addView(noPlayersView)
            return
        }

        // We will rank players by position (1st place, 2nd place, etc.)
        val sortedPlayers = players.sortedBy { it.position }

        sortedPlayers.forEachIndexed { index, player ->
            val playerView = createPlayerItemView(player)
            playersContainer.addView(playerView)

            // We will add a divider between players (except the last one)
            if (index < sortedPlayers.size - 1) {
                val divider = createDivider()
                playersContainer.addView(divider)
            }
        }
    }

    private fun createPlayerItemView(player: PlayerData): LinearLayout {
        val playerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, dpToPx(6), 0, dpToPx(6))
            gravity = Gravity.CENTER_VERTICAL
        }

        // Position circle
        val positionView = TextView(this).apply {
            text = player.position.toString()
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(24),
                dpToPx(24)
            ).apply {
                setMargins(0, 0, dpToPx(8), 0)
            }

            // Set background based on position
            background = when(player.position) {
                1 -> ContextCompat.getDrawable(this@HistoryActivity, R.drawable.position_circle_gold)
                2 -> ContextCompat.getDrawable(this@HistoryActivity, R.drawable.position_circle_silver)
                3 -> ContextCompat.getDrawable(this@HistoryActivity, R.drawable.position_circle_bronze)
                else -> ContextCompat.getDrawable(this@HistoryActivity, R.drawable.position_circle_default)
            }

            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        // Player name
        val nameView = TextView(this).apply {
            text = player.name
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@HistoryActivity, R.color.on_surface_color))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        // Player score
        val scoreView = TextView(this).apply {
            text = "${player.score} pts"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@HistoryActivity, android.R.color.holo_green_dark))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        playerLayout.addView(positionView)
        playerLayout.addView(nameView)
        playerLayout.addView(scoreView)

        return playerLayout
    }

    private fun createDivider(): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1)
            ).apply {
                setMargins(0, dpToPx(2), 0, dpToPx(2))
            }
            setBackgroundColor(ContextCompat.getColor(this@HistoryActivity, android.R.color.darker_gray))
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun confirmDelete(sessionId: Long) {
        AlertDialog.Builder(this)
            .setTitle("Delete Confirmation")
            .setMessage("Do you really want to remove this session?\nOnce removed, it will be lost permanently.")
            .setPositiveButton("Yes") { _, _ ->
                deleteSession(sessionId)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteSession(sessionId: Long) {
        try {
            val sessions = loadSessionsFromHistory().toMutableList()
            val initialSize = sessions.size
            sessions.removeAll { it.id == sessionId }

            if (sessions.size < initialSize) {
                saveSessionsToHistory(sessions)
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
                        val players = mutableListOf<PlayerData>()
                        if (sessionJson.has("players")) {
                            val playersArray = sessionJson.getJSONArray("players")
                            for (j in 0 until playersArray.length()) {
                                try {
                                    val playerJson = playersArray.getJSONObject(j)
                                    players.add(PlayerData(
                                        name = playerJson.optString("name", "Unknown"),
                                        score = playerJson.optInt("score", 0),
                                        position = playerJson.optInt("position", 0)
                                    ))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }

                        val session = GameSession(
                            id = sessionJson.optLong("id", System.currentTimeMillis()),
                            gameName = sessionJson.optString("gameName", "Unknown Game"),
                            gameDate = Calendar.getInstance().apply { timeInMillis = sessionJson.optLong("gameDate", System.currentTimeMillis()) },
                            startTime = Calendar.getInstance().apply { timeInMillis = sessionJson.optLong("startTime", System.currentTimeMillis()) },
                            endTime = Calendar.getInstance().apply { timeInMillis = sessionJson.optLong("endTime", System.currentTimeMillis()) },
                            players = players,
                            notes = sessionJson.optString("notes", "")
                        )
                        sessions.add(session)
                    } catch (e: Exception) {
                        e.printStackTrace()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            val updatedSessionId = data.getLongExtra("session_id", -1L)
            if (updatedSessionId != -1L) {
                val sessions = loadSessionsFromHistory().toMutableList()
                val sessionIndex = sessions.indexOfFirst { it.id == updatedSessionId }

                //load players from intent
                val updatedPlayersJson = data.getStringExtra("players") ?: "[]"
                val updatedPlayers = mutableListOf<PlayerData>()
                try {
                    val jsonArray = JSONArray(updatedPlayersJson)
                    for (i in 0 until jsonArray.length()) {
                        val playerJson = jsonArray.getJSONObject(i)
                        updatedPlayers.add(
                            PlayerData(
                                name = playerJson.optString("name", "Unknown"),
                                score = playerJson.optInt("score", 0),
                                position = playerJson.optInt("position", 0)
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (sessionIndex != -1) {
                    val updatedGameDate = Calendar.getInstance().apply { timeInMillis = data.getLongExtra("game_date", sessions[sessionIndex].gameDate.timeInMillis) }
                    val updatedStartTime = Calendar.getInstance().apply { timeInMillis = data.getLongExtra("start_time", sessions[sessionIndex].startTime.timeInMillis) }
                    val updatedEndTime = Calendar.getInstance().apply { timeInMillis = data.getLongExtra("end_time", sessions[sessionIndex].endTime.timeInMillis) }
                    val updatedNotes = data.getStringExtra("notes") ?: sessions[sessionIndex].notes

                    sessions[sessionIndex] = sessions[sessionIndex].copy(
                        gameDate = updatedGameDate,
                        startTime = updatedStartTime,
                        endTime = updatedEndTime,
                        notes = updatedNotes,
                        players = updatedPlayers
                    )
                    saveSessionsToHistory(sessions)
                    loadAndDisplayHistory() // UI update
                }
                else {
                    Toast.makeText(this, "Edited session not found in history", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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