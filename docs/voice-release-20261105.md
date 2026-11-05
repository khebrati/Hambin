# Voice v4 coordinated local release — November 5, 2026

This release candidate retains Node/PostgreSQL/private Coturn and audio-only
mesh. It is prepared locally; production has not been changed. Voice uses
Socket.IO protocol **4**, while room realtime remains **3**. Deploy matching
backend, browser, and Android artifacts together. An older voice client cannot
connect to the revised voice namespace. No legacy-data migration is included.

## Verified implementation

Join, resume, and TURN credential refresh are separate operations. Resume keeps
the session generation during the 30-second disconnect grace; refresh neither
creates nor invalidates a connection ticket. Socket authority uses distinct
leases, peer negotiation is serialized and deduplicated, and both clients order
and acknowledge signaling. Healthy peer links survive signaling recovery or
another peer's failure. Per-peer state and failed-link retry remain visible.

Android voice lifetime belongs to an application-scoped Metro coordinator. A
bound service confirms microphone foreground protection before capture setup.
Service loss closes voice and requires explicit rejoin. Capture mutes
immediately; unmute requires the current successful acknowledgement. Process
death does not restore capture. Playback remains independent of voice.

## Verification evidence

| Check | Recorded result |
| --- | --- |
| Native data JVM | 56 passed, including six real local WebSocket termination/error-code tests |
| Native shared UI JVM | 51 passed, one existing skip; coordinator/service failure and leave-during-join regressions included |
| Native compile/build | Metro JVM graph, Android debug assembly, and signed release assembly passed |
| Native recovery UI | Compact light and dark RTL at 1.8× text passed |
| Browser unit/build | 75 tests passed; final production artifact recorded below |
| Real backend integration | Seven PostgreSQL/Socket.IO tests passed with zero skipped; backend typecheck and eight default unit tests also passed |
| Real Chrome media | Two/four/eight browser calls passed distinct received tones, inbound RTP/energy, initial mute, acknowledged unmute, and signaling resume preserving peer objects; eight-member check covered all 56 directed links |
| Local forced relay | Two-browser identifiable bidirectional audio and RTP passed with each of UDP 3478, TCP 3478, TLS 5349, and TLS 443 separately |
| Repeated browser joins | Earlier 65-test adapter: 100 cycles passed in 486.6 seconds with one room/two Chrome 154 browsers; received RTP/energy and track/peer cleanup checked each cycle |
| Shared TLS ingress | HAProxy/Caddy/Compose/shell validation passed; actual local SNI routed website and TURN traffic to their separate TLS backends |
| Client IP preservation | Full HAProxy→Caddy→API→PostgreSQL fixture passed: distinct real source addresses retain independent rate buckets, forged forwarded headers cannot borrow another bucket, and unknown PROXY sources are rejected |
| Production API image | Pinned production image build passed; production-only dependencies and typed server modules load under Node 24.18.1 as user `node` |
| Bounded concurrent load | Two eight-person rooms on this workstation failed with Engine.IO ping timeouts; host CPU reached 100% while over 6 GiB RAM remained available, with no HTTP 429 responses and verified distinct test client-IP headers |

Native final command, from `Roomio/`:

```sh
./gradlew :data:jvmTest :sharedUI:jvmTest :app:compileKotlinJvm :androidApp:assembleDebug
```

The final native verification log is `/tmp/hambin-native-refresh-fence-final.log`.
Signed release assembly (`./gradlew :androidApp:assembleRelease`) also passed.
`apksigner` verified APK v1/v2
signatures, and APK metadata confirms version **1.0.3**, version code **5**.
These paths are ephemeral workstation evidence, not repository deliverables. See
[the acceptance matrix and reproducible media/load commands](voice-stability-acceptance.md)
for the full test scope.

The cycle harness scales its deadline with the requested cycle count and paces
rejoins to respect the API's per-IP request policy. The earlier 65-test adapter's 100-cycle run took
486.6 seconds; its evidence is `/tmp/hambin-cycles-final65-paced-evidence/`.
A prior successful 100-cycle run needed 844.4 seconds. Its persisted evidence is
`/tmp/hambin-cycles-final63-evidence/` and
`/tmp/hambin-cycles-final63-resources.jsonl`. The 233 host resource samples
recorded maximum CPU 89.0%, minimum available memory 1,838.8 MiB, and maximum
summed Chrome RSS 2,482 MiB (RSS sums can double-count shared pages). The last
API sample had RSS 108,160 KiB, 24 open file descriptors, and 11 threads. These
are whole-workstation observations during a two-browser cycle check, not
100-room capacity evidence. That run preceded the final privacy and stale-credential-response hardening;
the final 75-test adapter passed its targeted regressions and build.

## Deployment and relay boundary

The staged API Dockerfile pins Node 24.18.1 by image digest; PostgreSQL, Caddy,
Coturn, and HAProxy are also digest-pinned. HAProxy binds host TCP 443, drops to
its image user/group, and routes the exact TURN SNI to Coturn 5349. Other TLS
goes to Caddy's loopback-only 8443 mapping. Caddy retains TCP 80 and UDP 443.
Website TLS carries PROXY protocol v2 into Caddy's trusted listener before TLS,
preserving client addresses for API rate limiting. Trust is limited to loopback
and the fixed Docker bridge gateway; client-supplied forwarded headers cannot
override that address. This was verified end-to-end against the real API's
20-per-minute guest creation policy using two actual TCP source addresses.
HTTP 80 must remain reachable for the TURN hostname's ACME HTTP challenge;
TLS-ALPN challenge is disabled for that name. Certificate synchronization
exports Caddy's TURN certificate and restarts Coturn only when it changes.
Renewal therefore interrupts relayed calls and still needs recovery acceptance.

