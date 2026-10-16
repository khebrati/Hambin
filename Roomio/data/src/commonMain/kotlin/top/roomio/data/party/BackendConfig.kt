package top.roomio.data.party

/** Backend connection settings resolved by the application graph. */
data class BackendConfig(
    val baseUrl: String,
    val firebaseProjectId: String = DEFAULT_FIREBASE_PROJECT_ID,
    val firebaseApiKey: String = BuildFirebaseConfig.apiKey,
    /** Emulator host/port, for example `10.0.2.2:9099` on Android emulators. */
    val firebaseAuthEmulatorHost: String? = null,
)

const val DEFAULT_FIREBASE_PROJECT_ID = "hambin-dab14"

data class FirebaseAuthConfig(
    val projectId: String,
    val apiKey: String,
    val emulatorHost: String? = null,
) {
    private fun requiredApiKey(): String = apiKey.also {
        check(it.isNotBlank()) {
            "Firebase API key is missing. Set ROOMIO_FIREBASE_API_KEY, the roomio.firebaseApiKey Gradle property, or firebase.apiKey in local.properties, then rebuild."
        }
    }

    val signUpUrl: String
        get() = if (emulatorHost == null) {
            "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${requiredApiKey()}"
        } else {
            "http://$emulatorHost/identitytoolkit.googleapis.com/v1/accounts:signUp?key=localkey"
        }

    val refreshUrl: String
        get() = if (emulatorHost == null) {
            "https://securetoken.googleapis.com/v1/token?key=${requiredApiKey()}"
        } else {
            "http://$emulatorHost/securetoken.googleapis.com/v1/token?key=localkey"
        }
}

internal data class BackendOverrides(
    val backendUrl: String? = null,
    val firebaseProjectId: String? = null,
    val authEmulatorHost: String? = null,
)

internal expect fun platformBackendOverrides(): BackendOverrides
