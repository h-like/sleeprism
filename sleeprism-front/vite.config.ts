import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // '/api/sounds' 경로로 시작하는 모든 요청을 백엔드 서버로 프록시합니다.
      '/api/sounds': {
        target: 'http://localhost:8080', // Context Path 제거
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/sounds/, '/api/sounds'), // 경로는 그대로 유지
        configure: (proxy, options) => {
          proxy.on('proxyReq', (proxyReq, req, res) => {
            console.log(`[Vite Proxy] Proxying request: ${req.method} ${req.url} to ${options.target}${proxyReq.path}`);
          });
          proxy.on('proxyRes', (proxyRes, req, res) => {
            console.log(`[Vite Proxy] Received response for ${req.url}: ${proxyRes.statusCode}`);
          });
        },
      },
      // Freesound 검색 API도 백엔드로 프록시되도록 추가합니다.
      '/api/freesound-search': {
        target: 'http://localhost:8080', // Context Path 제거
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/freesound-search/, '/api/freesound-search'),
      },
    },
  },
  define: {
    global: {},
  },
});
