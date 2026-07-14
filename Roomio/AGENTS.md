# Roomio project guidance

## Purpose

Roomio is the Kotlin Multiplatform implementation of the product defined by the parent repository. It turns the validated browser prototype and written requirements into a shared Compose Multiplatform application for Android, iOS, and desktop.

This project is an application implementation, not a place to redesign the product without evidence. Preserve the intent, content, flows, and state behavior established by the requirements and prototype while adapting them appropriately to each platform.

Do not add backend services or server projects to this client repository. Use local mock or fixture data until an explicit, documented service contract exists. Add client-side persistence, authentication integration, or other infrastructure only when a requirement needs it; keep it multiplatform where practical and document the dependency and boundary.

## Sources of truth

Before substantial product or UI work, inspect the relevant sources in the parent repository:

- `../requirements.md` defines product requirements.
- `../docs/` contains the product brief, requirements analysis, user flow, screen inventory, design principles, accepted decisions, assumptions, and open questions.
- `../prototype/` is the interactive visual and behavior reference.
- The current KMP implementation shows what has already been integrated, but it does not override documented product intent by accident.

When these sources disagree, do not silently invent a resolution. Prefer explicit accepted decisions, identify the conflict, state the working assumption, and update the appropriate decision or open-question document when a decision is accepted.

## Project structure

- `sharedUI/` is the shared KMP application and the default home for product code.
  - `src/commonMain/kotlin/` contains shared Compose UI, navigation, presentation state, domain logic, data abstractions, and platform-neutral implementations.
  - `src/commonMain/composeResources/` contains shared strings, images, icons, fonts, and other Compose resources. User-visible text belongs in resources rather than inline Kotlin strings.
  - `src/commonTest/` contains shared unit and Compose UI tests.
  - `src/androidMain/`, `src/iosMain/`, and `src/jvmMain/` contain only integrations that genuinely require platform APIs or platform-specific library engines.
- `androidApp/` is the thin Android launcher and Android application configuration. Keep product features in `sharedUI` unless they are Android-only by requirement.
- `iosApp/` is the thin Swift/Xcode host for the shared Compose UI. Keep Swift code limited to lifecycle and required iOS integration.
- `desktopApp/` is the thin JVM desktop launcher, window configuration, icons, and packaging setup.
- `gradle/libs.versions.toml` is the single dependency and plugin catalog. Declare or update versions there rather than hard-coding coordinates in module build files.
- Root Gradle files configure shared repositories and plugins. Add modules only when a clear boundary justifies the maintenance cost.

As the codebase grows, organize `commonMain` by product feature, with small shared `core`, `designsystem`, or `data` areas only when multiple features genuinely reuse them. Keep each feature's screen, UI state, actions, and presentation logic close together. Avoid both a single monolithic `App.kt` and premature layers made only for architectural symmetry.

## KMP implementation rules

- Implement in `commonMain` by default. Move code to a platform source set only when it directly depends on a platform API.
- Prefer a common interface with platform implementations supplied through dependency injection over `expect`/`actual`. Use `expect`/`actual` only when it is the clearest boundary for a small platform primitive.
- Keep platform entry points thin; they should configure the host and call the shared application.
- Use immutable UI state and one-way event flow. Expose observable screen state with `StateFlow` from shared lifecycle-aware ViewModels.
- Use structured coroutines. Do not create unmanaged scopes, block threads, or hide dispatcher choices in UI code.
- Separate external DTOs from product/domain models when their shapes or lifecycles differ.
- Keep mock data behind the same repository boundary intended for a future real data source so screens remain deterministic and testable.
- Do not introduce platform-only behavior unless the requirement calls for it or the adaptation is documented.

## Prefer the installed libraries

Use the dependencies already declared in `gradle/libs.versions.toml` before adding another library or writing a competing solution:

