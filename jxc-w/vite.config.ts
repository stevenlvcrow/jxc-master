import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import path from 'node:path';

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  build: {
    chunkSizeWarningLimit: 900,
    rollupOptions: {
      output: {
        manualChunks(id) {
          const moduleId = id.replace(/\\/g, '/');
          if (!moduleId.includes('node_modules')) {
            return undefined;
          }
          if (
            moduleId.includes('/vue/') ||
            moduleId.includes('/@vue/') ||
            moduleId.includes('/vue-router/') ||
            moduleId.includes('/pinia/')
          ) {
            return 'vue-core';
          }
          if (moduleId.includes('/axios/')) {
            return 'http-client';
          }
          return undefined;
        },
      },
    },
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
});
