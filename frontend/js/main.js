/* ══════════════════════════════════════════════════════
   main.js — Shop page logic
   All 7 features integrated — no existing code broken
   ══════════════════════════════════════════════════════ */

let currentPage      = 0;
let currentKeyword   = '';
let currentCategory  = '';   // active category filter
let searchTimeout    = null;
let selectedPayMethod = 'cod';

// FEATURE 4: autocomplete cache
let allProductsCache = [];
let highlightedIndex = -1;

/* ════════ INIT ════════ */
document.addEventListener('DOMContentLoaded', function () {
    updateNavbar();
    loadCartCount();
    loadProducts();
    selectPayment('cod');

    // FEATURE 5: Show recently viewed on load
    setTimeout(renderRecentlyViewed, 800);
});

/* ════════ SCROLL HELPERS ════════ */
function scrollToShop()    { document.getElementById('shop').scrollIntoView({ behavior: 'smooth' }); }
function scrollToContact() { document.getElementById('contact').scrollIntoView({ behavior: 'smooth' }); }
function scrollToWhy()     { document.getElementById('why').scrollIntoView({ behavior: 'smooth' }); }


/* ════════════════════════════════════════
   FEATURE 1: SKELETON CARDS
════════════════════════════════════════ */
function renderSkeletonCards(count) {
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


/* ════════════════════════════════════════
   PRODUCTS — load from API
════════════════════════════════════════ */
async function loadProducts(page = 0, keyword = '') {
    currentPage    = page;
    currentKeyword = keyword;

    const grid = document.getElementById('productsGrid');
    // FEATURE 1: Show skeleton cards instead of spinner
    grid.innerHTML = renderSkeletonCards(4);

    try {
        const data = keyword
            ? await apiSearchProducts(keyword, page)
            : await apiGetProducts(page, 8, 'id');

        const pageData = data.data;

        if (!pageData || pageData.content.length === 0) {
            grid.innerHTML = `
                <div class="no-products">
                    <div style="font-size:48px;margin-bottom:12px">🌾</div>
                    <p>No products found.</p>
                </div>`;
            document.getElementById('pagination').innerHTML = '';
            return;
        }

        renderProducts(pageData.content);
        renderPagination(pageData.totalPages, pageData.number);

        // FEATURE 4: Cache products for search autocomplete
        allProductsCache = [...allProductsCache, ...pageData.content];
        allProductsCache = allProductsCache.filter((p, i, arr) =>
            arr.findIndex(x => x.id === p.id) === i
        );

    } catch (e) {
        grid.innerHTML = `
            <div class="no-products">
                <p style="font-size:16px;margin-bottom:8px">⚠ Could not load products</p>
                <small style="color:#aaa">Make sure Spring Boot is running at http://localhost:8080</small>
                <br><br>
                <button class="btn-primary btn-sm" onclick="loadProducts()">🔄 Try Again</button>
            </div>`;
    }
}


/* ════════════════════════════════════════
   PRODUCTS — render cards
   FEATURES 2, 3, 5 integrated here
════════════════════════════════════════ */
function renderProducts(products) {
    const grid = document.getElementById('productsGrid');

    grid.innerHTML = products.map(p => `
        <div class="product-card" data-product-id="${p.id}">

            ${/* FEATURE 2 LEFT: badge (Best Seller etc.) */ ''}
            ${p.badge ? `<div class="product-badge">${p.badge}</div>` : ''}

            ${/* FEATURE 2 RIGHT: 100% Natural badge */ ''}
            <div class="natural-badge">🌿 100% Natural</div>

            ${/* FEATURE 3: Image with zoom on hover + click to open modal */ ''}
            <div class="product-image"
                 onclick="openImageZoom('${getImageUrl(p.imageUrl)}', '${escapeStr(p.name)}')">
                ${p.imageUrl
                    ? `<img src="${getImageUrl(p.imageUrl)}" alt="${escapeStr(p.name)}"
                               loading="lazy"
                               onerror="this.style.display='none';this.nextElementSibling.style.display='flex'"/>`
                    : ''}
                <div class="product-img-fallback"
                     style="${p.imageUrl ? 'display:none' : 'display:flex'}">🌾</div>
            </div>

            ${/* FEATURE 2: "Only X left" stock warning when stock < 10 */ ''}
            ${(p.stockQuantity > 0 && p.stockQuantity < 10)
                ? `<div class="stock-warning">
                       <span class="stock-pulse-dot"></span>
                       Only ${p.stockQuantity} left in stock!
                   </div>`
                : ''}

            <div class="product-info">
                ${/* FEATURE 2: FSSAI mini badge */ ''}
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
                               onclick="window.open('https://wa.me/916304212346?text='
                               + encodeURIComponent('🌾 I want to order: ${escapeStr(p.name)} (${escapeStr(p.weight)}) — ${formatPrice(p.price)}'), '_blank')">
                           💬 ORDER ON WHATSAPP
                       </button>`
                }
            </div>
        </div>
    `).join('');

    // FEATURE 5: Track for recently viewed
    products.forEach(p => trackRecentlyViewed(p));
}


/* ════════ PAGINATION ════════ */
function renderPagination(totalPages, currentPageNum) {
    const el = document.getElementById('pagination');
    if (totalPages <= 1) { el.innerHTML = ''; return; }

    let html = '';
    if (currentPageNum > 0)
        html += `<button class="page-btn" onclick="loadProducts(${currentPageNum - 1}, '${currentKeyword}')">← Prev</button>`;
    for (let i = 0; i < totalPages; i++)
        html += `<button class="page-btn ${i === currentPageNum ? 'active' : ''}"
                         onclick="loadProducts(${i}, '${currentKeyword}')">${i + 1}</button>`;
    if (currentPageNum < totalPages - 1)
        html += `<button class="page-btn" onclick="loadProducts(${currentPageNum + 1}, '${currentKeyword}')">Next →</button>`;

    el.innerHTML = html;
}


/* ════════════════════════════════════════
   FEATURE 4: SEARCH AUTOCOMPLETE
════════════════════════════════════════ */
function handleSearchWithSuggestions(value) {
    const keyword = value.trim();
    // Regular debounced search (unchanged behaviour)
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => loadProducts(0, keyword), 400);
    // Show suggestions immediately
    if (keyword.length < 2) { closeSuggestions(); return; }
    showSuggestions(keyword);
}

// Legacy alias — keeps old oninput="handleSearch()" working if any page uses it
function handleSearch(value) {
    handleSearchWithSuggestions(value);
}

function showSuggestions(keyword) {
    const box = document.getElementById('searchSuggestions');
    if (!box) return;

    const matches = allProductsCache.filter(p =>
        p.name.toLowerCase().includes(keyword.toLowerCase()) ||
        (p.category && p.category.toLowerCase().includes(keyword.toLowerCase()))
    ).slice(0, 6);

    function highlight(text, kw) {
        const re = new RegExp(`(${kw.replace(/[.*+?^${}()|[\]\\]/g,'\\$&')})`, 'gi');
        return text.replace(re, '<mark>$1</mark>');
    }

    if (matches.length === 0) {
        box.innerHTML = `
            <div class="suggestion-header">Suggestions</div>
            <div class="suggestion-empty">No products match "${keyword}"</div>`;
        box.classList.add('open');
        return;
    }

    box.innerHTML = `
        <div class="suggestion-header">Suggestions</div>
        ${matches.map((p, i) => `
            <div class="suggestion-item" id="suggestion-${i}"
                 onclick="selectSuggestion('${escapeStr(p.name)}', ${p.id})"
                 onmouseenter="highlightSuggestion(${i})">
                <div class="suggestion-icon">
                    ${p.imageUrl
                        ? `<img src="${getImageUrl(p.imageUrl)}" onerror="this.parentNode.textContent='🌾'" />`
                        : '🌾'}
                </div>
                <div class="suggestion-text">
                    <div class="suggestion-name">${highlight(p.name, keyword)}</div>
                    <div class="suggestion-price">${p.weight} — ${formatPrice(p.price)}</div>
                </div>
            </div>
        `).join('')}`;

    highlightedIndex = -1;
    box.classList.add('open');
}

function selectSuggestion(productName, productId) {
    const input = document.getElementById('searchInput');
    if (input) input.value = productName;
    closeSuggestions();

    // Load filtered results then scroll to specific product
    loadProducts(0, productName).then(() => {
        if (productId) {
            setTimeout(() => {
                const card = document.querySelector(`[data-product-id="${productId}"]`);
                if (card) {
                    card.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    card.style.boxShadow = '0 0 0 3px #c8922a';
                    setTimeout(() => { card.style.boxShadow = ''; }, 2000);
                }
            }, 400);
        }
    });
}

function handleSearchKeydown(e) {
    const box   = document.getElementById('searchSuggestions');
    const items = box ? box.querySelectorAll('.suggestion-item') : [];
    if (e.key === 'ArrowDown') {
        e.preventDefault();
        highlightedIndex = Math.min(highlightedIndex + 1, items.length - 1);
        highlightSuggestion(highlightedIndex);
    } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        highlightedIndex = Math.max(highlightedIndex - 1, 0);
        highlightSuggestion(highlightedIndex);
    } else if (e.key === 'Enter') {
        if (highlightedIndex >= 0 && items[highlightedIndex]) items[highlightedIndex].click();
        else closeSuggestions();
    } else if (e.key === 'Escape') {
        closeSuggestions();
    }
}

function highlightSuggestion(index) {
    document.querySelectorAll('.suggestion-item').forEach((el, i) =>
        el.classList.toggle('highlighted', i === index));
    highlightedIndex = index;
}

function closeSuggestions() {
    const box = document.getElementById('searchSuggestions');
    if (box) box.classList.remove('open');
    highlightedIndex = -1;
}

// Close suggestions when clicking outside
document.addEventListener('click', function(e) {
    if (!e.target.closest('.search-bar-wrapper')) closeSuggestions();
});


/* ════════════════════════════════════════
   FEATURE 5: RECENTLY VIEWED
════════════════════════════════════════ */
const RV_KEY = 'orivya_recently_viewed';
const RV_MAX = 8;

function trackRecentlyViewed(product) {
    try {
        let rv = JSON.parse(localStorage.getItem(RV_KEY) || '[]');
        rv = rv.filter(p => p.id !== product.id);
        rv.unshift({ id: product.id, name: product.name, price: product.price, weight: product.weight, imageUrl: product.imageUrl || '' });
        rv = rv.slice(0, RV_MAX);
        localStorage.setItem(RV_KEY, JSON.stringify(rv));
    } catch (e) {}
    renderRecentlyViewed();
}

function renderRecentlyViewed() {
    const section = document.getElementById('recentlyViewed');
    const row     = document.getElementById('rvRow');
    if (!section || !row) return;
    try {
        const rv = JSON.parse(localStorage.getItem(RV_KEY) || '[]');
        if (rv.length === 0) { section.style.display = 'none'; return; }
        section.style.display = 'block';
        row.innerHTML = rv.map(p => `
            <div class="rv-card" onclick="openRecentlyViewedProduct(${p.id})" style="cursor:pointer">
                <div class="rv-card-image">
                    ${p.imageUrl
                        ? `<img src="${getImageUrl(p.imageUrl)}" alt="${escapeStr(p.name)}"
                                onerror="this.parentNode.textContent='🌾'" />`
                        : '🌾'}
                </div>
                <div class="rv-card-info">
                    <div class="rv-card-name">${p.name}</div>
                    <div class="rv-card-price">${formatPrice(p.price)}</div>
                </div>
            </div>`
        ).join('');
    } catch (e) { section.style.display = 'none'; }
}

/**
 * openRecentlyViewedProduct — scrolls to shop and highlights specific product.
 * ROOT CAUSE FIX: onclick used scrollToShop() which ignored the product ID,
 * so clicking any recently viewed product always opened the first product shown.
 */
function openRecentlyViewedProduct(productId) {
    // Scroll to shop section
    const shopSection = document.getElementById('shop');
    if (shopSection) {
        shopSection.scrollIntoView({ behavior: 'smooth' });
    }

    // After scroll, find and highlight/click the specific product card
    setTimeout(() => {
        // Look for product card with matching data-id
        const productCard = document.querySelector(`[data-product-id="${productId}"]`);
        if (productCard) {
            productCard.scrollIntoView({ behavior: 'smooth', block: 'center' });
            // Flash highlight to show which product
            productCard.style.boxShadow = '0 0 0 3px #c8922a';
            setTimeout(() => { productCard.style.boxShadow = ''; }, 2000);
        } else {
            // Product card not visible — load it then open buy modal
            loadProducts(0, '').then(() => {
                setTimeout(() => {
                    const card = document.querySelector(`[data-product-id="${productId}"]`);
                    if (card) {
                        card.scrollIntoView({ behavior: 'smooth', block: 'center' });
                        card.style.boxShadow = '0 0 0 3px #c8922a';
                        setTimeout(() => { card.style.boxShadow = ''; }, 2000);
                    }
                }, 500);
            });
        }
    }, 600);
}

function clearRecentlyViewed() {
    localStorage.removeItem(RV_KEY);
    const section = document.getElementById('recentlyViewed');
    if (section) section.style.display = 'none';
}


/* ════════════════════════════════════════
   FEATURE 6: TESTIMONIALS COUNT-UP
════════════════════════════════════════ */
function startCountUpAnimation() {
    document.querySelectorAll('.t-stat-number').forEach(el => {
        const target    = parseFloat(el.getAttribute('data-target'));
        const isDecimal = target % 1 !== 0;
        const steps     = 60;
        const increment = target / steps;
        let step        = 0;
        const timer = setInterval(() => {
            step++;
            const current = increment * step;
            if (step >= steps) {
                clearInterval(timer);
                if (target >= 1000)    el.textContent = Math.round(target).toLocaleString('en-IN') + '+';
                else if (isDecimal)    el.textContent = target.toFixed(1);
                else                   el.textContent = Math.round(target) + '+';
                return;
            }
            if (target >= 1000)        el.textContent = Math.round(current).toLocaleString('en-IN') + '+';
            else if (isDecimal)        el.textContent = current.toFixed(1);
            else                       el.textContent = Math.round(current) + '+';
        }, 2000 / steps);
    });
}

// Animate only when testimonials section scrolls into view
const testimonialsObserver = new IntersectionObserver(entries => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            startCountUpAnimation();
            testimonialsObserver.disconnect();
        }
    });
}, { threshold: 0.3 });

document.addEventListener('DOMContentLoaded', function() {
    const t = document.getElementById('testimonials');
    if (t) testimonialsObserver.observe(t);
});


/* ════════════════════════════════════════
   FEATURE 7: WHATSAPP POPUP (10 seconds)
════════════════════════════════════════ */
(function initWaPopup() {
    if (sessionStorage.getItem('wa_popup_dismissed')) return;
    setTimeout(() => {
        const popup = document.getElementById('waPopup');
        if (popup) popup.classList.add('visible');
    }, 10000);
})();

function closeWaPopup() {
    const popup = document.getElementById('waPopup');
    if (popup) popup.classList.remove('visible');
    sessionStorage.setItem('wa_popup_dismissed', '1');
}

function openWaFromPopup() {
    closeWaPopup();
    const msg = encodeURIComponent('🌾 Hello Orivya Rice! I need help choosing rice. Can you help?');
    window.open(`https://wa.me/916304212346?text=${msg}`, '_blank');
}

// Auto-close popup when user scrolls past hero
window.addEventListener('scroll', function() {
    if (window.scrollY > 500) {
        const popup = document.getElementById('waPopup');
        if (popup && popup.classList.contains('visible')) closeWaPopup();
    }
});


/* ════════════════════════════════════════
   FEATURE 3: IMAGE ZOOM
════════════════════════════════════════ */
function openImageZoom(imageUrl, productName) {
    if (!imageUrl || imageUrl.includes('rice-placeholder')) return;
    const overlay = document.getElementById('imgModalOverlay');
    const img     = document.getElementById('imgModalImg');
    if (!overlay || !img) return;
    img.src = imageUrl;
    img.alt = productName;
    overlay.classList.add('open');
    document.body.style.overflow = 'hidden';
}

function closeImageZoom() {
    const overlay = document.getElementById('imgModalOverlay');
    if (overlay) overlay.classList.remove('open');
    document.body.style.overflow = '';
}

document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') closeImageZoom();
});


