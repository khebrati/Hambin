# Credential persistence

Firebase ID and refresh tokens are serialized only to `SecureCredentialStorage`.
Android encrypts this payload with AES-256-GCM and stores the non-exportable
encryption key in Android Keystore. iOS stores the payload as a Keychain generic
password item. The remaining Multiplatform Settings record contains only the
profile name, avatar, language, backend identity, and Firebase UID.

The former `party.firebaseSession.v1` plaintext entry is deleted at startup;
credentials from that format are intentionally not migrated. Users must sign in
again after installing this migration.

The desktop/JVM target currently uses process-memory-only credential storage.
That prevents tokens from being written in plaintext, but signing in again is
required after each desktop app restart. Add a platform-specific Keychain or
Credential Manager adapter before shipping persistent desktop sessions. Tests
use a deterministic in-memory fake; it is not part of production DI.

On Android, app data restored without its original Keystore key fails closed:
the unreadable encrypted value is discarded and the guest identity is recreated.
