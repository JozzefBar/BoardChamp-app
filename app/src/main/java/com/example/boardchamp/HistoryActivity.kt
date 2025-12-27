package com.example.boardchamp

import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var historyContainer: LinearLayout
    private lateinit var tvNoHistory: TextView
    private lateinit var btnBack: Button
    private lateinit var btnImportSession: FloatingActionButton

    private val editSessionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            handleEditSessionResult(result.data!!)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        try {
            initializeViews()
            setupClickListeners()
            loadAndDisplayHistory()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.error_loading_history, e.message), Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeViews() {
        historyContainer = findViewById(R.id.historyContainer)
        tvNoHistory = findViewById(R.id.tvNoHistory)
        btnBack = findViewById(R.id.btnBack)
        btnImportSession = findViewById(R.id.btnImportSession)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }
        btnImportSession.setOnClickListener { importSessionFromClipboard() }
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
            Toast.makeText(this, getString(R.string.error_displaying_history, e.message), Toast.LENGTH_SHORT).show()
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
            // Tag the view with session ID for scrolling
            sessionView.tag = session.id

            val tvGameName = sessionView.findViewById<TextView>(R.id.tvGameName)
            val tvImportedBadge = sessionView.findViewById<TextView>(R.id.tvImportedBadge)
            val tvTimeRange = sessionView.findViewById<TextView>(R.id.tvTimeRange)
            val tvDuration = sessionView.findViewById<TextView>(R.id.tvDuration)
            val playersListContainer = sessionView.findViewById<LinearLayout>(R.id.playersListContainer)
            val tvNotes = sessionView.findViewById<TextView>(R.id.tvNotes)
            val btnDelete = sessionView.findViewById<ImageButton>(R.id.btnDeleteSession)
            val btnEdit = sessionView.findViewById<ImageButton>(R.id.btnEditSession)
            val btnShare = sessionView.findViewById<ImageButton>(R.id.btnShareSession)

            tvGameName.text = session.gameName

            // Show imported badge if session was imported
            if (session.isImported) {
                tvImportedBadge.visibility = View.VISIBLE
            } else {
                tvImportedBadge.visibility = View.GONE
            }

            val dayDifference = session.endTime.get(Calendar.DAY_OF_YEAR) - session.startTime.get(Calendar.DAY_OF_YEAR)
            val timeRange = if (dayDifference > 0) {
                getString(R.string.time_range_plus_day, formatTime(session.startTime), formatTime(session.endTime))
            } else {
                getString(R.string.time_range, formatTime(session.startTime), formatTime(session.endTime))
            }
            tvTimeRange.text = timeRange

            val durationMillis = session.endTime.timeInMillis - session.startTime.timeInMillis
            val hours = durationMillis / (1000 * 60 * 60)
            val minutes = (durationMillis % (1000 * 60 * 60)) / (1000 * 60)
            tvDuration.text = getString(R.string.duration_format, hours, minutes)

            // We will fill the player list with a new layout
            populatePlayersInCard(playersListContainer, session.players)

            if (session.notes.isNotEmpty()) {
                tvNotes.text = getString(R.string.notes, session.notes)
                tvNotes.visibility = View.VISIBLE
            } else {
                tvNotes.visibility = View.GONE
            }

            btnDelete.setOnClickListener {
                confirmDelete(session.id)
            }

            btnShare.setOnClickListener {
                shareSession(session)
            }

            btnEdit.setOnClickListener {
                // Show confirmation if session is imported
                if (session.isImported) {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.edit_imported_session)
                        .setMessage(R.string.edit_imported_message)
                        .setPositiveButton(R.string.yes_edit) { _, _ ->
                            launchEditActivity(session)
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                } else {
                    launchEditActivity(session)
                }
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
                text = getString(R.string.no_players)
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
            text = getString(R.string.points_format, player.score)
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
            .setTitle(R.string.delete_session_confirmation)
            .setMessage(R.string.delete_session_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                deleteSession(sessionId)
            }
            .setNegativeButton(R.string.no, null)
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
                Toast.makeText(this, R.string.session_deleted, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.session_not_found, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.error_deleting_session, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSessionsFromHistory(): List<GameSession> {
        val sessions = mutableListOf<GameSession>()
        try {
            val sharedPref = getSharedPreferences("game_data", MODE_PRIVATE)
            val json = sharedPref.getString("game_sessions", null)

            if (!json.isNullOrEmpty()) {
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
                            notes = sessionJson.optString("notes", ""),
                            isImported = sessionJson.optBoolean("isImported", false)
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
                        put("isImported", gameSession.isImported)
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
            Toast.makeText(this, getString(R.string.error_saving_sessions, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatDate(calendar: Calendar): String {
        return try {
            val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            format.format(calendar.time)
        } catch (e: Exception) {
            getString(R.string.invalid_date_error, e.message ?: "Unknown")
        }
    }

    private fun formatTime(calendar: Calendar): String {
        return try {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            format.format(calendar.time)
        } catch (e: Exception) {
            getString(R.string.invalid_time_error, e.message ?: "Unknown")
        }
    }

    private fun launchEditActivity(session: GameSession) {
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
        editSessionLauncher.launch(intent)
    }

    private fun handleEditSessionResult(data: Intent) {
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
                Toast.makeText(this, R.string.edited_session_not_found, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareSession(session: GameSession) {
        try {
            // Create JSON object for the session
            val sessionJson = JSONObject().apply {
                put("gameName", session.gameName)
                put("gameDate", session.gameDate.timeInMillis)
                put("startTime", session.startTime.timeInMillis)
                put("endTime", session.endTime.timeInMillis)
                put("notes", session.notes)
                val playersArray = JSONArray()
                session.players.forEach { player ->
                    val playerJson = JSONObject().apply {
                        put("name", player.name)
                        put("score", player.score)
                        put("position", player.position)
                    }
                    playersArray.put(playerJson)
                }
                put("players", playersArray)
            }

            // Copy to clipboard
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Game Session", sessionJson.toString())
            clipboard.setPrimaryClip(clip)

            Toast.makeText(this, R.string.session_copied, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.error_sharing_session, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun importSessionFromClipboard() {
        try {
            // Get clipboard content
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            if (!clipboard.hasPrimaryClip()) {
                Toast.makeText(this, R.string.clipboard_is_empty, Toast.LENGTH_SHORT).show()
                return
            }

            val clipData = clipboard.primaryClip
            if (clipData == null || clipData.itemCount == 0) {
                Toast.makeText(this, R.string.no_data_in_clipboard, Toast.LENGTH_SHORT).show()
                return
            }

            val clipText = clipData.getItemAt(0).text.toString()

            // Parse JSON
            val sessionJson = JSONObject(clipText)

            // Validate required fields
            if (!sessionJson.has("gameName") || !sessionJson.has("gameDate") ||
                !sessionJson.has("startTime") || !sessionJson.has("endTime") ||
                !sessionJson.has("players")) {
                Toast.makeText(this, R.string.invalid_session_format, Toast.LENGTH_LONG).show()
                return
            }

            // Extract session data
            val gameName = sessionJson.getString("gameName")
            val gameDate = Calendar.getInstance().apply {
                timeInMillis = sessionJson.getLong("gameDate")
            }
            val startTime = Calendar.getInstance().apply {
                timeInMillis = sessionJson.getLong("startTime")
            }
            val endTime = Calendar.getInstance().apply {
                timeInMillis = sessionJson.getLong("endTime")
            }
            val notes = sessionJson.optString("notes", "")

            // Extract players
            val players = mutableListOf<PlayerData>()
            val playersArray = sessionJson.getJSONArray("players")

            // Validate that there is at least one player
            if (playersArray.length() == 0) {
                Toast.makeText(this, R.string.invalid_session_no_players, Toast.LENGTH_LONG).show()
                return
            }

            for (i in 0 until playersArray.length()) {
                val playerJson = playersArray.getJSONObject(i)

                if (!playerJson.has("name") || !playerJson.has("score") || !playerJson.has("position")) {
                    Toast.makeText(this, R.string.invalid_player_data, Toast.LENGTH_LONG).show()
                    return
                }

                players.add(PlayerData(
                    name = playerJson.getString("name"),
                    score = playerJson.getInt("score"),
                    position = playerJson.getInt("position")
                ))
            }

            // Create new session
            val newSession = GameSession(
                id = System.currentTimeMillis(), // Generate new unique ID
                gameName = gameName,
                gameDate = gameDate,
                startTime = startTime,
                endTime = endTime,
                players = players,
                notes = notes,
                isImported = true
            )

            // Check for duplicates
            val sessions = loadSessionsFromHistory().toMutableList()
            val duplicate = sessions.find {
                it.gameName == newSession.gameName &&
                        it.gameDate.timeInMillis == newSession.gameDate.timeInMillis &&
                        it.startTime.timeInMillis == newSession.startTime.timeInMillis &&
                        it.endTime.timeInMillis == newSession.endTime.timeInMillis
            }

            if (duplicate != null) {
                // Show confirmation dialog for duplicate
                AlertDialog.Builder(this)
                    .setTitle(R.string.duplicate_session)
                    .setMessage(R.string.duplicate_session_message)
                    .setPositiveButton(R.string.yes_import) { _, _ ->
                        sessions.add(newSession)
                        saveSessionsToHistory(sessions)
                        loadAndDisplayHistory()
                        Toast.makeText(this, R.string.session_imported, Toast.LENGTH_LONG).show()

                        historyContainer.postDelayed({
                            scrollToSession(newSession.id)
                        }, 300)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
                return
            }

            // Add to history
            sessions.add(newSession)
            saveSessionsToHistory(sessions)

            // Refresh display
            loadAndDisplayHistory()

            Toast.makeText(this, R.string.session_imported, Toast.LENGTH_LONG).show()

            // Scroll to imported session after a short delay
            historyContainer.postDelayed({
                scrollToSession(newSession.id)
            }, 300)


        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, R.string.error_importing_session, Toast.LENGTH_LONG).show()
        }
    }

    private fun scrollToSession(sessionId: Long) {
        try {
            // Find the imported session card
            for (i in 0 until historyContainer.childCount) {
                val view = historyContainer.getChildAt(i)

                // Check if this view contains our session by checking the session data
                if (view.tag == sessionId) {
                    val scrollView = findViewById<ScrollView>(R.id.historyScrollView)

                    // Scroll to the session card with smooth animation
                    scrollView?.smoothScrollTo(0, view.top - 20)

                    // highlight the card
                    view.alpha = 0.3f
                    view.animate().alpha(1f).setDuration(500).start()

                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
        val notes: String,
        val isImported: Boolean = false
    )
}