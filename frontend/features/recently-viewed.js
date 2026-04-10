/* ══════════════════════════════════════════════════════
   FEATURE 5: Recently Viewed Products — JS + HTML
   FILE: features/recently-viewed.js

   WHERE TO PLACE:
   ─────────────────────────────────────────────────────
   PART A — Add HTML section to index.html
   Find the closing </section> tag of the SHOP section
   (the one with id="shop", around line 110).
   ADD this HTML block IMMEDIATELY AFTER that </section>:

   <section class="recently-viewed-section" id="recentlyViewed" style="display:none">
     <div class="rv-header">
       <h3>👁 Recently Viewed</h3>
       <button class="rv-clear-btn" onclick="clearRecentlyViewed()">Clear History</button>
     </div>
     <div class="rv-row" id="rvRow"></div>
   </section>

   ─────────────────────────────────────────────────────
   PART B — Add these functions to the BOTTOM of main.js
   ──────────────────────────────────────────────────── */

// === ADD TO main.js — paste at the BOTTOM ===

const RV_KEY      = 'orivya_recently_viewed';
const RV_MAX      = 8;   // max number of products to store

/* Track a product as recently viewed (called in renderProducts) */
function trackRecentlyViewed(product) {
    try {
        let rv = JSON.parse(localStorage.getItem(RV_KEY) || '[]');
        // Remove if already exists
        rv = rv.filter(p => p.id !== product.id);
        // Add to front
        rv.unshift({
            id:       product.id,
            name:     product.name,
            price:    product.price,
            weight:   product.weight,
            imageUrl: product.imageUrl || ''
        });
        // Keep only RV_MAX items
        rv = rv.slice(0, RV_MAX);
        localStorage.setItem(RV_KEY, JSON.stringify(rv));
    } catch (e) { /* localStorage may be unavailable */ }

    // Refresh the display
    renderRecentlyViewed();
}

/* Render the Recently Viewed section */
function renderRecentlyViewed() {
    const section = document.getElementById('recentlyViewed');
    const row     = document.getElementById('rvRow');
    if (!section || !row) return;

    try {
        const rv = JSON.parse(localStorage.getItem(RV_KEY) || '[]');

        if (rv.length === 0) {
            section.style.display = 'none';
            return;
        }

        section.style.display = 'block';
        row.innerHTML = rv.map(p => `
            <div class="rv-card" onclick="scrollToProductInGrid(${p.id})">
                <div class="rv-card-image">
                    ${p.imageUrl
                        ? `<img src="${getImageUrl(p.imageUrl)}"
                                alt="${escapeStr(p.name)}"
                                onerror="this.parentNode.textContent='🌾'" />`
                        : '🌾'}
                </div>
                <div class="rv-card-info">
                    <div class="rv-card-name">${p.name}</div>
                    <div class="rv-card-price">${formatPrice(p.price)}</div>
                </div>
            </div>
        `).join('');

    } catch (e) {
        section.style.display = 'none';
    }
}

/* Clear recently viewed history */
function clearRecentlyViewed() {
    localStorage.removeItem(RV_KEY);
    const section = document.getElementById('recentlyViewed');
    if (section) section.style.display = 'none';
}

/* Scroll to shop section when rv card is clicked */
function scrollToProductInGrid(productId) {
    const shopSection = document.getElementById('shop');
    if (shopSection) shopSection.scrollIntoView({ behavior: 'smooth' });
}

/* Show recently viewed on page load */
document.addEventListener('DOMContentLoaded', function() {
    // Small delay so products section loads first
    setTimeout(renderRecentlyViewed, 800);
});
