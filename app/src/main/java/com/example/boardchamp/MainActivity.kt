package com.example.boardchamp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray

class MainActivity : AppCompatActivity() {

    private lateinit var addButton: Button
    private lateinit var historyButton: FloatingActionButton
    private lateinit var staticLayout: LinearLayout
    private val gameList = mutableListOf<String>() // uchovávaný zoznam hier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Layout initialization for static games
        staticLayout = findViewById(R.id.linearLayoutStatic)

        // Setting the buttons
        addButton = findViewById(R.id.button)
        historyButton = findViewById(R.id.btnHistory)

        // Loading saved games and creating cards
        gameList.addAll(loadGameList())
        gameList.forEach { gameName ->
            addNewGameCard(gameName)
        }

        addButton.setOnClickListener {
            showAddGameDialog()
        }

        historyButton.setOnClickListener {
            openHistoryActivity()
        }
    }

    private fun showAddGameDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add New Game")

        // Creating an EditText to enter the game name
        val input = EditText(this)
        input.hint = "Game name"
        input.isSingleLine = true
        builder.setView(input)

        builder.setPositiveButton("Add") { _, _ ->
            val gameName = input.text.toString().trim()
            if (gameName.isNotEmpty()) {
                gameList.add(gameName)
                saveGameList(gameList)
                addNewGameCard(gameName)
                Toast.makeText(this, "Game '$gameName' added", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter game name", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun addNewGameCard(gameName: String) {
        //Creating a new CardView from a layout
        val inflater = LayoutInflater.from(this)
        val cardView = inflater.inflate(R.layout.game_card_layout, staticLayout, false)

        //Setting the game name
        val gameNameText = cardView.findViewById<android.widget.TextView>(R.id.gameNameText)
        gameNameText.text = gameName

        //Setting up a click listener for the play button - opens a SessionActivity
        val playButton = cardView.findViewById<ImageButton>(R.id.gamePlayButton)
        playButton.setOnClickListener {
            openGameSession(gameName)
        }

        // Hold to display game management dialog
        cardView.setOnLongClickListener {
            val options = arrayOf("Move Up", "Move Down", "Remove")
            AlertDialog.Builder(this)
                .setTitle("Options for \"$gameName\"")
                .setItems(options) { _, which ->
                    val index = staticLayout.indexOfChild(cardView)
                    when (which) {
                        0 -> { // Move Up
                            if (index > 0) {
                                // Moving a card in the UI
                                staticLayout.removeViewAt(index)
                                staticLayout.addView(cardView, index - 1)
                                //Move in the game list
                                val movedGame = gameList.removeAt(index)
                                gameList.add(index - 1, movedGame)
                                saveGameList(gameList)
                            }
                        }
                        1 -> { // Move Down
                            if (index < staticLayout.childCount - 1) {
                                staticLayout.removeViewAt(index)
                                staticLayout.addView(cardView, index + 1)
                                val movedGame = gameList.removeAt(index)
                                gameList.add(index + 1, movedGame)
                                saveGameList(gameList)
                            }
                        }
                        2 -> { // Remove
                            staticLayout.removeView(cardView)
                            gameList.removeAt(index)
                            saveGameList(gameList)
                            Toast.makeText(this, "Game \"$gameName\" removed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .show()
            true
        }

        // Adding a card to the layout (before the last element - Add button)
        val indexBeforeAddButton = maxOf(0, staticLayout.childCount - 1)
        staticLayout.addView(cardView, indexBeforeAddButton)
    }

    // New function to open a game session
    private fun openGameSession(gameName: String) {
        val intent = Intent(this, SessionActivity::class.java)
        intent.putExtra("GAME_NAME", gameName)
        startActivity(intent)
    }

    // New function to open history
    private fun openHistoryActivity() {
        try {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error opening history: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveGameList(gameList: List<String>) {
        val sharedPref = getSharedPreferences("game_data", MODE_PRIVATE)
        val editor = sharedPref.edit()
        val json = JSONArray(gameList).toString()
        editor.putString("games", json)
        editor.apply()
    }

    private fun loadGameList(): List<String> {
        val sharedPref = getSharedPreferences("game_data", MODE_PRIVATE)
        val json = sharedPref.getString("games", null)
        val list = mutableListOf<String>()
        if (json != null) {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
        }
        return list
    }
}