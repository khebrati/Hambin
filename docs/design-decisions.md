# Design Decisions

## DD-001: Approved Primary Visual Direction

- **Status:** Approved
- **Decision:** Use **Immersive Cinema** for the prototype's primary screen.
- **Rationale:** The direct-video player and custom local controls are the strongest visual focus, while Sync, participant presence, voice activity, URL inspection, leave, and owner-only abort remain clearly available. Supported by REQ-013, REQ-017, REQ-020, REQ-023, REQ-025, REQ-032, REQ-035, and REQ-039.
- **Implementation consequence:** On compact screens, the player leads a vertical composition. On wider screens, the player remains dominant and participant/room information moves into a supporting pane. Supported by REQ-001 and REQ-030.
- **Tradeoff accepted:** Participant presence is visually secondary to the player, so avatars and speaking indicators must remain persistent and high contrast. Supported by REQ-023 and REQ-025.

## DD-002: Prototype Platform

- **Status:** Approved for prototype only
- **Decision:** Implement the experience as a React, TypeScript, and Vite browser client under `webUI/`; use it as the interactive product prototype while the clients evolve.
- **Rationale:** This follows repository guidance while preserving a cross-platform mobile-focused design direction. Supported by REQ-001, REQ-031, and ASM-001.
- **Constraint:** Use local state and fake repositories only. Do not add backend, authentication, production media, or real-time synchronization architecture. Supported by REQ-031.

## DD-003: Material 3 Expressive on Web

- **Status:** Approved for prototype only
- **Decision:** Use semantic CSS custom properties, tonal surfaces, emphasized typography, expressive shape tokens, responsive canonical layouts, and reduced-motion-aware CSS transitions.
- **Rationale:** Material Web is maintenance-only and does not provide full M3 Expressive parity or an official React component library. The prototype therefore uses accessible, spec-aligned React wrappers rather than claiming Compose-level feature parity. Supported by REQ-029, REQ-030, and REQ-039.

## DD-004: Invite and Pre-join Confirmation

- **Status:** Approved for documentation; prototype implementation deferred
- **Decision:** Any current party member can copy the party code or a mock invite link. Invite recipients review the party title, owner status, participant count, and stream status before confirming entry.
- **Rationale:** Sharing completes the path from room creation to participation, while the preview prevents users from entering an unexpected or unavailable room. Supported by REQ-042 and REQ-043.
- **Implementation consequence:** The future prototype needs a party-level invite/share surface plus a pre-join preview with loading, available, owner-absent, full, ended/unavailable, confirmation, and cancellation states. Use semantic Material 3 tonal containers and accessible status text rather than color alone.

## DD-005: Ownerless Room Continuity

- **Status:** Approved for documentation; prototype implementation deferred
- **Decision:** When the owner leaves, the room remains active without an owner. No participant is promoted, and no one can start, replace, or abort the shared stream until the same owner rejoins and regains owner controls.
- **Rationale:** Remaining participants keep their shared context and local playback freedom without weakening the established owner-only permission boundary. Supported by REQ-015, REQ-016, REQ-017, REQ-018, REQ-037, and REQ-044.
- **Implementation consequence:** The future prototype must show a persistent owner-absent status, preserve local playback, Sync, voice, URL inspection, and invite actions, and restore owner-only controls only for the returning original owner.

## DD-006: Compact Room Header and Expressive Participant Presence

- **Status:** Approved for the Compose implementation
- **Decision:** At compact widths below 600dp, use a 64dp small top app bar with a single-line room title, compact brand mark, and unchanged 48dp action targets. Increase party-screen participant avatars from 56dp to 72dp; use a large-increased avatar shape and high-contrast ring for active speakers.
- **Rationale:** The room header must preserve more vertical space for the player on small screens, while participant identity and speaking activity need stronger visual presence within the approved Immersive Cinema direction.
- **Accessibility consequence:** Compacting the header must not reduce action touch targets. Speaking remains communicated through status text, shape, and a high-contrast border rather than color alone.

## DD-007: Invite Friends Share Dialog

- **Status:** Approved for the Compose implementation
- **Decision:** The room invite action opens a responsive Material dialog with a selectable party code and mock invite link. Each option has its own copy action, visible copied feedback, snackbar confirmation, and a manual-selection recovery message when clipboard access fails.
- **Android update (2026-11-11):** Replace the invite-link Copy link action with Share and a share icon. Open the Android Sharesheet with the public invite URL as plain text; retain Copy code and selectable fields. Dismissing the sheet leaves the dialog available, without claiming the invite was sent. If launch fails, show retry/manual-selection recovery. Other clients retain their current link action.
- **Rationale:** Showing both options preserves the prototype flow and lets users share the most convenient identifier without bypassing the required pre-join room preview.
- **Responsive consequence:** Share fields and actions sit side by side when the dialog has at least 400dp of content width and stack at narrower widths or under equivalent layout pressure. All actions retain at least 48dp touch targets.

## DD-008: Home Entry and Room Continuity

- **Status:** Approved for the Compose implementation
- **Decision:** The Home screen opens with the local Nika identity and an ownerless “Friday night screening” room that can be resumed. The primary Create and Join actions remain equally prominent, while the returnable room is a supporting card rather than a competing primary action.
- **Responsive consequence:** At 840dp and wider, the identity and primary actions occupy the leading pane and the returnable room occupies a narrower supporting pane. Below 840dp the card stacks after the primary actions; the two primary actions share a row from 600dp upward and stack on compact widths.
- **Material consequence:** The implementation uses semantic color roles, expressive large-increased containers, 48dp minimum action targets, mirrored directional icons for RTL, and layouts that reflow under large text instead of scaling fixed coordinates.
- **Prototype behavior:** Create, Join, and room-resume actions navigate locally to the existing room experience. Settings edits the mock display name in memory, and the theme action switches the local Material theme.

## DD-009: Party Manager Create and Join Modes

- **Status:** Approved for the Compose implementation
- **Decision:** Use one Party Manager destination with a two-option segmented control. Create is the default mode when entered from “Create a party”; Join is the default when entered from “Join with code.”
- **Create consequence:** The create mode explains ownership before showing one full-width Create action and an abstract cinema illustration. Creating enters the existing empty owner room using local fixture state.
- **Join consequence:** The join action remains disabled until a party code is present. Loading and invalid-code states are represented by the screen API and previews so local repository behavior can be connected without redesigning the surface.
- **Accessibility consequence:** Both mode targets expose tab semantics, directional navigation mirrors in RTL, every icon action retains a 48dp target, and scrolling preserves the complete flow at large font scales.

## DD-010: Full-Screen Profile Editing

- **Status:** Approved for the Compose implementation
- **Decision:** Replace the temporary settings dialog with a dedicated profile destination containing a live identity preview, display-name field, twelve local avatar choices, a disabled language field, and a persistent Save action.
- **Responsive consequence:** Profile content is capped at 712dp inside the 760dp prototype page width. The avatar grid uses three columns from 460dp and two columns below it; the page scrolls behind a safe-area-aware bottom action at compact heights and large font scales.
- **State consequence:** Name and avatar changes remain draft values until Save. Saving trims the display name and updates the Home identity in memory; Back discards the draft. Save remains disabled while the trimmed name is empty.
- **Expressive consequence:** Avatar choices are containerless so their grid width belongs to the portraits rather than card chrome. Selection morphs the portrait into a larger asymmetric shape that overlaps a contrasting tonal frame, adds slight rotation and elevation, and animates between states while retaining a check mark and emphasized label.
- **Accessibility consequence:** Avatar choices expose radio-button selection semantics and visible selected borders/check marks. The live preview and picker have explicit labels, directional navigation mirrors in RTL, and interactive controls retain 48dp minimum targets.