/* ════════════════════════════════════════
   CART
════════════════════════════════════════ */

/**
 * loadCartCount — fetches cart and updates the badge number on nav.
 * ROOT CAUSE FIX: This function was called but never defined — cart
 * count always stayed at 0 because nothing was updating the badge.
 */
async function loadCartCount() {
    try {
        if (!isLoggedIn()) {
            document.getElementById('cartCount').textContent = '0';
            return;
        }
        const data = await apiGetCart();
        const count = data?.data?.totalItems ?? 0;
        document.getElementById('cartCount').textContent = count;
    } catch (e) {
        // Silent fail — don't disrupt page on cart count error
        document.getElementById('cartCount').textContent = '0';
    }
}

async function handleAddToCart(productId, productName) {
    if (!requireLogin()) return;
    showLoading(true);
    try {
        await apiAddToCart(productId, 1);
        await loadCartCount();
        showToast(`✅ ${productName} added to cart!`);
    } catch (e) {
        showToast('Failed to add: ' + e.message, 'error');
    } finally {
        showLoading(false);
    }
}

function openCart() {
    document.getElementById('cartOverlay').classList.add('open');
    document.getElementById('cartSidebar').classList.add('open');
    renderCart();
}

function closeCart() {
    document.getElementById('cartOverlay').classList.remove('open');
    document.getElementById('cartSidebar').classList.remove('open');
}

