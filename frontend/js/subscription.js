/* ══════════════════════════════════════════════════════════
   subscription.js — Subscription page logic
   Standalone file — no impact on existing scripts
   ══════════════════════════════════════════════════════════ */

'use strict';

/* ── PRICING TABLE ──────────────────────────────────────── */
const PRICES = {
  rice: {
    '5':  { monthly: 299,  weekly: 89  },
    '10': { monthly: 549,  weekly: 159 },
    '25': { monthly: 1199, weekly: 349 }
  },
  milk: {
    '500':  { daily: 39,  alternate: 22 },
    '1000': { daily: 69,  alternate: 39 },
    '2000': { daily: 129, alternate: 72 }
  }
};

/* ── CAROUSEL STATE ─────────────────────────────────────── */
const carouselState = { rice: 0, milk: 0 };

/* ── MODAL STATE ────────────────────────────────────────── */
let currentSub = {};

/* ═══════════════════════════════════════════════════════════
   PARTICLE CANVAS  —  green/gold floating particles
═══════════════════════════════════════════════════════════ */
(function initParticles() {
    const canvas = document.getElementById('particleCanvas');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    let W = canvas.width  = window.innerWidth;
    let H = canvas.height = window.innerHeight;

    const particles = Array.from({ length: 60 }, () => ({
        x: Math.random() * W,
        y: Math.random() * H,
        r: Math.random() * 2.5 + 0.5,
        vx: (Math.random() - 0.5) * 0.3,
        vy: (Math.random() - 0.5) * 0.3,
        alpha: Math.random() * 0.4 + 0.1,
        color: Math.random() > 0.5
            ? `rgba(26,92,42,`
            : `rgba(200,146,42,`
    }));

    function draw() {
        ctx.clearRect(0, 0, W, H);
        particles.forEach(p => {
            ctx.beginPath();
            ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
            ctx.fillStyle = p.color + p.alpha + ')';
            ctx.fill();
            p.x += p.vx; p.y += p.vy;
            if (p.x < 0) p.x = W;
            if (p.x > W) p.x = 0;
            if (p.y < 0) p.y = H;
            if (p.y > H) p.y = 0;
        });
        requestAnimationFrame(draw);
    }

    draw();
    window.addEventListener('resize', () => {
        W = canvas.width  = window.innerWidth;
        H = canvas.height = window.innerHeight;
    });
})();

/* ═══════════════════════════════════════════════════════════
   3D TILT EFFECT  —  mouse-move on cards
═══════════════════════════════════════════════════════════ */
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.sub-card').forEach(card => {
        card.addEventListener('mousemove', (e) => {
            const rect   = card.getBoundingClientRect();
            const cx     = rect.left + rect.width  / 2;
            const cy     = rect.top  + rect.height / 2;
            const dx     = (e.clientX - cx) / (rect.width  / 2);
            const dy     = (e.clientY - cy) / (rect.height / 2);
            const rotY   =  dx * 10;
            const rotX   = -dy * 6;
            card.style.transform = `translateY(-12px) rotateX(${rotX}deg) rotateY(${rotY}deg) scale(1.02)`;
        });

        card.addEventListener('mouseleave', () => {
            card.style.transition = 'transform 0.5s ease';
            card.style.transform  = '';
            setTimeout(() => card.style.transition = '', 500);
        });

        card.addEventListener('mouseenter', () => {
            card.style.transition = 'transform 0.1s linear';
        });
    });

    /* Set min date for start date picker */
    const dateInput = document.getElementById('subStartDate');
    if (dateInput) {
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        dateInput.min   = tomorrow.toISOString().split('T')[0];
        dateInput.value = tomorrow.toISOString().split('T')[0];
    }
});

/* ═══════════════════════════════════════════════════════════
   CATEGORY SWITCH
═══════════════════════════════════════════════════════════ */
function switchCategory(cat, btn) {
    /* Tab styles */
    document.querySelectorAll('.sub-tab').forEach(t => t.classList.remove('active'));
    btn.classList.add('active');

    /* Show/hide card stages */
    document.getElementById('riceCards').style.display = cat === 'rice' ? 'block' : 'none';
    document.getElementById('milkCards').style.display = cat === 'milk' ? 'block' : 'none';
}

/* ═══════════════════════════════════════════════════════════
   FREQUENCY PILL SELECT + PRICE UPDATE
═══════════════════════════════════════════════════════════ */
function selectFreq(btn, cardKey) {
    /* Update pill active state within the same card */
    const card = btn.closest('.sub-card');
    card.querySelectorAll('.freq-pill').forEach(p => p.classList.remove('active'));
    btn.classList.add('active');

    /* Recalculate price */
    const freq   = btn.getAttribute('data-freq');
    const type   = card.getAttribute('data-type');
    const qty    = card.getAttribute('data-qty');
    const unit   = card.getAttribute('data-unit');
    const priceEl = card.querySelector('.price-amount');
    const perEl   = card.querySelector('.price-per');

    if (!PRICES[type]) return;
    const qtyKey  = unit === 'ml' ? String(parseInt(qty)) : String(qty);
    const tableRow = PRICES[type][qtyKey];
    if (!tableRow) return;

    const price = tableRow[freq];
    if (price === undefined) return;

    priceEl.textContent = '₹' + price.toLocaleString('en-IN');

    /* Update "per" label */
    const perLabel = {
        monthly: '/month', weekly: '/week',
        daily: '/day', alternate: '/delivery'
    };
    if (perEl) perEl.textContent = perLabel[freq] || '';
}

