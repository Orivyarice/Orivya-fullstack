/**
 * pwa.js — Orivya PWA: Service Worker Registration + Install Prompt
 *
 * Place at: frontend/js/pwa.js
 * Include in ALL HTML pages just before </body>:
 *   <script src="js/pwa.js"></script>
 *
 * This file handles:
 *   1. Service worker registration
 *   2. "Install App" button appearance
 *   3. Install prompt trigger
 *   4. SW update notification
 */

'use strict';

/* ══════════════════════════════════════════════════════════════
   1. REGISTER SERVICE WORKER
══════════════════════════════════════════════════════════════ */
(function registerSW() {
  if (!('serviceWorker' in navigator)) {
    console.log('[PWA] Service workers not supported in this browser.');
    return;
  }

  window.addEventListener('load', async () => {
    try {
      const reg = await navigator.serviceWorker.register('/service-worker.js', {
        scope: '/'
      });

      console.log('[PWA] Service worker registered. Scope:', reg.scope);

      // ── DETECT UPDATES ────────────────────────────────────────
      reg.addEventListener('updatefound', () => {
        const newWorker = reg.installing;
        newWorker.addEventListener('statechange', () => {
          if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
            // New version available — show update banner
            showUpdateBanner();
          }
        });
      });

    } catch (err) {
      console.error('[PWA] Service worker registration failed:', err);
    }
  });
})();

/* ══════════════════════════════════════════════════════════════
   2. INSTALL PROMPT
   Capture the browser's beforeinstallprompt event and show
   a custom "Install App" button instead of browser default.
══════════════════════════════════════════════════════════════ */
let deferredInstallPrompt = null;

window.addEventListener('beforeinstallprompt', (e) => {
  // Prevent Chrome from showing the mini-infobar
  e.preventDefault();
  deferredInstallPrompt = e;
  console.log('[PWA] Install prompt captured — app is installable');

  // Show our custom install button/banner
  showInstallBanner();
});

// Called when user actually installs the app
window.addEventListener('appinstalled', () => {
  console.log('[PWA] App installed successfully!');
  deferredInstallPrompt = null;
  hideInstallBanner();

  // Optional: track installation
  if (typeof showToast === 'function') {
    showToast('✅ Orivya app installed! Find it on your home screen.', 'success');
  }
});

/* ══════════════════════════════════════════════════════════════
   3. INSTALL BANNER — injected into page DOM
══════════════════════════════════════════════════════════════ */
function showInstallBanner() {
  // Don't show if already installed (running as standalone PWA)
  if (window.matchMedia('(display-mode: standalone)').matches) return;
  // Don't show if already visible
  if (document.getElementById('pwa-install-banner')) return;

  const banner = document.createElement('div');
  banner.id = 'pwa-install-banner';
  banner.innerHTML = `
    <div style="
      position: fixed;
      bottom: 0; left: 0; right: 0;
      z-index: 99999;
      background: linear-gradient(135deg, #1a5c2a, #2d7a3e);
      color: white;
      padding: 14px 20px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      box-shadow: 0 -4px 20px rgba(0,0,0,0.2);
      font-family: system-ui, sans-serif;
      animation: slideUpBanner 0.4s ease;
    ">
      <style>
        @keyframes slideUpBanner {
          from { transform: translateY(100%); opacity: 0; }
          to   { transform: translateY(0);    opacity: 1; }
        }
        #pwa-install-banner button {
          font-family: system-ui, sans-serif;
          cursor: pointer;
          border: none;
          border-radius: 6px;
          font-weight: 700;
          font-size: 13px;
          padding: 9px 18px;
          transition: transform 0.2s;
        }
        #pwa-install-banner button:hover { transform: translateY(-1px); }
      </style>

      <div style="display:flex;align-items:center;gap:10px;min-width:0">
        <span style="font-size:28px;flex-shrink:0">🌾</span>
        <div style="min-width:0">
          <div style="font-weight:700;font-size:14px">Install Orivya Rice</div>
          <div style="font-size:12px;opacity:0.85">Add to home screen for quick access</div>
        </div>
      </div>

      <div style="display:flex;gap:8px;flex-shrink:0">
        <button
          onclick="triggerInstallPrompt()"
          style="background:#c8922a;color:#1a0e00"
        >
          📲 Install
        </button>
        <button
          onclick="hideInstallBanner()"
          style="background:rgba(255,255,255,0.2);color:white"
        >
          ✕
        </button>
      </div>
    </div>
  `;

  document.body.appendChild(banner);
}

