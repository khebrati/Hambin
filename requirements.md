A watch party mobile app where users can stream from a direct URL. 


The app has several sections:

In home page, users can either:
1. Enter setting, where they can choose English or Persian (فارسی), a simple name and a logo. Saving the profile applies and remembers the language; Persian uses right-to-left layout in both browser and Android. Video links, room codes, names, and subtitle content retain their original values. Settings also offers the 24 named color seeds listed in docs/theme-palettes.md; selecting one immediately applies its full light/dark Material palette and remembers it on the current device.
2. A party manager page, where they can either create or join a party (via ID)

after they join or create a party, we have the party screen.

In party screen:
1. There is an input field where the URL must be pasted and then played.
2. The URL then becomes a video player 

Inside that screen users can see their own icon and other users in party.

Syncronization rules:
1. Only the room owner can past URLs to start streaming.
2. Only room owner can fully abort the stream for everyone and start with another URL in that same room.
2. When a stream is started, everyone can pause, play, forward/backward for themselves. This does not 
effect the streaming process for others (streaming is done on device for each client.)
3. A Sync button is available. When tapped, the user's playback is fast forwarded to match the playback position of the leading participant (the one currently furthest ahead).
Other notes:
1. Users are able to speak in the chatroom and when they do so there should be an indicator on their icon.
2. Video playback has play/pause forware/backward and volume set capabilities. 

Confirmed additions:
1. Party members can invite others by copying the party ID or sharing an invite link. Android provides a Share button in the Invite friends dialog that opens the Android share sheet with the invite link.
2. Before joining, users see a room preview with the party title, owner status, participant count, and current stream status, then confirm or cancel joining.
3. If the room owner leaves, the room stays active without an owner. Remaining users keep their local playback controls but cannot start, replace, or abort the shared video. Ownership does not transfer; owner controls return only when the same owner rejoins.
4. Android viewers can select soft subtitle tracks provided by a video URL or choose a local subtitle file for their own playback. Hardcoded subtitles remain part of the video image. Subtitle selection is local to each viewer and does not change other participants’ playback.
5. Android viewers can switch between audio tracks provided by a video URL, or restore automatic track selection. Audio-track selection is local to each viewer and does not change other participants’ playback.
6. Browser viewers can select supported embedded text subtitles and audio tracks when the source permits direct browser access and the browser supports its codecs. Integrated playback should share media reads rather than separately download video data to extract subtitles. Unsupported sources retain local subtitle-file selection and a browser-playback recovery option. Selection stays local to each viewer. Browser playback automatically tries a compatible fallback when its preferred player fails or cannot start, retaining local playback settings where supported and offering Retry if recovery fails.

Confirmed guest identity requirements (2026-10-23):
1. Users receive an automatic durable guest identity from our own backend, without registration or login screens. All authentication, credential refresh, verification, and revocation are owned by our backend; neither client requires Firebase or Google authentication services.
2. A saved guest identity restores the same profile and room ownership after refresh or restart. Temporary connection or service failures retain the saved identity and offer retry; they must not create a replacement guest. Permanently unusable credentials have an explicit, confirmed identity-reset recovery action.
3. New guests receive a random display name from a curated list of at least 100 Marvel and DC characters, without numbers. Names do not need to be unique, persist with the profile, and remain editable in Settings. Existing saved names are preserved.
4. The Firebase-removal release starts with fresh backend guest identities. Existing identities and room ownership are not migrated; local non-secret preferences remain.

Confirmed last-room shortcut (2026-11-11):
1. Home shows a Last room card for every viewer, whether they created the room or joined as a guest. Remember the most recently successfully joined or created room for the current guest identity on that device/browser, including across app restarts or browser sessions.
2. Returning opens the existing room preview and normal join flow without retyping the code. The backend determines availability and restores the original owner's permissions; the shortcut does not bypass admission rules.
3. Keep the shortcut after leaving a room that remains open. Hide it when no room has been saved or the saved room is confirmed ended/unavailable. Temporary network/service failures retain it and offer retry through the normal preview flow.

Confirmed Android voice routing (2026-11-11):
1. Voice uses the loudspeaker by default from joining voice, including before movie playback. Prefer connected headphones or a Bluetooth communication device when available, and respond to device connection/removal.
2. Starting or pausing a movie preserves the voice output choice. Voice and movie play concurrently with separate call and media volume controls. Release voice routing settings when the voice session ends.

Confirmed room bug reports (2026-11-11):
1. Both clients show a bug icon button beside Share/Invite in the room top app bar. It opens a simple dialog with one large description input and Send.
2. Sending includes the description, available device/browser information, and the saved application log from this app session's startup through submission. Reports go to our backend; show success only after durable acceptance and retain the draft on failure for retry.

Confirmed speaking-indicator semantics (2026-11-11):
1. Browser and Android self-profile highlights show local microphone activity while voice is joined and the microphone is authorized and unmuted, including when alone in voice. They do not certify remote delivery or audibility.
2. Silence releases the highlight after a short hold; mute, capture suspension, and leaving clear it immediately. Microphone samples used for the indicator are transient and are not stored or uploaded.


Confirmed Android launcher icon foundation (2026-11-11):
1. Use the approved Shared Screen app icon, with bundled variants for all 24 named theme seed colors. Each background uses the exact supplied seed color with a contrasting foreground.
2. Android launcher icon color follows the selected named theme, including restoration at app launch. Roomio default uses Shared Screen in Basil, matching the approved concept. Light/dark mode does not change the chosen icon color. Keep the saved theme if switching fails and offer Retry.
