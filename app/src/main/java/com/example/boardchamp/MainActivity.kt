package com.example.boardchamp

import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray

class MainActivity : AppCompatActivity() {

    private lateinit var addButton: Button
    private lateinit var historyButton: FloatingActionButton
    private lateinit var themeSwitcherButton: FloatingActionButton
    private lateinit var staticLayout: LinearLayout

    // Info card
    private lateinit var infoPanel: View
    private lateinit var btnToggleInfo: ImageButton
    private lateinit var tvInfoText: TextView
    private lateinit var btnGitHubSmall: ImageButton
    private var isInfoExpanded = false

    private val gameList = mutableListOf<String>() // list of games

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Layout initialization for static games
        staticLayout = findViewById(R.id.linearLayoutStatic)

        // Setting the buttons
        addButton = findViewById(R.id.button)
        historyButton = findViewById(R.id.btnHistory)
        themeSwitcherButton = findViewById(R.id.btnThemeSwitch)

        // Info panel initialization
        infoPanel = findViewById(R.id.infoPanel)
        btnToggleInfo = infoPanel.findViewById(R.id.btnToggleInfo)
        tvInfoText = infoPanel.findViewById(R.id.tvInfoText)
        btnGitHubSmall = infoPanel.findViewById(R.id.btnGitHubSmall)
        setupInfoPanel()

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

        themeSwitcherButton.setOnClickListener {
            val currentMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

            if(currentMode == Configuration.UI_MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            else{
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
    }

    private fun setupInfoPanel() {
        btnToggleInfo.setOnClickListener {
            toggleInfoPanel()
        }

        btnGitHubSmall.setOnClickListener {
            openGitHub()
        }
    }

    private fun toggleInfoPanel() {
        isInfoExpanded = !isInfoExpanded

        if (isInfoExpanded) {
            // Expand - show text and GitHub button
            tvInfoText.visibility = View.VISIBLE
            btnGitHubSmall.visibility = View.VISIBLE
        } else {
            // Collapse - hide text and GitHub button
            tvInfoText.visibility = View.GONE
            btnGitHubSmall.visibility = View.GONE
        }
    }

    private fun openGitHub() {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://github.com/JozzefBar/BoardChamp-app")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_opening_github, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddGameDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.add_new_game)

        // Creating an EditText to enter the game name
        val input = EditText(this)
        input.hint = getString(R.string.game_name_hint)
        input.isSingleLine = true
        input.filters = arrayOf(InputFilter.LengthFilter(42))
        builder.setView(input)

        builder.setPositiveButton(R.string.add) { _, _ ->
            val gameName = input.text.toString().trim()
            if(gameName.isEmpty()){
                Toast.makeText(this, R.string.please_enter_game_name, Toast.LENGTH_SHORT).show()
            }
            //If the game has same name but with different capitals, it will not add new game card
            else if(gameList.any {it.equals(gameName, ignoreCase = true) }) {
                Toast.makeText(this, R.string.game_card_already_exists, Toast.LENGTH_SHORT).show()
            }
            else{
                gameList.add(gameName)
                saveGameList(gameList)
                addNewGameCard(gameName)
                Toast.makeText(this, getString(R.string.game_added, gameName), Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun confirmDelete(cardView: View, gameName: String, index: Int) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_confirmation)
            .setMessage(getString(R.string.delete_confirmation_message, gameName))
            .setPositiveButton(R.string.yes) { _, _ ->
                // card deletion
                staticLayout.removeView(cardView)
                gameList.removeAt(index)
                saveGameList(gameList)
                Toast.makeText(this, getString(R.string.game_removed, gameName), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }


    private fun addNewGameCard(gameName: String) {
        //Creating a new CardView from a layout
        val inflater = LayoutInflater.from(this)
        val cardView = inflater.inflate(R.layout.game_card_layout, staticLayout, false)

        //Setting the game name
        val gameNameText = cardView.findViewById<TextView>(R.id.gameNameText)
        gameNameText.text = gameName

        //Setting up a click listener for the play button - opens a SessionActivity
        val playButton = cardView.findViewById<ImageButton>(R.id.gamePlayButton)
        playButton.setOnClickListener {
            openGameSession(gameName)
        }

        // Hold to display game management dialog
        cardView.setOnLongClickListener {
            val options = arrayOf(
                    getString(R.string.move_up),
                    getString(R.string.move_down),
                    getString(R.string.remove))
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.options_for, gameName))
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
                            if (index < staticLayout.childCount - 2) {
                                staticLayout.removeViewAt(index)
                                staticLayout.addView(cardView, index + 1)
                                val movedGame = gameList.removeAt(index)
                                gameList.add(index + 1, movedGame)
                                saveGameList(gameList)
                            }
                        }
                        2 -> { // Remove
                            confirmDelete(cardView, gameName, index)
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
            Toast.makeText(this, getString(R.string.error_opening_history, e.message), Toast.LENGTH_SHORT).show()
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