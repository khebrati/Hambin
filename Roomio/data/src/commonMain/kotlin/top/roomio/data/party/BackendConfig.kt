package top.roomio.data.party

/** Backend connection settings resolved by the application graph. */
data class BackendConfig(
    val baseUrl: String,
    val firebaseProjectId: String = DEFAULT_FIREBASE_PROJECT_ID,
    val firebaseApiKey: String = DEFAULT_FIREBASE_API_KEY,
    /** Emulator host/port, for example `10.0.2.2:9099` on Android emulators. */
    val firebaseAuthEmulatorHost: String? = null,
)

const val DEFAULT_FIREBASE_PROJECT_ID = "hambin-dab14"
const val DEFAULT_FIREBASE_API_KEY = "REMOVED_CONFIGURE_LOCALLY"

data class FirebaseAuthConfig(
    val projectId: String,
    val apiKey: String,
    val emulatorHost: String? = null,
) {
    val signUpUrl: String
        get() = if (emulatorHost == null) {
            "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$apiKey"
        } else {
            "http://$emulatorHost/identitytoolkit.googleapis.com/v1/accounts:signUp?key=localkey"
        }

    val refreshUrl: String
        get() = if (emulatorHost == null) {
            "https://securetoken.googleapis.com/v1/token?key=$apiKey"
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
