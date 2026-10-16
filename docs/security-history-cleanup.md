# Public repository credential audit — 2026-10-16

## Scope and findings

The audit covered all public Git branches and tags in `khebrati/Hambin`
and its linked `khebrati/watchparty` repository, plus local reachable history.
Gitleaks 8.30.1 scanned 46 public Hambin commits and 1,828 public WatchParty
commits before cleanup. Supplemental checks compared local confidential
configuration values against every reachable blob and inspected sensitive
filenames. The published Android APK was checked for private-key and Google
API-key patterns. Neither repository had pull requests, issues, comments,
Actions runs, or Actions artifacts at the time of inspection.

- Hambin contained a Firebase client API key in native source. GitHub had one
  open secret-scanning alert for it. The key was removed from reachable history
  and native configuration now comes from the local build environment.
- WatchParty's inherited history contained legacy TURN credentials, a development
  private key at `server/insecurekey.pem`, and a Google/Firebase client key.
  The credentials and client key were replaced throughout history, and the
  private-key file was removed throughout history.
- No Firebase Admin private key, signing keystore, or local confidential
  configuration value was found in public Git objects. No private-key or Google
  API-key pattern was found in the published APK. These are scan results, not a
  guarantee that no unrecognized credential exists.
- Remaining scanner findings were synthetic test/development values and public
  Discord client/reCAPTCHA site identifiers. These were reviewed and retained.

## Handling after cleanup

History changes require collaborators to use the cleaned history. Prefer a
fresh clone; preserve uncommitted work separately and inspect patches before
reapplying them. Do not merge or push old branches back into the public repos.

Removal from Git does not revoke credentials. Any service still accepting the
legacy TURN credentials or using the removed private key must replace them.
No third-party service credentials were exercised or revoked during this audit.

Firebase client API keys are public identifiers, unlike Firebase Admin service
account private keys. Keeping a client key outside source does not hide it in a
distributed app. Review its API restrictions and quotas in the Google Cloud
console; avoid revoking a live client key without coordinating affected clients.

GitHub may retain old objects and cached commit views after a history rewrite.
Repository owners must contact GitHub Support if eligible sensitive cached
objects need purging. No support request was sent by this audit.

References: [GitHub sensitive-data removal](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository)
and [Firebase API-key guidance](https://firebase.google.com/docs/projects/api-keys).