async function renderCart() {
    const itemsEl  = document.getElementById('cartItems');
    const footerEl = document.getElementById('cartFooter');

    if (!isLoggedIn()) {
        itemsEl.innerHTML = `
            <div class="cart-empty">
                <div class="emoji">🔒</div>
                <p>Please <a href="login.html" style="color:#1a5c2a;font-weight:700">login</a>
                   to view your cart.</p>
            </div>`;
        footerEl.style.display = 'none';
        return;
    }

    itemsEl.innerHTML = `<div style="text-align:center;padding:30px"><div class="spinner"></div></div>`;

    try {
        const data = await apiGetCart();
        const cart = data.data;

        if (!cart || cart.items.length === 0) {
            itemsEl.innerHTML = `
                <div class="cart-empty">
                    <div class="emoji">🛒</div>
                    <p>Your cart is empty.<br />Add some premium rice!</p>
                </div>`;
            footerEl.style.display = 'none';
            return;
        }

        itemsEl.innerHTML = cart.items.map(item => `
            <div class="cart-item">
                <div class="cart-item-img">
                    ${item.productImage
                        ? `<img src="${getImageUrl(item.productImage)}" alt="${item.productName}"
                                onerror="this.style.display='none';this.parentNode.textContent='🌾'"/>`
                        : '🌾'}
                </div>
                <div class="cart-item-info">
                    <div class="cart-item-name">${item.productName}</div>
                    <div style="font-size:12px;color:#888;margin-bottom:4px">${item.weight}</div>
                    <div class="cart-item-price">${formatPrice(item.unitPrice)}</div>
                    <div class="cart-item-qty">
                        <button class="qty-btn" onclick="changeQty(${item.cartItemId}, ${item.quantity - 1})">−</button>
                        <span class="qty-val">${item.quantity}</span>
                        <button class="qty-btn" onclick="changeQty(${item.cartItemId}, ${item.quantity + 1})">+</button>
                    </div>
                </div>
                <button class="remove-item" onclick="removeItem(${item.cartItemId})">🗑</button>
            </div>
        `).join('');

        document.getElementById('cartTotal').textContent = formatPrice(cart.totalAmount);
        document.getElementById('cartCount').textContent = cart.totalItems;
        footerEl.style.display = 'block';

    } catch (e) {
        itemsEl.innerHTML = `<div class="cart-empty"><p>Error loading cart.<br><small>${e.message}</small></p></div>`;
    }
}

