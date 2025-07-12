/**
 * Generate a random string for unique test data
 * @param length - Length of the random string (default: 8)
 * @returns Random alphanumeric string
 */
export function randomString(length: number = 8): string {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
  let result = '';
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
}

/**
 * Wait for a specific amount of time
 * @param ms - Milliseconds to wait
 */
export function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}

/**
 * Format date for display
 * @param date - Date to format
 * @returns Formatted date string
 */
export function formatDate(date: Date): string {
  return new Intl.DateTimeFormat('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date);
}

/**
 * Generate test data for debates
 */
export function generateDebateData() {
  return {
    name: `Test Debate ${randomString()}`,
    topic: `Should AI ${randomString()} be regulated?`,
    participants: [
      { name: 'AI Assistant 1', position: 'Pro' },
      { name: 'AI Assistant 2', position: 'Con' }
    ],
    rules: [
      'Each participant has 5 minutes per turn',
      'No personal attacks',
      'Cite sources when making claims'
    ]
  };
}

/**
 * Generate test template content
 */
export function generateTemplateContent(type: 'simple' | 'complex' = 'simple') {
  if (type === 'simple') {
    return `Welcome to the debate on {{ topic }}.

Participants:
- {{ participant1 }}
- {{ participant2 }}

Let's begin!`;
  }

  return `# {{ title }}

## Topic: {{ topic }}

{% if introduction %}
{{ introduction }}
{% endif %}

## Participants:
{% for participant in participants %}
- **{{ participant.name }}** ({{ participant.position }})
  {% if participant.bio %}
  Bio: {{ participant.bio }}
  {% endif %}
{% endfor %}

## Rules:
{% for rule in rules %}
{{ loop.index }}. {{ rule }}
{% endfor %}

{% if moderator %}
**Moderator**: {{ moderator }}
{% endif %}

---
*Generated on {{ date }}*`;
}

/**
 * Retry a function with exponential backoff
 */
export async function retry<T>(
  fn: () => Promise<T>,
  options: {
    maxAttempts?: number;
    delay?: number;
    backoff?: number;
  } = {}
): Promise<T> {
  const { maxAttempts = 3, delay = 1000, backoff = 2 } = options;
  
  let lastError: Error;
  
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error as Error;
      
      if (attempt === maxAttempts) {
        throw lastError;
      }
      
      const waitTime = delay * Math.pow(backoff, attempt - 1);
      await sleep(waitTime);
    }
  }
  
  throw lastError!;
}

/**
 * Check if a service is healthy
 */
export async function checkServiceHealth(url: string): Promise<boolean> {
  try {
    const response = await fetch(url);
    return response.ok;
  } catch (error) {
    return false;
  }
}

/**
 * Wait for all services to be ready
 */
export async function waitForServices(services: { name: string; url: string }[], timeout: number = 60000) {
  const startTime = Date.now();
  
  for (const service of services) {
    let isReady = false;
    
    while (!isReady && Date.now() - startTime < timeout) {
      isReady = await checkServiceHealth(service.url);
      
      if (!isReady) {
        console.log(`Waiting for ${service.name}...`);
        await sleep(2000);
      }
    }
    
    if (!isReady) {
      throw new Error(`Service ${service.name} did not start within ${timeout}ms`);
    }
    
    console.log(`✅ ${service.name} is ready`);
  }
}