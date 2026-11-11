import { ArrowDownToLine, Smartphone } from 'lucide-react';
import './android-download-row.css';

export const androidDownloadUrl = 'https://watchparty.top/downloads/roomio-1.1.3-9.apk';

export function AndroidDownloadRow() {
  return (
    <aside className="android-download-row" aria-label="Android app">
      <Smartphone size={24} aria-hidden="true" />
      <div>
        <strong>Native Android app</strong>
        <p>Background voice party · v1.1.3</p>
      </div>
      <a className="md-icon-button md-icon-button--tonal" href={androidDownloadUrl} download="roomio-1.1.3-9.apk" aria-label="Download native Android app" title="Download native Android app" data-tooltip="Download for Android">
        <ArrowDownToLine size={22} aria-hidden="true" />
      </a>
    </aside>
  );
}