async function changeQty(cartItemId, qty) {
    showLoading(true);
    try {
        await apiUpdateCartItem(cartItemId, qty);
        await renderCart();
        await loadCartCount();
    } catch (e) {
        showToast(e.message, 'error');
    } finally {
        showLoading(false);
    }
}

async function removeItem(cartItemId) {
    showLoading(true);
    try {
        await apiRemoveCartItem(cartItemId);
        await renderCart();
        await loadCartCount();
        showToast('Item removed.');
    } catch (e) {
        showToast(e.message, 'error');
    } finally {
        showLoading(false);
    }
}

function goToCheckout() {
    if (!requireLogin()) return;
    closeCart();
    openCheckout();
}


/* ════════════════════════════════════════
   CHECKOUT
════════════════════════════════════════ */
/* ════════════════════════════════════════
   FIRST ORDER DISCOUNT HELPER
   Calls backend to count user's previous orders.
   Returns true if this is their first order.
════════════════════════════════════════ */
/* ════════════════════════════════════════
   DELIVERY DISTANCE HELPER
   Reads the distance value from the hidden input
   in checkout (customer enters their km distance).
   Defaults to 0 if not entered = free delivery.
════════════════════════════════════════ */
/* ════════════════════════════════════════
   LIVE DELIVERY CHARGE INFO
   Called oninput on the distance field.
   Shows "Free" or "Rs.60" message instantly
   as customer types their distance.
════════════════════════════════════════ */
/* ════════════════════════════════════════════════════════════
   renderCheckoutSummary()
   ────────────────────────────────────────────────────────────
   NEW shared helper — called by both openCheckout() and
   updateDeliveryCharge() so both always show the same numbers.

   Uses the stored _checkout* variables + current distance input.
════════════════════════════════════════════════════════════ */
function renderCheckoutSummary() {
    const cartTotal   = _checkoutCartTotal;
    const isFirstOrder = _checkoutIsFirst;
    if (!cartTotal) return;   // checkout not open yet

    const DISCOUNT       = 50;
    const FREE_MIN       = 600;
    const MAX_FREE_KM    = 15;
    const DELIVERY_CHARGE = 60;

    const distanceKm   = getDeliveryDistance();
    const withinRadius = (distanceKm <= MAX_FREE_KM);
    // Free delivery: cart >= ₹600 AND within 15 km
    // If distance = 0 (not entered yet) treat as local (free)
    const freeDelivery = distanceKm === 0
        ? (cartTotal >= FREE_MIN)
        : (cartTotal >= FREE_MIN && withinRadius);
    const deliveryCharge = freeDelivery ? 0 : DELIVERY_CHARGE;

    // ── First-order discount banner ─────────────────────────
    const discountBanner = (isFirstOrder && cartTotal >= DISCOUNT)
        ? `<div style="background:#e8f5e9;border:1px solid #c3e6cb;border-radius:6px;padding:10px 14px;margin-top:8px;display:flex;justify-content:space-between;align-items:center">
              <span style="color:#155724;font-weight:700;font-size:13px">🎉 First Order Discount</span>
              <span style="color:#155724;font-weight:900;font-size:14px">- ₹50</span>
           </div>`
        : '';

    // ── Delivery banner — shows ₹0 or + ₹60 ───────────────
    let deliveryBanner;
    if (distanceKm === 0) {
        // Distance not entered yet — show neutral message
        deliveryBanner = `<div style="background:#f9f9f9;border:1px solid #eee;border-radius:6px;padding:10px 14px;margin-top:8px;display:flex;justify-content:space-between;align-items:center">
              <span style="color:#888;font-size:13px">🚚 Enter distance above to calculate delivery</span>
              <span style="color:#888;font-size:13px">—</span>
           </div>`;
    } else if (freeDelivery) {
        deliveryBanner = `<div style="background:#e8f5e9;border:1px solid #c3e6cb;border-radius:6px;padding:10px 14px;margin-top:8px;display:flex;justify-content:space-between;align-items:center">
              <span style="color:#155724;font-weight:700;font-size:13px">🚚 Free Delivery (within ${MAX_FREE_KM} km)</span>
              <span style="color:#155724;font-weight:900;font-size:14px">₹0</span>
           </div>`;
    } else {
        const reason = !withinRadius
            ? '(beyond 15 km from our mill)'
            : '(add ₹' + (FREE_MIN - cartTotal).toFixed(0) + ' more for free delivery)';
        deliveryBanner = `<div style="background:#fff8e1;border:1px solid #ffe082;border-radius:6px;padding:10px 14px;margin-top:8px;display:flex;justify-content:space-between;align-items:center">
              <span style="color:#7a4a1e;font-weight:700;font-size:13px">🚚 Delivery Charge ${reason}</span>
              <span style="color:#7a4a1e;font-weight:900;font-size:14px">+ ₹60</span>
           </div>`;
    }

    // ── Final total ─────────────────────────────────────────
    let finalAmount = cartTotal;
    if (isFirstOrder && finalAmount >= DISCOUNT) finalAmount -= DISCOUNT;
    finalAmount += deliveryCharge;

    // ── Write to DOM ────────────────────────────────────────
    document.getElementById('checkoutSummary').innerHTML =
        _checkoutSummaryHTML + discountBanner + deliveryBanner;
    document.getElementById('checkoutTotalLine').innerHTML =
        `<span>Total</span><span>${formatPrice(finalAmount)}</span>`;
    document.getElementById('payTotal').textContent  = formatPrice(finalAmount);
    document.getElementById('upiAmount').textContent = formatPrice(finalAmount);
}

