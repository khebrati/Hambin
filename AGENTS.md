# Hambin repository guidance

## Purpose

Hambin is a multi-technology watch-party product repository. Product requirements and accepted experience decisions are shared, while each top-level client or service owns its technology-specific implementation.

The repository currently contains a native client and a browser client. The backend boundary is reserved but intentionally empty until an explicit service contract and implementation scope are approved.

## Repository structure

- `requirements.md` is the stakeholder-authored baseline for product behavior and constraints. Keep it technology-neutral and update it when product requirements are confirmed or changed.
- `docs/` contains shared product analysis and decisions, including the product brief, requirements analysis, user flows, screen inventory, design principles, accepted decisions, assumptions, open questions, glossary, and technical proposals.
- `webUI/` is the React, TypeScript, and Vite browser client. It also serves as the fastest interactive product and design prototype.
- `Roomio/` is the native Kotlin Multiplatform client for Android, iOS, and desktop. Follow `Roomio/AGENTS.md` for its Clean Architecture, Metro dependency-injection, Compose, and platform rules.
- `backend/` is reserved for a future backend service. Do not add backend code, databases, authentication infrastructure, deployment configuration, or select a backend technology until the backend requirements and API contract are explicitly defined.

Do not move shared product requirements into a client folder. Do not place one client's source code, build configuration, or technology-specific architecture inside another client.

## Sources of truth

Before substantial product work, inspect the relevant shared sources:

1. `requirements.md` defines the baseline product requirements.
2. `docs/design-decisions.md` records accepted product and experience decisions.
3. The remaining files in `docs/` provide analysis, flows, principles, assumptions, unresolved questions, and technical proposals.
4. `webUI/` and `Roomio/` demonstrate implemented behavior, but implementation accidents do not override documented intent.

When sources disagree, do not silently choose one. Prefer explicit accepted decisions, identify the conflict and working assumption, and record the resolution in `docs/design-decisions.md` or the unresolved issue in `docs/open-questions.md`.

Documentation under `docs/` is shared by all technologies unless a document or decision explicitly scopes itself to `webUI`, `Roomio`, or `backend`. Client-specific build instructions and architecture belong inside that client's folder.

## Cross-technology rules

- Keep user-visible behavior, terminology, validation, permissions, and relevant screen states aligned across clients.
- Allow platform-appropriate interaction and layout adaptations; shared product intent does not require identical implementation code or pixel-for-pixel rendering.
- Keep technology dependencies local to their owning client or service.
- Do not create source-level dependencies between `webUI` and `Roomio`.
- Use local mock data until an approved backend contract exists. Keep mock data behind boundaries that can later be replaced without redesigning screens.
- Define future client/server contracts in shared documentation before implementing backend or networking code.
- Never commit secrets, credentials, private keys, local environment files, or generated build output.

## Web UI client

Use React, TypeScript, and Vite in `webUI/`.

- Invoke the `material-3-expressive` skill for major design tasks when it is available.
- Use CSS variables and semantic design tokens rather than scattered raw visual values.
- Prefer lightweight dependencies and local interaction state. Do not add production networking or state-management libraries without a demonstrated requirement.
- Keep components reusable while avoiding premature abstraction.
- Use realistic Roomio content instead of lorem ipsum.
- Treat accessibility as part of implementation: semantic markup, keyboard access, visible focus, adequate contrast, and no color-only communication.
- Support light theme, dark theme, large text, RTL, and mobile-first responsive layouts.
- Do not add a component library that visually conflicts with Material 3 or imitate an existing product too closely.

## Design and documentation workflow

For substantial product or design work:

1. Inspect `requirements.md`, the relevant files in `docs/`, and both client implementations where behavior already exists.
2. State assumptions and the proposed direction.
3. Make a focused change in the owning client or shared documentation.
4. Run and review the affected client.
5. Check behavior at relevant themes, text scales, directions, and responsive sizes.
6. Record accepted decisions in `docs/design-decisions.md` and unresolved issues in `docs/open-questions.md`.

## Required experience states

A screen or reusable component is not complete unless all relevant states are represented or intentionally documented as not applicable:

- Initial
- Populated
- Loading
- Empty
- Error with a recovery path
- Disabled
- Selected
- Pressed or active
- Offline, where relevant