## DD-011: Type-Safe Navigation 3 Back Stack

- **Status:** Approved for the Compose implementation
- **Decision:** Replace the sample-level destination enum with a Navigation 3 `NavDisplay`, a saveable back stack, serializable route keys, and the type-safe entry-provider DSL.
- **Route model:** Home is the root route. Profile and Room are object routes. Party Manager carries its initial Create or Join mode as a serializable route argument rather than mutable navigation state.
- **Back behavior:** App-bar Up actions and system/predictive Back remove the same top entry and never remove the root Home entry. Repeated requests for the current route are ignored to prevent accidental duplicate destinations.
- **Leave behavior:** Confirming Leave in a room clears every entry above Home, so rooms entered through Party Manager cannot reveal that setup flow after departure. Dismissing the confirmation keeps the current Room entry.
- **State consequence:** The local profile identity remains app state outside destination entries and is saveable independently of the navigation stack. Draft profile edits still belong to the Profile entry and are committed only on Save.
- **Motion consequence:** Forward navigation uses a short emphasized horizontal/fade transition; pop and predictive-pop use the matching reverse transition.

## DD-012: Pre-Join Room Preview

- **Status:** Approved for the Compose implementation
- **Decision:** A valid party code opens a dedicated Room Preview route before the user can enter the room. The serializable route carries only the normalized party code; preview details come from local fixtures.
- **Navigation consequence:** “Not now” and app-bar Up pop back to Party Manager with its entered code intact. “Join party” pushes a guest Room route only when availability is Open. Confirmed room departure still clears the entire flow back to Home.
- **State coverage:** Local fixtures represent open/playing, open/owner-away, full, and ended/waiting states. Invalid codes remain in Party Manager with inline error feedback; unavailable previews explain the reason and disable Join.
- **Responsive consequence:** The preview card is capped at 672dp. Its three facts share a row at 560dp and wider and stack below that width; the full screen remains scrollable for compact heights and large text.

## DD-013: Direct Video URL Playback in the Native Client

- **Status:** Approved for the Roomio Compose client (Android first)
- **Decision:** The Android client plays user-supplied direct video URLs through Media3 ExoPlayer within the room screen. The playback engine is owned by an Android-only composable and surfaced to the shared UI through a thin common player API; the ViewModel stays platform-neutral.
- **Scope:** Progressive HTTP(S) files and HLS live streams are supported. Desktop and iOS render the existing canvas placeholder until a platform engine is approved; the shared player seam remains the integration point for them.
- **Validation consequence:** Any well-formed HTTP(S) URL is accepted; playback failures surface through the player error path instead of URL pre-validation. Cleartext HTTP is permitted for this feature through a debug network security configuration.
- **Live consequence:** Live HLS streams (short or unset duration) show a Live indicator and hide scrubber and skip controls. The Sync control remains a local placeholder (targets the room position constant) until a synchronization contract exists.
- **State consequence:** Player state reports IDLE, BUFFERING, READY, PLAYING, PAUSED, ENDED, and ERROR plus position, duration, buffered position, and live flag. The room screen's Loading/Buffering/Playing/Paused/Error states derive from those reports, preserving the required experience states.
- **Platform consequence:** The engine is created lazily only when a player surface is needed (not in previews or the guest waiting states), released with the composable, and paused automatically when the app stops.

## DD-014: Cost-Conscious Go Backend Implementation

- **Status:** Approved for the backend implementation
- **Decision:** Implement the backend defined in `backend/backend-architecture.md` as a portable Go modular monolith. PostgreSQL is authoritative; room events flow through a transactional outbox to authenticated room WebSockets; playback telemetry and single-use realtime tickets stay in memory; LiveKit voice is optional and failure-isolated.
- **Scope:** Guest identity with rotating refresh tokens, room lifecycle with owner-seat reservation and 24-hour tombstones, shared stream start/abort with revisions, explicit sync-to-leader, signed LiveKit webhooks, an OpenAPI 3.1 HTTP contract, an AsyncAPI 3.0 WebSocket contract, and Docker Compose services for TLS ingress, the API, PostgreSQL, LiveKit, and Coturn. Registered accounts, moderation, recording, text chat, server-hosted video, multi-region deployment, Redis, and Kubernetes remain out of scope for v1.
- **Constraint superseded:** The earlier "backend out of scope" constraints in DD-002 and the design-phase restraint guidance continue to apply to `webUI/`; the backend boundary is now implemented under `backend/`.
- **Consequence:** KMP clients can replace fixtures with repositories backed by this contract; the shared player seam in `Roomio/` remains the integration point. Contract tests against the generated Kotlin client and a 1/10/100/1,000-room load profile remain follow-up work.

## DD-015: Single-VM Deployment with LiveKit Built-in TURN

- **Status:** Approved for the backend deployment
- **Decision:** Deploy the Docker Compose stack on one Linux VM (AWS EC2) with Caddy (TLS ingress), the API, PostgreSQL, and LiveKit. Voice media relays through LiveKit's built-in TURN server. This supersedes the Coturn service described in DD-014 for the v1 deployment.
- **Rationale:** LiveKit's built-in TURN advertises itself to clients and issues short-lived relay credentials automatically, removing a container, a shared static-auth-secret, and TLS certificate coordination between two products. A single VM matches the 1-10 concurrent room stage in `backend-architecture.md`.
- **Security consequence:** Only Caddy (80/443) and the TURN endpoint (3478/udp+tcp, 50000-50100/udp) are publicly reachable; the API (8080), LiveKit signaling (7880), and PostgreSQL (5432) stay on the internal network. Secrets live in a `0600` `.env`; the API image runs non-root; Caddy terminates TLS with HSTS and content-type hardening.
- **Tradeoff accepted:** TURN-over-TLS on 5349 and offsite backups/monitoring are deferred. Relay traffic remains DTLS-SRTP encrypted; only TURN-as-TLS-fallback for restrictive networks is unavailable. A dedicated Coturn (or multi-node TURN) returns at the ~100-room scaling stage.
- **Setup guidance:** `backend/deploy-aws.md` documents the AWS console steps (EC2, Elastic IP, security group, Route 53) and how to bring the stack up.

## DD-016: Offline Network Requests Show a Snackbar

- **Status:** Approved for the Roomio native client
- **Decision:** When a network-backed request fails before reaching the backend (DNS, connection, or timeout, e.g. while offline), the native client shows a short snackbar ("You're offline. Check your connection and try again.") instead of crashing or silently doing nothing. Non-connectivity failures facing user input keep their existing inline errors, such as the invalid party code hint.
- **Scope:** Party creation, party code preview, joining, room snapshot on open, and stream start. Backend contract errors continue to map to their stable machine codes; only transport-level failures map to the shared connectivity code.
- **Consequence:** Repository transport failures normalize to a stable `PARTY_CODE_NETWORK` (`NETWORK`) `PartyException`, allowing presentation to detect offline states uniformly. Coroutine cancellation is always rethrown. Voice and token refresh failures for non-party actions remain out of scope for this pass.

## DD-017: Directional Sync with Consent (webUI prototype exploration)

- **Status:** Archived/deferred by stakeholder request on 2026-10-15; preserved on `archive/web-directional-sync`
- **Decision:** Replace the single "Sync to room" action with a directional sync flow. Tapping sync offers two intents:
  - **Bring everyone to me:** sends a consent request to the other participants showing who is asking and the target time ("Mira wants to move to 25:42 — do you agree?"), with a "turn off sync requests from others" option that is also configurable as a room setting.
  - **Take me to others:** opens a chooser where the requester sees each participant's current playback time (and how far ahead or behind they are) and picks one person to follow, then moves to that person's time.
