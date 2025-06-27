// vite.config.js
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  define: {
    global: {},
  },
  server: {
     port: 5173, // 주석 해제하여 명시하거나, 그대로 두어도 됨 (기본값)
    proxy: {
     
    },
  },
});