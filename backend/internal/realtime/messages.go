package realtime

import (
	"encoding/json"

	"github.com/khebrati/Hambin/backend/internal/room"
	"github.com/khebrati/Hambin/backend/internal/stream"
)

// Client to server message types.
const (
	clientPlaybackReport = "playback.report"
	clientSyncRequest    = "sync.request"
	clientPing           = "ping"
	clientAck            = "ack"
)

// Server to client message types.
const (
	serverSnapshot   = "snapshot"
	serverEvent      = "event"
	serverSyncResult = "sync.result"
	serverPong       = "pong"
	serverError      = "error"
)

type clientMessage struct {
	Type            string `json:"type"`
	StreamSessionID string `json:"streamSessionId,omitempty"`
	PositionMs      int64  `json:"positionMs,omitempty"`
	Playing         bool   `json:"playing,omitempty"`
	EventID         string `json:"eventId,omitempty"`
}

type eventMessage struct {
	Type    string          `json:"type"`
	Topic   string          `json:"topic"`
	EventID string          `json:"eventId"`
	Payload json.RawMessage `json:"payload"`
}

type snapshotMessage struct {
	Type    string               `json:"type"`
	Room    room.Room            `json:"room"`
	Members []room.MemberSummary `json:"members"`
	Stream  stream.Stream        `json:"stream"`
}

type syncResultMessage struct {
	Type             string     `json:"type"`
	Status           SyncStatus `json:"status"`
	TargetPositionMs int64      `json:"targetPositionMs"`
}

type errorMessage struct {
	Type    string `json:"type"`
	Code    string `json:"code"`
	Message string `json:"message"`
}
