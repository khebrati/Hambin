# Owner-led synchronization

Accepted UX: DD-059/DD-060 and subsequent stakeholder refinements. This is a fresh-deployment contract; old clients and old sync behavior are not supported. Room and voice transport remains Socket.IO/Engine.IO v4, authenticated with existing room tickets. No database migration is required: attempts are transient within the single API runtime, bounded to one attempt per room. Restart/reconnect snapshots explicitly clear unavailable attempts.

## Commands and state

All commands use Socket.IO acknowledgements, return `{ok:true, serverTimeMs, state}` or `{code,message}`, and require an authenticated, active room connection for the matching active stream. Every mutation carries `requestId` and `streamSessionId`. The server serializes attempt mutations synchronously after authorization; the commit boundary is authoritative.

- `sync.invite`: owner only; `{requestId,streamSessionId,positionMs}`. Client first pauses and samples the actual position. Creates an INVITING attempt. A retry with the same id returns existing state; a different concurrent invitation is rejected.
- `sync.respond`: non-owner; `{requestId,streamSessionId,response:ACCEPTED|DECLINED|WITHDRAWN}`. Only INVITING accepts responses. Acceptance pauses locally without seeking; withdrawal closes the dialog and resumes from the saved acceptance position. Declined/unanswered users are unaffected.
- `sync.commit`: owner only. Locks currently ACCEPTED members plus owner, enters PREPARING with a 15-second deadline. Late responses fail with SYNC_COMMITTED. No manual cancellation after commitment.
- `sync.cancel`: owner only, INVITING only. Release paused clients to their preceding playback state and position. No seek to target.
- `sync.ready`: included participant; `{requestId,streamSessionId,ready:boolean,reason?:string}`. True means the player has sought to the target, is paused, can decode and has at least 2 seconds buffered (or the remaining duration near end). False during preparation means still buffering; false after scheduling invalidates readiness and excludes that participant (owner failure fails attempt).
- `sync.failed`: included participant; reports unsupported/live/nonseekable media, playback denial, player failure, or background suspension. Excludes viewer; owner failure ends the attempt. No simulated readiness.
- `sync.state`: server push `{serverTimeMs,state:SyncAttempt|null}` immediately after room snapshot and on each change. Attempt fields: `requestId`, `streamSessionId`, `streamRevision`, `ownerMembershipId`, `targetPositionMs`, `createdAtMs`, `revision`, `phase` (INVITING/PREPARING/SCHEDULED/COMPLETED/CANCELLED/FAILED), `participants` (`membershipId`, `response`, `ready`, optional `reason`), optional `deadlineMs`, `startAtMs`, `reason`. Each revision is a complete replacement. Clients ignore older revisions and stale stream sessions. Newer attempts supersede old terminal ones.
- `sync.clock`: `{}` acknowledgement `{serverTimeMs}` for RTT clock calibration. Sample several times and retain the lowest RTT estimate; use a monotonic local clock between calibrations.
- `sync.request`: private owner catch-up only; `{requestId,streamSessionId,positionMs}`. Returns/emits `sync.result` `{requestId,status:APPLIED|UNAVAILABLE,targetPositionMs,serverTimeMs,playing}`. Uses fresh owner telemetry only, may rewind, projects elapsed playback time. Owner absent/stale telemetry is unavailable. No group disturbance.

## Timing and recovery

Owner is mandatory. Preparation ends when every remaining included player is ready; server then announces a start at least 3.5 seconds ahead. Each included client schedules against that shared epoch, displays 3/2/1/Action over playback and starts locally at `startAtMs`. The countdown does not depend on message arrival. Late delivery/clock failure fails that player's participation rather than starting a fresh local countdown. Excluded clients do not receive playback commands.

At the 15-second deadline, exclude unready viewers and proceed if the owner and remaining members are ready; unready/failed owner fails the attempt. Owner disconnect cancels invitations or fails committed attempts; disconnected viewers are withdrawn/excluded. Stream replacement/abort and room closure invalidate attempts. During countdown a loss of readiness excludes the viewer or fails the attempt if it is the owner. A committed failure releases controls and preserves a recoverable local playback state. Clients have a bounded watchdog for transport loss and missing terminal updates, and never remain locked indefinitely. Terminal states are retained briefly for reconnect recovery, then pruned; no persisted replay across runtime restarts.

Group sync supports a seekable shared media timeline. Live streams without a stable seekable timeline report unsupported rather than falsely preparing. Local playback freedom resumes after one coordinated restart; continuous drift correction is outside this flow.

## Presentation

Use selected focused dialog/mobile sheet. Owner draft: Sync together, live Meet at card, Ask everyone to sync; no roster and no pause before Ask. Viewer invitation: owner name beside sync icon, request heading, accept-and-pause and decline. Acceptance is revocable before commit. Loading replaces roster during preparation. Countdown has only 3/2/1/Action. Response text uses the latest approved casual English/Persian copy, with check/color for accepted and X/color for declined. Brief recovery completion is an owner-only snackbar; no persistent recovery card. Essential failures keep a short recovery action.

## Platform implementation and validation

Browser uses the existing player bridge, actual buffered ranges and decoder state. Android uses Media3 telemetry. iOS uses AVPlayer with actual loaded time ranges and exact paused seeking; its Apple build and device run require macOS. Desktop uses vlcj 4.12.1 and VLC 3.x installed on the host; its cache readiness is reported explicitly when a buffered-end timestamp is unavailable. Desktop enables [Compose Swing interop blending](https://kotlinlang.org/docs/multiplatform/compose-desktop-swing-interoperability.html) so controls and countdown render above the callback video surface.

All clients share consent, participant locking, automatic failure recovery and concise bilingual copy. Timing coordinates playback commands; hardware decoding, audio output, browser timer throttling and network asymmetry still limit perfect frame alignment. A suspended or late player is excluded rather than given a separate countdown.

Verification on 2026-11-12: backend unit suite and six PostgreSQL/Socket.IO room + synchronization integrations pass; browser production build and 104 unit tests pass. Live three-viewer Chrome checks pass in desktop light and compact dark/RTL/large-text layouts, including private owner rewind and coordinated start within the test's 350 ms tolerance. Shared UI (80 tests) and native data (100 tests) suites pass with one unrelated platform-gated skip each; added coordinator and parser regressions pass separately. Android debug assembly and desktop compilation pass. A real VLC smoke test confirms playback, pause, seek and readiness. iOS implementation is present, but Apple compilation and device playback remain unverified on this Linux host. No production deployment was performed.
