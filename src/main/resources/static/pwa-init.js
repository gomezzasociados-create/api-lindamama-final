// pwa-init.js - Manejo del Service Worker y Acelerador PWA Linda Mamá Spa
if ('caches' in window) {
  caches.keys().then(keys => {
    keys.forEach(k => {
      if (k !== 'spa-pwa-cache-v15') {
        caches.delete(k);
      } else {
        caches.open(k).then(c => {
          c.delete('/admin');
          c.delete('/pos');
        });
      }
    });
  }).catch(() => {});
}

if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js')
      .then(registration => {
        registration.update();
        console.log('ServiceWorker PWA registrado con éxito. Scope:', registration.scope);
      }, err => {
        console.log('Fallo al registrar el ServiceWorker PWA:', err);
      });
  });
}

// Inyección automática del Acelerador Instantáneo en todas las vistas
if (!window.instantLoaderLoaded && !document.querySelector('script[src*="instant-loader.js"]')) {
  window.instantLoaderLoaded = true;
  const s = document.createElement('script');
  s.src = '/instant-loader.js';
  s.async = true;
  document.head.appendChild(s);
}

// Interceptar y controlar el evento de instalación nativa para PWA
let deferredPrompt;
window.addEventListener('beforeinstallprompt', (e) => {
  e.preventDefault();
  deferredPrompt = e;
});
