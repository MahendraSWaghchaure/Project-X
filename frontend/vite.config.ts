import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react-swc';
import tailwindcss from '@tailwindcss/vite';
import path from 'node:path';

export default defineConfig({
  plugins: [react(), tailwindcss()],
  // sockjs-client (STOMP transport, see src/lib/wsClient.ts) references Node's `global` at module
  // load time; browsers don't have it. Without this the whole module graph throws
  // "ReferenceError: global is not defined" before React ever mounts - a blank white page with no
  // React error overlay, since it happens before React's own error handling exists.
  define: {
    global: 'globalThis',
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      // Lets the dev server talk to the backend without CORS ceremony; prod builds hit
      // VITE_API_BASE_URL directly (see src/lib/apiClient.ts).
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/api/, ''),
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
      },
    },
  },
});
