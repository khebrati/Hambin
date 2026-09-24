package top.roomio.domain.party

/**
 * Keeps a room session alive while the app is in the background. Android runs a
 * foreground service so the realtime connection and voice call survive; other
 * platforms do nothing.
 */
interface SessionKeeper {
    /**
     * Starts or updates the background session. [voiceActive] selects a
     * microphone-capable foreground service so voice keeps working when the app
     * is not in the foreground.
     */
    fun start(voiceActive: Boolean)

    /** Stops the background session once the user leaves the room. */
    fun stop()
}
