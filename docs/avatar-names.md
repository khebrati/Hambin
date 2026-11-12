# Avatar names

DD-054 replaces editable/random guest names in browser and native clients. Each avatar determines a fixed English display name, independent of interface language. Picker labels are localized; profile, room and chat names use the canonical name so everyone sees the same identity label. Avatar IDs and guest UUIDs stay stable.

| Avatar ID | Display name | Persian picker label |
| --- | --- | --- |
| comet | Ice Queen | ملکهٔ یخی |
| mint | Frog Wizard | قورباغهٔ جادوگر |
| sunny | Skeleton Detective | کارآگاه اسکلتی |
| berry | Captain Octopus | کاپیتان اختاپوس |
| cloud | Gamer TV | تلویزیون گیمر |
| ember | Alien Scientist | دانشمند فضایی |
| nova | Robot Poet | ربات شاعر |
| orbit | Coffee Wolf | گرگ قهوه‌دوست |
| prism | Professor Fox | پروفسور روباه |
| echo | Bookworm Dragon | اژدهای کتاب‌خوان |
| spark | Frankie | فرانکی |
| bloom | Mummy Barista | مومیایی باریستا |

Save commits avatar and language; Back discards the draft. Selecting an avatar updates the name preview immediately. There is no custom name input or blank-name state. Fixed choices remain available offline; browser Save retains its existing loading/error/retry states. Native local persistence failures retain the saved profile.

The backend derives the name from the avatar on creation and profile updates. The legacy request `name` field is optional and ignored. Avatar IDs are case-insensitive on input and stored lowercase; unsupported IDs are rejected. Existing profiles and membership labels are normalized in a new migration, preserving guest IDs, credentials, membership IDs and ownership. Previously sent chat messages retain their historical name/avatar snapshots. Unsupported historical avatar IDs fall back to comet.

Duplicate names and avatars are allowed, including within one room. Authentication, ownership, membership, voice and chat use UUIDs rather than display names. Existing You/Owner indicators distinguish roles, but two other guests with the same avatar remain visually ambiguous. Do not invent numeric suffixes or restrict avatar availability.

## Validation

On 2026-11-12, verification in an isolated checkout containing only the avatar feature passed: browser production build and 96 unit tests; backend TypeScript build and 12 unit tests; Android debug assembly and app JVM dependency-graph compilation. Focused native profile, persistence, ViewModel and Compose checks passed (27 passed, one existing skip).

Earlier cross-client validation passed five Chrome checks covering Save, draft discard, failed Save/retry, reload persistence and compact/expanded Persian layouts, plus ten real PostgreSQL/Socket.IO integration checks. The duplicate-name integration case verifies separate credentials, owner/member roles and chat senders, plus migration preservation of membership IDs, ownership and historical chat names.

Two native rendering tests passed for compact dark Persian at 150% text scale and expanded light English. Reviewed screenshots confirm that avatar names wrap within the grid, custom text inputs are absent, and Save persists the selected name/avatar pair. Physical Android and iOS runtime verification were not performed on this Linux host. No deployment was performed.
