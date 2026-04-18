const CACHE_NAME = 'spa-pwa-cache-v1';
const urlsToCache = [
  '/',
  '/manifest.json',
  '/pwa-init.js',
  'https://cdn-icons-png.flaticon.com/512/3135/3135715.png'
  // Al ser una aplicacion dinamica respaldada por base de datos, 
  // evitamos cachear el HTML transaccional o dependencias pesadas por defecto 
  // para no mostrar datos de inventario o reservas desactualizados.
];

// Instalación
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => {
        return cache.addAll(urlsToCache);
      })
  );
  // Fuerza al SW activo a convertirse en el SW dominante de inmediato
  self.skipWaiting();
});

// Activación
self.addEventListener('activate', event => {
  const cacheWhitelist = [CACHE_NAME];
  event.waitUntil(
    caches.keys().then(cacheNames => {
      return Promise.all(
        cacheNames.map(cacheName => {
          if (cacheWhitelist.indexOf(cacheName) === -1) {
            return caches.delete(cacheName); // Borra caches antiguas
          }
        })
      );
    })
  );
  self.clients.claim();
});

// Peticiones: Red primero, Caché como respaldo (Network-First Strategy)
// Esto asegura que la App Nativa muestre siempre datos reales si hay internet, 
// y si se cae la red temporalmente puede usar el caché para no mostrar el temido dinosaurio de Google.
self.addEventListener('fetch', event => {
  if (event.request.method !== 'GET') {
    return; // No cacheamos llamadas POST / PUT / DELETE
  }
  
  event.respondWith(
    fetch(event.request).catch(() => {
      return caches.match(event.request);
    })
  );
});