- **Rationale:** The previous single action always forwarded the user to the leading participant and gave the rest of the room no say. Directional sync keeps the two legitimate goals explicit (gather the room, or catch up to someone) and makes cross-device position changes consent-based instead of silent.
- **Interaction consequence:** The sync surface must represent a direction chooser, an outgoing request composer with pending feedback, an incoming request with agree/decline, a participant-with-time picker, and a room-level "allow sync requests" setting. These are required states beyond the existing idle/sync states.
- **Accessibility consequence:** Direction, target time, requester identity, and pending/agreed/declined status must be conveyed with text and shape, not color alone. Time comparisons use tabular numerals. Every new control keeps a 48dp target, and request surfaces use live-region/alert semantics.
- **Prototype variations:** Three coherent directions are implemented behind the `sync` preview states and an in-room variation switcher:
  - **A · Sheet:** modal bottom sheet with two expressive action cards, then a composer or participant list.
  - **B · Inline:** the sync band itself splits into the two intents and expands in place, with an inline incoming-request banner instead of a dialog.
  - **C · Center:** a "Sync center" bottom sheet built around a horizontal participant timeline; users pick a target marker or follow a person's marker.
- **Conflict:** This supersedes the resolved answer to Open Question Important 7 ("do not design visible ahead/behind indicators, participant timelines, or drift badges") and refines Important 4 (the old single sync target was the leading participant). The conflict and the unresolved request semantics are tracked in `open-questions.md`.
- **Scope:** Prototype-only, local state and fixtures. No real-time synchronization, backend contract, or persistence is implied. The native client (`Roomio/`) keeps its existing local sync placeholder until this direction is accepted and a shared contract is defined.



## DD-018: Background Room Session Foreground Service (Roomio Android)

- **Status:** Approved for the Roomio native client (Android first)
- **Decision:** While a user is inside a room, the Android client runs a foreground service that keeps the room session, realtime connection, and voice call alive off-screen. The service shows a persistent notification whose content intent deep-links back into the owning room and whose actions are **Unmute/Mute voice** and **Leave**.
- **Tap consequence:** The notification carries the room id and owner flag as intent extras. The Android host reads and consumes those extras (including on `onNewIntent`) and the shared navigation pushes the matching room route, so tapping the notification returns the user to the room page rather than an arbitrary screen.
- **Action consequence:** Notification actions are delivered to the service and forwarded to the in-process room session over a process-wide channel. **Unmute/Mute voice** flips the local microphone through the same path as the in-room mic control; **Leave** runs the same leave flow as the in-room confirmation, disconnecting voice, stopping the service, and returning the app home.
- **Permission consequence:** The service uses the `microphone|dataSync` foreground service types and requests `POST_NOTIFICATIONS` at runtime on Android 13+ so the notification stays visible. If the platform refuses a microphone foreground start from the background, the service degrades to a data-sync session that still keeps the room connected.
- **Back consequence:** In-room system back opens the leave confirmation instead of dropping straight to Home; when the video is fullscreen, back exits fullscreen first.
- **Limitation:** Voice is owned by the in-process room session, so the service keeps an existing process alive but cannot restore voice after the process is killed; the notification only exists while the session does. Ownership of the voice call inside the service remains follow-up work.

## DD-019: Closing the Android App Leaves the Room

- **Status:** Approved for the Roomio native client (Android first)
- **Decision:** Backgrounding a room keeps the session alive through the DD-018 foreground service, but closing the app is a leave. When the room screen is destroyed or the task is removed from recents, the client sends the same `POST /v1/rooms/{roomId}/leave` request as the in-room Leave action before stopping the foreground service, so the membership is released for every other participant.
- **Rationale:** A user who swipes the app away has left the party. Without an explicit leave the backend only saw a dropped socket, marked the member *absent* during the reconnect grace period, and kept them in the participant list as "away" even though they were gone.
- **Consequence:** The service exposes a task-removal hook that ends the session (`SessionKeeper.stopAndLeave`). The in-process session releases the membership first and only then stops the service, so the process stays alive long enough to deliver the request. When the owner closes the app, the backend emits `member.left` plus `owner.presence: false`; guests and the owner are removed from the voice party list and the owner-away banner remains until the same owner rejoins, consistent with DD-005.
- **Scope:** Android. Other platforms have no background session to keep alive, so process exit continues to release the socket without an explicit leave.

## DD-020: Local Subtitle Selection in the Android Player

- **Status:** Approved for the Roomio Android client
- **Decision:** The Android player supports soft subtitle tracks exposed by the video URL, including supported embedded/container tracks and HLS subtitle renditions, plus user-selected local subtitle files. Hardcoded subtitles are already part of the video image and need no separate track.
- **Interaction consequence:** A subtitles control is available in the room player controls. It lists available tracks, allows subtitles to be turned off, and opens Android's document picker for a local subtitle file.
- **Scope consequence:** Subtitle choices apply only to the current device, consistent with independent local playback. The Android document picker and Media3 track selection remain behind the shared player/platform boundaries; other targets retain their existing placeholder player behavior.
- **Supported local formats:** SubRip (`.srt`), WebVTT (`.vtt`), SSA/ASS (`.ssa`/`.ass`), and TTML (`.ttml`, `.dfxp`, `.xml`) as supported by Media3.

## DD-021: Local Audio Track Selection in the Android Player

- **Status:** Approved for the Roomio Android client
- **Decision:** Android viewers can choose among supported audio tracks exposed by the direct video URL or restore Media3's automatic audio-track selection.
- **Interaction consequence:** When a URL provides supported audio, an audio-track icon appears alongside the room player's other media controls. It opens a Material 3 selection dialog with the automatic option; available source labels are listed when there are multiple tracks, while a single-track URL presents Automatic as the sole option.
- **Scope consequence:** Audio-track selection applies only to the current device and does not affect other participants. The player reports available tracks through the shared player boundary; non-Android targets continue using their existing placeholder implementation until platform playback is approved.

## DD-022: WatchParty Fork Migration with Peer-to-Peer Voice

