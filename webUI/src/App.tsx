import { t, useLanguage } from './i18n';
import { useLayoutEffect } from 'react';
import { lazy, Suspense } from 'react';
import { previewStates } from './previewStates';
import { LoadingIndicator } from './components/Material';
import { PreviewApp } from './PreviewApp';
const LocalFileDesigns = lazy(() => import('./designs/LocalFileDesigns'));
const LiveApp = lazy(() => import('./LiveApp'));
const AndroidDownloadDesigns = lazy(() => import('./designs/AndroidDownloadDesigns'));
const BugReportDesigns = lazy(() => import('./designs/BugReportDesigns'));
const ChatDesigns = lazy(() => import('./designs/ChatDesigns'));
const SyncWaitingDesigns = lazy(() => import('./designs/SyncWaitingDesigns'));
const SyncDesigns = lazy(() => import('./designs/SyncDesigns'));
export function App() {
  const language = useLanguage();
  useLayoutEffect(() => {
    document.title = t("Watch Party");
    document.querySelector('meta[name="description"]')?.setAttribute('content', t("Watch movies together with Watch Party."));
    document.documentElement.lang = language;
    document.documentElement.dir = language === 'fa' || new URLSearchParams(location.search).get('dir') === 'rtl' ? 'rtl' : 'ltr';
  }, [language]);
  const preview = new URLSearchParams(location.search).get('preview');
  if (preview === 'local-file') return <Suspense fallback={<LoadingIndicator label={t('Loading')} />}><LocalFileDesigns /></Suspense>;
  if (preview === 'sync-waiting') return <Suspense fallback={<div className="preview-loading"><LoadingIndicator expressive label={t("Opening designs")} /></div>}><SyncWaitingDesigns /></Suspense>;
  if (preview === 'sync') return <Suspense fallback={<div className="preview-loading"><LoadingIndicator expressive label={t("Opening designs")} /></div>}><SyncDesigns /></Suspense>;
  if (preview === 'chat') return <Suspense fallback={<div className="preview-loading"><LoadingIndicator expressive label={t("Opening designs")} /></div>}><ChatDesigns /></Suspense>;
  if (preview === 'bug-reports') return <Suspense fallback={<div className="preview-loading"><LoadingIndicator expressive label={t("Opening designs")} /></div>}><BugReportDesigns /></Suspense>;
  if (preview === 'android-downloads') return <Suspense fallback={<div className="preview-loading"><LoadingIndicator expressive label={t("Opening designs")} /></div>}><AndroidDownloadDesigns /></Suspense>;
  return preview && previewStates[preview] ? <PreviewApp /> : <Suspense fallback={<main className="form-page"><LoadingIndicator label={t("Opening Watch Party")} /></main>}><LiveApp /></Suspense>;
}
