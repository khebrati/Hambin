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

- **Status:** Proposed for review — three prototype variations implemented in `webUI/` for comparison
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
