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
      // 웹소켓 엔드포인트에 대한 프록시 설정
      '/ws': { // 백엔드의 웹소켓 엔드포인트 경로 (예: http://localhost:8080/ws)
        target: 'http://localhost:8080', // 백엔드 서버 주소
        ws: true, // WebSocket 프록시 활성화!
        changeOrigin: true, // 대상 호스트의 원본을 변경
        secure: false, // HTTPS 백엔드인 경우 SSL 인증서 유효성 검사를 무시 (개발 환경에서 유용)
        // rewrite: (path) => path.replace(/^\/ws/, ''), // 필요하다면 경로 재작성 (대부분의 경우 '/ws'는 그대로 사용)
      },
      // 일반 REST API 엔드포인트에 대한 프록시 설정 (필요하다면 추가)
      '/api': { // 백엔드의 API 경로 (예: http://localhost:8080/api)
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
    },
  },
  define: {
    global: {},
  },
});
