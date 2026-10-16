import { lazy, Suspense } from 'react';
import { previewStates } from './previewStates';
import { PreviewApp } from './PreviewApp';
const LiveApp = lazy(() => import('./LiveApp'));
export function App() {
  const preview = new URLSearchParams(location.search).get('preview');
  return preview && previewStates[preview] ? <PreviewApp /> : <Suspense fallback={<main role="status">Opening Roomio…</main>}><LiveApp /></Suspense>;
}
