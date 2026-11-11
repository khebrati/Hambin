import { lazy, Suspense } from 'react';
import { previewStates } from './previewStates';
import { LoadingIndicator } from './components/Material';
import { PreviewApp } from './PreviewApp';
const LiveApp = lazy(() => import('./LiveApp'));
const AndroidDownloadDesigns = lazy(() => import('./designs/AndroidDownloadDesigns'));
const BugReportDesigns = lazy(() => import('./designs/BugReportDesigns'));
export function App() {
  const preview = new URLSearchParams(location.search).get('preview');
  if (preview === 'bug-reports') return <Suspense fallback={<LoadingIndicator label="Opening designs" />}><BugReportDesigns /></Suspense>;
  if (preview === 'android-downloads') return <Suspense fallback={<LoadingIndicator label="Opening designs" />}><AndroidDownloadDesigns /></Suspense>;
  return preview && previewStates[preview] ? <PreviewApp /> : <Suspense fallback={<main className="form-page"><LoadingIndicator label="Opening Roomio" /></main>}><LiveApp /></Suspense>;
}
