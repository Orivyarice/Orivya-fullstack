/**
 * service-worker.js — Orivya Rice PWA Service Worker
 *
 * Strategy:
 *   - STATIC ASSETS (HTML, CSS, JS, images): Cache-first
 *     → Serve from cache instantly, update in background
 *   - API CALLS (/api/**): Network-first
 *     → Try network, fall back to cached response if offline
 *   - Offline page shown when network fails and no cache exists
 *
 * Place this file at: frontend/service-worker.js
 */

const CACHE_NAME     = 'orivya-v1';
const API_CACHE_NAME = 'orivya-api-v1';

// ── STATIC FILES TO PRE-CACHE ON INSTALL ──────────────────────
const STATIC_ASSETS = [
  '/',
  '/index.html',
  '/login.html',
  '/register.html',
  '/orders.html',
  '/admin.html',
  '/checkout.html',
  '/offline.html',
  '/css/style.css',
  '/css/admin.css',
  '/css/auth.css',
  '/css/features.css',
  '/js/api.js',
  '/js/main.js',
  '/manifest.json',
  '/icons/icon-192.png',
  '/icons/icon-512.png',
];

// ══════════════════════════════════════════════════════════════
// INSTALL — pre-cache all static assets
// ══════════════════════════════════════════════════════════════
self.addEventListener('install', (event) => {
  console.log('[SW] Installing Orivya service worker...');

  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      console.log('[SW] Pre-caching static assets');
      // addAll fails if any file is missing — use individual add() to be safe
      return Promise.allSettled(
        STATIC_ASSETS.map((url) =>
          cache.add(url).catch((err) =>
            console.warn('[SW] Could not cache:', url, err.message)
          )
        )
      );
    }).then(() => {
      console.log('[SW] Install complete');
      // Activate immediately without waiting for old SW to finish
      return self.skipWaiting();
    })
  );
});

// ══════════════════════════════════════════════════════════════
// ACTIVATE — clean up old caches
// ══════════════════════════════════════════════════════════════
self.addEventListener('activate', (event) => {
  console.log('[SW] Activating...');

  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames
          .filter((name) => name !== CACHE_NAME && name !== API_CACHE_NAME)
          .map((name) => {
            console.log('[SW] Deleting old cache:', name);
            return caches.delete(name);
          })
      );
    }).then(() => {
      console.log('[SW] Now controlling all pages');
      return self.clients.claim();
    })
  );
});

// ══════════════════════════════════════════════════════════════
// FETCH — intercept all requests
// ══════════════════════════════════════════════════════════════
self.addEventListener('fetch', (event) => {
  const { request } = event;
  const url = new URL(request.url);

  // Skip non-GET requests (POST, PUT, DELETE) — don't cache these
  if (request.method !== 'GET') return;

  // Skip browser extensions and chrome-extension://
  if (!url.protocol.startsWith('http')) return;

  // ── API CALLS: Network-first ──────────────────────────────
  // Try network → cache response → on failure serve cached
  if (url.pathname.startsWith('/api/')) {
    event.respondWith(networkFirstStrategy(request));
    return;
  }

  // ── STATIC ASSETS: Cache-first ────────────────────────────
  // Serve from cache → if not cached, fetch and cache it
  event.respondWith(cacheFirstStrategy(request));
});

// ══════════════════════════════════════════════════════════════
// STRATEGIES
// ══════════════════════════════════════════════════════════════

/**
 * Cache-first: serve from cache, fall back to network.
 * Used for: HTML pages, CSS, JS, images
 */
async function cacheFirstStrategy(request) {
  const cached = await caches.match(request);
  if (cached) {
    // Serve from cache and refresh in background (stale-while-revalidate)
    refreshCache(request);
    return cached;
  }

  // Not in cache — fetch from network and cache it
  try {
    const networkResponse = await fetch(request);
    if (networkResponse.ok) {
      const cache = await caches.open(CACHE_NAME);
      cache.put(request, networkResponse.clone());
    }
    return networkResponse;
  } catch (err) {
    // Offline + not in cache → show offline page
    console.warn('[SW] Offline, no cache for:', request.url);
    const offlinePage = await caches.match('/offline.html');
    return offlinePage || new Response(
      '<h1>You are offline</h1><p>Please check your internet connection.</p>',
      { headers: { 'Content-Type': 'text/html' } }
    );
  }
}

/**
 * Network-first: try network, fall back to cache.
 * Used for: API calls (/api/*)
 */
async function networkFirstStrategy(request) {
  try {
    const networkResponse = await fetch(request);
    if (networkResponse.ok) {
      const cache = await caches.open(API_CACHE_NAME);
      cache.put(request, networkResponse.clone());
    }
    return networkResponse;
  } catch (err) {
    // Network failed — try cached API response
    const cached = await caches.match(request);
    if (cached) return cached;

    // Return offline JSON response for API calls
    return new Response(
      JSON.stringify({
        success: false,
        message: 'You are offline. Please check your internet connection.'
      }),
      {
        status: 503,
        headers: { 'Content-Type': 'application/json' }
      }
    );
  }
}

/**
 * Background cache refresh — keeps cached files up to date
 * without blocking the response to the user.
 */
function refreshCache(request) {
  fetch(request).then((response) => {
    if (response && response.ok) {
      caches.open(CACHE_NAME).then((cache) => {
        cache.put(request, response);
      });
    }
  }).catch(() => {
    // Background refresh failed — silently ignore
  });
}

// ══════════════════════════════════════════════════════════════
// PUSH NOTIFICATIONS (optional — for future order updates)
// ══════════════════════════════════════════════════════════════
self.addEventListener('push', (event) => {
  const data = event.data ? event.data.json() : {};
  const options = {
    body:    data.body    || 'Your order has been updated!',
    icon:    data.icon    || '/icons/icon-192.png',
    badge:   data.badge   || '/icons/icon-96.png',
    data:    { url: data.url || '/orders.html' },
    actions: [
      { action: 'view',    title: 'View Order' },
      { action: 'dismiss', title: 'Dismiss' }
    ]
  };

  event.waitUntil(
    self.registration.showNotification(
      data.title || '🌾 Orivya Rice', options
    )
  );
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  if (event.action !== 'dismiss') {
    event.waitUntil(
      clients.openWindow(event.notification.data.url || '/orders.html')
    );
  }
});