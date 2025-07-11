/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  async rewrites() {
    return [
      {
        source: '/api/mcp/:path*',
        destination: 'http://localhost:5003/:path*', // Debate service
      },
      {
        source: '/api/llm/:path*',
        destination: 'http://localhost:5002/:path*', // LLM service
      },
      {
        source: '/api/context/:path*',
        destination: 'http://localhost:5001/:path*', // Context service
      },
      {
        source: '/api/rag/:path*',
        destination: 'http://localhost:5004/:path*', // RAG service
      },
    ]
  },
}

module.exports = nextConfig