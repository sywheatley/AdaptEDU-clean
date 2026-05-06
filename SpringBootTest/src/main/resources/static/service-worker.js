const CACHE_NAME = 'adaptedu-cache-v2';

// Add the core files of your web app here
const urlsToCache = [
    '/',
    '/script2.js',
    '/manifest.json'
    // Add your CSS file here, e.g., '/style.css'
];

// Install the service worker and cache files
self.addEventListener('install', event => {
    event.waitUntil(
        caches.open(CACHE_NAME)
            .then(cache => {
                return cache.addAll(urlsToCache);
            })
    );
    self.skipWaiting();
});

self.addEventListener('activate', event => {
    event.waitUntil(
        caches.keys()
            .then(names => Promise.all(
                names.filter(name => name !== CACHE_NAME).map(name => caches.delete(name))
            ))
            .then(() => self.clients.claim())
    );
});

// Serve cached files when offline
self.addEventListener('fetch', event => {
    event.respondWith(
        caches.match(event.request)
            .then(response => {
                // Return cached version or fetch from the network
                return response || fetch(event.request);
            })
    );
});
