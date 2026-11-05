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


### Backend configuration

Guest sessions and product APIs use `https://watchparty.top`. No external authentication
configuration is required. Debug Android builds and desktop builds can override the
origin through `roomio.backendUrl`; Android accepts that extra only in debug builds.
Credentials belong to that exact backend origin and are kept in the platform secure store.

### Android invite links

Room invites use `https://watchparty.top/?invite=CODE`. Android handles cold and warm
`ACTION_VIEW` launches and shows the room preview; entry requires tapping Join.
Loading, offline/error retry, full, and ended states use the same preview flow.

Publish `webUI/public/.well-known/assetlinks.json` with the website before expecting
automatic Android App Links routing. It trusts the existing release certificate
only, not debug keys. Verify the public endpoint returns JSON over HTTPS without a
redirect, then run `adb shell pm verify-app-links --re-verify top.roomio.app.androidApp`
and `adb shell pm get-app-links top.roomio.app.androidApp` on a release installation.
If Play App Signing is introduced, add its application signing certificate fingerprint.
To test handler delivery without domain verification, use:

```sh
adb shell am start -W -a android.intent.action.VIEW \
  -d 'https://watchparty.top/?invite=ABCD2345' -p top.roomio.app.androidApp
```
