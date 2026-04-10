/* ══════════════════════════════════════════════════════
   FEATURE 7: WhatsApp Popup — HTML + JS
   FILE: features/whatsapp-popup.js

   WHERE TO PLACE:
   ─────────────────────────────────────────────────────
   PART A — Add HTML to index.html
   Find the FLOATING WHATSAPP section (around line 280 in index.html).
   ADD this block BEFORE the existing floating WA button div:

   <!-- WhatsApp Popup (shown after 10 seconds) -->
   <div class="wa-popup" id="waPopup">
     <div class="wa-popup-header">
       <div class="wa-popup-avatar">🌾</div>
       <div>
         <div class="wa-popup-name">Orivya Rice</div>
         <div class="wa-popup-status">Online now</div>
       </div>
       <button class="wa-popup-close" onclick="closeWaPopup()">✕</button>
     </div>
     <div class="wa-popup-body">
       <div class="wa-chat-bubble">
         👋 Hi! Need help choosing the right rice?
         We have Sona Masoori, Raw Rice, and Broken Rice!
         <div class="wa-chat-time">Just now</div>
       </div>
     </div>
     <div class="wa-popup-footer">
       <button class="wa-popup-btn" onclick="openWaFromPopup()">
         💬 Chat with us on WhatsApp
       </button>
     </div>
   </div>

   ─────────────────────────────────────────────────────
   PART B — Add these functions to the BOTTOM of main.js
   ══════════════════════════════════════════════════════ */

// === ADD TO main.js — paste at the BOTTOM ===

/* Show WhatsApp popup after 10 seconds (only once per session) */
(function initWaPopup() {
    // Only show if user hasn't dismissed it this session
    if (sessionStorage.getItem('wa_popup_dismissed')) return;

    setTimeout(() => {
        const popup = document.getElementById('waPopup');
        if (popup) {
            popup.classList.add('visible');
        }
    }, 10000);  // 10 seconds
})();

function closeWaPopup() {
    const popup = document.getElementById('waPopup');
    if (popup) popup.classList.remove('visible');
    // Remember for this session
    sessionStorage.setItem('wa_popup_dismissed', '1');
}

function openWaFromPopup() {
    closeWaPopup();
    const msg = encodeURIComponent(
        '🌾 Hello Orivya Rice! I need help choosing the right rice product. Can you help?'
    );
    window.open(`https://wa.me/919640525341?text=${msg}`, '_blank');
}

/* Also close popup when user opens cart or scrolls down a lot */
window.addEventListener('scroll', function() {
    if (window.scrollY > 600) {
        const popup = document.getElementById('waPopup');
        if (popup && popup.classList.contains('visible')) {
            closeWaPopup();
        }
    }
});
