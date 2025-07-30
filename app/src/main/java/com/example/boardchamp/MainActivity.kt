package com.example.boardchamp

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray

class MainActivity : AppCompatActivity() {

    private lateinit var addButton: Button
    private lateinit var staticLayout: LinearLayout
    private val gameList = mutableListOf<String>() // uchovávaný zoznam hier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializácia layout-u pre statické hry
        staticLayout = findViewById(R.id.linearLayoutStatic)

        // Nastavenie tlačidla na pridanie novej hry
        addButton = findViewById(R.id.button)

        gameList.addAll(loadGameList())
        gameList.forEach { gameName ->
            addNewGameCard(gameName)
        }

        addButton.setOnClickListener {
            showAddGameDialog()
        }
    }

    private fun showAddGameDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pridať novú hru")

        // Vytvorenie EditText pre zadanie názvu hry
        val input = EditText(this)
        input.hint = "Názov hry"
        input.isSingleLine = true
        builder.setView(input)

        builder.setPositiveButton("Pridať") { _, _ ->
            val gameName = input.text.toString().trim()
            if (gameName.isNotEmpty()) {
                gameList.add(gameName)
                saveGameList(gameList)
                addNewGameCard(gameName)
                Toast.makeText(this, "Hra '$gameName' bola pridaná", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Zadajte názov hry", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Zrušiť") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun addNewGameCard(gameName: String) {
        // Vytvorenie nového CardView-u z layout-u
        val inflater = LayoutInflater.from(this)
        val cardView = inflater.inflate(R.layout.game_card_layout, staticLayout, false)

        // Nastavenie názvu hry
        val gameNameText = cardView.findViewById<android.widget.TextView>(R.id.gameNameText)
        gameNameText.text = gameName

        // Nastavenie click listener-a pre play button
        val playButton = cardView.findViewById<ImageButton>(R.id.gamePlayButton)
        playButton.setOnClickListener {
            Toast.makeText(this, "Spúšťam: $gameName", Toast.LENGTH_SHORT).show()
        }

        // >>> Podržaním zobraz dialóg na odstránenie <<<
        cardView.setOnLongClickListener {
            val options = arrayOf("Presunúť hore", "Presunúť dole", "Odstrániť")
            AlertDialog.Builder(this)
                .setTitle("Možnosti pre \"$gameName\"")
                .setItems(options) { _, which ->
                    val index = staticLayout.indexOfChild(cardView)
                    when (which) {
                        0 -> { // Hore
                            if (index > 0) {
                                // Presun karty v UI
                                staticLayout.removeViewAt(index)
                                staticLayout.addView(cardView, index - 1)
                                // Presun v zozname hier
                                val movedGame = gameList.removeAt(index)
                                gameList.add(index - 1, movedGame)
                                saveGameList(gameList)
                            }
                        }
                        1 -> { // Dole
                            if (index < staticLayout.childCount - 1) {
                                staticLayout.removeViewAt(index)
                                staticLayout.addView(cardView, index + 1)
                                val movedGame = gameList.removeAt(index)
                                gameList.add(index + 1, movedGame)
                                saveGameList(gameList)
                            }
                        }
                        2 -> { // Odstrániť
                            staticLayout.removeView(cardView)
                            gameList.removeAt(index)  // Odstráni podľa indexu
                            saveGameList(gameList)
                            Toast.makeText(this, "Hra \"$gameName\" bola odstránená", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .show()
            true
        }

        // Pridanie karty do layout-u (pred poslednou kartou s tlačidlom +)
        // Posledná karta má index staticGamesLayout.childCount - 1
        // Takže pridáme novú kartu na pozíciu childCount - 1 (pred poslednú)
        val indexBeforeLast = staticLayout.childCount - 1
        staticLayout.addView(cardView, indexBeforeLast)
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