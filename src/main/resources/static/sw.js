// sw.js - Service Worker Ultra-Acelerador Linda Mamá Spa
// Implementa estrategia Stale-While-Revalidate para carga instantánea de recursos multimedia, fuentes y scripts
const CACHE_NAME = 'spa-pwa-cache-v5';
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

// 2. ACTIVACIÓN Y LIMPIEZA DE CACHÉ ANTIGUA
self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(keys => {
      return Promise.all(
        keys.map(key => {
          if (key !== CACHE_NAME) {
            return caches.delete(key);
          }
        })
      );
    })
  );
  self.clients.claim();
});

// 3. ESTRATEGIA INTELIGENTE DE PETICIONES
self.addEventListener('fetch', event => {
  const req = event.request;
  
  // No cachear peticiones que no sean GET (POST, PUT, DELETE son transaccionales)
  if (req.method !== 'GET') {
    return;
  }

  const url = new URL(req.url);

  // A) Recursos estáticos, imágenes (/uploads/...), fuentes y librerías CDN:
  // Estrategia: STALE-WHILE-REVALIDATE (Devuelve caché de inmediato y actualiza en segundo plano)
  const isStaticResource = 
    url.pathname.startsWith('/uploads/') ||
    url.pathname.endsWith('.png') ||
    url.pathname.endsWith('.jpg') ||
    url.pathname.endsWith('.jpeg') ||
    url.pathname.endsWith('.webp') ||
    url.pathname.endsWith('.css') ||
    url.pathname.endsWith('.js') ||
    url.pathname.endsWith('.woff2') ||
    url.hostname.includes('fonts.gstatic.com') ||
    url.hostname.includes('cdn.jsdelivr.net');

  if (isStaticResource) {
    event.respondWith(
      caches.open(CACHE_NAME).then(async cache => {
        const cachedResponse = await cache.match(req);
        
        // Petición en segundo plano para refrescar caché (revalidate)
        const fetchPromise = fetch(req).then(networkResponse => {
          if (networkResponse && networkResponse.status === 200) {
            cache.put(req, networkResponse.clone());
          }
          return networkResponse;
        }).catch(() => null);

        // Si ya está en caché, responder de inmediato (0ms)
        return cachedResponse || fetchPromise;
      })
    );
    return;
  }

  // B) Páginas HTML de la aplicación:
  // Estrategia: NETWORK-FIRST CON FALLBACK A CACHÉ
  event.respondWith(
    fetch(req)
      .then(networkResponse => {
        if (networkResponse && networkResponse.status === 200) {
          const responseToCache = networkResponse.clone();
          caches.open(CACHE_NAME).then(cache => {
            cache.put(req, responseToCache);
          });
        }
        return networkResponse;
      })
      .catch(async () => {
        const cached = await caches.match(req);
        if (cached) return cached;
        // Si no hay red ni caché de esa url específica, mostrar catálogo si está en caché
        return caches.match('/catalogo');
      })
  );
});