/* ════════════════════════════════════════════════════════════
   updateDeliveryCharge()
   ────────────────────────────────────────────────────────────
   Called by oninput on #deliveryDistanceKm.
   FIX: now calls renderCheckoutSummary() so the order summary
   and Total always stay in sync with the typed distance.
════════════════════════════════════════════════════════════ */
function updateDeliveryCharge() {
    const infoBox = document.getElementById('deliveryChargeInfo');
    if (!infoBox) return;

    const km          = getDeliveryDistance();
    const cartTotal   = _checkoutCartTotal || 0;
    const FREE_MIN    = 600;
    const MAX_FREE_KM = 15;

    // ── Small info box below distance input ─────────────────
    if (km === 0) {
        infoBox.innerHTML         = '🚚 Enter distance to see delivery charge';
        infoBox.style.background  = '#f9f9f9';
        infoBox.style.borderColor = '#eee';
        infoBox.style.color       = '#555';
    } else if (km <= MAX_FREE_KM && cartTotal >= FREE_MIN) {
        infoBox.innerHTML         = '✅ <strong style="color:#155724">Free Delivery!</strong> Your order qualifies (above ₹600 &amp; within 15 km)';
        infoBox.style.background  = '#e8f5e9';
        infoBox.style.borderColor = '#c3e6cb';
        infoBox.style.color       = '#155724';
    } else if (km > MAX_FREE_KM) {
        infoBox.innerHTML         = '🚚 <strong style="color:#7a4a1e">Delivery Charge: ₹60</strong> — Location is beyond 15 km from our mill';
        infoBox.style.background  = '#fff8e1';
        infoBox.style.borderColor = '#ffe082';
        infoBox.style.color       = '#7a4a1e';
    } else {
        const needed = (FREE_MIN - cartTotal).toFixed(0);
        infoBox.innerHTML         = `🚚 <strong style="color:#7a4a1e">Delivery Charge: ₹60</strong> — Add ₹${needed} more for free delivery`;
        infoBox.style.background  = '#fff8e1';
        infoBox.style.borderColor = '#ffe082';
        infoBox.style.color       = '#7a4a1e';
    }

    // ── FIX: Re-render order summary + total with new distance ──
    // This is the line that was MISSING — without this, the summary
    // never updated when user typed a distance.
    renderCheckoutSummary();
}

