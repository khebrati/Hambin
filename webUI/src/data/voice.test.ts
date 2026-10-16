import { afterEach, describe, expect, it, vi } from 'vitest'
import { MeshVoice, PeerGenerationFence, audioOnlySdp, type VoiceState } from './voice'

const sockets = vi.hoisted(() => [] as FakeSocket[])
vi.mock('socket.io-client', () => ({ io: () => { const socket = new FakeSocket(); sockets.push(socket); return socket } }))
class FakeSocket {
  connected = false
  events = new Map<string, (...args: any[]) => void>()
  sent: { event: string; payload: any }[] = []
  on(event: string, handler: (...args: any[]) => void) { this.events.set(event, handler); return this }
  connect() { this.connected = true; this.events.get('connect')?.(); return this }
  disconnect() { this.connected = false; return this }
  removeAllListeners() { this.events.clear(); return this }
  timeout() { return this }
  emit(event: string, payload: any, ack: (...args: any[]) => void) { this.sent.push({ event, payload }); ack(null, { ok: true }); return this }
  deliver(event: string, payload: unknown) { this.events.get(event)?.(payload) }
}
const descriptor = { mode: 'MESH', roomId: 'room', membershipId: 'a', voiceSessionId: 'g1', namespace: '/hambin/voice/room', ticket: 'secret', iceServers: [], iceExpiresAt: new Date(Date.now() + 600000).toISOString() }
const flush = async () => { for (let i = 0; i < 15; i++) await Promise.resolve() }
const clients: MeshVoice[] = []
function setup() {
  const track = { enabled: true, stop: vi.fn() }
  const stream = { getAudioTracks: () => [track], getTracks: () => [track] }
  vi.stubGlobal('navigator', { mediaDevices: { getUserMedia: vi.fn().mockResolvedValue(stream) } })
  vi.stubGlobal('window', { location: { origin: 'https://example.test' } })
  const request = vi.fn().mockResolvedValue(descriptor)
  const states: VoiceState[] = []
  const client = new MeshVoice({ roomId: 'room', request, token: async () => 'id-token', onState: state => states.push(state) })
  clients.push(client)
  return { client, request, track, states }
}
afterEach(() => { clients.splice(0).forEach(client => client.disconnect()); sockets.length = 0; vi.unstubAllGlobals(); vi.useRealTimers() })

describe('Android-compatible peer epochs', () => {
  it('fences stale answers, candidates and restarts after replacement', () => {
    const fence = new PeerGenerationFence(3)
    expect(fence.accept('answer', 7, 2)).toBe(false)
    expect(fence.accept('ice', 7, 0)).toBe(true)
    expect(fence.remote).toBeUndefined()
    expect(fence.accept('answer', 7, 3)).toBe(true)
    expect(fence.accept('ice', 6, 3)).toBe(false)
    expect(fence.accept('restart', 7, 2)).toBe(false)
    expect(fence.accept('restart', 7, 3)).toBe(true)
  })
  it('only accepts newer offers and safe integer epochs', () => {
    const fence = new PeerGenerationFence(1)
    expect(fence.accept('offer', 2, 0)).toBe(true)
    expect(fence.accept('offer', 2, 0)).toBe(false)
    expect(fence.accept('offer', 1, 0)).toBe(false)
    expect(fence.accept('offer', 3, 0)).toBe(true)
    expect(fence.accept('ice', Number.MAX_SAFE_INTEGER + 1, 0)).toBe(false)
    expect(fence.accept('ice', 3, -1)).toBe(false)
  })
})
describe('microphone-only SDP', () => {
  it('rejects video, control data channels, duplicate media, and oversized SDP', () => {
    expect(audioOnlySdp('v=0\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111\r\n')).toBe(true)
    for (const sdp of ['m=video 9 RTP/AVP 96', 'm=audio 9 RTP/AVP 111\nm=application 9 UDP/DTLS/SCTP webrtc-datachannel', 'm=audio 9 x\nm=audio 9 x', 'm=audio ' + 'x'.repeat(60000)]) expect(audioOnlySdp(sdp)).toBe(false)
  })
})
describe('voice capture lifecycle', () => {
  it('opts in muted, waits for roster, updates auth without replacing media, and stops capture on leave', async () => {
    const { client, track, states, request } = setup()
    const joining = client.join(); await flush()
    expect(track.enabled).toBe(false)
    expect(states.at(-1)?.status).toBe('connecting')
    sockets[0].deliver('voice.roster', { members: [{ membershipId: 'a', voiceSessionId: 'g1', muted: true }] })
    await joining
    expect(states.at(-1)?.status).toBe('connected')
    await client.refreshAuth('new-id-token')
    expect(request).toHaveBeenCalledTimes(1)
    expect(sockets[0].sent).toContainEqual({ event: 'auth.refresh', payload: { idToken: 'new-id-token' } })
    await client.setMuted(false); expect(track.enabled).toBe(true)
    await client.setMuted(true); expect(track.enabled).toBe(false)
    client.disconnect()
    expect(track.stop).toHaveBeenCalledTimes(1)
    expect(sockets[0].connected).toBe(false)
    expect(states.at(-1)?.status).toBe('off')
  })
  it('stops a late permission result after leave without creating a socket', async () => {
    const { client, track } = setup()
    let resolve!: (value: MediaStream) => void
    vi.mocked(navigator.mediaDevices.getUserMedia).mockImplementation(() => new Promise(done => { resolve = done }))
    const joining = client.join()
    client.disconnect()
    resolve({ getAudioTracks: () => [track], getTracks: () => [track] } as unknown as MediaStream)
    await joining
    expect(track.stop).toHaveBeenCalledTimes(1)
    expect(sockets).toHaveLength(0)
  })
  it('settles an in-flight join when disconnected before the roster', async () => {
    const { client, track } = setup()
    const joining = client.join(); await flush()
    client.disconnect()
    await joining
    expect(track.stop).toHaveBeenCalledTimes(1)
    expect(sockets[0].connected).toBe(false)
  })
  it('reports microphone denial and releases the session', async () => {
    const { client, states } = setup()
    vi.mocked(navigator.mediaDevices.getUserMedia).mockRejectedValue(new Error('Microphone permission denied'))
    await expect(client.join()).rejects.toThrow('permission denied')
    expect(states.at(-1)?.status).toBe('error')
    expect(sockets).toHaveLength(0)
  })
})
