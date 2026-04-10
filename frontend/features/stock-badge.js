/* ══════════════════════════════════════════════════════
   FEATURE 2: Stock Warning + Natural Badge — JS
   FILE: features/stock-badge.js

   WHERE TO PLACE THIS CODE:
   ─────────────────────────────────────────────────────
   Open: frontend/js/main.js

   Find the renderProducts() function (around line 71).
   You will see this HTML template inside it:

       function renderProducts(products) {
           const grid = document.getElementById('productsGrid');
           grid.innerHTML = products.map(p => `
               <div class="product-card">
                   ${p.badge ? `<div class="product-badge">${p.badge}</div>` : ''}
                   <div class="product-image">
                       ...

   REPLACE the ENTIRE renderProducts() function
   with the one below.
   
   WHAT CHANGES:
   ✅ Adds "🌿 100% Natural" badge (top-right of card)
   ✅ Adds "✅ FSSAI" mini badge in product info
   ✅ Adds "⚡ Only X left!" warning when stock < 10
   ✅ Adds lazy loading to product images
   ── Everything else stays exactly the same ──
   ══════════════════════════════════════════════════════ */

function renderProducts(products) {
    const grid = document.getElementById('productsGrid');

    // Track recently viewed (for Feature: Recently Viewed)
    products.forEach(p => trackRecentlyViewed(p));

    grid.innerHTML = products.map(p => `
        <div class="product-card">

            ${/* LEFT badge (Best Seller, Wholesale etc.) */ ''}
            ${p.badge ? `<div class="product-badge">${p.badge}</div>` : ''}

            ${/* RIGHT badge — 100% Natural */ ''}
            <div class="natural-badge">🌿 100% Natural</div>

            ${/* Product Image — with lazy loading */ ''}
            <div class="product-image">
                ${p.imageUrl
                    ? `<img src="${getImageUrl(p.imageUrl)}" alt="${p.name}"
                               loading="lazy"
                               onerror="this.style.display='none';this.nextElementSibling.style.display='flex'"/>`
                    : ''}
                <div class="product-img-fallback"
                     style="${p.imageUrl ? 'display:none' : 'display:flex'}">🌾</div>
            </div>

            ${/* "Only X left" warning — shown when stock < 10 */ ''}
            ${(p.stockQuantity > 0 && p.stockQuantity < 10)
                ? `<div class="stock-warning">
                       <span class="pulse-dot"></span>
                       Only ${p.stockQuantity} left in stock!
                   </div>`
                : ''}

            <div class="product-info">

                ${/* FSSAI mini badge */ ''}
                <div class="fssai-mini-badge">✅ FSSAI Certified</div>

                <div class="product-name">${p.name}</div>
                <div class="product-desc">${p.description || ''}</div>

                <div class="product-price-row">
                    <div class="product-price">${formatPrice(p.price)}<span>/ pack</span></div>
                    <div class="product-weight">${p.weight}</div>
                </div>

                ${(p.stockQuantity === 0)
                    ? `<div class="out-of-stock">Out of Stock</div>`
                    : `<button class="add-to-cart"
                               onclick="handleAddToCart(${p.id}, '${escapeStr(p.name)}')">
                           🛒 ADD TO CART
                       </button>
                       <button class="whatsapp-order-btn"
                               onclick="window.open('https://wa.me/919640525341?text='
                               + encodeURIComponent('🌾 I want to order: ${escapeStr(p.name)} (${escapeStr(p.weight)}) — ${formatPrice(p.price)}'), '_blank')">
                           💬 ORDER ON WHATSAPP
                       </button>`
                }
            </div>
        </div>
    `).join('');
}
