# Side-by-side party chat implementation

The selected DD-050 side-by-side concept is implemented for the active Node/PostgreSQL backend, browser, and shared Compose Android/iOS/desktop client under DD-052. The shared contract is [chat-contract.md](chat-contract.md), with machine-readable HTTP and room-event schemas under `backend/openapi` and `backend/asyncapi`.

## Delivered behavior

- Authenticated current members can send plain text/emoji independently of voice, owner presence and stream playback. Server-authoritative names, avatars, timestamps and room event ordering accompany messages.
- Durable PostgreSQL messages and an atomic room event outbox support unchanged UUID retries without duplicate sends. A separate chat rate limit permits 10 new messages per identity per sliding 10 seconds; replays do not consume that budget.
- Authoritative snapshots contain the latest 50 messages; exclusive sequence cursors load earlier pages. Reconnect replaces the displayed remote history window rather than merging a gap. Stale pagination responses cannot overwrite newer snapshots. The client bounds displayed remote history to 500 messages and pending sends to 20.
- Web and shared native UI provide the selected panel alongside the movie at expanded widths and below player/sync at compact widths. They include sender avatars, plain-text bubbles, actual timestamps, emoji insertion, hardware/IME send controls, drafts, failed-send retry/discard, earlier history, collapse/unread, latest-message navigation and offline feedback.
- English/Persian labels, original message direction, light/dark Material themes, large text and keyboard access are covered. Native layout includes IME padding. Messages are not added to diagnostics by the chat implementation.
- History is private to current members while the room is open and is deleted with the existing 24-hour room tombstone cleanup. No attachments, reactions, read receipts, typing indicators or background notifications were added.

## Verification

- Backend TypeScript build and 12 unit checks passed. All nine real PostgreSQL/Socket.IO integration checks passed, including chat authorization/isolation, durable retries, profile-safe sender assignment, rollback, rate limiting, history pages and room cleanup, plus existing guest/room/voice invariants. A regression verifies correct chat sequence allocation after membership-expiry events. The existing advisory-lock test now scopes its lookup to its own database so isolated runtimes do not interfere.
- Web production build and 96 unit tests passed, including ordered chat reduction and stale-history rejection. Two real Chrome acceptance checks exercised separate identities, HTTP/native-shaped messages, lost-response reconciliation, explicit same-UUID retry, offline draft/reconnect, unread count, rejoin history and responsive 1440px/390px/360px Persian RTL/large-text layouts. Screenshots were reviewed.
- Native final verification passed `:data:jvmTest`, `:sharedUI:jvmTest`, `:app:compileKotlinJvm`, `:androidApp:assembleDebug` and `:desktopApp:compileKotlin`. The measured suites contained 97 data tests and 72 shared UI tests, with one pre-existing UI skip. Tests include Unicode/500-unit validation, DTO/event framing, retry/draft reconciliation, unread deduplication, stale pagination, and Compose scrolling/retry/collapse/disabled-send behavior.
- A real Ktor HTTP/Socket.IO JVM integration test passed against the isolated Node/PostgreSQL runtime, using the production native session/repository/realtime adapters and two native identities. It is opt-in with `CHAT_BACKEND_URL=http://127.0.0.1:8082`.
- Maximum-size Unicode chat bootstrap regression passes within a bounded 256 KiB native Socket.IO frame limit. The former 64 KiB limit could reject a valid 50-message Unicode snapshot.
- Shared Compose compact Persian RTL/1.5x text and expanded English screenshots were reviewed. The current-video-link regression now scrolls to its action after the new panel increases the support column height.
- Both YAML contracts parse and their internal references resolve; English/Persian native chat resources have matching keys; whitespace checks pass.

## Delivery and limits

This implementation is delivered as local source commits. No public deployment, signed release or phone installation was performed for this task. Migration `0005_chat.sql` is applied by the existing backend startup migration runner and must ship with the updated runtime before clients are released. Existing clients tolerate the additive chat event/snapshot fields; the room protocol remains version 3.

Local live web acceptance uses Vite on `http://127.0.0.1:4173` with the isolated API on port 8082. Existing mock design comparisons remain at `/?preview=chat`.

The iOS implementation is in shared domain/data/Compose code and uses the existing Darwin Ktor engine. iOS compilation/runtime and macOS desktop packaging require macOS/Xcode and were not validated on this Linux host. Physical Android touch/keyboard/IME behavior and installation were not verified. Desktop common UI and real JVM transport were checked, without launching a packaged desktop application. Database backups retain their existing operator policy; the room purge is the application's live-data retention boundary.