function getDeliveryDistance() {
    const input = document.getElementById('deliveryDistanceKm');
    if (!input) return 0;
    const val = parseFloat(input.value);
    return isNaN(val) ? 0 : Math.max(0, val);
}

/* ── CHECKOUT STATE ─────────────────────────────────────────────────
   Stored when openCheckout() loads cart so that updateDeliveryCharge()
   can re-render the order summary whenever the user changes distance.
   ─────────────────────────────────────────────────────────────────── */
let _checkoutCartTotal  = 0;       // raw cart total before any discount/charge
let _checkoutIsFirst    = false;   // true if this is user's first order
let _checkoutSummaryHTML = '';     // product rows HTML (no banners)

async function checkIsFirstOrder() {
    try {
        if (!isLoggedIn()) return false;
        // Use existing getMyOrders API — if empty array, it's first order
        const data = await apiGetMyOrders();
        const orders = data.data || [];
        return orders.length === 0;
    } catch (e) {
        return false; // If any error, don't apply discount — safe default
    }
}

async function openCheckout() {
    if (!requireLogin()) return;
    const user = getUser();
    if (user.name)  document.getElementById('checkName').value  = user.name;
    if (user.phone) document.getElementById('checkPhone').value = user.phone;

    try {
        const data = await apiGetCart();
        const cart = data.data;
        if (!cart || cart.items.length === 0) { showToast('Your cart is empty!', 'error'); return; }

        const summaryHTML = cart.items.map(i =>
            `<div style="display:flex;justify-content:space-between;margin-bottom:6px;font-size:14px">
                <span>${i.productName} <span style="color:#999">(${i.weight}) ×${i.quantity}</span></span>
                <strong>${formatPrice(i.subtotal)}</strong>
             </div>`
        ).join('');

        // ── FIRST ORDER DISCOUNT ──────────────────────────────
        const isFirstOrder = await checkIsFirstOrder();

        // ── STORE CART STATE for updateDeliveryCharge() to reuse ──────
        // This is the key fix: store these values so whenever the user
        // changes the distance input, updateDeliveryCharge() can
        // recalculate AND re-render the full order summary + totals.
        _checkoutCartTotal   = cart.totalAmount;
        _checkoutIsFirst     = isFirstOrder;
        _checkoutSummaryHTML = summaryHTML;   // product rows only, no banners

        // ── Render summary using the shared helper ─────────────────────
        // renderCheckoutSummary() uses the stored values + current distance
        renderCheckoutSummary();
    } catch (e) {
        showToast('Error loading cart: ' + e.message, 'error');
        return;
    }

    document.getElementById('checkStep1').style.display   = 'block';
    document.getElementById('checkStep2').style.display   = 'none';
    document.getElementById('checkoutBody').style.display = 'block';
    document.getElementById('successState').style.display = 'none';
    setStep(1);
    document.getElementById('checkoutOverlay').classList.add('open');
}

