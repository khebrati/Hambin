import { t, useLanguage } from './i18n';
import { useLayoutEffect } from 'react';
import { lazy, Suspense } from 'react';
import { previewStates } from './previewStates';
import { LoadingIndicator } from './components/Material';
import { PreviewApp } from './PreviewApp';
const LiveApp = lazy(() => import('./LiveApp'));
const AndroidDownloadDesigns = lazy(() => import('./designs/AndroidDownloadDesigns'));
const BugReportDesigns = lazy(() => import('./designs/BugReportDesigns'));
export function App() {
  const language = useLanguage();
  useLayoutEffect(() => {
    document.documentElement.lang = language;
    document.documentElement.dir = language === 'fa' || new URLSearchParams(location.search).get('dir') === 'rtl' ? 'rtl' : 'ltr';
  }, [language]);
  const preview = new URLSearchParams(location.search).get('preview');
  if (preview === 'bug-reports') return <Suspense fallback={<LoadingIndicator label={t("Opening designs")} />}><BugReportDesigns /></Suspense>;
  if (preview === 'android-downloads') return <Suspense fallback={<LoadingIndicator label={t("Opening designs")} />}><AndroidDownloadDesigns /></Suspense>;
  return preview && previewStates[preview] ? <PreviewApp /> : <Suspense fallback={<main className="form-page"><LoadingIndicator label={t("Opening Roomio")} /></main>}><LiveApp /></Suspense>;
}
