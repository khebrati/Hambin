A watch party mobile app where users can stream from a direct URL. 


The app has several sections:

In home page, users can either:
1. Enter setting, where they can choose language, a simple name and a logo.
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
1. Party members can invite others by copying the party ID or a shareable prototype link.
2. Before joining, users see a room preview with the party title, owner status, participant count, and current stream status, then confirm or cancel joining.
3. If the room owner leaves, the room stays active without an owner. Remaining users keep their local playback controls but cannot start, replace, or abort the shared video. Ownership does not transfer; owner controls return only when the same owner rejoins.
4. Android viewers can select soft subtitle tracks provided by a video URL or choose a local subtitle file for their own playback. Hardcoded subtitles remain part of the video image. Subtitle selection is local to each viewer and does not change other participants’ playback.
5. Android viewers can switch between audio tracks provided by a video URL, or restore automatic track selection. Audio-track selection is local to each viewer and does not change other participants’ playback.
6. Browser viewers can select supported embedded text subtitles and audio tracks when the source permits direct browser access and the browser supports its codecs. Integrated playback should share media reads rather than separately download video data to extract subtitles. Unsupported sources retain local subtitle-file selection and a browser-playback recovery option. Selection stays local to each viewer.

Confirmed guest identity requirements (2026-10-23):
1. Users receive an automatic durable guest identity from our own backend, without registration or login screens. All authentication, credential refresh, verification, and revocation are owned by our backend; neither client requires Firebase or Google authentication services.
2. A saved guest identity restores the same profile and room ownership after refresh or restart. Temporary connection or service failures retain the saved identity and offer retry; they must not create a replacement guest. Permanently unusable credentials have an explicit, confirmed identity-reset recovery action.
3. New guests receive a random display name from a curated list of at least 100 Marvel and DC characters, without numbers. Names do not need to be unique, persist with the profile, and remain editable in Settings. Existing saved names are preserved.
4. The Firebase-removal release starts with fresh backend guest identities. Existing identities and room ownership are not migrated; local non-secret preferences remain.
