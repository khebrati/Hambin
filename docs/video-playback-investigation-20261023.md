# Direct-video startup investigation

Date: 2026-10-23. Reported source: `Cyberpunk.Edgerunners.2.S02E01.1080p.WEB-DL.Farsi.Sub.Film2Media.mkv` on `bnrhd3fvb374s4bkwzxm54xf.abrtech.top`.

## Findings

The browser player sets the shared URL directly on its video element (or gives an HLS URL to hls.js). The API distributes URLs, membership, and synchronization messages. It does not fetch movie bytes. A diagnostic HEAD request from the VPS established reachability and response headers; playback and media-byte diagnostics ran on the development client.

The source redirects to `sjdcbjdhbscjnhsdjcbjsd.mehracdn.com`. Its response advertises 466,358,407 bytes, `application/octet-stream`, and byte-range support. Browser range requests receive HTTP 206. Chrome identifies 1920×1080 H.264 Main video, two stereo AAC tracks, and a 1708.373-second duration. The default selected tracks decode successfully.

### Firefox

System Firefox 153 on Linux reproduced the prolonged startup in a minimal native-video page, independently of Hambin authentication, room signaling, React, and UI. Both metadata-first and immediate-playback attempts remained at `readyState=0` through separate 180-second observation windows. No media error was reported. These observations reproduce prolonged metadata loading; they do not establish that the whole movie finished downloading.

Mozilla [bug 2000420](https://bugzilla.mozilla.org/show_bug.cgi?id=2000420), fixed for Firefox 158, documents the matching cause: the AAC-in-MKV metadata path counts frames by scanning clusters until end of file. Changing preload settings or calling play earlier does not bypass browser demuxer code. This is the principal explanation for the reported multi-minute Firefox delay; it is not a room server download.

### Browser startup and source latency

Hambin previously set `preload="metadata"` and called `play()` only after `loadedmetadata`. Chrome's media trace showed a metadata-only suspended startup, resume, and tail/body range requests. The revised code sets `preload="auto"`, requests native playback immediately, and no longer requests playback from the metadata event. The revised startup trace does not show the earlier suspended-start cycle. HLS playback is requested when hls.js parses its manifest.

External source timings vary substantially. In simplified Chrome 154 probes, time to 0.5 seconds of playback was 13.55 seconds for metadata-first, 4.88 seconds for eager playback, and 6.22 seconds for another metadata-first run. The first metadata-first run spent about 9.4 seconds reaching the redirected HTTP 206 response. These sequential probes do not establish a controlled speedup. Network/cache variance remains material.

The actual revised React player reached more than one second of playback with decoded video frames after 9.15 seconds in one direct-source run, then passed local pause, seek to 60 seconds, and resume. The source also produced recoverable demuxer underflows; seeking required further slow range responses. No website change can guarantee instant startup or eliminate source throughput limitations.

## Changes and validation

- Request native playback before metadata completes; handle autoplay denial immediately with Play. Keep HLS startup at manifest readiness.
- After 15 seconds of loading/buffering, show nonfatal guidance and an explicit retry while preserving the existing source request. Explain the known MKV scan only for initial loading on affected Firefox versions. Do not automatically retry, discard the download, or reject a slow source.
- Keep fatal errors stable when late pause/canplay/playing events arrive.
- All 26 unit/contract tests and four focused Playwright regressions pass. Browser regressions cover pending metadata, immediate autoplay refusal, late-event error stability, and mobile dark/RTL/large-text slow guidance with reachable retry controls.
- Actual Firefox 153 ran the revised React component: `play()` was requested at `readyState=0` with `preload=auto`; the slow-MKV explanation appeared while the source stayed active. This verifies guidance and startup intent, not a fix to Firefox's internal scan.

## Deployment

Frontend release: `20261023T081616Z-player`. Server activation and hosted acceptance are recorded after deployment. Backend/API, database, and TURN behavior are outside this change. No commit or push is part of this task.
