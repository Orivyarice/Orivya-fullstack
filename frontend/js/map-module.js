/* ═══════════════════════════════════════════════════════════════
   map-module.js  —  Delivery Map Feature (NEW — standalone)
   ───────────────────────────────────────────────────────────────
   WHAT THIS FILE DOES:
     1. Adds a "📍 Locate on Map" button next to the address field
     2. Geocodes the typed address via OpenStreetMap Nominatim API
     3. Calculates distance using Haversine formula (no backend call)
     4. Shows a Leaflet map: shop marker + customer marker + line
     5. Auto-fills the existing #deliveryDistanceKm input
     6. Calls the EXISTING updateDeliveryCharge() function

   WHAT THIS FILE DOES NOT DO:
     ✗ Does NOT modify any existing functions
     ✗ Does NOT change checkout, payment or order flow
     ✗ Does NOT modify main.js or api.js
     ✗ Does NOT add any backend API calls

   EXISTING HOOKS USED (read-only, never overwritten):
     • document.getElementById('checkAddress')     — reads address
     • document.getElementById('deliveryDistanceKm') — sets km value
     • window.updateDeliveryCharge()               — calls after setting km
     • document.getElementById('checkoutOverlay')  — listens for open/close

   MAP LIBRARY: Leaflet.js 1.9.4 via CDN (loaded in index.html)
   GEOCODING:   Nominatim (OpenStreetMap) — free, no API key needed
   DISTANCE:    Haversine formula — pure JavaScript, no backend
   ═══════════════════════════════════════════════════════════════ */

