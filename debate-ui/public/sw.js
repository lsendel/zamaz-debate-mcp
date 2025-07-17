// Service Worker for Zamaz Debate System PWA
// Provides offline functionality and caching

const CACHE_NAME = 'zamaz-debate-v1.0.0';
const STATIC_CACHE_NAME = 'zamaz-debate-static-v1.0.0';
const DYNAMIC_CACHE_NAME = 'zamaz-debate-dynamic-v1.0.0';
const API_CACHE_NAME = 'zamaz-debate-api-v1.0.0';

// Resources to cache immediately
const STATIC_ASSETS = [
  '/',
  '/index.html',
  '/static/js/bundle.js',
  '/static/css/main.css',
  '/manifest.json',
  '/icons/icon-192x192.png',
  '/icons/icon-512x512.png',
  '/offline.html' // Fallback page
];

// API endpoints to cache with strategies
const API_CACHE_PATTERNS = {
  // Cache debates list for offline viewing
  debates: /\/api\/v1\/debates$/,
  // Cache individual debate details
  debateDetails: /\/api\/v1\/debates\/[^/]+$/,
  // Cache organization data
  organizations: /\/api\/v1\/organizations/,
  // Cache user profiles
  profiles: /\/api\/v1\/users\//
};

// URLs that should never be cached
const NO_CACHE_PATTERNS = [
  /\/api\/v1\/debates\/.*\/ws$/, // WebSocket endpoints
  /\/api\/v1\/events/, // Server-sent events
  /\/api\/v1\/auth\//, // Authentication endpoints
  /\/api\/v1\/notifications\// // Push notifications
];

// Install event - cache static assets
self.addEventListener('install', event => {
  console.log('[SW] Installing service worker...');
  
  event.waitUntil(
    Promise.all([
      // Cache static assets
      caches.open(STATIC_CACHE_NAME).then(cache => {
        console.log('[SW] Caching static assets');
        return cache.addAll(STATIC_ASSETS.filter(url => {
          // Only cache assets that exist
          return fetch(url, { method: 'HEAD' })
            .then(response => response.ok)
            .catch(() => false);
        }));
      }),
      
      // Initialize other caches
      caches.open(DYNAMIC_CACHE_NAME),
      caches.open(API_CACHE_NAME)
    ]).then(() => {
      console.log('[SW] Installation complete');
      return self.skipWaiting(); // Activate immediately
    })
  );
});

// Activate event - clean up old caches
self.addEventListener('activate', event => {
  console.log('[SW] Activating service worker...');
  
  event.waitUntil(
    caches.keys().then(cacheNames => {
      return Promise.all(
        cacheNames.map(cacheName => {
          // Delete old caches
          if (![
            STATIC_CACHE_NAME, 
            DYNAMIC_CACHE_NAME, 
            API_CACHE_NAME
          ].includes(cacheName)) {
            console.log('[SW] Deleting old cache:', cacheName);
            return caches.delete(cacheName);
          }
        })
      );
    }).then(() => {
      console.log('[SW] Activation complete');
      return self.clients.claim(); // Take control immediately
    })
  );
});

// Fetch event - implement caching strategies
self.addEventListener('fetch', event => {
  const { request } = event;
  const url = new URL(request.url);
  
  // Skip non-GET requests and chrome-extension requests
  if (request.method !== 'GET' || url.protocol === 'chrome-extension:') {
    return;
  }
  
  // Skip requests that should never be cached
  if (NO_CACHE_PATTERNS.some(pattern => pattern.test(url.pathname))) {
    return;
  }
  
  event.respondWith(handleFetchEvent(request));
});

// Main fetch handler with different strategies
async function handleFetchEvent(request) {
  const url = new URL(request.url);
  
  try {
    // API requests - Cache First strategy with network fallback
    if (url.pathname.startsWith('/api/')) {
      return await handleApiRequest(request);
    }
    
    // Static assets - Cache First strategy
    if (isStaticAsset(url.pathname)) {
      return await handleStaticAsset(request);
    }
    
    // Navigation requests - Network First with cache fallback
    if (request.mode === 'navigate') {
      return await handleNavigation(request);
    }
    
    // Other requests - Network First
    return await handleNetworkFirst(request);
    
  } catch (error) {
    console.error('[SW] Fetch error:', error);
    return await handleOfflineFallback(request);
  }
}

