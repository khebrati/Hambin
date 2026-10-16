# Roomio

### Android
To run the application on android device/emulator:  
 - open project in Android Studio and run imported android run configuration  

To build the application bundle:  
 - run `./gradlew :androidApp:assembleDebug`  
 - find `.apk` file in `androidApp/build/outputs/apk/debug/androidApp-debug.apk`  

### Desktop
Run the desktop application: `./gradlew :desktopApp:run`  
Run the desktop **hot reload** application: `./gradlew :desktopApp:hotRun --auto`  

### iOS
To run the application on iPhone device/simulator:  
 - Open `iosApp/iosApp.xcproject` in Xcode and run standard configuration  
 - Or use [Kotlin Multiplatform Mobile plugin](https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform-mobile) for Android Studio  


### Firebase client configuration

Supply the Firebase API key before building any client, using one of these sources
(in priority order): the `roomio.firebaseApiKey` Gradle property, the
`ROOMIO_FIREBASE_API_KEY` environment variable, or `firebase.apiKey` in the ignored
`Roomio/local.properties` file. For Android, copy the `current_key` for the matching
application ID from your local `androidApp/google-services.json` into that property.
Do not commit either local configuration file or put a real key in tracked Gradle properties.

The data module generates shared Kotlin configuration under its ignored `build/`
directory. Rebuild after changing the key. Without a key, compilation and emulator
authentication remain available, but production authentication fails explicitly
with configuration instructions. Tests supply synthetic keys and need no live credentials.
