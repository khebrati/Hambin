package top.roomio.data.party

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FirebaseAuthConfigTest {
    @Test
    fun missingProductionKeyExplainsHowToConfigureAuthentication() {
        val config = FirebaseAuthConfig("test-project", "")
        val signUpError = assertFailsWith<IllegalStateException> { config.signUpUrl }
        assertTrue(signUpError.message.orEmpty().contains("ROOMIO_FIREBASE_API_KEY"))
        assertFailsWith<IllegalStateException> { config.refreshUrl }
    }

    @Test
    fun emulatorDoesNotRequireProductionKey() {
        val config = FirebaseAuthConfig("demo-project", "", "localhost:9099")
        assertEquals("http://localhost:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=localkey", config.signUpUrl)
        assertEquals("http://localhost:9099/securetoken.googleapis.com/v1/token?key=localkey", config.refreshUrl)
    }
}
