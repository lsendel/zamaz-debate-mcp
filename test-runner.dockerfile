FROM mcr.microsoft.com/playwright:v1.48.2-noble

# Install additional dependencies
RUN apt-get update && apt-get install -y \
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
RUN wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google.list \
    && apt-get update \
    && apt-get install -y google-chrome-stable \
    && rm -rf /var/lib/apt/lists/*

# Set up working directory
WORKDIR /tests

# Install Node.js 20
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y nodejs \
    && npm install -g npm@latest

# Copy all test directories
COPY e2e-tests/package*.json e2e-tests/
COPY playwright-tests/package*.json playwright-tests/

# Install dependencies for both test suites
WORKDIR /tests/e2e-tests
RUN npm ci

WORKDIR /tests/playwright-tests
RUN npm ci
RUN npx playwright install --with-deps

# Copy test files
WORKDIR /tests
COPY e2e-tests/ e2e-tests/
COPY playwright-tests/ playwright-tests/

# Copy test runner script
COPY run-all-tests.sh /tests/
RUN chmod +x /tests/run-all-tests.sh

# Create directories for test artifacts
RUN mkdir -p /test_probe/{screenshots,videos,logs,reports}

# Set environment variables
ENV HEADLESS=true
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true
ENV PUPPETEER_EXECUTABLE_PATH=/usr/bin/google-chrome-stable

# Default command
CMD ["/tests/run-all-tests.sh"]