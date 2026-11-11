package top.roomio.data.party

import co.touchlab.kermit.Severity
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSessionLogTest {
    @Test fun redactsCredentialsAndUrlsBeforeSavingWhileRetainingUsefulEvents() {
        AppSessionLog.log(Severity.Info, "Voice failed accessToken=secret refreshToken=refresh ticket=private https://media.example/movie?key=hidden Bearer authorization", "Test", null)
        val saved = AppSessionLog.snapshot()
        assertTrue(saved.contains("Application started"))
        assertTrue(saved.contains("Voice failed"))
        for (secret in listOf("secret", "refreshToken=refresh", "private", "media.example", "hidden", "Bearer authorization")) assertFalse(saved.contains(secret), secret)
    }
}
