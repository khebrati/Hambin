import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const target = env.BACKEND_PROXY_TARGET || 'http://127.0.0.1:8081';
  return { plugins: [react()], server: { proxy: { '/v1': { target }, '/socket.io': { target, ws: true } } } };
});