Before activation, align `PUBLIC_IP`, certificate names, DNS, advertised ICE
URLs, listeners, and firewall rules. Allow TCP 80/443/3478/5349, UDP 3478/443,
and the configured UDP relay range; keep 8443, API, and PostgreSQL private.
Relay range, allocation quota, and bandwidth ceilings are configurable. The
16,000-allocation default removes the old 400 ceiling but establishes no
measured capacity. See [deployment instructions](../backend/deploy/README.md).

The loopback `8443` port is a host-side mapping to Caddy container port `443`.
Caddy still uses its normal HTTPS port 443, so redirects and HTTP/3 Alt-Svc do
not advertise the private host port. Host UDP 443 maps to Caddy UDP 443 for
HTTP/3. Validation of the adapted configuration confirms its `:443` listener.
The actual Caddy fixture also returned the canonical HTTPS redirect and
`h3=":443"` advertisement. Its sanitized result is
`/tmp/hambin-ingress-client-ip-result.json`; reproduce it with
`backend/deploy/verify-ingress-client-ip.py` on an isolated Docker host.

## Remaining release gates

- Physical two/four/eight-party Android/browser audio, including every sender
  heard by every other listener, simultaneous speech, and movie/voice coexistence.
- Wi-Fi/mobile changes, VPN, packet loss, UDP blocking, background/screen lock,
  notification actions, interrupted capture, headset and Bluetooth routes.
- Two-hour eight-person soak and 100 concurrent eight-person rooms with direct
  and forced-relay profiles, measuring CPU, RAM, traffic, allocations and leaks.
- Production DNS/certificate/firewall and authenticated bidirectional media on
  every advertised transport. Local self-signed fixtures do not verify these.
- Platform builds unavailable here, including iOS/macOS validation.

Only one Android device is attached. The workstation has 8 CPUs and 16 GiB RAM;
the 800-browser capacity profile requires a dedicated or distributed load host.
Local synthetic media and socket tests do not establish physical audio stability.
The two-room failure is correlated with local CPU saturation; the exact
client/server starvation layer remains unresolved. That run used earlier
persistent tone-analysis instrumentation; the revised harness uses transient
probes, and the corrected two-room profile remains unverified. The prior result
does not establish product capacity. The final 75-test adapter did not pass a two-hour call: forced TLS startup
reached 49 of 56 links with CPU near 100% and 18 rejected acknowledgements; a
direct fallback entered hold but failed after 75.3 seconds with 54 of 56 links
connected. These are preserved failures, not successful soak evidence. Physical
Android/browser audio and 100-room acceptance remain unverified.

## Artifact staging

Artifacts are staged under `/tmp` and contain no production environment files,
production credentials, signing keys, dependency directories, build caches, or
Git metadata. Isolated relay-verification scripts include clearly marked local
fixture credentials; these are never production deployment settings.
The API context contains its package manifest/lockfile, production server sources,
SQL, and the pinned Dockerfile. Browser production assets are copied into `web/`;
deployment configuration and shared OpenAPI/AsyncAPI contracts accompany them.
The signed Android 1.0.3 (5) APK accompanies the server payload in the archive;
the older-metadata debug build is excluded. A manifest records every
staged file hash, source revision, and the fact that this checkout includes
uncommitted changes. These artifacts are local candidates, not published releases.

| Artifact | Local path | SHA-256 |
| --- | --- | --- |
| Signed Android APK 1.0.3 (5) | `Roomio/androidApp/build/outputs/apk/release/androidApp-release.apk` | `67d37ec94f11f9af86f384194078671029bf23f3c9ee9504acf04d5170fbec66` |
| Coordinated candidate archive | `/tmp/hambin-voice-20261105-v4-rc2.tar.gz` | `d5c2de32e66b92af7903f9bbcd8b6765a657204205a6af559e6b06b378f1bec3` |

The archive contains 124 payload files and its `release.json` manifest. It was
extracted to `/tmp/hambin-voice-final-extracted-t7kl25zs/hambin-voice-20261105-v4-rc2/` and every payload checksum
was verified. Archive validation rejected environment files, private key formats,
keystores, dependency directories, caches, symlinks, and Git metadata. The
public signing-certificate SHA-256 is
`195cb06fd908490a7217bebbd7fccdf0c1abe133acb300b2768b1b625c09e227`.
The archive contains the current 75-test browser build and the signed 56-test
native build. The failed long-call acceptance gate remains explicit in its
manifest; this candidate is not a production stability claim.

The production API image is `hambin-api:20261105-voice-v4-rc2`, image ID
`sha256:1943d068b39b685db2aca3cfb4bc1f53a7991eb3e08b963d3bf186ac78cf045b`.
It includes aggregate `voice_metrics` output and explicit `voice.leave` handling.
It also includes fixed-label Socket.IO close diagnostics. Build evidence is
`/tmp/hambin-voice-api-image-rc2-request-offer-build.log`; no API image was
deployed. Structured voice metrics are cumulative process counters/gauges without
participant IDs, credentials, SDP, or raw audio. The local resource sampler also
counts relay-exhaustion log events without persisting the raw TURN log.


## Final acceptance limit

The final 75-test browser adapter failed forced-TLS eight-person startup
(49/56 connected links, CPU near 100%, 18 rejected acknowledgements). The direct
fallback failed after 75.3 seconds of hold with 54/56 links connected. The
two-hour gate has **not passed**. Physical audio, public relay deployment, and
100 concurrent eight-person rooms remain unverified. No production deployment
was performed. Preserve `/tmp/hambin-soak75-eight-direct` and the TLS failure
evidence alongside the earlier successful short media and cycle runs.
