package room

import (
	"context"
	"time"

	"github.com/khebrati/Hambin/backend/internal/events"
)

// Repository persists rooms, memberships, and their durable events.
type Repository interface {
	CreateRoomWithOwner(ctx context.Context, r Room, owner Membership, event *events.Event) error
	GetRoomByCode(ctx context.Context, code string) (Room, error)
	GetRoomByID(ctx context.Context, id string) (Room, error)
	ListActiveRoomIDs(ctx context.Context, now time.Time) ([]string, error)
	ListMemberships(ctx context.Context, roomID string) ([]Membership, error)
	GetMembership(ctx context.Context, roomID, identityID string) (Membership, error)
	FindActiveMembershipByIdentity(ctx context.Context, identityID string, now time.Time) (Membership, error)
	// JoinRoom upserts a membership (insert or reactivate) and appends events.
	JoinRoom(ctx context.Context, membership Membership, evts ...*events.Event) error
	UpdateMembership(ctx context.Context, membership Membership, evts ...*events.Event) error
	MarkRoomEnded(ctx context.Context, roomID string, endedAt, tombstoneUntil time.Time, event *events.Event) error
	PurgeTombstones(ctx context.Context, before time.Time) ([]string, error)
}
