# Design Decisions

## DD-001: Approved Primary Visual Direction

- **Status:** Approved
- **Decision:** Use **Immersive Cinema** for the prototype's primary screen.
- **Rationale:** The direct-video player and custom local controls are the strongest visual focus, while Sync, participant presence, voice activity, URL inspection, leave, and owner-only abort remain clearly available. Supported by REQ-013, REQ-017, REQ-020, REQ-023, REQ-025, REQ-032, REQ-035, and REQ-039.
- **Implementation consequence:** On compact screens, the player leads a vertical composition. On wider screens, the player remains dominant and participant/room information moves into a supporting pane. Supported by REQ-001 and REQ-030.
- **Tradeoff accepted:** Participant presence is visually secondary to the player, so avatars and speaking indicators must remain persistent and high contrast. Supported by REQ-023 and REQ-025.

## DD-002: Prototype Platform

- **Status:** Approved for prototype only
- **Decision:** Implement the experience as a React, TypeScript, and Vite browser prototype under `prototype/`.
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