function closeCheckout() {
    document.getElementById('checkoutOverlay').classList.remove('open');
}

function setStep(step) {
    document.getElementById('step1ind').className = 'step-pill' + (step === 1 ? ' active' : ' done');
    document.getElementById('step2ind').className = 'step-pill' + (step === 2 ? ' active' : '');
}

function goToPayment() {
    const name    = document.getElementById('checkName').value.trim();
    const phone   = document.getElementById('checkPhone').value.trim();
    const address = document.getElementById('checkAddress').value.trim();
    if (!name)    { showToast('Please enter your name.', 'error');    return; }
    if (!phone)   { showToast('Please enter your phone.', 'error');   return; }
    if (!address) { showToast('Please enter your address.', 'error'); return; }
    document.getElementById('checkStep1').style.display = 'none';
    document.getElementById('checkStep2').style.display = 'block';
    setStep(2);
}

function backToDetails() {
    document.getElementById('checkStep2').style.display = 'none';
    document.getElementById('checkStep1').style.display = 'block';
    setStep(1);
}

function selectPayment(method) {
    selectedPayMethod = method;
    ['cod', 'upi'].forEach(m => {
        const lbl = document.getElementById('lbl-' + m);
        if (lbl) lbl.style.border = (m === method) ? '2px solid #1a5c2a' : '2px solid transparent';
    });
    document.getElementById('upiBox').style.display = (method === 'upi') ? 'block' : 'none';
}

