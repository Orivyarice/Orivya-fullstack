/* ══════════════════════════════════════════════════════
   FEATURE 1: Skeleton Loading — JS Code
   FILE: features/skeleton-loading.js

   WHERE TO PLACE THIS CODE:
   ─────────────────────────────────────────────────────
   Open: frontend/js/main.js

   STEP 1: Add renderSkeletonCards() function.
   Paste the entire function ABOVE the loadProducts() function
   (around line 33 in main.js, before "async function loadProducts")

   STEP 2: In loadProducts(), change ONE line.
   Find this line (around line 38):
       grid.innerHTML = `<div class="loading-products">
            <div class="spinner"></div><p>Loading products...</p></div>`;

   Replace it with:
       grid.innerHTML = renderSkeletonCards(4);

   That's it! 2 changes only.
   ══════════════════════════════════════════════════════ */

/* ─────────────────────────────────────────────────────
   PASTE THIS FUNCTION into main.js
   Place it ABOVE the loadProducts() function
   ───────────────────────────────────────────────────── */

function renderSkeletonCards(count = 4) {
    const card = `
        <div class="product-card skeleton-card">
            <div class="skeleton-image"></div>
            <div class="skeleton-info">
                <div class="skeleton-block skeleton-title"></div>
                <div class="skeleton-block skeleton-desc-1"></div>
                <div class="skeleton-block skeleton-desc-2"></div>
                <div class="skeleton-block skeleton-price"></div>
                <div class="skeleton-block skeleton-btn"></div>
            </div>
        </div>`;
    return Array(count).fill(card).join('');
}
