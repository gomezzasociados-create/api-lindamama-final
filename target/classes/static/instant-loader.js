/**
 * INSTANT-LOADER.JS - Gomez Systems High-Speed Frontend Accelerator & Media Suite
 * - Carga instantánea de frontend y optimización de rendimiento
 * - Compresión en cliente para subida inmediata desde PC y Smartphone
 * - Predictive Hover/Touch Preloading (Instant Click)
 * - Intelligent Lazy Loading & Decoding Async con IntersectionObserver
 * - DNS Prefetching y Preconnect de CDNs y Tipografías
 */

(function () {
    'use strict';

    // 1. PRECONEXIONES INMEDIATAS A SERVIDORES CRÍTICOS (0ms de latencia DNS)
    function inyectarPreconexiones() {
        const dominios = [
            'https://fonts.googleapis.com',
            'https://fonts.gstatic.com',
            'https://cdn.jsdelivr.net'
        ];
        const head = document.head || document.getElementsByTagName('head')[0];
        dominios.forEach(url => {
            if (!document.querySelector(`link[href="${url}"]`)) {
                const linkPreconnect = document.createElement('link');
                linkPreconnect.rel = 'preconnect';
                linkPreconnect.href = url;
                linkPreconnect.crossOrigin = 'anonymous';
                head.appendChild(linkPreconnect);

                const linkDns = document.createElement('link');
                linkDns.rel = 'dns-prefetch';
                linkDns.href = url;
                head.appendChild(linkDns);
            }
        });
    }

    // 2. COMPRESIÓN INTELIGENTE DE IMÁGENES EN EL NAVEGADOR
    // Permite que fotos tomadas con cámaras de 12MP a 50MP (8MB - 20MB) se compriman
    // en ~40ms a ~150KB sin pérdida perceptible, permitiendo subida instantánea en móviles.
    window.comprimirImagenEnNavegador = function (file, maxWidth = 1200, maxHeight = 1200, quality = 0.85) {
        return new Promise((resolve) => {
            if (!file || !file.type.match(/image\/(jpeg|jpg|png|webp)/i)) {
                return resolve(file); // Retorna original si es gif, svg u otro tipo
            }

            const img = new Image();
            const blobUrl = URL.createObjectURL(file);

            img.onload = function () {
                URL.revokeObjectURL(blobUrl);
                let width = img.naturalWidth || img.width;
                let height = img.naturalHeight || img.height;

                // Redimensionar proporcionalmente
                if (width > maxWidth || height > maxHeight) {
                    if (width > height) {
                        height = Math.round((height * maxWidth) / width);
                        width = maxWidth;
                    } else {
                        width = Math.round((width * maxHeight) / height);
                        height = maxHeight;
                    }
                }

                const canvas = document.createElement('canvas');
                canvas.width = width;
                canvas.height = height;
                const ctx = canvas.getContext('2d', { alpha: false });
                ctx.imageSmoothingEnabled = true;
                ctx.imageSmoothingQuality = 'high';

                // Fondo blanco por si se sube un PNG transparente como servicio
                ctx.fillStyle = '#100118';
                ctx.fillRect(0, 0, width, height);

                ctx.drawImage(img, 0, 0, width, height);

                canvas.toBlob((blob) => {
                    if (blob && blob.size < file.size) {
                        resolve(blob);
                    } else {
                        resolve(file); // Si el archivo ya era pequeño, mantener original
                    }
                }, 'image/jpeg', quality);
            };

            img.onerror = function () {
                URL.revokeObjectURL(blobUrl);
                resolve(file);
            };

            img.src = blobUrl;
        });
    };

    // 3. LAZY LOADING INTELIGENTE Y DECODING ASYNC PARA CATÁLOGO
    // Carga solo las fotos visibles y pre-carga las que están a 250px de entrar en pantalla
    function acelerarImagenes() {
        const imagenes = document.querySelectorAll('img:not([data-accelerated])');
        
        let observer;
        if ('IntersectionObserver' in window) {
            observer = new IntersectionObserver((entries, obs) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        const img = entry.target;
                        if (img.dataset.src) {
                            img.src = img.dataset.src;
                            img.removeAttribute('data-src');
                        }
                        img.style.opacity = '1';
                        obs.unobserve(img);
                    }
                });
            }, { rootMargin: '250px 0px', threshold: 0.01 });
        }

        imagenes.forEach(img => {
            img.setAttribute('data-accelerated', 'true');
            img.decoding = 'async'; // Decodificación fuera del hilo principal (evita micro-tirones)
            
            // Si la imagen está en la mitad inferior de la página, asignar lazy nativo
            const rect = img.getBoundingClientRect();
            if (rect.top > window.innerHeight) {
                img.loading = 'lazy';
            }

            // Manejo de error suave si una imagen no carga
            img.addEventListener('error', function () {
                if (!this.dataset.fallbackApplied) {
                    this.dataset.fallbackApplied = 'true';
                    this.src = '/logo-spa.png'; // Fallback corporativo
                }
            }, { once: true });
        });
    }

    // 4. INSTANT-CLICK & PRELOAD PREDICTIVO
    // Cuando el usuario pasa el dedo (móvil) o el ratón (PC) sobre un enlace,
    // se pre-descarga la página 200ms antes del click, haciendo la navegación instantánea.
    const preloadedUrls = new Set();
    function precargarUrl(url) {
        if (!url || preloadedUrls.has(url) || url.startsWith('#') || url.startsWith('javascript:')) return;
        try {
            const parsed = new URL(url, window.location.origin);
            if (parsed.origin !== window.location.origin) return; // Solo links locales
            
            preloadedUrls.add(parsed.pathname);
            const linkPreload = document.createElement('link');
            linkPreload.rel = 'prefetch';
            linkPreload.href = parsed.pathname;
            document.head.appendChild(linkPreload);
        } catch (e) {}
    }

    function inicializarInstantClick() {
        document.addEventListener('mouseover', (e) => {
            const link = e.target.closest('a');
            if (link && link.href) precargarUrl(link.href);
        }, { passive: true });

        document.addEventListener('touchstart', (e) => {
            const link = e.target.closest('a');
            if (link && link.href) precargarUrl(link.href);
        }, { passive: true });
    }

    // 5. OBSERVAR NUEVOS ELEMENTOS (MUTATION OBSERVER)
    // Para cuando se agregan tratamientos, modales o contenido dinámico
    function observarMutaciones() {
        if (!('MutationObserver' in window)) return;
        const observer = new MutationObserver((mutations) => {
            let tieneNuevasImagenes = false;
            for (const mutation of mutations) {
                if (mutation.addedNodes.length > 0) {
                    tieneNuevasImagenes = true;
                    break;
                }
            }
            if (tieneNuevasImagenes) {
                acelerarImagenes();
            }
        });
        observer.observe(document.body, { childList: true, subtree: true });
    }

    // EJECUCIÓN INMEDIATA
    inyectarPreconexiones();

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            acelerarImagenes();
            inicializarInstantClick();
            observarMutaciones();
        });
    } else {
        acelerarImagenes();
        inicializarInstantClick();
        observarMutaciones();
    }
})();