(function () {
    'use strict';

    /* ── SHOP LOCATION ────────────────────────────────────────────
       Sri Lakshmi Ganapati Mini Rice Mill
       Gorinta, Peddapuram Mandal, Kakinada District, AP – 533433
       ──────────────────────────────────────────────────────────── */
    const SHOP = {
        lat:  17.11053,
        lng:  82.17063,
        name: 'Orivya — Sri Lakshmi Ganapati Mini Rice Mill',
        addr: 'Gorinta, Peddapuram, Kakinada Dist, AP'
    };

    /* ── STATE ───────────────────────────────────────────────────── */
    let leafletMap         = null;   // Leaflet map instance
    let shopMarker         = null;   // Shop pin
    let customerMarker     = null;   // Customer pin
    let routeLine          = null;   // Line between shop & customer
    let mapInitialized     = false;  // Guard against double-init
    let geocodeDebounce    = null;   // Debounce timer for address input

    /* ── INIT: Run after DOM is ready ────────────────────────────── */
    document.addEventListener('DOMContentLoaded', function () {
        injectMapUI();
        listenForCheckoutOpen();
    });

    /* ─────────────────────────────────────────────────────────────
       STEP 1 — Inject the "Locate on Map" button and map container
       into the existing checkout Step 1 HTML.

       Injection point: AFTER #distanceGroup, BEFORE .order-summary-box
       Both elements already exist in index.html — we insert between them.
       ───────────────────────────────────────────────────────────── */
    function injectMapUI() {
        const distanceGroup = document.getElementById('distanceGroup');
        if (!distanceGroup) return; // checkout not on this page

        /* ── "Locate on Map" button — injected after address textarea ── */
        const addressField = document.getElementById('checkAddress');
        if (addressField && !document.getElementById('mapLocateBtn')) {
            const btn = document.createElement('button');
            btn.type        = 'button';
            btn.id          = 'mapLocateBtn';
            btn.textContent = '📍 Locate on Map';
            btn.title       = 'Geocode address and show delivery distance on map';
            btn.onclick     = handleLocateClick;

            /* Insert button right after the address textarea */
            addressField.insertAdjacentElement('afterend', btn);
        }

        /* ── Status message (geocoding feedback) ── */
        if (!document.getElementById('mapStatus')) {
            const status = document.createElement('div');
            status.id = 'mapStatus';
            distanceGroup.appendChild(status);
        }

        /* ── Map section container ── */
        if (!document.getElementById('mapSection')) {
            const mapSection = document.createElement('div');
            mapSection.id        = 'mapSection';
            mapSection.innerHTML =
                '<div id="mapInfoBar">' +
                '  <span>' +
                '    <span class="map-shop-label">📍 Shop → Customer:</span> ' +
                '    <span class="map-distance-label" id="mapDistanceText">Calculating...</span>' +
                '  </span>' +
                '  <span class="map-shop-label" id="mapCustomerText"></span>' +
                '</div>' +
                '<div id="deliveryMap"></div>';

            /* Insert AFTER distanceGroup, BEFORE order-summary-box */
            distanceGroup.insertAdjacentElement('afterend', mapSection);
        }
    }

    /* ─────────────────────────────────────────────────────────────
       STEP 2 — Listen for checkout modal open/close to
       initialise and reset the Leaflet map appropriately.
       Leaflet requires the container to be visible before init.
       ───────────────────────────────────────────────────────────── */
    function listenForCheckoutOpen() {
        const overlay = document.getElementById('checkoutOverlay');
        if (!overlay) return;

        /* MutationObserver watches for 'open' class being added to overlay */
        const observer = new MutationObserver(function (mutations) {
            mutations.forEach(function (m) {
                if (m.attributeName === 'class') {
                    if (overlay.classList.contains('open')) {
                        onCheckoutOpened();
                    } else {
                        onCheckoutClosed();
                    }
                }
            });
        });
        observer.observe(overlay, { attributes: true });

        /* Also watch address field for typing (auto-debounce geocode) */
        const addressField = document.getElementById('checkAddress');
        if (addressField) {
            addressField.addEventListener('input', function () {
                clearTimeout(geocodeDebounce);
                geocodeDebounce = setTimeout(function () {
                    /* Only auto-geocode if user has typed a reasonable address */
                    if (addressField.value.trim().length > 15) {
                        geocodeAndShowMap(addressField.value.trim());
                    }
                }, 1800); /* 1.8 second delay after user stops typing */
            });
        }
    }

    function onCheckoutOpened() {
        /* Small delay to ensure modal is fully visible for Leaflet */
        setTimeout(function () {
            if (!mapInitialized) {
                initLeafletMap();
            } else if (leafletMap) {
                leafletMap.invalidateSize(); /* Fix size if modal just became visible */
            }
        }, 300);
    }

    function onCheckoutClosed() {
        /* Hide map section when checkout closes */
        const mapSection = document.getElementById('mapSection');
        if (mapSection) mapSection.style.display = 'none';
    }

    /* ─────────────────────────────────────────────────────────────
       STEP 3 — Initialise Leaflet map (called once, lazily)
       ───────────────────────────────────────────────────────────── */
    function initLeafletMap() {
        if (mapInitialized) return;
        if (typeof L === 'undefined') {
            console.warn('[MapModule] Leaflet not loaded yet.');
            return;
        }

        const mapEl = document.getElementById('deliveryMap');
        if (!mapEl) return;

        mapInitialized = true;

        /* Create Leaflet map centred on shop location */
        leafletMap = L.map('deliveryMap', {
            center:    [SHOP.lat, SHOP.lng],
            zoom:      11,
            zoomControl: true,
            scrollWheelZoom: false  /* prevent accidental zoom on checkout scroll */
        });

        /* OpenStreetMap tile layer — free, no API key */
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
            maxZoom: 18
        }).addTo(leafletMap);

        /* Shop marker (green) */
        const shopIcon = L.divIcon({
            className: '',
            html: '<div style="background:#1a5c2a;color:white;padding:5px 8px;border-radius:6px;font-size:11px;font-weight:700;white-space:nowrap;box-shadow:0 2px 6px rgba(0,0,0,0.3)">🌾 Our Shop</div>',
            iconAnchor: [35, 30]
        });

        shopMarker = L.marker([SHOP.lat, SHOP.lng], { icon: shopIcon })
            .addTo(leafletMap)
            .bindPopup('<div class="map-shop-popup">🌾 ' + SHOP.name + '<br><small>' + SHOP.addr + '</small></div>');
    }

    /* ─────────────────────────────────────────────────────────────
       STEP 4 — Handle "Locate on Map" button click
       ───────────────────────────────────────────────────────────── */
    function handleLocateClick() {
        const addressField = document.getElementById('checkAddress');
        const address      = addressField ? addressField.value.trim() : '';

        if (!address || address.length < 5) {
            showMapStatus('⚠ Please enter your delivery address first.', 'error');
            return;
        }

        geocodeAndShowMap(address);
    }

    /* ─────────────────────────────────────────────────────────────
       STEP 5 — Geocode address using Nominatim (OpenStreetMap)
       Nominatim API: free, no key required, max 1 req/sec
       ───────────────────────────────────────────────────────────── */
    function geocodeAndShowMap(address) {
        const btn = document.getElementById('mapLocateBtn');
        if (btn) { btn.disabled = true; btn.textContent = '⏳ Locating...'; }

        showMapStatus('🔍 Finding location on map...', 'loading');

        /* Append "Andhra Pradesh India" to improve accuracy for local addresses */
        const query = encodeURIComponent(address + ', Andhra Pradesh, India');
        const url   = 'https://nominatim.openstreetmap.org/search?format=json&limit=1&q=' + query;

        fetch(url, {
            headers: { 'Accept-Language': 'en', 'User-Agent': 'OrivyaRiceApp/1.0' }
        })
        .then(function (res) { return res.json(); })
        .then(function (results) {
            if (!results || results.length === 0) {
                showMapStatus('❌ Address not found. Try entering more details like district or pincode.', 'error');
                if (btn) { btn.disabled = false; btn.textContent = '📍 Locate on Map'; }
                return;
            }

            const result = results[0];
            const custLat = parseFloat(result.lat);
            const custLng = parseFloat(result.lon);

            /* Calculate distance using Haversine */
            const distKm = haversineKm(SHOP.lat, SHOP.lng, custLat, custLng);

            /* Show map and markers */
            showCustomerOnMap(custLat, custLng, distKm, result.display_name);

            /* ── HOOK INTO EXISTING SYSTEM ────────────────────
               Set the value of the existing #deliveryDistanceKm input.
               Then call the existing updateDeliveryCharge() function.
               Both exist in main.js — we are NOT modifying them, only
               triggering them with the geocoded distance value.
               ──────────────────────────────────────────────── */
            const kmInput = document.getElementById('deliveryDistanceKm');
            if (kmInput) {
                kmInput.value = distKm.toFixed(1);
                /* Fire 'input' event so oninput="updateDeliveryCharge()" triggers */
                kmInput.dispatchEvent(new Event('input', { bubbles: true }));
            }

            /* Also call directly as fallback */
            if (typeof window.updateDeliveryCharge === 'function') {
                window.updateDeliveryCharge();
            }

            /* Also re-trigger openCheckout total recalculation */
            if (typeof window.recalcCheckoutTotals === 'function') {
                window.recalcCheckoutTotals();
            }

            hideMapStatus();
            if (btn) { btn.disabled = false; btn.textContent = '📍 Locate on Map'; }
        })
        .catch(function (err) {
            showMapStatus('❌ Could not reach map service. Check your internet connection.', 'error');
            console.error('[MapModule] Nominatim error:', err);
            if (btn) { btn.disabled = false; btn.textContent = '📍 Locate on Map'; }
        });
    }

    /* ─────────────────────────────────────────────────────────────
       STEP 6 — Show customer marker and route line on the map
       ───────────────────────────────────────────────────────────── */
    function showCustomerOnMap(lat, lng, distKm, displayName) {
        /* Make sure map is initialized */
        if (!mapInitialized) initLeafletMap();
        if (!leafletMap) return;

        /* Show the map section */
        const mapSection = document.getElementById('mapSection');
        if (mapSection) mapSection.style.display = 'block';

        /* Fix Leaflet size (required when container was hidden) */
        setTimeout(function () { leafletMap.invalidateSize(); }, 100);

        /* Remove old customer marker and route line if present */
        if (customerMarker) { leafletMap.removeLayer(customerMarker); }
        if (routeLine)      { leafletMap.removeLayer(routeLine);      }

        /* Customer marker (amber/gold) */
        const custIcon = L.divIcon({
            className: '',
            html: '<div style="background:#c8922a;color:white;padding:5px 8px;border-radius:6px;font-size:11px;font-weight:700;white-space:nowrap;box-shadow:0 2px 6px rgba(0,0,0,0.3)">📦 Delivery Here</div>',
            iconAnchor: [50, 30]
        });

        const shortName = displayName ? displayName.split(',').slice(0, 3).join(',') : 'Your Location';

        customerMarker = L.marker([lat, lng], { icon: custIcon })
            .addTo(leafletMap)
            .bindPopup('<div class="map-cust-popup">📦 Delivery Location<br><small>' + shortName + '</small></div>')
            .openPopup();

        /* Straight line between shop and customer */
        routeLine = L.polyline(
            [[SHOP.lat, SHOP.lng], [lat, lng]],
            {
                color:     '#1a5c2a',
                weight:    2.5,
                opacity:   0.7,
                dashArray: '6, 8'    /* dashed line */
            }
        ).addTo(leafletMap);

        /* Fit map to show both markers */
        const bounds = L.latLngBounds(
            [SHOP.lat, SHOP.lng],
            [lat, lng]
        );
        leafletMap.fitBounds(bounds, { padding: [30, 30] });

        /* Update info bar */
        const distText = document.getElementById('mapDistanceText');
        const custText = document.getElementById('mapCustomerText');
        if (distText) {
            const chargeNote = distKm <= 15 ? '✅ Free delivery zone' : '⚠ ₹60 delivery charge zone';
            distText.textContent = distKm.toFixed(1) + ' km  —  ' + chargeNote;
        }
        if (custText) custText.textContent = shortName;
    }

    /* ─────────────────────────────────────────────────────────────
       HAVERSINE FORMULA
       Calculates straight-line distance between two lat/lng points.
       Returns: distance in kilometres (number)
       ───────────────────────────────────────────────────────────── */
    function haversineKm(lat1, lng1, lat2, lng2) {
        const R   = 6371;                          // Earth radius in km
        const dLat = toRad(lat2 - lat1);
        const dLng = toRad(lng2 - lng1);
        const a   = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
                    Math.sin(dLng / 2) * Math.sin(dLng / 2);
        const c   = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    function toRad(deg) { return deg * (Math.PI / 180); }

    /* ─────────────────────────────────────────────────────────────
       STATUS MESSAGE HELPERS
       ───────────────────────────────────────────────────────────── */
    function showMapStatus(msg, type) {
        const el = document.getElementById('mapStatus');
        if (!el) return;
        el.textContent  = msg;
        el.className    = 'mapStatus ' + (type || 'loading');  /* existing class untouched */
        el.style.display = 'block';
        el.style.padding = '6px 10px';
        el.style.borderRadius = '4px';
        el.style.fontSize = '12px';
        el.style.marginTop = '6px';
        if (type === 'error') {
            el.style.background = '#f8d7da';
            el.style.color      = '#721c24';
        } else {
            el.style.background = '#e8f5e9';
            el.style.color      = '#1a5c2a';
        }
    }

    function hideMapStatus() {
        const el = document.getElementById('mapStatus');
        if (el) el.style.display = 'none';
    }

})(); /* End IIFE — all code is scoped, nothing pollutes global namespace */