/* ═══════════════════════════════════════════════════════════
   CAROUSEL NAVIGATION
═══════════════════════════════════════════════════════════ */
function slideCards(cat, dir) {
    const track    = document.getElementById(cat + 'Track');
    const dotsEl   = document.getElementById(cat + 'Dots');
    const cards    = track.querySelectorAll('.sub-card');
    const total    = cards.length;
    let   current  = carouselState[cat];

    /* Only active on mobile */
    if (window.innerWidth > 900) return;

    current = Math.max(0, Math.min(total - 1, current + dir));
    carouselState[cat] = current;

    const cardW = cards[0].offsetWidth + 28; /* card width + gap */
    track.style.transform = `translateX(-${current * cardW}px)`;

    /* Update dots */
    dotsEl.querySelectorAll('.cdot').forEach((d, i) => {
        d.classList.toggle('active', i === current);
    });
}

/* ═══════════════════════════════════════════════════════════
   MODAL  —  open / close / submit
═══════════════════════════════════════════════════════════ */
function openSubModal(type, qty, freq, price) {
    currentSub = { type, qty, freq, price };

    /* Fill modal header */
    document.getElementById('modalIcon').textContent = type === 'rice' ? '🌾' : '🥛';
    document.getElementById('modalTitle').textContent =
        type === 'rice' ? 'Rice Subscription' : 'Milk Subscription';
    document.getElementById('modalSubtitle').textContent =
        type === 'rice' ? 'Fresh Sona Masoori to your doorstep' : 'Farm-fresh milk every morning';

    /* Fill summary badges */
    document.getElementById('modalQty').textContent  = qty;
    document.getElementById('modalFreq').textContent = capitalise(freq);
    document.getElementById('modalPrice').textContent = '₹' + parseInt(price).toLocaleString('en-IN');

    /* Reset form */
    document.getElementById('subAddress').value   = '';
    document.getElementById('subPhone').value     = '';
    document.getElementById('modalError').style.display   = 'none';
    document.getElementById('modalSuccess').style.display = 'none';
    document.getElementById('modalStep1').style.display        = 'block';
    document.getElementById('modalStepSuccess').style.display  = 'none';

    /* Pre-fill address from logged-in user if available */
    if (typeof isLoggedIn === 'function' && isLoggedIn()) {
        const user = typeof getUser === 'function' ? getUser() : {};
        if (user.phone) document.getElementById('subPhone').value = user.phone;
    }

    /* Open */
    document.getElementById('subModalOverlay').classList.add('open');
    document.getElementById('subModal').classList.add('open');
    document.body.style.overflow = 'hidden';
}

function closeSubModal() {
    document.getElementById('subModalOverlay').classList.remove('open');
    document.getElementById('subModal').classList.remove('open');
    document.body.style.overflow = '';
}

/* Close on Escape */
document.addEventListener('keydown', e => { if (e.key === 'Escape') closeSubModal(); });

/* ── SUBMIT SUBSCRIPTION ────────────────────────────────── */
async function submitSubscription() {
    const address   = document.getElementById('subAddress').value.trim();
    const phone     = document.getElementById('subPhone').value.trim();
    const startDate = document.getElementById('subStartDate').value;
    const btn       = document.getElementById('subSubmitBtn');
    const errEl     = document.getElementById('modalError');

    errEl.style.display = 'none';

    /* Validate */
    if (!address)             { showModalErr('⚠ Please enter your delivery address.'); return; }
    if (!phone)               { showModalErr('⚠ Please enter your phone number.'); return; }
    if (!/^\d{10}$/.test(phone)) { showModalErr('⚠ Enter a valid 10-digit phone number.'); return; }
    if (!startDate)           { showModalErr('⚠ Please select a start date.'); return; }

    /* Check login */
    if (typeof isLoggedIn === 'function' && !isLoggedIn()) {
        showModalErr('⚠ Please login to subscribe. Redirecting...');
        setTimeout(() => window.location.href = 'login.html', 1200);
        return;
    }

    btn.textContent = '⏳ Confirming...';
    btn.disabled    = true;

    try {
        const payload = {
            productType:     currentSub.type,
            quantity:        currentSub.qty,
            frequency:       currentSub.freq,
            price:           parseFloat(currentSub.price),
            startDate:       startDate,
            deliveryAddress: address,
            phone:           phone
        };

        const data = await apiCreateSubscription(payload);

        if (data.success) {
            document.getElementById('modalStep1').style.display       = 'none';
            document.getElementById('modalStepSuccess').style.display = 'block';
            document.getElementById('modalSuccessMsg').textContent =
                `Your ${capitalise(currentSub.type)} subscription of ${currentSub.qty} ` +
                `(${capitalise(currentSub.freq)}) starts on ${formatDate(startDate)}. ` +
                `We'll deliver right to your door!`;
        } else {
            showModalErr('❌ ' + (data.message || 'Subscription failed. Please try again.'));
        }
    } catch (e) {
        const msg = e.message || '';
        if (msg.includes('fetch') || msg.includes('refused')) {
            showModalErr('❌ Cannot connect to server. Please try again later.');
        } else {
            showModalErr('❌ ' + (msg || 'Subscription failed.'));
        }
    } finally {
        btn.textContent = '🔒 Confirm Subscription';
        btn.disabled    = false;
    }
}

/* ── HELPERS ────────────────────────────────────────────── */
function showModalErr(msg) {
    const el = document.getElementById('modalError');
    el.textContent   = msg;
    el.style.display = 'block';
}

function capitalise(str) {
    return str ? str.charAt(0).toUpperCase() + str.slice(1) : '';
}

function formatDate(dateStr) {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'long', year: 'numeric' });
}