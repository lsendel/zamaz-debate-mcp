FROM mcr.microsoft.com/playwright:v1.48.2-noble

# Install additional dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    gnupg \
    ca-certificates \
    fonts-liberation \
    libasound2 \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libatspi2.0-0 \
    libcups2 \
    libdbus-1-3 \
    libdrm2 \
    libgbm1 \
    libgtk-3-0 \
    libnspr4 \
    libnss3 \
    libxcomposite1 \
    libxdamage1 \
    libxrandr2 \
    xdg-utils \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# Install Chrome for Puppeteer
# Security: Using modern gpg method instead of deprecated apt-key
RUN wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | gpg --dearmor -o /usr/share/keyrings/google-chrome.gpg \
    && echo "deb [arch=amd64 signed-by=/usr/share/keyrings/google-chrome.gpg] https://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends google-chrome-stable \
    && rm -rf /var/lib/apt/lists/*

# Set up working directory
WORKDIR /tests

# Install Node.js 20
# Security: Using HTTPS for NodeSource setup script with verification
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && npm install -g npm@latest

# Copy all test directories
COPY e2e-tests/package*.json e2e-tests/
COPY playwright-tests/package*.json playwright-tests/

# Install dependencies for both test suites
# Security: Using npm ci with --ignore-scripts for security
WORKDIR /tests/e2e-tests
RUN npm ci --ignore-scripts

WORKDIR /tests/playwright-tests
RUN npm ci --ignore-scripts
RUN npx playwright install --with-deps

# Copy test files
WORKDIR /tests
COPY e2e-tests/ e2e-tests/
COPY playwright-tests/ playwright-tests/

# Copy test runner script
COPY run-all-tests.sh /tests/
RUN chmod +x /tests/run-all-tests.sh

# Create non-root user for security
RUN groupadd -r testuser && useradd -r -g testuser testuser

# Create directories for test artifacts
RUN mkdir -p /test_probe/{screenshots,videos,logs,reports} \
    && chown -R testuser:testuser /test_probe \
    && chown -R testuser:testuser /tests

# Set environment variables
ENV HEADLESS=true
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true
ENV PUPPETEER_EXECUTABLE_PATH=/usr/bin/google-chrome-stable

# Switch to non-root user
USER testuser

# Default command
USER node

USER node

USER node

CMD ["/tests/run-all-tests.sh"]