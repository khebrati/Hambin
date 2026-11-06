import { lazy, Suspense } from 'react';
import { previewStates } from './previewStates';
import { LoadingIndicator } from './components/Material';
import { PreviewApp } from './PreviewApp';
const LiveApp = lazy(() => import('./LiveApp'));
export function App() {
  const preview = new URLSearchParams(location.search).get('preview');
  return preview && previewStates[preview] ? <PreviewApp /> : <Suspense fallback={<main className="form-page"><LoadingIndicator label="Opening Roomio" /></main>}><LiveApp /></Suspense>;
}