- **Status:** Migration direction approved; implementation plan recorded, not yet implemented.
- **Decision:** Replace the existing application backend with an extension of [khebrati/watchparty](https://github.com/khebrati/watchparty), use Firebase Anonymous Authentication for guest credentials, and replace LiveKit voice with audio-only peer-to-peer WebRTC plus private TURN. Deploy the fork, PostgreSQL, Caddy, and private Coturn on a conventional VPS; Firebase databases, Functions, and App Hosting are not selected. Client implementation, transport, dependency, and interface changes are allowed; all existing user-visible functionality must remain available.
- **Authentication consequence:** Anonymous sign-in is automatic with no new registration/login UI. Firebase UID maps uniquely to an internal UUID; PostgreSQL retains profile/ownership data. Replace custom JWT/refresh issuance with Firebase token verification and direct client refresh, disable anonymous-account automatic cleanup, and preserve identity during provider outages. Client changes include bootstrap and credential/namespace reauthentication; domain/presentation stay provider-neutral.
- **Experience constraint:** Preserve guest identity/ownership, party codes/invitations, preview/confirmation, ownerless continuity, local player controls, requester-only explicit sync without rewind, opt-in muted voice, speaking indicators, background/notification behavior, clean leave, and local subtitles/audio tracks. Do not adopt the fork's shared play/pause/seek behavior or add camera/text/virtual-browser features.
- **Migration consequence:** Reuse unchanged fork capabilities first in an isolated harness, then implement product gaps and client adapters. The project is not live; old rooms, profiles, settings, and credentials are disposable, and testers may uninstall/reinstall. Switch directly to a fresh database/new app after parity tests; no legacy adapters, old-data/preference import, mixed voice modes, or drainage period is required. New profile/settings and identity functionality still work normally. Retire Go/LiveKit from the active project once the replacement passes verification.
- **Prior decisions:** This supersedes the final Go/LiveKit technology choice in DD-014/DD-015 once migration completes, while retaining their accepted product behavior. DD-016 and DD-018 through DD-021 remain in force. DD-017 remains an unaccepted archived exploration. DD-023 subsequently authorizes local browser/backend integration.
- **Plan:** [Migration phases and rollout](watchparty-backend-migration-plan.md) and [behavior/interface acceptance specification](watchparty-migration-compatibility.md).


## DD-023: Browser Integration with the Migrated Backend

- **Status:** Approved by the stakeholder on 2026-10-15; local implementation.
- **Decision:** Archive the browser-only directional sync exploration and restore the single requester-only Sync action. Connect the browser client to the migrated Firebase/WatchParty backend, including profiles, room lifecycle, real local playback, forward-only sync, and audio-only peer mesh voice. This supersedes earlier webUI-only restrictions against networking and the requirement to retain active DD-017 variations.
- **Preservation:** `archive/web-directional-sync` retains the original preview. Rollback `22c34ed` restores only the frontend delta from `e4f7071`; published Android/backend history remains intact.
- **Boundary:** Use the existing shared `/v1`, ticketed Socket.IO, and voice contracts. Normal entry uses live services; explicit fixture preview routes remain isolated with no backend/Auth activity. Browser dependencies remain within `webUI`.
- **Platform adaptation:** Browser codecs, microphone/autoplay permissions, HTTPS, cross-origin media policies, and WebVTT/HLS track support apply. Android foreground services and platform-specific track formats are not browser requirements. Established video stays local across room reconnects; unexpected tab closure uses lease expiry rather than a guaranteed background leave request.
- **Rollout:** Validate against the local Auth Emulator/PostgreSQL/Coturn stack first. Hosted deployment and physical browser-to-Android audio acceptance remain separate gates. See [browser integration evidence](web-browser-backend-integration-evidence.md) for measured checks and limitations.


## DD-024: Same-Origin Hosted Browser and API

- **Status:** Approved by the stakeholder on 2026-10-15.
- **Decision:** Deploy the integrated browser build and migrated Node API together on VPS `46.249.98.175`. Caddy serves the frontend and routes `/v1` and `/socket.io` to the internal API on the same origin. Use `watchparty.audiosense.ir` temporarily and `turn.watchparty.audiosense.ir` for authenticated voice relay; keep both DNS-only. This supersedes the earlier temporary `watchparty.me`/`watchparty.top` hostname proposals and the local-only deployment scope.
- **Configuration:** Domain names belong in protected deployment configuration. A later switch to `watchparty.top` changes ingress and TURN configuration. Because browser storage is origin-scoped, moving domains does not automatically preserve the browser's anonymous identity; retain the old origin or plan an explicit transition.
- **Boundary:** Deploy the existing browser integration and pinned WatchParty fork; no production Auth Emulator, legacy Go/LiveKit cutover, Git publication, or capacity claim is implied. Admin credentials stay outside Git and images. See [deployment operations](../backend/deploy/README.md).


## DD-025: Watchparty.top as Primary Hosted Domain

- **Status:** Approved by the stakeholder on 2026-10-16.
- **Decision:** Use `watchparty.top` as the primary website and same-origin API, and `turn.watchparty.top` for authenticated TURN. Keep `watchparty.audiosense.ir` as a Caddy-served legacy alias so existing browser identities remain usable on the origin where their anonymous credentials are stored.
- **Configuration:** Point both site hostnames and the TURN hostname to VPS `46.249.98.175`; configure `SITE_DOMAIN=watchparty.top` and `TURN_DOMAIN=turn.watchparty.top`. Native clients use `https://watchparty.top` as their backend base URL. Firebase's `firebaseapp.com` auth domain remains unchanged; add `watchparty.top` to authorized domains if required by project policy.
- **Consequence:** Browser identity storage is origin-scoped and is not copied to the new hostname. The legacy site alias must remain reachable while users depend on credentials stored there. See [deployment operations](../backend/deploy/README.md).


## DD-026: Fresh Deployment on the Replacement VPS

- **Status:** Approved by the stakeholder on 2026-10-23.
- **Decision:** Deploy a fresh browser/API/PostgreSQL/Coturn stack on `45.146.241.92`, using `watchparty.top` for the site/API and `relay.watchparty.top` for TURN. Start with an empty application database and fresh database/TURN secrets; do not migrate old server data.
- **Configuration:** Retain the existing Firebase project `hambin-dab14` and its authentication configuration. The application database is fresh; Firebase accounts and browser-local credentials are not deleted by this deployment. Redirect `www.watchparty.top` to the primary origin.
- **Prior decisions:** Supersedes DD-024/DD-025 host and TURN hostname settings and the requirement to serve a legacy origin on the replacement VPS. Leave the old server untouched.

## DD-027: Request Browser Playback Before Metadata Completes

- **Date:** 2026-10-23
- **Status:** Authorized by the request to investigate, fix, and deploy slow direct-video startup.
- **Decision:** After a shared URL reaches the player, request native playback immediately with automatic preloading. For hls.js, request playback when the manifest is parsed. Handle autoplay denial with a local Play action without waiting for metadata.
- **Slow sources:** After 15 seconds of loading/buffering, offer guidance and an explicit local retry while preserving the in-progress download. For initial MKV loading on Firefox before 158, explain the known full-file AAC metadata scan and suggest an updated browser or a different source format. Do not automatically restart or fail a slow source.
- **Boundary:** Each browser fetches movie bytes directly from the supplied source. No video proxy, server download, storage, transcoding, or new service contract is introduced. Native Android playback remains unchanged.
- **Evidence and limits:** See [playback investigation](video-playback-investigation-20261023.md). Removing the metadata-only startup cycle does not fix a browser demuxer bug or guarantee a startup time for external sources.


## DD-028: Backend-Owned Automatic Guest Authentication

- **Status:** Approved by the stakeholder on 2026-10-23; implementation and deployment authorized.
- **Decision:** Remove Firebase from both clients, the active Node backend, dependencies, development tooling, and deployment configuration. Keep automatic guests with no registration/login UI; PostgreSQL owns identity and sessions as well as existing product data. Retire the unused fork server/UI that depends on Firebase. Node, PostgreSQL, Caddy, and private Coturn remain the selected stack.
- **Credentials:** Use independent opaque 32-byte random access/refresh credentials and persist only SHA-256 credential hashes server-side. Access expires after fifteen minutes; stable refresh credentials have a sliding 180-day inactivity lifetime and are invalidated by revocation. Successful refresh retains identity and existing room/voice state. Stable refresh credentials make concurrent refresh and lost-response retries safe.
- **Client storage:** Browser access tokens stay in memory and refresh credentials use a host-only `Secure; HttpOnly; SameSite=Strict; Path=/` cookie. Browser session mutations require an exact allowed Origin and `X-Roomio-Client: browser`. Native credentials remain behind the existing platform secure-storage boundary. Transient network, storage, or database failures never authorize automatic guest replacement. Confirmed identity reset is the recovery path for permanently unusable credentials.
- **Cutover:** Start with fresh backend guest identities; no Firebase identity/profile/room-ownership importer or mixed-client period. Remove saved old room-resumption state while preserving non-secret local preferences. Existing database product rows need not be erased. Applied migration checksums remain unchanged; the new schema migration removes provider-specific identity mapping and adds credential tables. Rolling back to the Firebase-backed release requires restoring the pre-cutover database and matching release/configuration.
- **Interfaces:** Shared HTTP contracts define guest-session create/refresh/revoke and browser reset. Socket.IO protocol 3 uses `auth.refresh {accessToken}` and one-use room/voice tickets; authentication refresh must preserve membership, playback, voice peers, and mute intent. No Google authentication request is required for guest creation, restoration, or room use.
- **Prior decisions:** Supersedes the Firebase portions of DD-022, DD-023, DD-025, and DD-026. Other accepted product behavior and the current `watchparty.top` / `relay.watchparty.top` deployment remain unchanged. Historical evidence is retained as history, not active setup instructions.
- **Delivery:** Deploy the backend/browser on the current VPS after local tests and browser verification. Compile/test Android and provide the release APK; the stakeholder will perform actual-device verification. Deleting the external Firebase cloud project is outside this rollout.

## DD-029: Browser Media Controls Match Android

- **Status:** Approved by the stakeholder on 2026-10-30.
- **Decision:** Align the browser player with Android's media control order, Material 3 styling, selection dialogs, and compact/expanded layout. Include local play/pause, fifteen-second backward/forward seeking, timeline/time, Sync to room, subtitles, audio tracks, microphone mute/unmute, video mute/volume, and fullscreen. Fullscreen retains these actions, reveals controls on interaction, and hides idle controls after three seconds without hiding keyboard-focused controls. Live sources show a Live indicator and omit ordinary timeline/skip controls.
- **Track selection:** Use subtitle and audio radio-selection dialogs with selected-state feedback. Subtitles offer Off, available tracks, Choose subtitle file, and Done. Audio offers Automatic; list specific tracks when multiple supported tracks are exposed. Each viewer's playback, subtitle, audio, and volume choices stay local; explicit Sync remains requester-only and forward-only.
- **Local subtitle adaptation:** Extend the browser's VTT import to SRT, SSA/ASS, and TTML/DFXP/XML. Convert supported timing and textual content to browser WebVTT locally. Preserve Unicode text and line breaks; advanced font/layout, animation, drawing, and positioning behavior are not guaranteed to match Media3. Reject malformed or unsupported timed content with an actionable error.
- **Platform boundary:** Available source tracks come from HLS or the browser's exposed media-track APIs. Browsers do not expose every MP4/MKV embedded track or support every Android codec. This decision expands DD-023's local subtitle-format scope but does not promise universal container/codec parity. Preserve DD-027 direct-client playback, autoplay recovery, and slow-loading guidance; no server media proxy, download, storage, or transcoding is introduced.

## DD-030: Android Room Invite App Links

- **Status:** Approved by the stakeholder on 2026-10-30.
- **Decision:** Native copy-link actions produce the browser-compatible `https://watchparty.top/?invite=CODE`. Android accepts this HTTPS origin on fresh launches and while running, opens the pre-join preview, establishes the automatic guest session when necessary, and requires explicit Join confirmation. Preview failures provide retry and cancellation; full and ended parties cannot be joined.
- **Boundary:** Validate the incoming action, exact origin, root path, and a single bounded invite parameter. The backend validates room codes, availability, identity, and membership. Incoming links do not grant owner privileges or automatically leave an existing room. Existing notification room-return requests remain separate from invite previews.
- **Deployment:** The website must serve `/.well-known/assetlinks.json` for the Android package and release signing certificate. Debug certificates are excluded. Until website association is published and verified, the handler can be tested explicitly or selected in Android's supported-link settings; automatic browser-to-app routing is not yet guaranteed. This supersedes mock-link wording in DD-004 and DD-007 for production invites.

## DD-031: Retain Mesh Voice with Independent Peer Recovery

- **Status:** Approved by the stakeholder on 2026-11-05; implementation authorized; release acceptance pending.
- **Decision:** Retain Node/PostgreSQL/private Coturn and audio-only eight-person peer mesh. Authenticated roster completion enables the session. Report degraded calls and reconnecting/unavailable peer links explicitly; preserve healthy audio while another link retries. Retry failed links only after three bounded automatic attempts.
- **Lifecycle:** Explicit opt-in starts muted; authorize unmute before capture. Android confirms its microphone foreground service before capture. Task removal leaves; process death never automatically restores the microphone. Movie audio and local playback remain independent.
- **Contract:** Voice protocol 4 separates join, 30-second generation-preserving resume, and ticket-independent ICE credential refresh. Socket leases fence replacement races; stable negotiation IDs make acknowledgement retries idempotent. Room protocol remains 3. Ship coordinated clients/backend without legacy-data migration.
- **Relay:** Add TURN TLS 443 through TCP SNI routing alongside UDP/TCP 3478 and TLS 5349, with configurable capacity ceilings. Capacity and reliable audio require the [physical, relay, recovery, soak, and load acceptance gates](voice-stability-acceptance.md); signaling tests alone do not establish them.

- **DD-031 explicit peer repair:** A user retry by the non-offerer first creates a new local peer epoch, then sends authenticated `requestOffer: true` to the designated offerer. A missing remote epoch uses target epoch zero. Both voice generations and a requester-epoch high-water mark fence stale or duplicate requests. A newer requester epoch proves that remote peer disposal occurred and repairs that addressed pair even if its local ICE state still appears connected; other healthy links stay intact. Automatic restart keeps its strict negotiated-epoch fence.

## DD-032: Browser Active-party Recovery

- **Status:** Approved for browser implementation
- **Decision:** When create or join is blocked by an existing active membership, show a dismissible snackbar with a “Return to party” action. Keep the action visible until used, dismissed, or superseded.
- **Contract:** The authenticated 409 `ALREADY_IN_ROOM` response supplies optional `activePartyCode`. Return uses normal join validation; a failed return shows the relevant recovery error.
- **Boundary:** Preserve one active party per identity. Returning resumes the existing membership; it does not leave the party or create a new one. Native recovery UI is outside this browser change.

## DD-033: Compressed Local Avatar Artwork

- **Status:** Approved by the stakeholder on 2026-11-06.
- **Decision:** Both browser and native clients ship the twelve existing avatars as 512 × 512 WebP images, encoded at quality 80. Preserve avatar identities, artwork, and selection behavior.
- **Assets:** Browser imports reference WebP derivatives; its original PNGs remain unreferenced source artwork and are excluded from the Vite output. Native Compose drawable resources use WebP in place of PNG, retaining the same generated resource names. Native originals remain available in Git history.

## DD-034: Browser Status Feedback Components

- **Status:** Approved for browser implementation by the stakeholder on 2026-11-06.
- **Decision:** Use shared Material 3 styled tonal cards for persistent connection and error feedback, circular progress indicators for ongoing work, and inverse-surface snackbars for brief confirmations. Field validation stays attached to its text field. Recovery actions belong alongside the message.
- **Room continuity:** Per the stakeholder’s follow-up on 2026-11-06, room reconnection runs silently with no reconnecting message, progress card, or delayed notice. Preserve automatic recovery and connection-dependent control availability. Voice feedback lives in the voice pane with readable per-person connection labels and existing retry/resume actions.
- **Accessibility and states:** Status text uses polite live announcements; errors use alerts. Cards reflow for large text, compact screens, and RTL, use light/dark semantic color pairs, and respect reduced motion. Healthy connections add no notice. Persistent failures retain recovery controls; brief confirmations remain dismissible. Selected/pressed/disabled states belong to their existing action components; empty status is represented by absence. Room connection loss adds no notice; media offline errors retain Retry video. This changes browser presentation only.

## DD-035: Browser Voice Controls Match Android

- **Status:** Approved by the stakeholder on 2026-11-06.
- **Decision:** Use Android's voice interaction in the browser: “Talk with the room” with a visible “Join voice” button, “Joining voice…” with an inline spinner, then a secondary-container mic icon button with the same appearance in both mute states and “Mute”/“Unmute” labels. Explicit joining starts muted. Use Android's connected, degraded, reconnecting, and failed-join wording; failed joins offer “Join voice” again.
- **Recovery:** Show a full-width outlined “Retry voice connections” action when degraded or any peer is unavailable. Put peer recovery labels in participant rows. Remove the browser's separate voice status cards and “Disconnect voice” action. Retain “Resume voice audio” for browser autoplay restrictions. This supersedes DD-034's voice-pane presentation where it differs.
- **Interaction:** Voice controls remain available independently of room realtime connectivity. The player's mic control only toggles an existing voice session; joining requires the voice pane's explicit action, including when the player is fullscreen. Leaving the room still releases voice.

## DD-036: Browser Extraction of Embedded MKV Text Subtitles

- **Status:** Superseded by DD-037 after stakeholder review of duplicate media traffic on 2026-11-06. Retained as investigation history.
- **Decision:** When a viewer opens Subtitles for an MKV URL without browser-exposed subtitle tracks, inspect its metadata directly in that browser. Offer supported embedded UTF-8/SubRip text tracks alongside Off and the existing local-file picker. Selecting a track reads nearby indexed clusters and displays timed cues through the browser's text-track renderer. Choices remain local to the viewer.
- **Boundary:** Extend DD-029's browser-track limitation with a bounded client extraction fallback. Preserve DD-027: no media proxy, backend download, storage, or transcoding. Do not scan or download the entire file up front. Discovery reads bounded metadata; media-cluster reads start only after selecting an embedded track. Off, changing tracks, replacing/retrying the source, and leaving the player cancel pending extraction. Extraction failures do not stop video playback.
- **Limits:** Require CORS-readable HTTP byte ranges, usable seek metadata, and supported finite-size clusters. Initially support `S_TEXT/UTF8` with ordinary timed, unlaced BlockGroups; compressed/encrypted tracks, embedded ASS/SSA, and bitmap subtitles retain local-file recovery where applicable. Bound metadata to 2 MiB per element, cluster reads to 8 MiB, and uncached media to 16 MiB per playback window. Keep at most 128 parsed clusters / 2 MiB of estimated cue data. A window looks back 10 seconds and ahead 10 seconds, starting at the preceding indexed cluster; exceptionally long cues beginning before that lookback can be missed after a seek. Native video and extractor reads may duplicate much of the media traffic during continued viewing.
- **Feedback:** Show checking/loading states and recoverable extraction errors with Retry subtitles and local-file selection. Explain additional data use before selection. Unsupported tracks are acknowledged without offering a nonfunctional selection. Preserve light/dark themes, large text, RTL, keyboard access, native/HLS selection, and local imports.
- **Evidence:** See [exact-source investigation](browser-mkv-subtitles-20261106.md). Implementation/testing does not authorize deployment.


## DD-037: Hybrid Browser Playback with Integrated MKV Tracks

- **Status:** Approved by the stakeholder on 2026-11-06; implementation and exact-source testing authorized. Deployment is separate.
- **Decision:** Use libmedia AVPlayer 1.3.1 for URLs whose pathname ends in `.mkv`, in MediaSource mode with browser decoding. Preserve ordinary native playback for other files and existing native/hls.js HLS handling. Keep Hambin's existing controls, local selections, local subtitle imports, and requester-only forward synchronization.
- **Media boundary:** The integrated engine demuxes/remuxes media and renders supported embedded text subtitles from the same media reads. Remove DD-036's separate extractor and duplicate cluster downloads. Sources still require browser-readable CORS and useful range support; supported codecs depend on the browser. No server media proxy, storage, transcoding, or codec-WASM fallback is introduced.
- **Experience:** Subtitles start Off. Offer embedded text tracks and audio choices, preserve Unicode, normalize common SubRip markup, and retain local subtitle-file recovery. Retry failed media/assets and offer explicit browser playback when enhanced playback fails. Browser playback may lose embedded track access. Source changes dispose the old engine; autoplay denial offers Play. Seeks preserve paused state and align to the requested timestamp within available samples; reports exclude seeks.
- **Distribution:** Load versioned, self-hosted assets only on MKV use. Distribute upstream licenses, the pinned source reference, and Hambin's version-checked subtitle seek-order patch. The patch resets old cues before seeking so newly fetched cues survive.
- **Evidence and limits:** See [browser player evaluation](browser-mkv-subtitles-20261106.md). Bitmap subtitles, advanced subtitle layout parity, universal codec support, physical-device acceptance, and full-movie soak remain unverified or unsupported.

## DD-038: Minimal Android Download Row

- **Status:** Design selected and signed Android release hosting/browser integration authorized by the stakeholder on 2026-11-11.
- **Decision:** Use the outlined download row below the browser home page's Create/Join actions: a phone icon, “Native Android app”, “Background voice party”, and a download icon action with the accessible name “Download native Android app”. Preserve the existing Material 3 tokens and responsive light/dark, large-text, and RTL behavior.
- **Scope:** Selects the download-row concept in [Android download designs](android-download-designs.md). No promotional illustration, slogan, or additional paragraph. The stakeholder subsequently authorized a minor Android version bump, signed APK build, public hosting, and integration of its URL into the website. Use a versioned HTTPS URL on the existing host; keep the row visible to all visitors with explicit Android labeling.

- **Release:** The stakeholder explicitly chose a universal APK supporting all four CPU types. Android 1.1.0 (version code 6) uses release code/resource shrinking and compressed native libraries, with all four ABIs preserved. The signed 23.6 MB APK and row are published at `https://watchparty.top/downloads/roomio-1.1.0-6.apk`. The follow-up 1.1.1 (version code 7) includes the guest-name and last-room features and uses `https://watchparty.top/downloads/roomio-1.1.1-7.apk`; the original URL retains its original APK. See [release verification](android-release-20261111.md).

## DD-040: Last-room Home Card for Every Viewer

- **Status:** Approved by the stakeholder on 2026-11-11 for browser and native clients.
- **Decision:** Replace the browser's owner-only return behavior and native hidden fixture card with a Last room card containing the actual title and code of the most recently successfully created or joined room. Owners and guests receive the same shortcut. This supersedes DD-008's fixture identity/room continuity behavior.
- **Persistence:** Save only room metadata locally, scoped to the durable guest identity (and service origin in native preferences). It survives restart and explicit departure from an open room. Failed admissions and previews never replace the last successful room. There is no room-history list or cross-device synchronization; an identity reset cannot expose another identity's saved room.
- **Return:** Open the existing fresh room preview before normal join confirmation, preserving DD-004/DD-012. The server remains authoritative for room capacity, membership, and ownership. Native preview admission restores owner mode when the admitted room belongs to the current identity, matching browser behavior. Returning does not automatically join voice or restore local media position/track selections.
- **States:** No saved room means no card. A saved room shows its title/code and a directional return action using the existing adaptive card. Preview loading disables the browser action; the native preview destination owns its loading state. Temporary network/service failures retain cached metadata and allow retry. Full rooms show the current unavailable preview and remain saved. Confirmed ended/not-found rooms remove the shortcut; leaving the final seat also clears it when confirmed by the backend. A lost membership alone does not imply the room has closed. No separate selected state applies; existing buttons supply focus, pressed, and disabled states.
- **Boundary:** Uses existing preview/join APIs and client-local preferences; no backend contract or service changes. Availability displayed by a previous preview is advisory until the server admits the user. Existing same-tab browser active-room restoration and Android notification return behavior remain available.
- **Validation:** Production browser build and 77 browser unit tests passed; seven last-room browser checks and three active-party recovery checks passed, including owner/guest persistence, fresh previews, full/ended rooms, identity isolation, service retry, cancellation back to Home, and delayed startup responses. Reviewed desktop light and mobile dark/large-text/RTL screenshots. Native data tests: 64 passed; shared UI tests: 55 passed, one skipped; Android debug build passed. Final native verification used isolated test-result directories to avoid concurrent Gradle report collisions. iOS was not validated on this Linux host.


## DD-039: Marvel and DC Default Guest Names

- **Date:** 2026-11-11
- **Decision:** New browser and native guests receive a random name from the same curated list of 120 Marvel and DC characters (60 from each), such as “Spider-Man,” “Batman,” or “Harley Quinn.” Keep every name digit-free; use “Laura Kinney” rather than the numeric alias. Do not append numbers or enforce uniqueness.
- **Continuity:** Generate the default for initial profile creation and keep it through retries and restarts. Backend profiles remain authoritative; preserve existing names, including previous defaults, and allow editing in Settings. No identity reset or migration is needed.


## Deployment backup policy (2026-11-11)

The stakeholder requested removal of backups entirely. Remove the scheduled
backup service/timer, backup script, and stored `/opt/hambin/backups` directory.
Deployments do not create database dumps or file snapshots. Preserve live
PostgreSQL data, protected runtime configuration, public APKs, and TURN renewal.
Retained application releases do not provide database recovery. Historical
deployment evidence remains historical.

## DD-040: Stable Android Voice Output

- **Date:** 2026-11-11
- **Decision:** Android voice defaults to the loudspeaker as soon as voice is joined, including before a movie starts. Prefer wired/USB headphones or an available Bluetooth communication device over the loudspeaker. Retain the current Bluetooth choice when several are available. Starting, pausing, or stopping the movie does not select the voice route.
- **Lifecycle:** Manage communication routing for the voice session, respond to connected/disconnected audio devices, and release the route and restore the preceding audio mode when voice ends or joining fails. Let Android pair the input with the communication output.
- **Audio:** Keep voice and movie playback concurrent, with separate call and media volume categories. Do not request exclusive audio focus or change microphone opt-in/mute behavior. Bluetooth uses communication-capable routes; headset capabilities and Android policy determine the available routes and media quality.
- **Validation:** Data JVM tests passed (69 tests, including five output-policy cases); Android debug build passed. Physical speaker, headset/Bluetooth transitions, and simultaneous audible movie/voice playback remain unverified on hardware for this change.

## DD-041: Room Bug Reports with Session Diagnostics

- **Date:** 2026-11-11
- **Decision:** Stakeholder selected a bug icon next to Share/Invite in the room top app bar, one large description field and Send for browser/native. This supersedes the entry-point alternatives in `bug-report-designs.md`.
- **Delivery:** Send description, device/browser metadata and the current application session log to authenticated private PostgreSQL report storage. Disclose the automatic diagnostic attachment in the dialog, redact credentials/URLs in retained logs, retain drafts on failure, and acknowledge only durable server acceptance.
- **Implementation defaults:** See [shared contract](bug-report-contract.md) for schema, retry/idempotency, limits, 30-day retention and operational access. Captured application logs reset at page/process restart. Logs above the upload limit fail explicitly without silent truncation.


## DD-042: AVPlayer by Default with Browser Recovery

- **Date:** 2026-11-11
- **Status:** Approved by the stakeholder's request to make AVPlayer the default with browser fallback. Implementation is local; publication is separate.
- **Decision:** Route all HTTP(S) browser media sources through the existing pinned libmedia AVPlayer in MSE mode. This supersedes DD-037's MKV-only routing and asset-loading restriction. Retain existing controls and direct-client media access; native clients keep their platform players.
- **Recovery:** On an integrated-engine failure while online, or initial startup still lacking playable data after 30 seconds, dispose that engine and switch once to browser playback. Use native HLS when available and hls.js otherwise. Show “Switched to browser playback.” Autoplay denial offers Play; offline failures offer Retry. Ordinary buffering after readiness does not trigger a switch. This narrowly supersedes DD-027's no-automatic-restart rule for initial integrated-engine startup; native slow-source guidance remains unchanged.
- **Local state:** Preserve the current position, pause/play intent, volume, mute, and imported local subtitle across an automatic switch. Match embedded track selections by labels exposed by the fallback engine; otherwise use Automatic/Off. Browser failure offers Retry in that engine. An explicit Try enhanced player starts a new attempt; replacing the source/session resets the lifecycle. No automatic engine loop is allowed.
- **Manifest compatibility:** AVPlayer handles simple manifests; the pinned MSE engine stalls or recreates SourceBuffers during alternate rendition switching in acceptance tests. Manifests exposing multiple audio renditions or external subtitles automatically use browser/HLS recovery before playback to retain working track controls.
- **Limits:** Container support does not guarantee browser codec support or identical track exposure. Native MKV recovery can encounter Firefox's original metadata stall. Cross-browser/mobile Safari acceptance and full-movie soak remain release checks; no server proxy or transcoding is introduced.
- **Validation:** Production browser build, 83 unit tests, and all 28 focused Chrome player browser checks passed. Coverage includes MKV/MP4, WebM recovery, HLS seeking and alternate-track recovery, autoplay denial, one-shot startup timeout, offline errors, paused-state/settings/local-subtitle handoff, synchronization, fullscreen and light/dark/mobile/large-text/RTL layouts. Autoplay recovery additionally passed three consecutive runs after correcting the test to grant permission on the click rather than before it. Upstream SourceBuffer/IPC diagnostics can still be logged during successful WebM recovery; these are not suppressed. Safari/Firefox automation, physical-device checks and full-movie soak were not performed for this change.


## DD-043: Named Material Color Palettes

- **Status:** Approved for browser and native clients (2026-11-11).
- **Decision:** Settings offers the 24 named seed colors in [theme palettes](theme-palettes.md), each with a visible seed swatch and name. Selecting one immediately applies a full Material Tonal Spot palette at standard contrast, with light and dark variants. Keep Roomio default as the initial choice and reset option.
- **Persistence:** Store the color choice locally, independently of profile Save, navigation, guest identity, and backend connectivity. Unknown stored values fall back to Roomio default. A failed local write shows a retry message; browser selection remains applied for the session, while native retains its last successful choice.
- **Implementation:** Browser uses Google's Material Color Utilities; native uses the installed MaterialKolor. Both use Material 2021 Tonal Spot at contrast 0. Generate semantic accent, container, surface, outline, and foreground roles from the seed. The picker shows the original seed, which may differ from the generated primary. Cinema/player and illustration colors keep their dedicated semantic roles.
- **States:** The local catalog is always populated and works offline; network loading/empty states do not apply. Names, visible selection marks, keyboard interaction, focus/pressed states and touch targets support accessible choice. Light/dark mode remains independent of the selected color.

## DD-044: Local Microphone Speaking Indicators

- **Date:** 2026-11-11
- **Decision:** Browser and Android self-avatar speaking highlights indicate recent local microphone activity after explicit voice opt-in and authorized unmute. They work when alone in voice. They do not confirm delivery, remote playback, or that another person heard the speaker; the stakeholder explicitly retained microphone activity semantics.
- **Lifecycle:** Silence releases the highlight after a short hold; mute, capture suspension, and leaving clear it immediately. Keep initial/muted/loading/failed states inactive. Preserve existing remote-speaker indicators and voice recovery controls. This is a detection change with existing visual/accessibility presentation; themes, large text, RTL and layout remain applicable through the existing components.
- **Implementation:** Browser meters its existing capture stream without audible monitoring. Android observes WebRTC PCM while peers exist and uses a session-owned local AudioRecord meter while no peers exist, releasing that recorder synchronously before peer creation. Local sampling runs independently of remote statistics. Keep only bounded transient sample buffers and scalar levels; do not persist or upload microphone samples. Capture failure leaves the optional indicator inactive and does not break voice transport.
- **Scope:** No backend, signaling protocol, or remote delivery receipt changes. Physical microphone, speaker/headset routing transitions and alone-to-peer handoff remain device acceptance checks; build/unit checks do not establish hardware behavior.


## DD-045: Shared Screen Android Launcher Icon Foundation

- **Date:** 2026-11-11
- **Decision:** Stakeholder selected the Shared Screen concept, with bundled variants for all 24 named theme seed colors. Use the exact seed as each background and a contrasting cream or charcoal mark. Basil is the initial icon matching the selected green concept.
- **Scope:** Provide adaptive/legacy vector resources, stable launcher aliases, and a graph-owned selection API. Leave the theme picker, theme persistence and automatic theme/icon association to the separate integration. Keep app invite links on the always-enabled activity.
- **Behavior:** Android persists the enabled alias; updates are atomic on API 33+, replacement-first on older versions, idempotent, and expose recoverable failures. Android themed icons may override palette colors. Other native platforms explicitly report unsupported.
- **Validation:** Android debug assembly, app JVM graph compilation and data JVM tests passed, including eight switching tests. Catalog validation verifies all 24 colors and aliases. Palette preview reviewed; device/OEM launcher refresh, shortcuts and upgrade acceptance remain unverified because no Android device is connected. See [implementation and integration notes](android-launcher-icons.md).


## DD-046: Android Launcher Icon Follows Theme Color

- **Date:** 2026-11-12
- **Decision:** Connect the Shared Screen launcher icon to the saved named theme choice on Android. Each of the 24 named themes selects the matching exact-seed icon; Roomio default selects Basil. This completes the deferred theme integration in DD-045. Light/dark mode does not select another alias.
- **Lifecycle:** Apply the saved choice at app startup and subsequent successful theme changes independently of profile Save, navigation and connectivity. Keep the observer above navigation and use the application graph's controller. No separate icon preference is introduced.
- **Recovery:** Preserve the saved theme if launcher switching fails and show a Retry/dismiss snackbar. A new theme cancels an obsolete pending retry; reopening the app reconciles the saved color again. Failed theme storage leaves the preceding theme/icon selected. Unsupported platforms skip synchronization.
- **Limits:** Launcher refresh timing, OEM behavior and themed-icon overrides retain the limits documented in [Android launcher icons](android-launcher-icons.md).
- **Validation:** Android debug assembly and app JVM graph compilation passed. Nine focused tests passed, including Compose checks for restored/live color changes, dark-mode recomposition without switching, and snackbar Retry, plus mapping/failure/cancellation cases. Physical Android launcher refresh remains unverified; no device is connected.


## DD-047: Simplified Browser Home

- **Date:** 2026-11-12
- **Scope:** Browser client only, as requested by the stakeholder.
- **Decision:** Remove decorative app icons throughout the web UI, including design previews. Home has no app bar, app name, or tagline; place the theme and Settings icon buttons in the page content above the main headline. Remove “Ready when everyone is” and “Your next watch party is one room away.” Use “Stream and chill” as the home headline. Retain the profile, party actions, download link, and last-room shortcut.
- **States:** Existing theme switching, keyboard focus, pressed states, and settings navigation remain available. The controls require no network or loading/empty state. Native presentation remains governed by its existing decisions.


## DD-048: Simplified Native Home

- **Date:** 2026-11-12
- **Decision:** Extend DD-047's simplified home to Android through the shared native Compose screen, also used by iOS and desktop. Remove the home app bar, decorative brand mark, name, tagline, eyebrow and supporting copy. Use “Stream and chill” as the headline. Place theme and Settings controls in the scrollable content above it, aligned to the logical end.
- **Layout and states:** Retain safe drawing insets, 48dp icon targets, semantic themes, compact/expanded layouts and scrolling for smaller windows or large text. Retain the profile, Create/Join actions and Last room shortcut, including their existing offline behavior. The local header has no loading, empty or error state.
- **Scope:** This removes the native home’s decorative app branding; no other native in-app brand mark was found. Android launcher icons and saved-theme icon switching remain unchanged.

- **Validation:** Android debug assembly passed. Existing Compose suite passed 10 tests with one existing skip. Temporary Compose renderer checks passed and screenshots were reviewed at 390×844 light, 1280×800 dark, and 360×800 dark/RTL/1.8× text; theme toggling worked and Create/Join remained visible. No Android device was connected; physical-device and iOS validation were not performed.


## DD-049: Persian Language in Browser and Android

- **Date:** 2026-11-12
- **Decision:** Enable English and فارسی in Settings for both clients. Save applies the choice with the profile; Back discards an unsaved language choice. Existing and unsupported preferences use English. This supersedes DD-007's disabled language field and the earlier English-only design scope.
- **Persistence:** Browser maps the profile choice to the existing backend `en`/`fa` language field and caches the saved language for startup and recovery messages. Native stores the language in its local profile, observes it above navigation, and sends its tag when creating/joining/restoring the guest session. No service contract change is required.
- **Presentation:** Translate application copy, recovery messages, player/voice actions, labels, and named color/avatar choices. Persian uses RTL layout with mirrored navigation arrows. Keep URLs, room codes, playback timelines, and media content in their appropriate original direction. Do not translate guest names, room titles, track metadata or subtitle cues. Android foreground-service notification strings follow the selected process language.
- **States:** The bundled language choices are available offline with radio/select selection, keyboard/focus and pressed feedback. Native local Save failures retain the preceding applied language and show the existing retry error. Browser profile Save uses its existing loading, failure/draft retention and retry behavior; language applies only after server acceptance. Loading/empty states do not apply to the fixed language catalog.
- **Platform boundary:** Use Compose resource qualifiers with a small platform locale environment; shared native resources also become available to desktop/iOS. Android and JVM compilation/tests are checked locally; iOS runtime remains unverified on Linux.
- **Validation:** Browser production build and all 87 unit tests passed. Four Chrome Persian checks passed: saved language/reload/English recovery, draft discard and failed Save, compact dark/large-text and expanded light layouts across Home, Settings, party manager, preview and room. Screenshots reviewed. Android debug assembly and app JVM graph compilation passed; shared Compose language/resource/RTL switching test and all five profile persistence tests passed. All 255 native resource keys and format arguments match the Persian catalog. Android locale setup preserves the Activity context for fullscreen and sharing. Physical Android and iOS runtime checks were not performed. Published with Android 1.2.0/code 10 and the browser release `20261112-persian-code10`; see [release evidence](persian-release-20261112.md).