// Handle API requests with intelligent caching
async function handleApiRequest(request) {
  const url = new URL(request.url);
  const cache = await caches.open(API_CACHE_NAME);
  
  // Check if this API endpoint should be cached
  const shouldCache = Object.values(API_CACHE_PATTERNS)
    .some(pattern => pattern.test(url.pathname));
  
  if (!shouldCache) {
    // Don't cache, just pass through
    return fetch(request);
  }
  
  try {
    // Try network first for fresh data
    const networkResponse = await fetch(request.clone());
    
    if (networkResponse.ok) {
      // Cache successful responses
      cache.put(request.clone(), networkResponse.clone());
      
      // Add timestamp for cache invalidation
      const timestampedResponse = addTimestamp(networkResponse.clone());
      return timestampedResponse;
    }
    
    throw new Error('Network response not ok');
    
  } catch (error) {
    console.log('[SW] Network failed for API, trying cache:', url.pathname);
    
    // Fallback to cache
    const cachedResponse = await cache.match(request);
    if (cachedResponse) {
      // Add cache indicator header
      const response = cachedResponse.clone();
      response.headers.set('X-Cache-Status', 'HIT');
      return response;
    }
    
    // No cache available
    throw error;
  }
}

// Handle static assets
async function handleStaticAsset(request) {
  const cache = await caches.open(STATIC_CACHE_NAME);
  
  // Cache first strategy
  const cachedResponse = await cache.match(request);
  if (cachedResponse) {
    return cachedResponse;
  }
  
  // Fetch and cache
  try {
    const networkResponse = await fetch(request);
    if (networkResponse.ok) {
      cache.put(request, networkResponse.clone());
    }
    return networkResponse;
  } catch (error) {
    console.error('[SW] Failed to fetch static asset:', request.url);
    throw error;
  }
}

// Handle navigation requests
async function handleNavigation(request) {
  try {
    // Try network first for fresh content
    return await fetch(request);
  } catch (error) {
    console.log('[SW] Network failed for navigation, trying cache');
    
    // Fallback to cached index.html
    const cache = await caches.open(STATIC_CACHE_NAME);
    const cachedIndex = await cache.match('/index.html');
    
    if (cachedIndex) {
      return cachedIndex;
    }
    
    // Last resort - offline page
    const offlinePage = await cache.match('/offline.html');
    if (offlinePage) {
      return offlinePage;
    }
    
    throw error;
  }
}

// Network first strategy
async function handleNetworkFirst(request) {
  const cache = await caches.open(DYNAMIC_CACHE_NAME);
  
  try {
    const networkResponse = await fetch(request);
    
    if (networkResponse.ok) {
      // Cache successful responses
      cache.put(request, networkResponse.clone());
    }
    
    return networkResponse;
  } catch (error) {
    // Fallback to cache
    const cachedResponse = await cache.match(request);
    if (cachedResponse) {
      return cachedResponse;
    }
    
    throw error;
  }
}

// Offline fallback handling
async function handleOfflineFallback(request) {
  const url = new URL(request.url);
  
  // For API requests, return offline indicator
  if (url.pathname.startsWith('/api/')) {
    return new Response(
      JSON.stringify({
        error: 'Offline',
        message: 'This request requires an internet connection',
        offline: true,
        timestamp: Date.now()
      }),
      {
        status: 503,
        statusText: 'Service Unavailable',
        headers: {
          'Content-Type': 'application/json',
          'X-Offline': 'true'
        }
      }
    );
  }
  
  // For navigation, return offline page
  if (request.mode === 'navigate') {
    const cache = await caches.open(STATIC_CACHE_NAME);
    const offlinePage = await cache.match('/offline.html');
    
    if (offlinePage) {
      return offlinePage;
    }
  }
  
  // Default offline response
  return new Response('Offline', {
    status: 503,
    statusText: 'Service Unavailable'
  });
}

