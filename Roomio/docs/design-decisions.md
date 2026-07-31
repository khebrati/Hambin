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
- **Expressive consequence:** All avatars use larger, low-inset portraits. Selection morphs the portrait into a larger asymmetric shape that overlaps a contrasting tonal frame, adds slight rotation and elevation, and animates between states while retaining the card border and check mark.
- **Accessibility consequence:** Avatar choices expose radio-button selection semantics and visible selected borders/check marks. The live preview and picker have explicit labels, directional navigation mirrors in RTL, and interactive controls retain 48dp minimum targets.
