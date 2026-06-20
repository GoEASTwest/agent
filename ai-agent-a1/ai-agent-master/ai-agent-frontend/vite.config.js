import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  envDir: '..',
  server: {
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8123',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/api')
      }
    }
  }
});
