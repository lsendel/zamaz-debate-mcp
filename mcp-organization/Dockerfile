FROM python:3.11-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    gcc \
    g++ \
    libpq-dev \
    git \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements and install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy source code
COPY src/ ./src/
COPY config/ ./config/

# Create data directory for backups and exports
RUN mkdir -p /app/data

# Set environment variables
ENV PYTHONPATH=/app
ENV MCP_SERVER_NAME=mcp-organization
ENV MCP_SERVER_VERSION=1.0.0

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:5005/health || exit 1

# Expose port
EXPOSE 5005

# Run the MCP server
CMD ["python", "-m", "src.mcp_server"]