# Browser MKV playback and subtitle evaluation

Date: 2026-11-06. Accepted decision: DD-037. Implementation, local verification, and subsequently authorized browser deployment.

## Result

Hambin now uses libmedia AVPlayer 1.3.1 for `.mkv` URL pathnames, with client-side demux/remux and browser MediaSource decoding. Ordinary files keep native playback; HLS keeps the existing native/hls.js path. Embedded text subtitles and audio use the integrated engine's media reads. The separate subtitle extractor has been removed.

Both candidates were tested against the stakeholder's exact URL in Chrome 154. AVPlayer worked in MediaSource mode; its default WebCodecs route failed decoder/resampler initialization in this environment. Movi 0.4.1 failed twice with `WebAssembly.instantiate(): unknown type form: 2 @+1658`, despite WebGL2 availability. These results establish fit in the tested environment, not universal incompatibility of Movi.

The AV runtime is pinned, self-hosted and loaded only for MKV playback. No external player CDN, server proxy, storage, or transcoding is added. Hambin retains its existing controls, local track choices/imports, fullscreen, microphone actions and explicit forward-only Sync. A failed enhanced player offers Retry video and Use browser player; native fallback may lose embedded tracks.

## Exact source

Stakeholder URL: `https://bnrhd3fvb374s4bkwzxm54xf.abrtech.top/pnot/Movie/2026/Resident.Evil.2026/RDUB/Resident.Evil.2026.720p.WEB-DL.Farsi.Dubbed.Film2Media.mkv`.

It redirects to `sjdcbjdhbscjnhsdjcbjsd.mehracdn.com`, advertises 926,875,938 bytes, returns partial content for ranges, and permits cross-origin access. Duration is 5629.653 seconds. Tracks are H.264 video, Persian and English AAC audio, and Persian `S_TEXT/UTF8` subtitles labelled Film2Media - GapFilm.

Independent native Chrome playback exposed an empty `video.textTracks`, reproducing missing embedded subtitles outside Hambin. The integrated player exposes the subtitle and both audio choices.

The file's subtitle metadata reports two frames. Both indexed blocks inspected directly contain distributor credits, at 0.959–14.950 and 5178.100–5192.099 seconds. No cues appeared in sampled windows at 600, 2800, 5550 or 5615 seconds. The whole movie was not exhaustively scanned; do not promise dialogue subtitles from this file.

## Actual Hambin integration test

The final opt-in test ran the real React player in Chrome 154, using the exact remote source, actual demuxing and decoding, with no mocked media responses and no live-room mutation.

| Stage | Native position | Decoded frames | Cumulative received media bytes |
| --- | ---: | ---: | ---: |
| Opening playback, then pause | 2.080 s | 54 | 1,733,125 |
| Persian subtitles enabled | 2.080 s | 54 | 2,011,653 |
| Seek to 600 s, then play | 601.019 s | 86 | 4,714,676 |
| English audio selected, then pause | 601.677 s | 101 | 5,206,196 |
| Return to 3 s, cue restored | 3.021 s | 145 | 9,683,622 |
| Subtitles Off, then pause | 3.606 s | 161 | 10,158,758 |

All stages completed in 10.344 seconds with `readyState=4`, no page errors and no player alerts. Audio selection was checked against the engine's selected stream ID. The screenshot was reviewed for readable Persian text without literal SubRip HTML markup.

**Enabling subtitles started zero additional media requests.** Bytes continued arriving from existing media prefetch while paused. The roughly 10.16 MB above includes initial probing, buffering and two distant seeks; it is not the bandwidth for ten seconds of ordinary viewing or a full-movie estimate. There were seven media request operations, each observed before and after the CDN redirect. Range requests can be cancelled before their advertised five-MiB end. The engine can reread media during initialization and seeking; shared subtitle reads do not mean zero overhead.

Two engine issues were addressed during verification: newly queued subtitles were reset after a seek, and seek targets could land at a neighbouring cluster/keyframe. A version-checked upstream patch resets old subtitle cues before seeking. The adapter aligns within decoded MediaSource samples and retries an earlier position when an index selects a later cluster. Paused seeks use silent decoding and restore the local paused state; in-progress seeks are excluded from playback reports.

## Remaining limits

