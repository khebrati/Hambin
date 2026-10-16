import { io, type Socket } from 'socket.io-client'

export interface VoiceState {
  status: 'off' | 'connecting' | 'connected' | 'error'
  muted: boolean
  members: Record<string, { muted: boolean; speaking: boolean }>
  error?: string
}
interface Options {
  roomId: string
  request: <T>(path: string, body?: unknown) => Promise<T>
  token: () => Promise<string>
  onState: (state: VoiceState) => void
}
interface Descriptor {
  mode: string; roomId: string; membershipId: string; voiceSessionId: string
  namespace: string; ticket: string; iceServers: RTCIceServer[]; iceExpiresAt: string
}
interface Member { membershipId: string; voiceSessionId: string; muted: boolean }
interface Signal {
  generation: string; targetGeneration: string; peerGeneration: number; targetPeerGeneration: number
  sdp?: RTCSessionDescriptionInit; ice?: RTCIceCandidateInit; restart?: boolean
}

/** Same peer-pair epoch fence as Android's PeerGenerationFence. */
export class PeerGenerationFence {
  remote?: number
  constructor(readonly local: number) {}
  accept(kind: 'offer' | 'answer' | 'ice' | 'restart', sender: number, target: number) {
    if (!Number.isSafeInteger(sender) || sender <= 0 || !Number.isSafeInteger(target) || target < 0) return false
    if (kind === 'restart') return target === this.local && sender === this.remote
    if (kind === 'answer') {
      if (target !== this.local || (this.remote !== undefined && sender !== this.remote)) return false
    } else {
      if (target !== 0 && target !== this.local) return false
      if (this.remote !== undefined && (kind === 'offer' ? sender <= this.remote : sender !== this.remote)) return false
    }
    if (kind === 'offer' || kind === 'answer') this.remote = sender
    return true
  }
}
export function audioOnlySdp(sdp: unknown): sdp is string {
  if (typeof sdp !== 'string' || new TextEncoder().encode(sdp).length > 60000) return false
  const media = sdp.split(/\r?\n/).filter(line => line.startsWith('m='))
  return media.length === 1 && /^m=audio\s/i.test(media[0])
}
interface Peer {
  member: Member; pc: RTCPeerConnection; fence: PeerGenerationFence; audio: HTMLAudioElement
  pending: { epoch: number; ice: RTCIceCandidateInit }[]; retries: number
  timer?: ReturnType<typeof setTimeout>; outgoing: Promise<void>; ready: boolean; localReady: boolean; localIce: RTCIceCandidateInit[]
}
const permanent = new Set(['SESSION_REVOKED', 'INVALID_TOKEN', 'NOT_A_MEMBER', 'ROOM_ENDED', 'ROOM_NOT_FOUND'])
function code(error: unknown) {
  return typeof error === 'object' && error !== null && 'code' in error ? String(error.code) : ''
}

/** Microphone-only mesh; lifetime is independent of the movie player. */
export class MeshVoice {
  private state: VoiceState = { status: 'off', muted: true, members: {} }
  private descriptor?: Descriptor
  private socket?: Socket
  private stream?: MediaStream
  private peers = new Map<string, Peer>()
  private epochs = new Map<string, number>()
  private timers = new Set<ReturnType<typeof setTimeout>>()
  private serial: Promise<void> = Promise.resolve()
  private signalQueue: Promise<void> = Promise.resolve()
  private lastSignalAt = 0
  private active = false
  private run = 0
  private rosterReady = false
  private reconnecting = false
  private blockedAudio = false
  private authPaused = false
  private pollBusy = false
  private cancelOpen?: () => void
  private activity = new Map<string, { hits: number; last: number }>()
  private energies = new Map<string, { energy: number; duration: number }>()
  constructor(private readonly options: Options) {}

