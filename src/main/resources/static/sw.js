// sw.js - Service Worker Ultra-Acelerador Linda Mamá Spa
// Implementa estrategia Stale-While-Revalidate para carga instantánea de recursos multimedia, fuentes y scripts
const CACHE_NAME = 'spa-pwa-cache-v6';
const STATIC_ASSETS = [
  '/',
  '/catalogo',
  '/manifest.json',
  '/pwa-init.js',
  '/instant-loader.js',
  '/spa-fondo.jpg',
  '/logo-spa.png'
];

// 1. INSTALACIÓN
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => {
      return cache.addAll(STATIC_ASSETS).catch(err => {
        console.warn('Algunos recursos iniciales no pudieron pre-cachearse:', err);
      });
    })
  );
  self.skipWaiting();
});

// 2. ACTIVACIÓN (Limpieza de cachés anteriores)
self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(keys => {
      return Promise.all(
        keys.map(key => {
          if (key !== CACHE_NAME) {
            console.log('Borrando caché antigua:', key);
            return caches.delete(key);
          }
        })
      );
    })
  );
  self.clients.claim();
});

// 3. ESTRATEGIA DE RESPUESTA A PETICIONES (FETCH)
self.addEventListener('fetch', event => {
  const url = new URL(event.request.url);

  // No interceptar peticiones de la API ni métodos no GET para evitar problemas con transacciones y ventas
  if (url.pathname.startsWith('/api/') || event.request.method !== 'GET') {
    return;
  }

  // A. Para páginas de administración o POS, estrategia Network-First (siempre datos frescos si hay red)
  if (url.pathname === '/admin' || url.pathname === '/pos') {
    event.respondWith(
      fetch(event.request)
        .then(response => {
          if (response && response.status === 200) {
            const clone = response.clone();
            caches.open(CACHE_NAME).then(cache => cache.put(event.request, clone));
          }
          return response;
        })
        .catch(() => caches.match(event.request))
    );
    return;
  }

  // B. Para recursos estáticos (imágenes, CSS, JS, fuentes), estrategia Stale-While-Revalidate
  event.respondWith(
    caches.match(event.request).then(cachedResponse => {
      const fetchPromise = fetch(event.request).then(networkResponse => {
        if (networkResponse && networkResponse.status === 200) {
          const clone = networkResponse.clone();
          caches.open(CACHE_NAME).then(cache => cache.put(event.request, clone));
        }
        return networkResponse;
      }).catch(err => {
        // En caso de estar sin internet, responder con lo que haya
        return cachedResponse;
      });

      return cachedResponse || fetchPromise;
    })
  );
});
