# Roomio Prototype

Interactive React/Vite prototype for the approved **Immersive Cinema** direction.

## Run locally

From `prototype/`:

```powershell
npm.cmd install
npm.cmd run dev --host 127.0.0.1
```

Open `http://127.0.0.1:5173/`.

## Primary flow

1. On Home, open Settings, change the name or illustrated avatar, and save.
2. Select **Create a party**.
3. Create the room and wait for the empty cinema.
4. Use the paste action or enter `https://media.example.org/films/aurora-station.mp4`.
5. Start the stream and exercise play/pause, backward, forward, timeline, volume, mic, Sync, link inspection, and invite sharing.
6. Open **End stream for everyone**, cancel once, then confirm to review the global action.
7. Start another stream in the same room.
8. Use Leave, cancel once, then confirm. The room remains ownerless; use the Home card to rejoin as its original owner.
9. From Home, choose **Join with code**. Enter `MOON-42` for an active room, `ORBIT-08` for an owner-absent room, `FULL-10` for a full room, or `ENDED-3` for an ended room.
10. Review the room preview, then confirm or cancel before joining.

## State previews

Append one of these query strings to the local URL:

| Preview | Query |
| --- | --- |
| Home | `?preview=home` |
| Empty settings | `?preview=settings-empty` |
| Populated settings | `?preview=settings-populated` |
| Party manager | `?preview=party-manager` |
| Party manager loading | `?preview=party-manager-loading` |
| Party manager error | `?preview=party-manager-error` |
| Available room preview | `?preview=room-preview` |
| Owner-absent room preview | `?preview=room-preview-owner-absent` |
| Full room preview | `?preview=room-preview-full` |
| Ended room preview | `?preview=room-preview-ended` |
| Owner empty cinema | `?preview=owner-empty` |
| Stream loading | `?preview=player-loading` |
| Player playing | `?preview=player-playing` |
| Player paused | `?preview=player-paused` |
| Player buffering | `?preview=player-buffering` |
| Player error | `?preview=player-error` |
| Non-owner player | `?preview=participant-playing` |
| Non-owner player, owner absent | `?preview=participant-owner-absent` |

Add `&theme=dark` for dark theme and `&largeText=1` for the large-text layout.

## Build

```powershell
npm.cmd run build
```
