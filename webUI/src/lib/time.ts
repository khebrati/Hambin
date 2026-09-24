export const TOTAL_SECONDS = 6138;

export function formatTime(value: number) {
  const safeValue = Math.max(0, Math.min(value, TOTAL_SECONDS));
  const hours = Math.floor(safeValue / 3600);
  const minutes = Math.floor((safeValue % 3600) / 60);
  const seconds = Math.floor(safeValue % 60);
  return hours > 0
    ? `${hours}:${minutes.toString().padStart(2, '0')}:${seconds
        .toString()
        .padStart(2, '0')}`
    : `${minutes}:${seconds.toString().padStart(2, '0')}`;
}

export function formatDelta(seconds: number) {
  const rounded = Math.round(seconds);
  if (rounded === 0) return 'with you';
  return rounded > 0
    ? `${formatTime(rounded)} ahead`
    : `${formatTime(-rounded)} behind`;
}
