/* ══════════════════════════════════════════════════════
   FEATURE 4: Search Autocomplete — JS + HTML changes
   FILE: features/search-autocomplete.js

   WHERE TO PLACE:
   ─────────────────────────────────────────────────────
   PART A — Change in index.html
   Find the search input (around line 103):
       <input type="text" id="searchInput" class="search-input"
              placeholder="🔍 Search rice products..."
              oninput="handleSearch(this.value)" />

   Replace the entire search-bar-wrapper div with:
   ─────────────────────────────────────────────────────
   <div class="search-bar-wrapper">
     <input type="text" id="searchInput" class="search-input"
            placeholder="🔍 Search rice products..."
            oninput="handleSearchWithSuggestions(this.value)"
            onkeydown="handleSearchKeydown(event)"
            autocomplete="off" />
     <div class="search-suggestions" id="searchSuggestions"></div>
   </div>
   ─────────────────────────────────────────────────────

   PART B — Add these functions to the BOTTOM of main.js
   ─────────────────────────────────────────────────────
   Paste everything below the "=== ADD TO main.js ===" line
   ══════════════════════════════════════════════════════ */

// === ADD TO main.js — paste at the BOTTOM ===

let allProductsCache   = [];   // stores all loaded products for suggestions
let highlightedIndex   = -1;   // keyboard navigation index

/* Called when user types in the search box */
function handleSearchWithSuggestions(value) {
    const keyword = value.trim();

    // Still run the regular search (debounced)
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => loadProducts(0, keyword), 400);

    // Also show autocomplete suggestions immediately
    if (keyword.length < 2) {
        closeSuggestions();
        return;
    }
    showSuggestions(keyword);
}

/* Shows the dropdown suggestions */
function showSuggestions(keyword) {
    const box = document.getElementById('searchSuggestions');
    if (!box) return;

    // Filter products from cache
    const matches = allProductsCache.filter(p =>
        p.name.toLowerCase().includes(keyword.toLowerCase()) ||
        (p.category && p.category.toLowerCase().includes(keyword.toLowerCase()))
    ).slice(0, 6);   // max 6 suggestions

    if (matches.length === 0) {
        box.innerHTML = `
            <div class="suggestion-header">Suggestions</div>
            <div class="suggestion-empty">No products match "${keyword}"</div>`;
        box.classList.add('open');
        return;
    }

    // Highlight matching text
    function highlight(text, keyword) {
        const regex = new RegExp(`(${keyword})`, 'gi');
        return text.replace(regex, '<mark>$1</mark>');
    }

    box.innerHTML = `
        <div class="suggestion-header">Suggestions</div>
        ${matches.map((p, i) => `
            <div class="suggestion-item" id="suggestion-${i}"
                 onclick="selectSuggestion('${escapeStr(p.name)}')"
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

/* Called when user clicks a suggestion */
function selectSuggestion(productName) {
    const input = document.getElementById('searchInput');
    if (input) input.value = productName;
    closeSuggestions();
    loadProducts(0, productName);
}

/* Keyboard navigation: up/down arrows + Enter */
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
        if (highlightedIndex >= 0 && items[highlightedIndex]) {
            items[highlightedIndex].click();
        } else {
            closeSuggestions();
        }
    } else if (e.key === 'Escape') {
        closeSuggestions();
    }
}

function highlightSuggestion(index) {
    document.querySelectorAll('.suggestion-item').forEach((el, i) => {
        el.classList.toggle('highlighted', i === index);
    });
    highlightedIndex = index;
}

function closeSuggestions() {
    const box = document.getElementById('searchSuggestions');
    if (box) box.classList.remove('open');
    highlightedIndex = -1;
}

/* Close suggestions when clicking outside */
document.addEventListener('click', function(e) {
    if (!e.target.closest('.search-bar-wrapper')) {
        closeSuggestions();
    }
});

/* Cache products when they load (call this in loadProducts()) */
/* In main.js loadProducts(), inside the try block AFTER renderProducts():
   allProductsCache = [...allProductsCache, ...pageData.content];
   // Remove duplicates by id:
   allProductsCache = allProductsCache.filter((p, i, arr) =>
       arr.findIndex(x => x.id === p.id) === i);
*/
