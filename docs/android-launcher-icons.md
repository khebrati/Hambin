# Android Shared Screen launcher icons

The approved Shared Screen mark has 24 bundled Android color variants. Each background uses the exact named seed hex supplied by the stakeholder, independently of Material's generated primary color. The foreground uses either cream (`#FFF8E7`) or charcoal (`#18201C`), whichever has higher contrast against the seed. All variants share the same vector silhouette. Basil is the initial launcher icon, matching the selected green concept; it is not an automatic theme choice.

## Foundation and integration boundary

`LauncherIconController` is available from the Metro application graph. It exposes `isSupported`, `currentColor(): Result<LauncherIconColor>`, and `selectColor(LauncherIconColor): Result<Unit>`. Android supports switching; iOS and desktop explicitly report unsupported. No picker, theme observer, or preference migration is wired in this change.

The 24 `LauncherIconColor` enum names match the named theme choices (`CHERRY_BLOSSOM` uses that exact spelling). A later integration should explicitly map named theme choices to icon choices and define what the extra `ROOMIO` default means. Do not couple the icon catalog to theme storage or infer an icon from a generated Material primary color. Automatic theme changes, light/dark mode, and dynamic system color are not currently triggers.

Use the graph-owned controller from the application boundary when wiring theme selection. Handle `Result` failures separately from saving the theme: a launcher failure must not silently imply success or discard the theme selection. Repeating the same choice is idempotent. A failed update can be retried by selecting the intended color again.

## Android behavior

- Twenty-four stable manifest aliases use `${applicationId}.launcher.<AliasSuffix>`. Basil is the only alias enabled by default. Keep names and default-enabled state stable across upgrades because Android persists component overrides.
- `AppActivity` remains enabled and owns invite-link filters. Only aliases have the launcher filter. Switching never disables the actual activity.
- Android 13/API 33 and newer update aliases atomically. Android 6–12 enable the replacement before disabling existing aliases. If an older-device update partially fails, return failure while keeping at least one launchable alias; retry repairs duplicates.
- PackageManager state is the source of truth, with no duplicate preference to drift. `currentColor()` fails explicitly if zero or multiple aliases are enabled.
- `DONT_KILL_APP` is requested. Launcher refresh timing, pinned-home-screen behavior, and OEM process behavior still need device acceptance; immediate refresh is not guaranteed.
- Adaptive icons and legacy vector launcher resources are included. A shared monochrome silhouette supports Android themed icons; the launcher can override the supplied colors when themed icons are enabled.
- The application fallback icon uses Shared Screen in Basil. Changing an alias does not dynamically recolor notification, splash, or system-settings artwork.

## Validation

Run `python3 Roomio/androidApp/tools/verify_launcher_icons.py` from the repository root to verify all enum/manifest/resource colors and defaults. Run data JVM tests for repeated selection, every color, API-specific ordering, partial failure and repair, unsupported platforms, and controller recreation. Build with `./gradlew :androidApp:assembleDebug` from `Roomio/`.

Launcher switching is local and offline. There is no new screen, loading state, empty picker, or selected/pressed UI in this foundation. Future picker states belong to the theme integration. Hardware acceptance should cover at least API 23–32 and API 33+, cold launch, invite links, process restart, app upgrades, pinned shortcuts and themed icons.

Android references: [activity aliases](https://developer.android.com/guide/topics/manifest/activity-alias-element), [PackageManager component updates](https://developer.android.com/reference/android/content/pm/PackageManager#setComponentEnabledSettings(java.util.List)).