- Automatic enhanced routing requires a `.mkv` pathname. Extensionless links and embedded MP4 track recovery retain the native path.
- CORS and source access still apply. Efficient seeking needs HTTP ranges and usable indexes. Slow or failing sources can time out and require Retry.
- MediaSource/browser codec support determines playback. This integration deliberately does not ship codec-WASM fallback. Unsupported audio/video codecs can still fail even when Android plays them.
- Supported embedded text formats are offered; bitmap tracks are acknowledged as unsupported. Unicode/timing and common markup normalization are covered. Advanced ASS styling, fonts, drawings, placement and exact Android rendering parity are not guaranteed.
- A sparse source cannot provide missing dialogue cues. Subtitle Off hides rendering while the demuxer continues its normal shared media pipeline.
- Self-hosted runtime assets add download/parse cost on first MKV use. Native ordinary-file playback avoids that cost.
- Automated desktop and compact dark/large-text/RTL viewport checks used desktop Chrome. Physical Android/iOS Safari/Firefox acceptance and a full-movie soak were not performed.

## Verification and reproduction

The production build and 77 unit tests pass. The integrated suite exercises actual decoding with a generated, owned MKV fixture: embedded/local subtitles, post-seek cue timing, audio switching, desktop/mobile layouts, fullscreen, asset retry, native fallback, source disposal, forward-only synchronization, playback reports and autoplay denial/recovery. All 24 focused integrated/native/HLS/player and voice UI browser tests pass. The existing Vite large-chunk warning remains.

Start Vite, then run the opt-in exact-source probe:

```sh
npm --prefix webUI run dev -- --host 127.0.0.1
node webUI/scripts/probe-mkv-subtitles.mjs 'MEDIA_URL' /tmp/hambin-integrated-player.json
```

Override `CHROME_PATH` or `MKV_PROBE_BASE_URL` if needed. It writes JSON and a screenshot. Final local evidence: `/tmp/hambin-integrated-complete.json` and `.json.png`. Reports/screenshots and generated runtime/build output are not committed source artifacts.

## Superseded extraction experiment (DD-036)

Before selecting the integrated engine, the separate extractor read 24,015 bytes for metadata, then 650,704 / 4,279,875 / 3,274,516 / 4,731,906 / 2,001,517 additional bytes at the five sampled positions above. Total: 14,962,533 extra bytes and 48 completed ranges. These were extraction-only samples, in addition to native-video traffic, not a whole-movie measurement. The integrated engine replaces that duplicate-read design. Its previous implementation and test counts are historical, not the current shipping architecture.


## Authorized production deployment

The stakeholder requested commit and deployment after local acceptance. Browser
commit `051b31c7d8f3b149b668c7054c9705ef4306b372` is deployed at
`https://watchparty.top`. Existing unrelated Android edits were excluded.

- Uploaded archive SHA-256: `74776aaf2be336ab666ed03ca3525fae3daa3d2d401a273a7eecc3849b546b36`.
- All 56 staged browser files were checked on the server. Public index,
  AVPlayer runtime, polyfill and notice bytes matched the local manifest.
- Browser index SHA-256: `429868bc2d0ae0891578c646d5bef262554f8481715dce8303d7ad2d9f31a339`.
- Static assets were added before atomically replacing the index. Previous hashed
  assets were retained for open clients. No API, database, voice service or
  container restart was required; all existing services retained their uptime.
- Active release directory: `/opt/hambin/releases/20261106-active-party-f04516c`.
  Browser manifest: `web-integrated-player-release.json` in that directory.
- Protected rollback: `/opt/hambin/backups/web-before-integrated-player-051b31c/web`.
  Restore its index atomically to roll back this browser update; no database restore
  is needed.
- External and server-local readiness checks returned ready/database/dispatcher/voice true.

Actual production UI verification created an isolated temporary party, started the
exact stakeholder URL, decoded frames, rendered the Persian cue, sought while
paused to 600 seconds, confirmed English audio selection, returned to 3 seconds
with the cue restored, and hid it with Off. The passing run had no page errors or
player alerts. The temporary party was left and its guest session revoked. One
initial attempt timed out waiting for startup; the subsequent complete run passed.
The cause of that first timeout was not established. This is a bounded functional
check, not a source-throughput or full-movie reliability guarantee.

Local production evidence: `/tmp/hambin-production-integrated.json` and
`/tmp/hambin-production-integrated.png`. No tokens or passwords were persisted in
those evidence files.
