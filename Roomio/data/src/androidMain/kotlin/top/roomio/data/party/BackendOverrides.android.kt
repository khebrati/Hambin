package top.roomio.data.party

internal actual fun platformBackendOverrides(): BackendOverrides = BackendOverrides(
    backendUrl = System.getProperty("roomio.backendUrl"),
    firebaseProjectId = System.getProperty("roomio.firebaseProjectId"),
    authEmulatorHost = System.getProperty("roomio.authEmulatorHost"),
)