- Compose Multiplatform runtime, UI, Foundation, resources, previews, and Material 3 for shared UI.
- Material 3 and MaterialKolor for the theme, semantic color roles, and generated tonal palettes. Do not add a visually conflicting component library.
- AndroidX Lifecycle ViewModel and runtime Compose for lifecycle-aware shared presentation state.
- Navigation 3 and `navigation3-browser` for typed navigation, back stacks, deep-link/browser integration, and navigation-scoped ViewModels.
- Metro for dependency injection. Do not add a second DI container or a service-locator singleton pattern.
- Kotlin coroutines and `StateFlow` for concurrency and reactive state.
- Ktor Client, Content Negotiation, logging, and the installed platform engines for HTTP when a documented API contract exists.
- Kotlinx Serialization JSON for navigation keys, persisted models, and network payloads where serialization is required.
- Coil 3 with its Ktor integration for remote and shared image loading.
- Multiplatform Settings for small preferences such as theme or onboarding state; do not treat it as a general database.
- Kotlinx DateTime for shared date and time logic instead of platform date APIs in common code.
- Kermit for application logging. Never log secrets, tokens, or sensitive personal data.
- `kotlin.test`, Compose UI Test, and coroutine test utilities for shared tests.

Adding a dependency requires a demonstrated gap, a multiplatform compatibility check for every supported target, and a short rationale. Prefer lightweight libraries and add aliases through the version catalog. Do not add alternatives to an installed library merely because they are more familiar.

## Design and interaction

- Follow the prototype's Material 3 Expressive direction while using platform-appropriate Compose Multiplatform components and behavior.
- Prefer semantic theme values (`MaterialTheme.colorScheme`, typography, shapes, and project tokens) over raw colors, sizes, or duplicated styling.
- Build reusable components when a pattern is repeated or is part of the design system; avoid abstracting one-off composition prematurely.
- Use realistic Roomio content rather than lorem ipsum or generic placeholder labels.
- Preserve clear hierarchy, expressive shape and motion, and strong state feedback without copying another product's visual identity.
- Motion must communicate state or spatial relationships, respect reduced-motion expectations where the platform exposes them, and never block task completion.
- Treat accessibility as part of implementation: meaningful semantics and labels, logical focus order, keyboard support on desktop, adequate touch targets, contrast, and no color-only communication.
- Support light and dark themes, large text, RTL layouts, and responsive window sizes. Avoid fixed layouts that only work on one phone size.
- Respect safe areas and system insets on every target.

## Required screen states

A screen or reusable component is not complete until all relevant states are implemented or intentionally documented as not applicable:

- Initial
- Populated
- Loading
- Empty
- Error with a recovery path
- Disabled
- Selected
- Pressed or active
- Offline, where relevant

State previews, fixtures, or tests should make these states easy to review without relying on timing or a live service.

## Workflow

For substantial work:

1. Read the relevant requirement, documentation, prototype flow, and existing implementation.
2. State assumptions and the proposed implementation direction.
3. Make a focused change using existing components and installed libraries.
4. Add or update deterministic previews, fixtures, and tests for affected behavior and states.
5. Run the narrowest useful tests, then build the affected application target.
6. Review the result at compact and expanded widths, light and dark themes, large text, and RTL when the change affects layout or text.
7. Record accepted product or design decisions in `../docs/design-decisions.md` and unresolved issues in `../docs/open-questions.md`.

## Verification commands

Run commands from the `Roomio/` directory. On Windows use `gradlew.bat`; on macOS or Linux use `./gradlew`.

- Android debug build: `./gradlew :androidApp:assembleDebug`
- Shared JVM tests: `./gradlew :sharedUI:jvmTest`
- Desktop run: `./gradlew :desktopApp:run`
- Desktop hot reload: `./gradlew :desktopApp:hotRun --auto`

Use targeted tests during iteration and finish code changes with at least the Android debug build. Validate iOS integration on macOS with Xcode when shared APIs, resources, or iOS-specific code change. If a platform cannot be tested in the current environment, report that limitation explicitly.

## Definition of done

A change is done when it follows the documented requirement, uses the established design system and installed stack, handles relevant UI states, remains usable across supported themes, directions, text sizes, and window sizes, includes proportionate tests or previews, and builds successfully for the affected target. Do not leave disconnected controls, empty callbacks, unexplained TODOs, or silent error paths.
