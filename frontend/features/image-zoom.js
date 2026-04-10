/* ══════════════════════════════════════════════════════
   FEATURE 3: Image Zoom Modal — HTML + JS
   FILE: features/image-zoom.js

   WHERE TO PLACE:
   ─────────────────────────────────────────────────────
   PART A — Add this HTML to index.html
   Place it just BEFORE the closing </body> tag
   (above the <script> tags at the bottom)

   PART B — Add openImageZoom() function to main.js
   Paste it at the BOTTOM of main.js

   PART C — In renderProducts() (main.js),
   on the <div class="product-image"> line, add onclick:
   Change:
       <div class="product-image">
   To:
       <div class="product-image" onclick="openImageZoom('${getImageUrl(p.imageUrl)}', '${escapeStr(p.name)}')">
   ══════════════════════════════════════════════════════ */

/* ─────────────────────────────────────────────────────
   PART A — HTML to add in index.html before </body>
   ───────────────────────────────────────────────────── */
/*
<!-- Image Zoom Modal -->
<div class="img-modal-overlay" id="imgModalOverlay" onclick="closeImageZoom()">
  <button class="img-modal-close" onclick="closeImageZoom()">✕</button>
  <img id="imgModalImg" src="" alt="" onclick="event.stopPropagation()" />
</div>
*/

/* ─────────────────────────────────────────────────────
   PART B — Add these 2 functions to the BOTTOM of main.js
   ───────────────────────────────────────────────────── */

function openImageZoom(imageUrl, productName) {
    if (!imageUrl || imageUrl.endsWith('rice-placeholder.png')) return;
    const overlay = document.getElementById('imgModalOverlay');
    const img     = document.getElementById('imgModalImg');
    if (!overlay || !img) return;
    img.src = imageUrl;
    img.alt = productName;
    overlay.classList.add('open');
    document.body.style.overflow = 'hidden';  // prevent background scroll
}

function closeImageZoom() {
    const overlay = document.getElementById('imgModalOverlay');
    if (overlay) overlay.classList.remove('open');
    document.body.style.overflow = '';
}

// Close on Escape key
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') closeImageZoom();
});