  async join() {
    if (this.active) { await this.resumeAudio(); return }
    this.active = true
    const run = ++this.run
    this.state = { status: 'connecting', muted: true, members: {} }
    this.publish()
    try {
      // Only this explicit opt-in operation requests capture; no camera is requested.
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false })
      stream.getAudioTracks().forEach(track => { track.enabled = false })
      if (!this.current(run)) { stream.getTracks().forEach(track => track.stop()); return }
      this.stream = stream
      await this.open(run, false)
      if (!this.current(run)) return
      this.later(() => void this.refreshCredentials(run), 5 * 60_000)
      this.later(() => void this.poll(run), 200)
    } catch (error) { if (this.current(run)) { this.fail(error, true); throw error } }
  }

  async setMuted(muted: boolean) {
    if (!this.active) return
    // Always stop capture immediately, even while signaling is offline.
    if (muted) this.stream?.getAudioTracks().forEach(track => { track.enabled = false })
    this.state.muted = muted
    this.publish()
    try {
      await this.ack('voice.mute', { muted })
      if (this.active && this.state.muted === muted) this.stream?.getAudioTracks().forEach(track => { track.enabled = !muted })
      await this.resumeAudio()
    } catch (error) {
      if (!muted) {
        this.state.muted = true
        this.stream?.getAudioTracks().forEach(track => { track.enabled = false })
      }
      this.fail(error)
      throw error
    }
  }

  async refreshAuth(idToken: string) {
    if (!this.active) return
    try {
      await this.ack('auth.refresh', { idToken })
      this.authPaused = false
    } catch (error) { this.authPaused = true; this.fail(error, permanent.has(code(error))); throw error }
  }

  /** Call directly from a click when the browser blocks remote audio autoplay. */
  async resumeAudio() {
    const results = await Promise.allSettled([...this.peers.values()].filter(p => p.audio.srcObject).map(p => p.audio.play()))
    this.blockedAudio = results.some(result => result.status === 'rejected')
    if (this.blockedAudio) {
      this.fail(new Error('Audio playback was blocked. Tap Resume voice audio.'))
      throw new Error('Audio playback was blocked. Tap Resume voice audio.')
    }
    this.updateStatus()
  }

  disconnect() {
    this.active = false
    ++this.run
    this.cancelOpen?.(); this.cancelOpen = undefined
    this.timers.forEach(clearTimeout); this.timers.clear()
    this.socket?.removeAllListeners(); this.socket?.disconnect(); this.socket = undefined
    this.peers.forEach(peer => this.closePeer(peer)); this.peers.clear()
    this.stream?.getTracks().forEach(track => track.stop()); this.stream = undefined
    this.descriptor = undefined; this.epochs.clear(); this.activity.clear(); this.energies.clear()
    this.rosterReady = false; this.reconnecting = false; this.blockedAudio = false; this.authPaused = false
    this.state = { status: 'off', muted: true, members: {} }; this.publish()
  }

  private current(run: number) { return this.active && this.run === run }
  private publish() {
    const members = Object.fromEntries(Object.entries(this.state.members).map(([id, value]) => [id, { ...value }]))
    this.options.onState({ ...this.state, members })
  }
  private fail(error: unknown, fatal = false) {
    const message = error instanceof Error ? error.message : code(error) || 'Voice connection failed. Retry voice.'
    if (fatal) this.disconnect()
    this.state = { ...this.state, status: 'error', error: message }; this.publish()
  }
  private later(action: () => void, ms: number) {
    const timer = setTimeout(() => { this.timers.delete(timer); action() }, ms)
    this.timers.add(timer)
    return timer
  }
  private enqueue(action: () => Promise<void>) {
    const run = this.run
    this.serial = this.serial.then(async () => { if (this.current(run)) await action() }).catch(error => {
      if (this.current(run)) this.fail(error, permanent.has(code(error)))
    })
  }
  private async configuration(generation?: string) {
    const result = await this.options.request<Descriptor>(`/v1/rooms/${this.options.roomId}/voice/config`, generation ? { voiceSessionId: generation } : {})
    if (result.mode !== 'MESH' || result.roomId !== this.options.roomId || result.namespace !== `/hambin/voice/${this.options.roomId}`) throw new Error('Unexpected voice configuration')
    return result
  }
  private async open(run: number, reconnect: boolean) {
    let descriptor: Descriptor
    if (reconnect && this.descriptor) {
      try { descriptor = await this.configuration(this.descriptor.voiceSessionId) }
      catch (error) {
        if (code(error) !== 'VOICE_SESSION_EXPIRED') throw error
        descriptor = await this.configuration()
      }
    } else descriptor = await this.configuration()
    if (!this.current(run)) return
    if (this.descriptor && descriptor.voiceSessionId !== this.descriptor.voiceSessionId) {
      this.peers.forEach(p => this.closePeer(p)); this.peers.clear(); this.epochs.clear()
    }
    this.descriptor = descriptor; this.rosterReady = false
    this.socket?.removeAllListeners(); this.socket?.disconnect()
    const origin = (import.meta.env.VITE_BACKEND_URL || window.location.origin).replace(/\/$/, '')
    const socket = io(`${origin}${descriptor.namespace}`, {
      path: '/socket.io/', transports: ['websocket'], reconnection: false, forceNew: true,
      autoConnect: false, auth: { ticket: descriptor.ticket, protocolVersion: 2 },
    })
    this.socket = socket
    await new Promise<void>((resolve, reject) => {
      let settled = false
      const timer = this.later(() => finish(new Error('Voice roster timed out')), 10_000)
      const finish = (error?: unknown) => {
        if (settled) return
        settled = true; clearTimeout(timer); this.timers.delete(timer); this.cancelOpen = undefined
        if (error) reject(error); else resolve()
      }
      this.cancelOpen = () => finish(new Error('Voice connection cancelled'))
      socket.on('voice.roster', (payload: { members: Member[] }) => this.enqueue(async () => {
        if (this.socket !== socket) return
        await this.applyRoster(payload.members)
        finish()
      }))
      socket.on('signal', (payload: { from: string; msg: string }) => this.enqueue(async () => {
        if (this.socket === socket) await this.receive(payload)
      }))
      socket.on('connect', () => { void this.ack('voice.mute', { muted: this.state.muted }).catch(error => this.fail(error)) })
      socket.on('connect_error', (error: Error & { data?: unknown }) => {
        finish(error.data || error)
        if (settled && this.current(run)) this.reconnect(run)
      })
      socket.on('disconnect', () => { finish(new Error('Voice signaling disconnected')); this.reconnect(run) })
      socket.on('error', (error: unknown) => this.fail(error, permanent.has(code(error))))
      socket.connect()
    })
  }
  private reconnect(run: number) {
    if (!this.current(run) || this.reconnecting) return
    this.reconnecting = true; this.rosterReady = false; this.updateStatus()
    let attempt = 0
    const retry = async () => {
      if (!this.current(run)) return
      try {
        await this.options.token()
        await this.open(run, true)
        this.reconnecting = false
      } catch (error) {
        if (!this.current(run)) return
        if (permanent.has(code(error)) || ++attempt >= 6) { this.reconnecting = false; this.fail(error, true); return }
        this.later(() => void retry(), Math.min(1000 * 2 ** attempt, 15_000))
      }
    }
    this.later(() => void retry(), 1000)
  }
  private ack(event: string, payload: unknown): Promise<unknown> {
    const socket = this.socket
    if (!socket?.connected) return Promise.reject(new Error('Voice signaling is offline'))
    return new Promise((resolve, reject) => {
      socket.timeout(8000).emit(event, payload, (error: Error | null, response: { code?: string } | undefined) => {
        if (error) reject(error)
        else if (response?.code) reject(Object.assign(new Error(response.code), response))
        else resolve(response)
      })
    })
  }
  private async applyRoster(members: Member[]) {
    if (!Array.isArray(members) || members.length > 8 || members.some(m => typeof m.membershipId !== 'string' || typeof m.voiceSessionId !== 'string' || typeof m.muted !== 'boolean')) throw new Error('Invalid voice roster')
    const self = members.find(m => m.membershipId === this.descriptor!.membershipId)
    if (!self || self.voiceSessionId !== this.descriptor!.voiceSessionId) throw new Error('Voice session was replaced')
    const roster = new Map(members.map(member => [member.membershipId, member]))
    for (const [id, peer] of this.peers) {
      if (roster.get(id)?.voiceSessionId !== peer.member.voiceSessionId) { this.closePeer(peer); this.peers.delete(id) }
    }
    this.state.members = Object.fromEntries(members.map(m => [m.membershipId, { muted: m.muted, speaking: !m.muted && (this.state.members[m.membershipId]?.speaking ?? false) }]))
    for (const member of members) {
      if (member.membershipId === this.descriptor!.membershipId) continue
      const peer = this.peers.get(member.membershipId) ?? this.createPeer(member)
      peer.member = member; peer.audio.muted = member.muted
    }
    this.rosterReady = true; this.updateStatus()
  }
  private createPeer(member: Member, retries = 0, offer = true): Peer {
    const pc = new RTCPeerConnection({ iceServers: this.descriptor!.iceServers })
    const epoch = (this.epochs.get(member.membershipId) ?? 0) + 1
    this.epochs.set(member.membershipId, epoch)
    const audio = new Audio(); audio.autoplay = true; audio.muted = member.muted
    const peer: Peer = { member, pc, fence: new PeerGenerationFence(epoch), audio, pending: [], retries, outgoing: Promise.resolve(), ready: false, localReady: false, localIce: [] }
    this.peers.set(member.membershipId, peer)
    this.stream!.getAudioTracks().forEach(track => pc.addTrack(track, this.stream!))
    pc.ondatachannel = event => event.channel.close()
    pc.ontrack = event => {
      if (event.track.kind !== 'audio') { event.track.enabled = false; return }
      audio.srcObject = new MediaStream([event.track])
      void audio.play().catch(() => { if (this.peers.get(member.membershipId) === peer) { this.blockedAudio = true; this.fail(new Error('Audio playback was blocked. Tap Resume voice audio.')) } })
    }
    pc.onicecandidate = event => {
      if (!event.candidate) return
      if (peer.localReady) this.send(peer, { ice: event.candidate.toJSON() })
      else if (peer.localIce.length < 64) peer.localIce.push(event.candidate.toJSON())
    }
    pc.onconnectionstatechange = () => {
      if (this.peers.get(member.membershipId) !== peer) return
      if (pc.connectionState === 'connected') { clearTimeout(peer.timer); peer.retries = 0 }
      if (pc.connectionState === 'failed') this.enqueue(() => this.retry(peer, true))
      if (pc.connectionState === 'disconnected') this.later(() => {
        if (pc.connectionState === 'disconnected') this.enqueue(() => this.retry(peer, true))
      }, 5000)
      this.updateStatus()
    }
    peer.timer = this.later(() => { if (pc.connectionState !== 'connected') this.enqueue(() => this.retry(peer, true)) }, 10_000)
    if (offer && this.descriptor!.membershipId < member.membershipId) this.enqueue(() => this.offer(peer))
    return peer
  }
  private live(peer: Peer) { return this.active && this.peers.get(peer.member.membershipId) === peer }
  private async offer(peer: Peer) {
    if (!this.live(peer) || !this.rosterReady) return
    const description = await peer.pc.createOffer()
    if (!this.live(peer)) return
    await peer.pc.setLocalDescription(description)
    this.sendDescription(peer, description)
  }
  private sendDescription(peer: Peer, description: RTCSessionDescriptionInit) {
    this.send(peer, { sdp: description })
    peer.localReady = true
    for (const ice of peer.localIce.splice(0)) this.send(peer, { ice })
  }
  private send(peer: Peer, content: Pick<Signal, 'sdp' | 'ice' | 'restart'>) {
    if (!this.live(peer) || !this.rosterReady || this.authPaused) return
    if (content.restart && peer.fence.remote === undefined) return
    const message: Signal = {
      generation: this.descriptor!.voiceSessionId, targetGeneration: peer.member.voiceSessionId,
      peerGeneration: peer.fence.local, targetPeerGeneration: content.sdp?.type === 'offer' ? 0 : peer.fence.remote ?? 0, ...content,
    }
    if (content.sdp && !audioOnlySdp(content.sdp.sdp)) { this.fail(new Error('Only microphone audio is supported')); return }
    // Per-peer FIFO preserves SDP/candidate order. Server remains authoritative.
    peer.outgoing = peer.outgoing.then(async () => {
      if (!this.live(peer)) return
      // The server budget is per member, shared by all seven peer links.
      // A per-peer FIFO alone bursts past that budget as a full mesh joins.
      this.signalQueue = this.signalQueue.catch(() => {}).then(async () => {
        const delay = Math.max(0, 60 - (Date.now() - this.lastSignalAt))
        if (delay) await new Promise(resolve => setTimeout(resolve, delay))
        if (!this.live(peer)) return
        this.lastSignalAt = Date.now()
        for (let attempt = 0; ; attempt++) {
          try { await this.ack('signal', { to: peer.member.membershipId, msg: JSON.stringify(message) }); return }
          catch (error) {
            if (code(error) !== 'RATE_LIMITED' || attempt >= 2) throw error
            await new Promise(resolve => setTimeout(resolve, 250 * (attempt + 1)))
            if (!this.live(peer)) return
            this.lastSignalAt = Date.now()
          }
        }
      })
      await this.signalQueue
    }).catch(error => {
      if (!this.live(peer) || code(error) === 'STALE_NEGOTIATION') return
      if (code(error) === 'SESSION_EXPIRED' || code(error) === 'AUTH_UNAVAILABLE') this.authPaused = true
      this.fail(error, permanent.has(code(error)))
    })
  }
  private async receive(payload: { from: string; msg: string }) {
    if (!payload || typeof payload.msg !== 'string' || new TextEncoder().encode(payload.msg).length > 65536) return
    let message: Signal
    try { message = JSON.parse(payload.msg) as Signal } catch { return }
    let peer = this.peers.get(payload.from)
    if (!peer || !message || message.generation !== peer.member.voiceSessionId || message.targetGeneration !== this.descriptor!.voiceSessionId) return
    if (Number(!!message.sdp) + Number(!!message.ice) + Number(message.restart === true) !== 1) return
    const sender = message.peerGeneration; const target = message.targetPeerGeneration
    if (message.sdp) {
      const { type, sdp } = message.sdp
      if ((type !== 'offer' && type !== 'answer') || !audioOnlySdp(sdp)) return
      const previous = peer.fence.remote
      if (type === 'offer' && this.descriptor!.membershipId < payload.from) return
      if (!peer.fence.accept(type, sender, target)) return
      if (type === 'offer' && previous !== undefined) {
        const member = peer.member; const retries = peer.retries
        this.closePeer(peer); this.peers.delete(payload.from)
        peer = this.createPeer(member, retries, false)
        if (!peer.fence.accept('offer', sender, target)) return
      }
      await peer.pc.setRemoteDescription({ type, sdp })
      if (!this.live(peer)) return
      peer.ready = true
      for (const candidate of peer.pending.splice(0)) if (candidate.epoch === peer.fence.remote) await peer.pc.addIceCandidate(candidate.ice)
      if (type === 'offer') {
        const answer = await peer.pc.createAnswer()
        if (!this.live(peer)) return
        await peer.pc.setLocalDescription(answer)
        this.sendDescription(peer, answer)
      }
    } else if (message.ice) {
      const ice = message.ice
      if (typeof ice.candidate !== 'string' || ice.candidate.length > 4096 || !peer.fence.accept('ice', sender, target)) return
      if (peer.ready) await peer.pc.addIceCandidate(ice)
      else if (peer.pending.length < 64) peer.pending.push({ epoch: sender, ice })
    } else if (message.restart && peer.fence.accept('restart', sender, target)) await this.retry(peer, false)
  }
  private async retry(peer: Peer, notify: boolean) {
    if (!this.live(peer) || !this.rosterReady || !this.socket?.connected) return
    if (peer.retries >= 3) { this.fail(new Error('Could not connect to every voice participant. Retry voice.'), true); return }
    if (notify && peer.fence.remote !== undefined) {
      this.send(peer, { restart: true }); await peer.outgoing
      if (!this.live(peer)) return
    }
    this.closePeer(peer); this.peers.delete(peer.member.membershipId)
    this.createPeer(peer.member, peer.retries + 1)
    this.updateStatus()
  }
  private closePeer(peer: Peer) {
    clearTimeout(peer.timer); if (peer.timer) this.timers.delete(peer.timer)
    peer.pc.onconnectionstatechange = null; peer.pc.onicecandidate = null; peer.pc.ontrack = null
    peer.pc.close(); peer.audio.pause(); peer.audio.srcObject = null; peer.pending.length = 0; peer.localIce.length = 0
    for (const key of this.energies.keys()) if (key.startsWith(`${peer.member.membershipId}:${peer.fence.local}:`)) this.energies.delete(key)
    this.activity.delete(peer.member.membershipId)
  }
  private updateStatus() {
    if (!this.active || this.blockedAudio || this.authPaused) return
    const ready = this.rosterReady && [...this.peers.values()].every(peer => peer.pc.connectionState === 'connected')
    this.state = { ...this.state, status: ready ? 'connected' : 'connecting', error: undefined }; this.publish()
  }
  private async refreshCredentials(run: number) {
    if (!this.current(run)) return
    let delay = 5 * 60_000
    try {
      await this.refreshAuth(await this.options.token())
      const descriptor = await this.configuration(this.descriptor!.voiceSessionId)
      if (!this.current(run)) return
      if (descriptor.voiceSessionId !== this.descriptor!.voiceSessionId || descriptor.membershipId !== this.descriptor!.membershipId) throw Object.assign(new Error('Voice session was replaced'), { code: 'SESSION_REVOKED' })
      this.descriptor = descriptor
      this.peers.forEach(peer => peer.pc.setConfiguration({ iceServers: descriptor.iceServers }))
      this.updateStatus()
    } catch (error) { if (this.current(run)) this.fail(error, permanent.has(code(error))); delay = 30_000 }
    if (this.current(run)) this.later(() => void this.refreshCredentials(run), delay)
  }
  private async poll(run: number) {
    if (!this.current(run)) return
    if (!this.pollBusy) {
      this.pollBusy = true
      const levels = new Map<string, number>()
      try {
        await Promise.all([...this.peers.values()].map(async peer => {
          if (peer.pc.connectionState !== 'connected') return
          const stats = await peer.pc.getStats()
          if (!this.live(peer)) return
          stats.forEach(report => {
            if (report.kind !== 'audio' && report.mediaType !== 'audio') return
            const local = report.type === 'media-source'
            if (!local && report.type !== 'inbound-rtp') return
            const id = local ? this.descriptor!.membershipId : peer.member.membershipId
            let level = typeof report.audioLevel === 'number' ? report.audioLevel : 0
            if (typeof report.totalAudioEnergy === 'number' && typeof report.totalSamplesDuration === 'number') {
              const key = `${peer.member.membershipId}:${peer.fence.local}:${report.id}`
              const previous = this.energies.get(key)
              if (previous && report.totalSamplesDuration > previous.duration && report.totalAudioEnergy >= previous.energy) level = Math.max(level, Math.sqrt((report.totalAudioEnergy - previous.energy) / (report.totalSamplesDuration - previous.duration)))
              this.energies.set(key, { energy: report.totalAudioEnergy, duration: report.totalSamplesDuration })
            }
            levels.set(id, Math.max(levels.get(id) ?? 0, level))
          })
        }))
        if (!this.current(run)) return
        for (const [id, member] of Object.entries(this.state.members)) {
          const item = this.activity.get(id) ?? { hits: 0, last: 0 }
          const muted = member.muted || (id === this.descriptor?.membershipId && this.state.muted)
          if (!muted && (levels.get(id) ?? 0) >= 0.02) { item.hits++; item.last = Date.now() }
          else item.hits = 0
          member.speaking = !muted && (item.hits >= 2 || (member.speaking && Date.now() - item.last < 500))
          this.activity.set(id, item)
        }
        this.publish()
      } catch { /* A closed peer may reject an in-flight stats request. */ }
      finally { this.pollBusy = false }
    }
    if (this.current(run)) this.later(() => void this.poll(run), 200)
  }
}
