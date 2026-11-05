# Android crash when a browser joins voice — November 5, 2026

The connected phone retained a fatal exception at **15:20:14.824 Asia/Tehran**
(11:50:14.824 UTC). `top.roomio.app.androidApp` died on
`DefaultDispatcher-worker-1` with `PartyException: Voice request rejected`, from
`validateVoiceAcknowledgement` through `MeshVoiceSession.acknowledged`.

Before the crash, Android logged repeated zero-peer rosters approximately four
times per second, then one-peer rosters at 15:20:13.499, 15:20:14.027 and
15:20:14.819. Source inspection identifies two defects:

- Every Android roster invoked `applyMuteIntent`. The backend broadcasts a new
  roster on every successful mute request, creating an endless feedback loop.
- Android's trickle-ICE callback launched an acknowledged signal without an
  exception boundary. A rejected ACK escaped its supervisor child and killed
  the application. Offer negotiation and mute delivery already handle errors;
  credential refresh also handles errors.

Production API logs from 11:47–11:52 UTC show the cumulative negotiation rejection
count increasing from zero to one during this interval, followed by transport
closures. These metrics do not include the rejected operation's error code or
room ID. The precise reason for rejection cannot be established from the retained
logs; do not label it a rate-limit or stale-generation rejection as a fact.

## Local repair

Android now replays mute intent only on the first roster after join or signaling
resume. Ordinary roster updates no longer send mute commands. Rejected trickle
ICE is caught and its error code logged without credentials; an unconnected peer
retains bounded peer recovery, and healthy audio links are preserved. Caller
cancellation still propagates.

Two common tests cover a rejected ICE request returning normally to its owner
and cancellation avoiding recovery. All **58 data JVM tests passed**, with zero
failures or skips; the Android debug build passed. The signed Android release build passed as well. The manual-install APK is
`Roomio/androidApp/build/outputs/apk/release/androidApp-release.apk` (1.0.3 / code 5).

## Browser and device limits

The personal running browser was Firefox with no remote-debug listener. Its
session-store entry identified the original invitation. The personal tab was
left untouched; its console and WebRTC state could not be inspected directly.
A separate temporary instance of the installed Firefox opened that invitation,
joined the party, and joined voice with the muted state and Unmute microphone
control visible after 12 seconds. It left the party afterward. This verifies
Firefox voice admission, not bidirectional audio with Android.

ADB disconnected after crash capture. The stakeholder will handle installation.
Physical Android/browser audio and recovery remain unverified for this repair.
No production server configuration or code was changed.

Local evidence: `/tmp/hambin-crash-logcat-20261105.txt`,
`/tmp/hambin-incident-server-20261105.log`,
`/tmp/hambin-incident-firefox-result.jsonl`, and the debug/release Gradle logs
prefixed `/tmp/hambin-incident-`.
