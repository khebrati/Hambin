# Glossary

| Term | Definition | Ambiguity / notes |
| --- | --- | --- |
| Watch party mobile app | An app experience where multiple users join the same party while each watches a stream on their own device. | The target platform is not specified beyond "mobile." |
| Direct URL | A URL provided by a user to start playback. | The source does not define supported formats, validation rules, or whether third-party page URLs are allowed. |
| Home page | The initial section where users can access settings or party management. | Could be a screen, route, or top-level app section. |
| Settings | A section where users choose language, simple name, and logo. | The source does not mention persistence, editing after joining, or validation. |
| Language | A user-selected app language. | Available languages and RTL requirements are unspecified in `requirements.md`; RTL is required by repository guidance. |
| Simple name | The user's chosen display name. | Length, uniqueness, and character rules are unspecified. |
| Logo | A chosen visual identity item. | Could mean user avatar, room logo, or app logo. Current analysis treats it as ambiguous. |
| Party manager | A section where users create or join a party. | The exact screen layout and whether create/join are separate flows are unspecified. |
| Party | A shared room/session that users create or join to watch from a URL. | Room lifetime, maximum size, ownership transfer, and leave behavior are unspecified. |
| Party ID | An identifier used to join a party. | Format, length, expiration, and error states are unspecified. |
| Room owner | The user allowed to paste/start URLs and abort the stream for everyone. | The source does not define how ownership is assigned; creation implies ownership but does not state it directly. |
| Party screen | The screen shown after creating or joining a party. | Contains URL/player behavior, participant icons, chatroom behavior, and sync controls. |
| URL input field | A field on the party screen where a URL is pasted and played. | It is unclear whether manual typing is allowed or only paste interaction. |
| Video player | The playback UI that appears after a URL is provided. | Could be native player controls, custom controls, or a hybrid. |
| Abort stream | Owner action that stops the current stream for everyone and allows a new URL in the same room. | Exact resulting state is unclear: stopped player, empty player, cleared URL, or reset party. |
| Local playback controls | Per-user controls for play, pause, forward, backward, and volume. | These should not affect others according to the source. |
| Synchronization button | A party screen control that moves the clicking user's playback forward to a target participant's playback position. | Target participant is ambiguous. |
| First person ahead | The person whose playback is ahead of the rest and is used as sync target. | Could mean furthest-ahead participant, room owner, or first joined participant. |
| User icon | Visual representation of a participant in the party screen. | Relationship to "logo" is unclear. |
| Chatroom | A communication area or mode inside the party. | "Speak" suggests voice, while "chatroom" may suggest text. |
| Speaking indicator | A visual indicator on a user's icon when they speak in the chatroom. | Depends on whether chat is voice activity, text activity, or a generic active-state indicator. |
| Material 3 Expressive | The design-system direction for adaptive layouts, expressive shape, motion, typography, and token-driven UI. | In this repository it should inform design docs and prototype UI; it does not imply Jetpack Compose code. |
| Design-only phase | Current scope focused on analysis, flows, screen structure, visual direction, and documentation. | Production backend, streaming infrastructure, and application architecture are outside this phase. |
