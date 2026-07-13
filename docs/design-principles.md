# Design Principles

## Principles Derived From Requirements

| Principle | Meaning | Support |
| --- | --- | --- |
| Mobile-first shared watching | Design for a cross-platform mobile watch party experience where users create or join rooms and watch direct video URLs. | Supported by REQ-001, REQ-002, REQ-009, REQ-010, REQ-011. |
| Clear role boundaries | Owner-only actions must be visually and behaviorally distinct from participant actions. | Supported by REQ-015, REQ-016, REQ-032, REQ-035. |
| Local control by default | Play, pause, seeking, and volume are personal controls and should not imply group control. | Supported by REQ-017, REQ-018, REQ-026, REQ-027, REQ-028. |
| Explicit synchronization | Sync should be an intentional user action that aligns local playback to the leading participant without exposing playback drift. | Supported by REQ-020, REQ-021, REQ-022, REQ-038. |
| Voice-only presence | Social communication should be designed around voice chat, participant avatars, and speaking indicators, not text chat. | Supported by REQ-023, REQ-024, REQ-025, REQ-040. |
| Playful identity | Personalization should use simple names and fun illustrated cartoonic avatars. | Supported by REQ-007, REQ-023. |
| Simple recovery | Join failures can use a simple error model; leave and abort actions should use clear confirmation or impact messaging. | Supported by REQ-033, REQ-034, REQ-035, REQ-036, REQ-037. |
| Design-phase restraint | Design should focus on product experience, not production backend, database, authentication, or streaming infrastructure. | Supported by REQ-031. |

## Material 3 Expressive Principles

| Principle | Meaning for this product | Support |
| --- | --- | --- |
| Expressive but usable | Use Material Design 3 Expressive to make the party and player experience lively while preserving clear media controls and role boundaries. | Supported by REQ-029, REQ-039. |
| Token-led visual system | Use semantic color, type, shape, spacing, and state decisions so the prototype can support themes and large text consistently. | Supported by REQ-029, REQ-030. |
| Adaptive mobile composition | Structure screens for cross-platform mobile first, with responsive behavior for larger or varied viewports. | Supported by REQ-001, REQ-030 and temporary decision based on ASM-001. |
| Custom player controls | The video control surface should feel like part of the Material 3 Expressive system rather than a default embedded browser control set. | Supported by REQ-026, REQ-027, REQ-028, REQ-039. |
| Meaningful state treatment | Initial, loading, empty, error, disabled, selected, pressed/active, and populated states should be visually planned, not added later. | Supported by REQ-030 and temporary decision based on ASM-012. |
| Accessible contrast and scale | Light theme, dark theme, large text, and responsive layouts are part of the design scope. | Supported by REQ-030. |

## Temporary Exploratory Choices

| Choice | Rationale | Risk if wrong |
| --- | --- | --- |
| Represent the first prototype as mobile-first browser-based while targeting cross-platform mobile. | Repository guidance points to React, TypeScript, Vite, and mobile-first layouts; product direction is cross-platform mobile. Temporary decision based on ASM-001 and supported by REQ-001. | If a native runtime is selected, navigation and media-control conventions may need adjustment. |
| Treat room creator as owner and ID joiners as non-owners. | Ownership is required for URL start and abort, and creation is the only available ownership cue. Temporary decision based on ASM-004, ASM-005 and supported by REQ-015, REQ-016. | If ownership can transfer or multiple owners exist, role states and controls must expand. |
| Use a simple mute/unmute voice model until voice behavior is clarified. | Voice chat is confirmed, but detailed interaction model is not. Temporary decision based on ASM-016 and supported by REQ-024, REQ-025. | Push-to-talk or open-mic requirements could change party screen controls. |
| Show a post-abort party state that keeps the room available for another URL. | The owner can start another URL in the same room, but exact post-abort state is undefined. Temporary decision based on ASM-009 and supported by REQ-016, REQ-035. | If abort ends the room, navigation should return to party manager or home. |
| Keep designs English-only for now while avoiding choices that block future RTL support. | Stakeholder clarified English-only for current designs, while repository guidance still mentions RTL. Temporary decision based on ASM-015 and supported by REQ-041. | If RTL returns, layout mirroring and language states must be added. |
| Represent direct video URL validation visually rather than implementing real compatibility checks. | This is a design prototype, not production media infrastructure. Temporary decision based on ASM-014 and supported by REQ-002, REQ-031. | If real validation is required, prototype scope and technical dependencies increase. |

## Anti-principles

- Do not design text chat unless requirements change. Supported by REQ-024, REQ-040.
- Do not expose visible playback drift or participant timelines for sync. Supported by REQ-038.
- Do not make non-owner URL controls look editable or start-capable. Supported by REQ-015, REQ-032.
- Do not make owner abort look like a personal pause action. Supported by REQ-016, REQ-017, REQ-018, REQ-035.
- Do not introduce production backend, database, authentication, or real-time infrastructure into the design phase. Supported by REQ-031.
