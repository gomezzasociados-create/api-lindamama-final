// pwa-init.js - Manejo del Service Worker y PWA Install Prompt
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js')
      .then(registration => {
        console.log('ServiceWorker PWA registrado con éxito. Scope:', registration.scope);
      }, err => {
        console.log('Fallo al registrar el ServiceWorker PWA:', err);
      });
  });
}

// Interceptar y controlar el evento de instalación nativa para poder ofrecer un botón custom si se desea a futuro
let deferredPrompt;
window.addEventListener('beforeinstallprompt', (e) => {
  // Previene que Chrome 67 y versiones menores muestren el prompt automáticamente
  e.preventDefault();
  // Guarda el evento para que pueda ser disparado más tarde
  deferredPrompt = e;
  // Opcional: Aquí podrías mostrar un botón flotante "Instalar App del Spa" en la UI.
});
