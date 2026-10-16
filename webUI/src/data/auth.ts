import { initializeApp } from 'firebase/app';
import { browserLocalPersistence, connectAuthEmulator, initializeAuth, onIdTokenChanged, signInAnonymously, type Auth } from 'firebase/auth';

let auth: Auth | undefined;
let signIn: Promise<void> | undefined;
function firebaseAuth() {
  if (auth) return auth;
  const emulator = import.meta.env.VITE_FIREBASE_AUTH_EMULATOR_URL;
  const projectId = import.meta.env.VITE_FIREBASE_PROJECT_ID || (emulator ? 'demo-hambin' : '');
  const apiKey = import.meta.env.VITE_FIREBASE_API_KEY || (emulator ? 'demo-key' : '');
  if (!projectId || !apiKey) throw new Error('Firebase is not configured. Follow the webUI README to start locally.');
  if (emulator && !import.meta.env.DEV) throw new Error('The Auth Emulator is development-only.');
  const app = initializeApp({ projectId, apiKey, authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || `${projectId}.firebaseapp.com` });
  auth = initializeAuth(app, { persistence: browserLocalPersistence });
  if (emulator) {
    connectAuthEmulator(auth, emulator, { disableWarnings: true });
  }
  return auth;
}
export async function token(force = false): Promise<string> {
  const client = firebaseAuth();
  await client.authStateReady();
  if (!client.currentUser) {
    // Once an identity existed, a provider failure or revocation must not silently replace it.
    if (localStorage.getItem('roomio-auth-established')) throw new Error('Your saved session is unavailable. Restore site storage or explicitly reset this site identity to continue.');
    if (!signIn) signIn = signInAnonymously(client).then(() => { localStorage.setItem('roomio-auth-established', '1'); }).finally(() => { signIn = undefined; });
    await signIn;
  }
  return client.currentUser!.getIdToken(force);
}
export function watchToken(callback: (idToken: string) => void): () => void {
  return onIdTokenChanged(firebaseAuth(), user => { if (user) void user.getIdToken().then(callback).catch(() => {}); });
}