// Utility functions
function isStaticAsset(pathname) {
  return (
    pathname.startsWith('/static/') ||
    pathname.endsWith('.js') ||
    pathname.endsWith('.css') ||
    pathname.endsWith('.png') ||
    pathname.endsWith('.jpg') ||
    pathname.endsWith('.jpeg') ||
    pathname.endsWith('.svg') ||
    pathname.endsWith('.ico') ||
    pathname.endsWith('.woff') ||
    pathname.endsWith('.woff2')
  );
}

function addTimestamp(response) {
  const headers = new Headers(response.headers);
  headers.set('X-Cache-Timestamp', Date.now().toString());
  
  return new Response(response.body, {
    status: response.status,
    statusText: response.statusText,
    headers: headers
  });
}

// Background sync for queuing failed requests
self.addEventListener('sync', event => {
  console.log('[SW] Background sync triggered:', event.tag);
  
  if (event.tag === 'background-sync-debates') {
    event.waitUntil(syncFailedRequests());
  }
});

// Sync failed requests when online
async function syncFailedRequests() {
  console.log('[SW] Syncing failed requests...');
  
  // Implementation would depend on your specific offline queue
  // This is a placeholder for queueing failed API requests
  try {
    // Get queued requests from IndexedDB
    // Retry them now that we're online
    // Remove from queue on success
    console.log('[SW] Sync completed successfully');
  } catch (error) {
    console.error('[SW] Sync failed:', error);
  }
}

// Handle push notifications
self.addEventListener('push', event => {
  console.log('[SW] Push notification received');
  
  const options = {
    body: 'New debate activity',
    icon: '/icons/icon-192x192.png',
    badge: '/icons/badge-72x72.png',
    vibrate: [100, 50, 100],
    data: {
      timestamp: Date.now()
    },
    actions: [
      {
        action: 'view',
        title: 'View Debate',
        icon: '/icons/view-action.png'
      },
      {
        action: 'dismiss',
        title: 'Dismiss',
        icon: '/icons/dismiss-action.png'
      }
    ]
  };
  
  if (event.data) {
    try {
      const payload = event.data.json();
      options.title = payload.title || 'Debate Update';
      options.body = payload.body || options.body;
      options.data = { ...options.data, ...payload.data };
    } catch (error) {
      console.error('[SW] Error parsing push payload:', error);
      options.title = 'Debate Update';
    }
  } else {
    options.title = 'Debate Update';
  }
  
  event.waitUntil(
    self.registration.showNotification(options.title, options)
  );
});

// Handle notification clicks
self.addEventListener('notificationclick', event => {
  console.log('[SW] Notification clicked:', event.action);
  
  event.notification.close();
  
  if (event.action === 'view') {
    // Open the debate
    const debateId = event.notification.data?.debateId;
    const url = debateId ? `/debates/${debateId}` : '/';
    
    event.waitUntil(
      clients.openWindow(url)
    );
  }
  
  // Dismiss action does nothing (notification already closed)
});

// Cache management - clean up old cache entries
self.addEventListener('message', event => {
  if (event.data && event.data.type === 'CACHE_CLEANUP') {
    event.waitUntil(cleanupCaches());
  }
});

async function cleanupCaches() {
  console.log('[SW] Cleaning up caches...');
  
  const cacheNames = [API_CACHE_NAME, DYNAMIC_CACHE_NAME];
  
  for (const cacheName of cacheNames) {
    const cache = await caches.open(cacheName);
    const requests = await cache.keys();
    
    for (const request of requests) {
      const response = await cache.match(request);
      const timestamp = response?.headers.get('X-Cache-Timestamp');
      
      if (timestamp) {
        const age = Date.now() - parseInt(timestamp);
        const maxAge = 24 * 60 * 60 * 1000; // 24 hours
        
        if (age > maxAge) {
          console.log('[SW] Deleting old cache entry:', request.url);
          await cache.delete(request);
        }
      }
    }
  }
  
  console.log('[SW] Cache cleanup completed');
}

console.log('[SW] Service worker script loaded');
