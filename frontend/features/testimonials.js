/* ══════════════════════════════════════════════════════
   FEATURE 6: Count-Up Animation for Stats — JS
   FILE: features/testimonials.js

   WHERE TO PLACE:
   ─────────────────────────────────────────────────────
   Add these functions to the BOTTOM of main.js
   ══════════════════════════════════════════════════════ */

// === ADD TO main.js — paste at the BOTTOM ===

/* Count-up animation for statistics numbers */
function startCountUpAnimation() {
    const statEls = document.querySelectorAll('.t-stat-number');
    if (statEls.length === 0) return;

    statEls.forEach(el => {
        const target   = parseFloat(el.getAttribute('data-target'));
        const isDecimal = target % 1 !== 0;
        const duration = 2000;  // 2 seconds
        const steps    = 60;
        const increment = target / steps;
        let current    = 0;
        let step       = 0;

        const timer = setInterval(() => {
            step++;
            current = increment * step;
            if (step >= steps) {
                current = target;
                clearInterval(timer);
            }
            // Format nicely
            if (target >= 1000) {
                el.textContent = Math.round(current).toLocaleString('en-IN') + '+';
            } else if (isDecimal) {
                el.textContent = current.toFixed(1);
            } else {
                el.textContent = Math.round(current) + '+';
            }
        }, duration / steps);
    });
}

/* Start animation when testimonials section enters viewport */
const testimonialsObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            startCountUpAnimation();
            testimonialsObserver.disconnect();  // Only animate once
        }
    });
}, { threshold: 0.3 });

/* Start observing when DOM is ready */
document.addEventListener('DOMContentLoaded', function() {
    const testimonialsSection = document.getElementById('testimonials');
    if (testimonialsSection) {
        testimonialsObserver.observe(testimonialsSection);
    }
});
