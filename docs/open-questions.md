# Open Questions

Only questions whose answers could materially change user flow, screen structure, navigation, or visual direction are listed.

Resolved questions are preserved with their answers for traceability.

## Blocking

1. [Resolved] What platform should the design target first: mobile web prototype, Android app, iOS app, or cross-platform mobile?
   - Answer: Cross-platform mobile focused on Material Design 3 Expressive.
   - Design decision affected: Sets mobile-first cross-platform screen structure and confirms Material 3 Expressive as the visual system. The production runtime remains unspecified.

2. [Resolved] Is "chatroom" text chat, voice chat, or both?
   - Answer: Only voice chat.
   - Design decision affected: Removes text message composer and message list from the design; adds voice activity, microphone, and speaking-indicator states.

3. [Resolved] What does "logo" mean in settings: a personal avatar, room logo, app logo, or selectable icon?
   - Answer: A personal avatar selected from a list of fun illustrated cartoonic avatars.
   - Design decision affected: Settings needs avatar selection; party member icons should use selected personal avatars.

4. [Resolved] Who is the "first person" used by the synchronization button?
   - Answer: The leading participant, meaning the one currently furthest ahead.
   - Design decision affected: Sync action targets the furthest-ahead participant, not the owner or earliest joined participant.

5. [Resolved] What exactly happens when the room owner aborts a stream?
   - Answer: The stream is aborted for everyone. This is different from simply pausing, which pauses only for the user who pressed pause.
   - Design decision affected: Owner abort must be visually distinct from local pause and should communicate global impact.

## Important

1. [Resolved] Can non-owner users see, copy, or inspect the current URL even though they cannot paste/start URLs?
   - Answer: Yes.
   - Design decision affected: Non-owner party screen needs read/copy/inspect affordances for the current URL without edit/start controls.

2. [Resolved] Can users type a URL manually, or must it be pasted from the clipboard?
   - Answer: Typing is also possible, but focus is on easy pasting.
   - Design decision affected: URL entry should support keyboard input and a prominent paste-oriented affordance.

3. [Resolved] What URL types must the design represent: direct video files, HLS streams, YouTube-like pages, or any URL?
   - Answer: Direct video URL.
   - Design decision affected: Placeholder text, validation copy, and examples should describe direct video URLs rather than general web links.

4. [Resolved] What party join states should be represented for invalid, expired, full, or unavailable party IDs?
   - Answer: A simple error is enough.
   - Design decision affected: Party manager does not need separate error layouts for each failure type.

5. [Resolved] How should users leave a party or return home?
   - Answer: A visual leave indicator that asks "Are you sure?"
   - Design decision affected: Party screen needs a leave affordance and confirmation prompt.

6. [Resolved] Should playback controls be custom-designed or use the platform/browser video controls?
   - Answer: Custom-designed based on Material Design 3 Expressive guidelines.
   - Design decision affected: Player controls should be part of the custom design system rather than relying on default browser/platform UI as the primary design.

7. [Resolved] Should local playback position differences be visible to users?
   - Answer: No. They are only synchronized if they want to by pressing the sync button.
   - Design decision affected: Do not design visible ahead/behind indicators, participant timelines, or drift badges.

8. [Resolved] Does language selection need RTL examples now?
   - Answer: No. Proceed only with English in designs.
   - Design decision affected: Current design examples can be English-only. This conflicts with repository guidance that asks for RTL support, so RTL status remains a project-level uncertainty.

9. [Resolved] Should the product include an invite/share flow?
   - Answer: Yes. Current party members can use a prominent action to copy the party code or a mock invite link.
   - Design decision affected: The party screen needs an accessible invite/share surface with copied and copy-failure feedback, without requiring production deep links or native share infrastructure.

10. [Resolved] Should invitees see the room before joining?
   - Answer: Yes. Show party title, current owner status, participant count, and stream status before the invitee confirms or cancels joining.
   - Design decision affected: Joining becomes a two-step flow with loading, available, owner-absent, full, ended/unavailable, and join-confirmation states.

11. [Resolved] What happens when the room owner leaves?
    - Answer: The room remains active without an owner until that same owner rejoins. Other users retain local playback but cannot start, replace, or abort the shared video. Ownership does not transfer.
    - Design decision affected: Add a persistent owner-absent room state, keep owner-only controls unavailable, and restore them only when the original owner returns.

12. [Open] Directional sync semantics, and the conflict with the resolved "no visible position differences" answer.
    - The new `webUI` sync exploration (DD-017) shows each participant's playback time and offers "Bring everyone to me" / "Take me to others". This conflicts with the resolved answer to Important question 7, which said not to design visible ahead/behind indicators or participant timelines.
    - Open sub-questions: Does "Bring everyone to me" need unanimous agreement, a majority, or move immediately with per-user opt-out? Can the room owner force a sync? Is "turn off sync requests from others" global, per-room, or per-sender, and does it persist across sessions? Should "Take me to others" allow following someone who is behind the requester? Which of the three variations (A · Sheet, B · Inline, C · Center) should be adopted?
    - Design decision affected: The accepted variation and consent rules must be settled before the "Synchronization rules" in `requirements.md` are updated and before the native client implements matching behavior.

13. [Resolved] What direct video URL formats and validation rules apply to the native client?
    - Answer: The Android client accepts any well-formed HTTP(S) direct video URL and plays it through Media3 ExoPlayer; progressive files and HLS live streams are supported. Validation is deferred to playback: failures surface through the player error state and a retry path. Cleartext HTTP is permitted for this feature via a debug network security configuration.
    - Design decision affected: Native URL entry needs an error-with-recovery player state rather than format pre-validation; live streams hide scrubber and skip controls. Recorded as DD-013 for the Roomio Compose client.

## Can be deferred

1. What exact name length, character rules, and avatar limits should settings enforce?
   - Affects form validation copy and edge states, but not the primary screen structure.

2. What is the party ID format?
   - Affects input masking and examples, but the design can proceed with a placeholder pattern.

3. How large can a party be?
   - Affects member list layout, overflow states, and avatar grouping.

4. Should volume be shown as a slider, stepper, icon menu, or another custom control?
   - Affects detailed player control design but not the main user flow.

5. Should the app show video title, thumbnail, duration, or loading metadata?
   - Affects the populated player state and pre-play preview.

6. What voice-chat control model should be used: open mic, mute/unmute, push-to-talk, or listen-only until enabled?
   - Affects party screen control density, permission prompts, accessibility labels, and speaking indicators.

7. What should the screen show immediately after the owner aborts a stream?
   - Affects whether the player becomes an aborted state, an empty URL-entry state, or a transition state.

## Remaining Uncertainty Summary

- Production runtime is still unspecified, even though the design target is cross-platform mobile.
- RTL is currently excluded from designs, but repository guidance still asks for RTL support.
- Voice-chat details are not yet defined beyond "voice only."
- Initial owner assignment is still inferred from party creation, but the owner-absent and returning-owner lifecycle is defined.
- Desktop and iOS playback engines are not yet implemented; the Android player (DD-013) is the approved reference seam for extending them.
- Synchronization is a local placeholder until a cross-device sync contract exists. Directional sync (DD-017) is an unaccepted `webUI` exploration whose request-consent rules and chosen variation are still open.