function hideInstallBanner() {
  const banner = document.getElementById('pwa-install-banner');
  if (banner) {
    banner.style.animation = 'none';
    banner.style.transform = 'translateY(100%)';
    banner.style.opacity   = '0';
    banner.style.transition = 'all 0.3s ease';
    setTimeout(() => banner.remove(), 300);
  }
  // Remember dismissal for 7 days
  localStorage.setItem('pwa-banner-dismissed', Date.now().toString());
}

async function triggerInstallPrompt() {
  if (!deferredInstallPrompt) {
    console.log('[PWA] No install prompt available');
    return;
  }

  // Show the browser's install dialog
  deferredInstallPrompt.prompt();

  // Wait for user choice
  const { outcome } = await deferredInstallPrompt.userChoice;
  console.log('[PWA] Install prompt outcome:', outcome);

  if (outcome === 'accepted') {
    console.log('[PWA] User accepted install');
  } else {
    console.log('[PWA] User dismissed install');
    // Re-show banner after 3 days if dismissed
    localStorage.setItem('pwa-banner-dismissed', Date.now().toString());
  }

  deferredInstallPrompt = null;
}

/* ══════════════════════════════════════════════════════════════
   4. UPDATE BANNER — shown when new version is available
══════════════════════════════════════════════════════════════ */
function showUpdateBanner() {
  if (document.getElementById('pwa-update-banner')) return;

  const banner = document.createElement('div');
  banner.id = 'pwa-update-banner';
  banner.innerHTML = `
    <div style="
      position: fixed;
      top: 0; left: 0; right: 0;
      z-index: 99999;
      background: #1a5c2a;
      color: white;
      padding: 12px 20px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      box-shadow: 0 2px 12px rgba(0,0,0,0.2);
      font-family: system-ui, sans-serif;
      font-size: 14px;
    ">
      <span>🔄 New version of Orivya available!</span>
      <div style="display:flex;gap:8px">
        <button
          onclick="window.location.reload(true)"
          style="background:#c8922a;color:#1a0e00;border:none;border-radius:6px;
                 padding:7px 14px;font-weight:700;cursor:pointer;font-size:13px"
        >
          Update Now
        </button>
        <button
          onclick="this.closest('#pwa-update-banner').remove()"
          style="background:rgba(255,255,255,0.2);color:white;border:none;
                 border-radius:6px;padding:7px 10px;cursor:pointer;font-size:13px"
        >
          ✕
        </button>
      </div>
    </div>
  `;

  document.body.prepend(banner);
}

/* ══════════════════════════════════════════════════════════════
   5. ONLINE / OFFLINE STATUS INDICATOR
══════════════════════════════════════════════════════════════ */
function showNetworkStatus(online) {
  // Remove existing status bar
  const existing = document.getElementById('pwa-network-status');
  if (existing) existing.remove();

  if (online) {
    // Show "Back online" briefly then hide
    const bar = document.createElement('div');
    bar.id = 'pwa-network-status';
    bar.style.cssText = `
      position:fixed; top:0; left:0; right:0; z-index:99998;
      background:#1a5c2a; color:white; text-align:center;
      padding:8px; font-size:13px; font-weight:600;
      font-family:system-ui,sans-serif;
    `;
    bar.textContent = '✅ Back online';
    document.body.prepend(bar);
    setTimeout(() => bar.remove(), 2500);
  } else {
    const bar = document.createElement('div');
    bar.id = 'pwa-network-status';
    bar.style.cssText = `
      position:fixed; top:0; left:0; right:0; z-index:99998;
      background:#c0392b; color:white; text-align:center;
      padding:8px; font-size:13px; font-weight:600;
      font-family:system-ui,sans-serif;
    `;
    bar.textContent = '📡 You are offline — some features may be limited';
    document.body.prepend(bar);
  }
}

window.addEventListener('online',  () => showNetworkStatus(true));
window.addEventListener('offline', () => showNetworkStatus(false));

// Show offline bar immediately if already offline on page load
if (!navigator.onLine) showNetworkStatus(false);