import { io, type Socket } from 'socket.io-client';
import { ApiError, backendUrl, request } from './api';
import type { RoomEvent, Snapshot, SyncResult } from './contracts';
import { reduceEvent } from './contracts';

interface Options { roomId: string; onSnapshot(value: Snapshot): void; onConnection(connected: boolean): void; onError(error: unknown): void; onClosed(): void }
export class RoomConnection {
  private socket?: Socket;
  private closed = false;
  private timer?: ReturnType<typeof setTimeout>;
  private retry = 0;
  private generation = 0;
  private snapshot?: Snapshot;
  private recovery?: Promise<void>;
  private pendingEvents: RoomEvent[] = [];
  private syncRequest = '';
  private authReady = false;
  private syncWaiters = new Map<string, { resolve: (value: SyncResult) => void; reject: (error: unknown) => void; timer: ReturnType<typeof setTimeout> }>();
  constructor(private options: Options) {}
  start() { void this.connect(); }
  private async connect() {
    const generation = ++this.generation;
    try {
      const admission = await request<{ ticket: string; namespace: string }>(`/v1/rooms/${this.options.roomId}/realtime-ticket`, {});
      if (this.closed || generation !== this.generation) return;
      const socket = io(`${backendUrl || location.origin}${admission.namespace}`, { path: '/socket.io/', transports: ['websocket'], auth: { ticket: admission.ticket, protocolVersion: 2 }, reconnection: false, forceNew: true, autoConnect: false, timeout: 10000 });
      this.socket = socket;
      socket.on('snapshot', (value: Snapshot) => {
        if (generation !== this.generation || this.closed) return;
        this.retry = 0;
        this.authReady = true;
        this.applySnapshot(value);
        this.options.onConnection(true);
      });
      socket.on('event', (event: RoomEvent) => {
        if (generation !== this.generation || this.closed) return;
        socket.emit('ack', { eventId: event.eventId });
        if (event.topic === 'room.closed') { this.options.onClosed(); this.close(); return; }
        if (this.recovery) {
          this.pendingEvents.push(event);
          if (this.pendingEvents.length > 256) { socket.disconnect(); this.schedule(); }
          return;
        }
        if (!this.snapshot) return;
        const next = reduceEvent(this.snapshot, event);
        if (next) this.applySnapshot(next); else { this.pendingEvents.push(event); void this.recover().catch(e => this.options.onError(e)); }
      });
      socket.on('sync.result', (result: SyncResult) => {
        const waiter = this.syncWaiters.get(result.requestId);
        if (!waiter) return;
        clearTimeout(waiter.timer); this.syncWaiters.delete(result.requestId); waiter.resolve(result);
      });
      socket.on('error', (error: { code: string }) => this.options.onError(new ApiError(error.code)));
      socket.on('disconnect', () => this.schedule());
      socket.on('connect_error', (error: Error & { data?: { code: string } }) => {
        this.options.onError(new ApiError(error.data?.code || 'NETWORK'));
        this.schedule();
      });
      socket.connect();
    } catch (error) {
      if (this.closed || generation !== this.generation) return;
      this.options.onError(error);
      if (error instanceof ApiError && ['NOT_A_MEMBER', 'ROOM_ENDED', 'ROOM_NOT_FOUND', 'SESSION_REVOKED'].includes(error.code)) { this.options.onClosed(); this.close(); } else this.schedule();
    }
  }
  private schedule() {
    if (this.closed || this.timer) return;
    this.authReady = false;
    this.options.onConnection(false);
    this.socket?.removeAllListeners(); this.socket?.disconnect(); this.socket = undefined;
    this.timer = setTimeout(() => { this.timer = undefined; void this.connect(); }, Math.min(10000, 500 * 2 ** Math.min(this.retry++, 5)));
  }
  applySnapshot(value: Snapshot) {
    if (this.closed || (this.snapshot && value.sequence < this.snapshot.sequence)) return;
    if (this.snapshot && value.stream.revision < this.snapshot.stream.revision) return;
    if (this.snapshot?.stream.sessionId !== value.stream.sessionId) this.syncRequest = '';
    value = { ...value, ownerName: value.members.find(m => m.isOwner)?.displayName || value.ownerName || this.snapshot?.ownerName };
    this.snapshot = value;
    this.options.onSnapshot(value);
  }
  async recover() {
    if (this.recovery) return this.recovery;
    const generation = this.generation;
    this.recovery = request<Snapshot>(`/v1/rooms/${this.options.roomId}/snapshot`).then(value => {
      if (!this.closed && generation === this.generation) this.applySnapshot(value);
    }).finally(() => { this.recovery = undefined; });
    await this.recovery;
    if (this.closed || generation !== this.generation) { this.pendingEvents = []; return; }
    const events = this.pendingEvents.splice(0).sort((a, b) => a.sequence - b.sequence);
    for (const event of events) {
      if (!this.snapshot) break;
      const next = reduceEvent(this.snapshot, event);
      if (!next) { this.schedule(); break; }
      this.applySnapshot(next);
    }
  }
  report(positionMs: number, playing: boolean) {
    const stream = this.snapshot?.stream;
    if (this.socket?.connected && this.authReady && stream?.state === 'ACTIVE') this.socket.volatile.emit('playback.report', { streamSessionId: stream.sessionId, positionMs: Math.max(0, Math.round(positionMs)), playing });
  }
  async sync(positionMs: number): Promise<SyncResult | undefined> {
    const stream = this.snapshot?.stream;
    if (!this.socket?.connected || !this.authReady || stream?.state !== 'ACTIVE') throw new ApiError('NETWORK');
    const requestId = crypto.randomUUID();
    const sessionId = stream.sessionId;
    this.syncRequest = requestId;
    const result = await new Promise<SyncResult>((resolve, reject) => {
      const timer = setTimeout(() => { this.syncWaiters.delete(requestId); reject(new ApiError('NETWORK')); }, 10000);
      this.syncWaiters.set(requestId, { resolve, reject, timer });
      this.socket!.emit('sync.request', { requestId, streamSessionId: sessionId, positionMs: Math.max(0, Math.round(positionMs)) });
    });
    if (this.closed || this.syncRequest !== requestId || this.snapshot?.stream.sessionId !== sessionId || this.snapshot.stream.state !== 'ACTIVE') return;
    return result;
  }
  async refreshAuth(idToken: string) {
    const socket = this.socket;
    if (!socket?.connected) return;
    this.authReady = false;
    await new Promise<void>((resolve, reject) => socket.timeout(10000).emit('auth.refresh', { idToken }, (error: Error | null, result: { code?: string }) => {
      if (error || result?.code) { this.schedule(); reject(error || new ApiError(result.code!)); } else { this.authReady = true; resolve(); }
    }));
  }
  close() {
    this.closed = true; ++this.generation; this.syncRequest = '';
    clearTimeout(this.timer); this.timer = undefined;
    this.socket?.removeAllListeners(); this.socket?.disconnect();
    for (const w of this.syncWaiters.values()) { clearTimeout(w.timer); w.resolve({ requestId: '', status: 'UNAVAILABLE', targetPositionMs: 0 }); }
    this.syncWaiters.clear();
  }
}
