package top.roomio.data.party

import io.ktor.http.URLBuilder
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackendAuthorizationScopeTest {
    private val backend = "http://10.0.2.2:8081"

    @Test
    fun firebaseAndOtherHostsNeverMatchBackendBearerOrigin() {
        assertTrue(hasBackendOrigin(URLBuilder("http://10.0.2.2:8081/v1/rooms").build(), backend))
        assertFalse(hasBackendOrigin(URLBuilder("http://10.0.2.2:9099/identitytoolkit.googleapis.com/v1/accounts:signUp").build(), backend))
        assertFalse(hasBackendOrigin(URLBuilder("https://identitytoolkit.googleapis.com/v1/accounts:signUp").build(), backend))
        assertFalse(hasBackendOrigin(URLBuilder("http://attacker.test:8081/v1/rooms").build(), backend))
        assertFalse(hasBackendOrigin(URLBuilder("https://10.0.2.2:8081/v1/rooms").build(), backend))
    }

    @Test
    fun firebaseAuthRequestsWithApiKeyQueriesAreExcludedFromHttpLogs() {
        val productionSignUp = URLBuilder(
            "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=test-api-key",
        ).build()
        val productionRefresh = URLBuilder(
            "https://securetoken.googleapis.com/v1/token?key=test-api-key",
        ).build()
        val emulatorSignUp = URLBuilder(
            "http://10.0.2.2:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=localkey",
        ).build()
        val emulatorRefresh = URLBuilder(
            "http://10.0.2.2:9099/securetoken.googleapis.com/v1/token?key=localkey",
        ).build()

        assertFalse(shouldLogHttpRequest(productionSignUp))
        assertFalse(shouldLogHttpRequest(productionRefresh))
        assertFalse(shouldLogHttpRequest(emulatorSignUp))
        assertFalse(shouldLogHttpRequest(emulatorRefresh))
        assertTrue(shouldLogHttpRequest(URLBuilder("http://10.0.2.2:8081/v1/rooms").build()))
    }
}
