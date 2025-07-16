const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function(app) {
  // Organization service proxy
  app.use(
    '/api/organization',
    createProxyMiddleware({
      target: 'http://localhost:5005',
      changeOrigin: true,
      pathRewrite: {
        '^/api/organization': ''
      },
      onError: (err, req, res) => {
        console.error('Organization proxy error:', err);
        res.status(500).json({ error: 'Organization service unavailable' });
      }
    })
  );

  // Debate controller service proxy
  app.use(
    '/api/debate',
    createProxyMiddleware({
      target: 'http://localhost:5013',
      changeOrigin: true,
      pathRewrite: {
        '^/api/debate': ''
      },
      onError: (err, req, res) => {
        console.error('Debate proxy error:', err);
        res.status(500).json({ error: 'Debate service unavailable' });
      }
    })
  );

  // LLM service proxy
  app.use(
    '/api/llm',
    createProxyMiddleware({
      target: 'http://localhost:5002',
      changeOrigin: true,
      pathRewrite: {
        '^/api/llm': ''
      },
      onError: (err, req, res) => {
        console.error('LLM proxy error:', err);
        res.status(500).json({ error: 'LLM service unavailable' });
      }
    })
  );

  // WebSocket proxy for live debate updates
  app.use(
    '/ws',
    createProxyMiddleware({
      target: 'ws://localhost:5013',
      ws: true,
      changeOrigin: true,
      onError: (err, req, res) => {
        console.error('WebSocket proxy error:', err);
      }
    })
  );
};