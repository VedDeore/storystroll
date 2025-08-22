# Story Stroll- A Treasure Hunt App

A location-based Android mobile game where users can create and discover virtual treasures in the real world using GPS and Google Maps.

## 📱 Overview

Story Stroll is an interactive mobile application that combines real-world exploration with gamification. Users can hide virtual "treasures" at their current location, which other users can then discover by physically visiting those locations. The app features a points-based reward system, leaderboards, and social elements to encourage exploration and discovery.

## ✨ Features

### Core Functionality
- **User Authentication**: Secure sign-up and sign-in using Firebase Authentication
- **Interactive Map**: Real-time location tracking with Google Maps integration
- **Treasure Creation**: Place treasures at your current location with custom titles, descriptions, and images
- **Treasure Discovery**: Find treasures by getting within 20 meters of their location
- **Points System**: Earn points for discovering treasures (first 3 discoverers get 10 points each)
- **Image Support**: Add photos to treasures using camera or gallery

### User Features
- **Profile Management**: Customizable user profiles with profile pictures
- **Leaderboard**: Global rankings based on points earned
- **My Treasures**: View all treasures you've created
- **Saved Treasures**: Bookmark discovered treasures for future reference
- **Navigation Drawer**: Easy access to all app sections

### Visual Indicators
- 🟡 Gold markers: Undiscovered treasures created by others
- 🟢 Green markers: Treasures you've already discovered
- ⚪ Silver markers: Your own treasures

## 🛠 Technical Stack

- **Language**: Java
- **Platform**: Android (Native)
- **Backend Services**:
    - Firebase Authentication
    - Firebase Realtime Database
    - Firebase Storage
- **APIs & Libraries**:
    - Google Maps API
    - Google Location Services
    - Glide (image loading)
    - Material Design Components

## 📋 Requirements

### System Requirements
- Android device running Android 5.0 (API level 21) or higher
- Active internet connection
- GPS/Location services enabled

### Permissions
The app requires the following permissions:
- **Location**: To track your position and enable treasure discovery
- **Camera**: To capture photos for treasures
- **Storage**: To access photos from gallery
- **Internet**: For Firebase services and map data

## 🚀 Installation

1. Clone the repository:
```bash
git clone https://github.com/yourusername/treasure-hunt-app.git
```

2. Open the project in Android Studio

3. Configure Firebase:
    - Create a new Firebase project
    - Add your Android app to the Firebase project
    - Download the `google-services.json` file
    - Place it in the `app/` directory

4. Set up Google Maps:
    - Get a Google Maps API key from the Google Cloud Console
    - Add the API key to your `AndroidManifest.xml`:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="YOUR_API_KEY_HERE"/>
   ```

5. Build and run the app on your device or emulator

## 📱 App Structure

### Activities
- **MainActivity**: Entry point that checks authentication status
- **SignInActivity/SignUpActivity**: User authentication screens
- **TreasureHuntActivity**: Main game screen with map and treasure interactions
- **LeaderboardActivity**: Global rankings display
- **MyTreasuresActivity**: List of treasures created by the user
- **SavedTreasuresActivity**: Bookmarked treasures
- **BaseActivity**: Common functionality for toolbar and navigation

### Data Models
- **User**: User profile information
- **Treasure**: Treasure details and location
- **LeaderboardItem**: User ranking information

## 🎮 How to Play

1. **Sign Up/Sign In**: Create an account or log in with existing credentials

2. **Create Treasures**:
    - Tap the floating action button (+) on the map
    - Add a title and description
    - Optionally add a photo
    - The treasure is placed at your current location

3. **Discover Treasures**:
    - Explore the map to find treasure markers
    - Get within 20 meters of a treasure
    - Tap the marker to discover it and earn points

4. **Track Progress**:
    - View your ranking on the leaderboard
    - Check your total points in your profile
    - Save favorite treasures for later

## 🏆 Points System

- **Discovery Rewards**: The first 3 users to discover a treasure earn 10 points each
- **No Points**: Subsequent discoveries don't earn points
- **Own Treasures**: You cannot earn points from your own treasures

## 🔒 Security & Privacy

- User authentication is handled securely through Firebase
- Location data is only used for treasure placement and discovery
- Profile images are stored securely in Firebase Storage
- No personal location history is tracked or stored

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👥 Team

NUMAD25SU Group 6

## 🙏 Acknowledgments

- Google Maps Platform for mapping services
- Firebase for backend infrastructure
- Material Design for UI components