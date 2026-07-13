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

