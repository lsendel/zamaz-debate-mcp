import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 3002,
    open: true,
  },
  build: {
    outDir: 'build',
    sourcemap: true,
    rollupOptions: {
      output: {
        manualChunks: {
          // Vendor chunks for better caching
          'vendor-react': ['react', 'react-dom'],
          'vendor-ui': ['@zamaz/ui', 'lucide-react'],
          'vendor-charts': ['recharts'],
          'vendor-maps': ['maplibre-gl'],
          'vendor-flow': ['reactflow'],
          'vendor-motion': ['framer-motion'],
          'vendor-query': ['@tanstack/react-query'],
          'vendor-utils': ['zustand', 'clsx', 'class-variance-authority'],
        },
      },
    },
    // Increase chunk size warning limit for large enterprise applications
    chunkSizeWarningLimit: 1000,
    // Enable compression
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true,
      },
    },
  },
});