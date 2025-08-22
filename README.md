
# 🎲 BoardChamp

**BoardChamp** is a mobile application written in Kotlin designed to store and manage your statistics of board game results. The app was created as a personal project to learn the basics of Android app development.

![Banner](app/src/main/res/mipmap-xhdpi/ic_banner.png)

**really detailed banner lol 🎉*

---


## 📋 Description

BoardChamp allows users to record their board game results and save them locally on the device in JSON format. This enables easy management of game history and tracking of progress.

Users can create and manage individual "cards" for each board game, allowing them to keep track of multiple different games. There is no limit to the number of game cards a user can create, making it easy to organize and maintain statistics across a variety of board games.

Moreover, users can log detailed statistics including the date and time range during which the game was played. They can also record the players involved, their final rankings, and optionally add notes summarizing the outcome or any important details about the game.

Additionally, users can browse their game history to see when and what games were played. They have the option to edit or update specific details of past game sessions.


---

## 🏗️ App Structure

The app contains four main screens (Activities):

- **Main Activity**  
  The entry screen of the app, where users can navigate to history, create a new game card or start a new session.

- **Session Activity**  
  Used to enter results of a new game session. Users can input scores and other match details.

- **History Activity**  
  Displays a list of all saved game sessions, allowing searching and viewing details.

- **Edit Activity**  
  Allows editing of saved records — changing scores, dates, or notes.


_All activities - dark theme_

<p align="left">
  	<img src="screenshots/MainScreen%20-%20dark.png" alt="Main Activity - dark theme" width="250"/>
	<img src="screenshots/SessionScreen%20-%20dark.png" alt="Session Activity - dark theme" width="250"/>
	<img src="screenshots/HistoryScreen%20-%20dark.png" alt="History Activity - dark theme" width="250"/>
	<img src="screenshots/EditScreen%20-%20dark.png" alt="Edit Activity - dark theme" width="250"/>
</p>

_All activities - light theme_

<p align="left">
  <img src="screenshots/MainScreen%20-%20light.png" alt="Main Activity - light theme" width="250"/>
  <img src="screenshots/SessionScreen%20-%20light.png" alt="Session Activity - light theme" width="250"/>
	<img src="screenshots/HistoryScreen%20-%20light.png" alt="History Activity - light theme" width="250"/>
	<img src="screenshots/EditScreen%20-%20light.png" alt="Edit Activity - light theme" width="250"/>
</p>

---

## ⚙️ Features of Each Activity

### Main Activity
- Main menu of the app.
- Navigation to other screens.
- Possibility to create a new game card.
- Option for moving your cards up, down and deletion.
- Possibility to create a new game session.
- Button for switching between light and dark theme

### Session Activity
- Input date, time, players with their score and positions and note
- Duration is calculated automatically, even if playing past midnight
- Save the session in JSON format.

### History Activity
- Displays a list of all saved sessions arranged by date.
- Search a specific game session and see the details.
- Select a session to edit some details.
- Remove existing session


### Edit Activity
- Edit existing records - time, note and player's score and position.

---

## 🛠️ Technologies Used

- Kotlin  
- Android SDK  
- JSON (for data storage and loading)  

---

## 📱 Download on your android
You can download the latest version of BoardChamp APK here:  
[Download BoardChamp.apk](https://github.com/JozzefBar/BoardChamp-app/releases/download/v1.1/app-release.apk)

---

## 🚀 How to Run the Project

1. Clone the repository using Git command:
   ```bash
   git clone https://github.com/JozzefBar/BoardChamp-app.git

---

## 🐞 To-Do / Known Issues

- [x] Fix the bug during the editing time on session - duration past midnight is calculated wrong
- [x] Possibility to edit player's name, score and position - *(name cannot be changed)*
- [ ] Better banner and visuals - *not that important*
- [ ] Performance is not tested with a large number of game cards/sessions in history etc.
- [ ] Improve visuals - *not that important*
- [ ] In some specific parts there is duplicity in code - *not that important*
- [x] Possibility to download on android easily
- [x] Confirmation of actions, such as deleting a session, etc. - to save if someone missclicks
- [x] Change colors for data entry in create/edit session - if the user has dark mode it is not clearly visible - *(dark theme and light theme added)*
- [x] In session editing there is no validation for player's position.
- [x] Fix deprecated code

---

## ⏳ Functionality that was planned but not done
- A button from the main menu that would create a new scenario, where there would be **player statistics** - this is related to the possibility of automatically adding someone with a name similar to the one the user enters when adding a new player, so that there would be no case where there are two names, _Josh_ and _JOSH_, and because of the different characters it would be taken as two different players. Therefore, the statistics would also show them differently.
