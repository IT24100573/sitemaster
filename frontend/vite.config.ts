import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The React app runs on 5173 and forwards every /api call to the Spring Boot API,
// so the browser sees one origin and the session + XSRF cookies just work.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': {
        target: process.env.VITE_API_TARGET ?? 'http://localhost:8080',
        changeOrigin: false,
      },
    },
  },
});