async function placeOrder() {
    const name    = document.getElementById('checkName').value.trim();
    const phone   = document.getElementById('checkPhone').value.trim();
    const address = document.getElementById('checkAddress').value.trim();
    const btn     = document.getElementById('placeOrderBtn');

    let transactionId = '';
    if (selectedPayMethod === 'upi') {
        transactionId = document.getElementById('utrNumber').value.trim();
        if (!transactionId) { showToast('Please enter your UPI Transaction ID.', 'error'); return; }
    }

    btn.textContent = '⏳ Placing order...';
    btn.disabled    = true;

    try {
        const distanceKm = getDeliveryDistance();
        const data  = await apiPlaceOrder(address, selectedPayMethod.toUpperCase(), transactionId, distanceKm);
        const order = data.data;

        const waMsg = encodeURIComponent(
            `🌾 *Order Placed — Orivya Rice*\n\n` +
            `📦 Order: ${order.orderId}\n👤 Name: ${name}\n📞 Phone: ${phone}\n` +
            `📍 Address: ${address}\n💰 Total: ${formatPrice(order.totalPrice)}\n` +
            `💳 Payment: ${order.paymentMethod}\n\nPlease confirm my order. Thank you! 🙏`
        );

        document.getElementById('whatsappLink').href = `https://wa.me/916304212346?text=${waMsg}`;
        const discountLine = (order.discountApplied)
            ? `<br><span style="color:#1a5c2a;font-weight:700">🎉 ₹50 First Order Discount Applied!</span>`
            : '';

        const deliveryLine = (order.freeDelivery)
            ? `<br><span style="color:#1a5c2a;font-weight:700">🚚 Free Delivery!</span>`
            : `<br><span style="color:#856404;font-weight:700">🚚 Delivery Charge: ₹60</span>`;

        document.getElementById('successMsg').innerHTML =
            `Thank you, <strong>${name}</strong>!<br>` +
            `Order <strong>#${order.orderId}</strong> placed.` +
            discountLine +
            deliveryLine +
            `<br>Total Paid: <strong>${formatPrice(order.totalPrice)}</strong>`;

        document.getElementById('checkoutBody').style.display = 'none';
        document.getElementById('successState').style.display = 'block';
        await loadCartCount();

    } catch (e) {
        showToast('❌ Order failed: ' + e.message, 'error');
    } finally {
        btn.textContent = '✅ Place Order';
        btn.disabled    = false;
    }
}


/* ════════════════════════════════════════
   BULK ORDER → WhatsApp
════════════════════════════════════════ */
function submitBulkOrder() {
    const name    = document.getElementById('orderName').value.trim();
    const phone   = document.getElementById('orderPhone').value.trim();
    const rice    = document.getElementById('orderRice').value;
    const qty     = document.getElementById('orderQty').value.trim();
    const address = document.getElementById('orderAddress').value.trim();

    if (!name || !phone || !rice || !qty || !address) {
        showToast('Please fill all fields.', 'error');
        return;
    }

    const msg = encodeURIComponent(
        `🌾 *Bulk Order Enquiry — Orivya Rice*\n\n` +
        `👤 Name: ${name}\n📞 Phone: ${phone}\n` +
        `🌾 Rice: ${rice}\n📦 Qty: ${qty} kg\n📍 Address: ${address}\n\n` +
        `Please confirm price and availability. Thank you!`
    );
    window.open(`https://wa.me/916304212346?text=${msg}`, '_blank');

    ['orderName', 'orderPhone', 'orderQty', 'orderAddress'].forEach(id => {
        document.getElementById(id).value = '';
    });
    document.getElementById('orderRice').value = '';
    showToast('✅ Opening WhatsApp for bulk order...');
}
