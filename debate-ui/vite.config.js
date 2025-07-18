import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// Custom middleware to handle URI malformed errors
const uriFixMiddleware = () => {
  return {
    name: 'uri-fix-middleware',
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        // Log all requests to debug
        console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
        
        // Check for problematic URLs
        if (req.url && req.url.includes('%')) {
          try {
            // Try to decode the URL
            const decoded = decodeURIComponent(req.url);
            console.log(`Decoded URL: ${decoded}`);
          } catch (e) {
            console.error(`URI Malformed Error for URL: ${req.url}`);
            console.error(`Raw URL bytes: ${Buffer.from(req.url).toString('hex')}`);
            
            // Try to fix common encoding issues
            try {
              // Replace problematic sequences
              req.url = req.url
                .replace(/%25/g, '%')  // Double-encoded percent
                .replace(/%20/g, ' ')  // Space
                .replace(/%2F/g, '/')  // Forward slash
                .replace(/%3A/g, ':')  // Colon
                .replace(/%3F/g, '?')  // Question mark
                .replace(/%3D/g, '=')  // Equals
                .replace(/%26/g, '&'); // Ampersand
              
              console.log(`Fixed URL: ${req.url}`);
            } catch (fixError) {
              console.error('Failed to fix URL:', fixError);
              // Return 400 Bad Request instead of crashing
              res.statusCode = 400;
              res.end('Bad Request: Malformed URI');
              return;
            }
          }
        }
        
        next();
      });
    }
  };
};

export default defineConfig(({ mode }) => {
  // Load env file based on mode
  const env = loadEnv(mode, process.cwd(), '')
  
  return {
  plugins: [
    react(),
    uriFixMiddleware()
  ],
  server: {
    port: parseInt(env.VITE_PORT || '3001'),
    host: true,
    strictPort: false,
    // Add middleware to handle errors
    hmr: {
      overlay: true
    },
    proxy: {
      '/api/organization': {
        target: env.VITE_ORGANIZATION_API_URL || 'http://localhost:5005',
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path.replace(/^\/api\/organization/, ''),
        configure: (proxy, options) => {
          proxy.on('error', (err, req, res) => {
            console.log('Organization API proxy error:', err.message);
          });
        }
      },
      '/api/llm': {
        target: env.VITE_LLM_API_URL || 'http://localhost:5002',
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path.replace(/^\/api\/llm/, ''),
        configure: (proxy, options) => {
          proxy.on('error', (err, req, res) => {
            console.log('LLM API proxy error:', err.message);
          });
        }
      },
      '/api/debate': {
        target: env.VITE_DEBATE_API_URL || 'http://localhost:5013',
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path.replace(/^\/api\/debate/, ''),
        configure: (proxy, options) => {
          proxy.on('error', (err, req, res) => {
            console.log('Debate API proxy error:', err.message);
          });
        }
      },
      '/api/rag': {
        target: env.VITE_RAG_API_URL || 'http://localhost:5004',
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path.replace(/^\/api\/rag/, ''),
        configure: (proxy, options) => {
          proxy.on('error', (err, req, res) => {
            console.log('RAG API proxy error:', err.message);
          });
        }
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
    // Ensure clean URLs
    rollupOptions: {
      output: {
        sanitizeFileName: (name) => {
          // Remove any problematic characters from filenames
          return name.replace(/[^\w.-]/g, '_');
        }
      }
    }
  },
  define: {
    'process.env.NODE_ENV': '"development"'
  }
  }
})