/* ══════════════════════════════════════════════════════
   api.js  —  All API calls + Auth helpers
   ══════════════════════════════════════════════════════ */

/* ── CONFIG ─────────────────────────────────────── */
// ── API BASE URL CONFIGURATION ──────────────────────────────────────────────
// Automatically picks the right backend URL based on where the frontend is running.
//
//  localhost / 127.0.0.1     → local development (mvn spring-boot:run)
//  LAN IP (e.g. 10.x.x.x)   → same machine, different port → still localhost:8080
//  Production (Render, etc.) → update PRODUCTION_URL below
//
const PRODUCTION_URL = 'https://orivya-fullstack-4.onrender.com/api';
// ↑ Replace with your actual Render URL when deploying, e.g.:
// const PRODUCTION_URL = 'https://orivya-rice-backend.onrender.com/api';

const _host = window.location.hostname;
const _isLocal = (
    _host === 'localhost' ||
    _host === '127.0.0.1' ||
    _host.startsWith('192.168.') ||   // LAN (router IP)
    _host.startsWith('10.')  ||        // LAN (e.g. 10.146.106.78)
    _host.startsWith('172.')           // LAN (Docker / VPN ranges)
);

const API_BASE = _isLocal
    ? `http://${_host.startsWith('10.') || _host.startsWith('192.168.') || _host.startsWith('172.') ? _host : 'localhost'}:8080/api`
    : PRODUCTION_URL;

console.log('[API] Backend URL:', API_BASE);

/* ── AUTH HELPERS ────────────────────────────────── */
function getToken() {
    return localStorage.getItem('orivya_token');
}

function getUser() {
    try {
        const raw = localStorage.getItem('orivya_user');
        if (!raw) return {};
        return JSON.parse(raw);
    } catch (e) {
        return {};
    }
}

function isLoggedIn() {
    const token = getToken();
    return !!(token && token.length > 10);
}

function isAdmin() {
    const user = getUser();
    return user && user.role === 'ADMIN';
}

function saveUserSession(authData) {
    // Save token
    localStorage.setItem('orivya_token', authData.token);

    // Save user info — make sure role is saved exactly as returned
    const userInfo = {
        id:    authData.userId   || authData.id,
        name:  authData.name,
        email: authData.email,
        role:  authData.role     // "ADMIN" or "CUSTOMER"
    };
    localStorage.setItem('orivya_user', JSON.stringify(userInfo));

    console.log('✅ Session saved:', userInfo); // debug
}

function clearSession() {
    localStorage.removeItem('orivya_token');
    localStorage.removeItem('orivya_user');
}

function handleLogout() {
    clearSession();
    showToast('Logged out successfully.');
    setTimeout(() => { window.location.href = 'index.html'; }, 500);
}

// Legacy alias
function apiLogout() { handleLogout(); }

/* ── REQUEST HELPERS ─────────────────────────────── */
function getHeaders() {
    const headers = { 'Content-Type': 'application/json' };
    const token = getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;
    return headers;
}

function getAuthHeader() {
    return { 'Authorization': 'Bearer ' + getToken() };
}

async function handleResponse(res) {
    // FIX: Spring Security sends HTML (not JSON) for 401 and 403 responses.
    // If we call res.json() on HTML, it throws → "Server returned invalid response"
    // Fix: check status code FIRST before attempting JSON parse.

    if (res.status === 401) {
        // Check if this is an auth API call (login/register) or a protected resource
        const isAuthEndpoint = res.url && (
            res.url.includes('/auth/login') ||
            res.url.includes('/auth/register') ||
            res.url.includes('/auth/verify') ||
            res.url.includes('/auth/resend')
        );

        if (isAuthEndpoint) {
            // Auth endpoint returning 401 = wrong password or invalid credentials
            // Do NOT redirect — let the error message show to the user
            let data;
            try { data = await res.clone().json(); } catch(e) { data = {}; }
            throw new Error(data.message || 'Invalid email or password.');
        }

        // Protected endpoint returning 401 = session expired
        clearSession();
        if (!window.location.href.includes('login.html')) {
            showToast && showToast('Session expired. Please login again.', 'error');
            setTimeout(() => window.location.href = 'login.html', 1000);
        }
        throw new Error('Session expired. Please login again.');
    }

    if (res.status === 403) {
        // Logged in but wrong role, OR token has role mismatch.
        // Tell user to logout and login again so a fresh token is issued.
        throw new Error(
            'Access denied (403). If you are admin, please logout and login again to refresh your session.'
        );
    }

    let data;
    try {
        data = await res.json();
    } catch (e) {
        throw new Error('Server returned invalid response. Status: ' + res.status);
    }
    if (!res.ok) {
        throw new Error(data.message || 'Request failed with status ' + res.status);
    }
    return data;
}

/* ══════════════════════════════════════════════════════
   AUTH APIs
   ══════════════════════════════════════════════════════ */

async function apiLogin(email, password) {
    const res = await fetch(`${API_BASE}/auth/login`, {
        method:  'POST',
        headers: getHeaders(),
        body:    JSON.stringify({ email, password })
    });
    const data = await handleResponse(res);
    // NOTE: Do NOT save session here.
    // Session is saved only after OTP is verified (apiVerifyLoginOtp).
    // This enforces the 2-step login security.
    return data;
}

// OLD simple register (kept for backward compatibility)
async function apiRegister(name, email, password, phone) {
    return apiRegisterFull({ name, email, password, phone, pincode:'', street:'', village:'', city:'', state:'' });
}

// NEW: Full registration with address fields + triggers OTP
async function apiRegisterFull(fields) {
    const res = await fetch(`${API_BASE}/auth/register`, {
        method:  'POST',
        headers: getHeaders(),
        body:    JSON.stringify(fields)
    });
    const data = await handleResponse(res);
    // NOTE: Do NOT save session here.
    // Session is saved only after email OTP is verified (apiVerifyRegistrationOtp).
    return data;
}

// Verify registration OTP — marks user as VERIFIED in DB
async function apiVerifyRegistrationOtp(email, otp) {
    const res = await fetch(`${API_BASE}/auth/verify-registration`, {
        method:  'POST',
        headers: getHeaders(),
        body:    JSON.stringify({ email, otp })
    });
    return handleResponse(res);
}

// Verify login OTP — returns JWT token if correct
async function apiVerifyLoginOtp(email, otp) {
    const res = await fetch(`${API_BASE}/auth/verify-login`, {
        method:  'POST',
        headers: getHeaders(),
        body:    JSON.stringify({ email, otp })
    });
    const data = await handleResponse(res);
    // Save session ONLY after OTP is verified
    if (data.success && data.data && data.data.token) {
        saveUserSession(data.data);
    }
    return data;
}

// Resend OTP (type = 'REGISTRATION' or 'LOGIN')
async function apiResendOtp(email, type) {
    const res = await fetch(`${API_BASE}/auth/resend-otp`, {
        method:  'POST',
        headers: getHeaders(),
        body:    JSON.stringify({ email, type })
    });
    return handleResponse(res);
}

// ── FORGOT PASSWORD — NEW (does not change existing auth functions) ──

// Step 1: Send reset OTP to email
// POST /api/auth/forgot-password
async function apiForgotPassword(email) {
    const res = await fetch(`${API_BASE}/auth/forgot-password`, {
        method:  'POST',
        headers: getHeaders(),
        body:    JSON.stringify({ email })
    });
    return handleResponse(res);
}

// Step 2: Verify OTP + set new password in one call
// POST /api/auth/reset-password
async function apiResetPassword(email, otp, newPassword) {
    const res = await fetch(`${API_BASE}/auth/reset-password`, {
        method:  'POST',
        headers: getHeaders(),
        body:    JSON.stringify({ email, otp, newPassword })
    });
    return handleResponse(res);
}

/* ── SUBSCRIPTION APIs — NEW (do not change existing functions) ── */

// Create a new subscription
// POST /api/subscription/create
async function apiCreateSubscription(subData) {
    const res = await fetch(`${API_BASE}/subscription/create`, {
        method:  'POST',
        headers: getHeaders(),
        body:    JSON.stringify(subData)
    });
    return handleResponse(res);
}

// Get all subscriptions for logged-in user
// GET /api/subscription/my
async function apiGetMySubscriptions() {
    const res = await fetch(`${API_BASE}/subscription/my`, {
        headers: getHeaders()
    });
    return handleResponse(res);
}

// Update subscription (address, phone, start date)
// PUT /api/subscription/update/{id}
async function apiUpdateSubscription(id, updateData) {
    const res = await fetch(`${API_BASE}/subscription/update/${id}`, {
        method:  'PUT',
        headers: getHeaders(),
        body:    JSON.stringify(updateData)
    });
    return handleResponse(res);
}

// Cancel a subscription
// DELETE /api/subscription/cancel/{id}
async function apiCancelSubscription(id) {
    const res = await fetch(`${API_BASE}/subscription/cancel/${id}`, {
        method:  'DELETE',
        headers: getHeaders()
    });
    return handleResponse(res);
}

// Pause or resume a subscription
// PUT /api/subscription/status/{id}
async function apiSetSubscriptionStatus(id, status) {
    const res = await fetch(`${API_BASE}/subscription/status/${id}`, {
        method:  'PUT',
        headers: getHeaders(),
        body:    JSON.stringify({ status })
    });
    return handleResponse(res);
}

/* ── ADMIN SUBSCRIPTION APIs (new — do not change existing functions) ── */

// Admin: get ALL subscriptions (all users)
// GET /api/subscription/all
async function apiGetAllSubscriptions() {
    const res = await fetch(`${API_BASE}/subscription/all`, {
        headers: getHeaders()
    });
    return handleResponse(res);
}

// Admin: cancel any subscription (no ownership check)
// PUT /api/subscription/admin/cancel/{id}
async function apiAdminCancelSubscription(id) {
    const res = await fetch(`${API_BASE}/subscription/admin/cancel/${id}`, {
        method:  'PUT',
        headers: getHeaders()
    });
    return handleResponse(res);
}

// Admin: update any subscription (address, phone, startDate)
// PUT /api/subscription/admin/update/{id}
async function apiAdminUpdateSubscription(id, data) {
    const res = await fetch(`${API_BASE}/subscription/admin/update/${id}`, {
        method:  'PUT',
        headers: getHeaders(),
        body:    JSON.stringify(data)
    });
    return handleResponse(res);
}

/* ══════════════════════════════════════════════════════
   NAVBAR — update every page after login/logout
   ══════════════════════════════════════════════════════ */
function updateNavbar() {
    // All possible navbar element IDs
    const loginBtn    = document.getElementById('loginBtn');
    const logoutBtn   = document.getElementById('logoutBtn');
    const adminNavBtn = document.getElementById('adminNavBtn');
    const myOrdersBtn = document.getElementById('myOrdersBtn');
    const navUserName = document.getElementById('navUserName');

    if (isLoggedIn()) {
        const user = getUser();
        const name = user.name || 'User';
        const role = user.role || '';

        // Show username greeting
        if (navUserName) navUserName.textContent = 'Hi, ' + name;

        // Hide login button
        if (loginBtn)  loginBtn.style.display  = 'none';

        // Show logout button
        if (logoutBtn) logoutBtn.style.display = 'inline-block';

        // Show My Orders for customers
        if (myOrdersBtn) myOrdersBtn.style.display = 'inline-block';

        // Show Admin button ONLY for ADMIN role
        if (adminNavBtn) {
            if (role === 'ADMIN') {
                adminNavBtn.style.display = 'inline-block';
                console.log('✅ Admin button shown for role:', role);
            } else {
                adminNavBtn.style.display = 'none';
            }
        }

    } else {
        // Not logged in
        if (navUserName) navUserName.textContent = '';
        if (loginBtn)    loginBtn.style.display  = 'inline-block';
        if (logoutBtn)   logoutBtn.style.display = 'none';
        if (adminNavBtn) adminNavBtn.style.display = 'none';
        if (myOrdersBtn) myOrdersBtn.style.display = 'none';
    }
}

async function loadCartCount() {
    const badge = document.getElementById('cartCount');
    if (!badge) return;
    if (!isLoggedIn()) { badge.textContent = '0'; return; }
    try {
        const data = await apiGetCart();
        if (data && data.data) {
            badge.textContent = data.data.totalItems || '0';
        }
    } catch (e) {
        badge.textContent = '0';
    }
}

function requireLogin() {
    if (!isLoggedIn()) {
        showToast('Please login to continue.', 'error');
        setTimeout(() => { window.location.href = 'login.html'; }, 900);
        return false;
    }
    return true;
}

function requireAdmin() {
    if (!isLoggedIn()) {
        showToast('Please login as admin.', 'error');
        setTimeout(() => { window.location.href = 'login.html'; }, 900);
        return false;
    }
    if (!isAdmin()) {
        showToast('Access denied. Admin only.', 'error');
        return false;
    }
    return true;
}

/* ══════════════════════════════════════════════════════
   PRODUCTS
   ══════════════════════════════════════════════════════ */
async function apiGetProducts(page = 0, size = 8, sortBy = 'id') {
    const res = await fetch(
        `${API_BASE}/products?page=${page}&size=${size}&sortBy=${sortBy}`,
        { headers: getHeaders() }
    );
    return handleResponse(res);
}

async function apiSearchProducts(keyword, page = 0) {
    const res = await fetch(
        `${API_BASE}/products/search?keyword=${encodeURIComponent(keyword)}&page=${page}`,
        { headers: getHeaders() }
    );
    return handleResponse(res);
}

async function apiGetProduct(id) {
    const res = await fetch(`${API_BASE}/products/${id}`, { headers: getHeaders() });
    return handleResponse(res);
}

async function apiGetAllProductsAdmin() {
    const res = await fetch(`${API_BASE}/products/admin/all`, { headers: getHeaders() });
    return handleResponse(res);
}

async function apiCreateProduct(productData, imageFile) {
    const formData = new FormData();
    formData.append('product', new Blob(
        [JSON.stringify(productData)],
        { type: 'application/json' }
    ));
    if (imageFile) formData.append('image', imageFile);

    const res = await fetch(`${API_BASE}/products`, {
        method:  'POST',
        headers: getAuthHeader(),
        body:    formData
    });
    return handleResponse(res);
}

async function apiUpdateProduct(id, productData, imageFile) {
    const formData = new FormData();
    formData.append('product', new Blob(
        [JSON.stringify(productData)],
        { type: 'application/json' }
    ));
    if (imageFile) formData.append('image', imageFile);

    const res = await fetch(`${API_BASE}/products/${id}`, {
        method:  'PUT',
        headers: getAuthHeader(),
        body:    formData
    });
    return handleResponse(res);
}

async function apiDeleteProduct(id) {
    const res = await fetch(`${API_BASE}/products/${id}`, {
        method:  'DELETE',
        headers: getHeaders()
    });
    return handleResponse(res);
}

/* ══════════════════════════════════════════════════════
   CART
   ══════════════════════════════════════════════════════ */
async function apiGetCart() {
    const res = await fetch(`${API_BASE}/cart`, { headers: getHeaders() });
    return handleResponse(res);
}

async function apiAddToCart(productId, quantity = 1) {
    const res = await fetch(`${API_BASE}/cart`, {
        method:  'POST',
        headers: getHeaders(),
        body:    JSON.stringify({ productId, quantity })
    });
    return handleResponse(res);
}

async function apiUpdateCartItem(cartItemId, quantity) {
    const res = await fetch(`${API_BASE}/cart/${cartItemId}?quantity=${quantity}`, {
        method:  'PUT',
        headers: getHeaders()
    });
    return handleResponse(res);
}

async function apiRemoveCartItem(cartItemId) {
    const res = await fetch(`${API_BASE}/cart/${cartItemId}`, {
        method:  'DELETE',
        headers: getHeaders()
    });
    return handleResponse(res);
}

/* ══════════════════════════════════════════════════════
   ORDERS
   ══════════════════════════════════════════════════════ */
async function apiPlaceOrder(deliveryAddress, paymentMethod, transactionId = '', distanceKm = 0) {
    const res = await fetch(`${API_BASE}/orders`, {
        method:  'POST',
        headers: getHeaders(),
        // NEW: distanceKm sent to backend so it can calculate delivery charge
        body:    JSON.stringify({ deliveryAddress, paymentMethod, transactionId, distanceKm })
    });
    return handleResponse(res);
}

async function apiGetMyOrders() {
    const res = await fetch(`${API_BASE}/orders/my`, { headers: getHeaders() });
    return handleResponse(res);
}

async function apiGetAllOrders() {
    const res = await fetch(`${API_BASE}/orders/admin/all`, { headers: getHeaders() });
    return handleResponse(res);
}

async function apiUpdateOrderStatus(orderId, status) {
    const res = await fetch(
        `${API_BASE}/orders/admin/${orderId}/status?status=${status}`,
        { method: 'PUT', headers: getHeaders() }
    );
    return handleResponse(res);
}

/* ══════════════════════════════════════════════════════
   ORDER NOTIFICATION APIs — NEW
   ══════════════════════════════════════════════════════ */

// Lightweight poll — returns only { latestId, totalCount }
// Called every 8 seconds by notification poller (~40 bytes response)
// GET /api/orders/admin/latest-order-id
async function apiGetLatestOrderId() {
    const res = await fetch(`${API_BASE}/orders/admin/latest-order-id`, {
        headers: getHeaders()
    });
    return handleResponse(res);
}

// Fetch full details of the most recent order (for toast popup)
// GET /api/orders/admin/latest-order
async function apiGetLatestOrder() {
    const res = await fetch(`${API_BASE}/orders/admin/latest-order`, {
        headers: getHeaders()
    });
    return handleResponse(res);
}

/* ══════════════════════════════════════════════════════
   DELIVERY BOY APIs — NEW (do not change existing functions)
   ══════════════════════════════════════════════════════ */

// ── ADMIN: DELIVERY BOY MANAGEMENT ───────────────────────

// Add new delivery boy
// POST /api/delivery/boys
async function apiAddDeliveryBoy(boyData) {
    const res = await fetch(`${API_BASE}/delivery/boys`, {
        method:  'POST',
        headers: getHeaders(),
        body:    JSON.stringify(boyData)
    });
    return handleResponse(res);
}

// Get all delivery boys (admin)
// GET /api/delivery/boys
async function apiGetAllDeliveryBoys() {
    const res = await fetch(`${API_BASE}/delivery/boys`, { headers: getHeaders() });
    return handleResponse(res);
}

// Get active delivery boys only — for dropdown
// GET /api/delivery/boys/active
async function apiGetActiveDeliveryBoys() {
    const res = await fetch(`${API_BASE}/delivery/boys/active`, { headers: getHeaders() });
    return handleResponse(res);
}

// Update delivery boy details
// PUT /api/delivery/boys/{id}
async function apiUpdateDeliveryBoy(id, data) {
    const res = await fetch(`${API_BASE}/delivery/boys/${id}`, {
        method:  'PUT',
        headers: getHeaders(),
        body:    JSON.stringify(data)
    });
    return handleResponse(res);
}

// Delete delivery boy
// DELETE /api/delivery/boys/{id}
async function apiDeleteDeliveryBoy(id) {
    const res = await fetch(`${API_BASE}/delivery/boys/${id}`, {
        method:  'DELETE',
        headers: getHeaders()
    });
    return handleResponse(res);
}

// ── ADMIN: ASSIGN DELIVERY BOY TO ORDER ──────────────────

// Assign delivery boy to an order
// POST /api/delivery/assign
async function apiAssignDeliveryBoy(orderId, deliveryBoyId) {
    const res = await fetch(`${API_BASE}/delivery/assign`, {
        method:  'POST',
        headers: getHeaders(),
        body:    JSON.stringify({ orderId, deliveryBoyId })
    });
    return handleResponse(res);
}

// ── DELIVERY BOY: THEIR OWN ORDERS ───────────────────────

// Get orders assigned to a specific delivery boy
// GET /api/delivery/orders/{deliveryBoyId}
async function apiGetDeliveryOrders(deliveryBoyId) {
    const res = await fetch(`${API_BASE}/delivery/orders/${deliveryBoyId}`, {
        headers: getHeaders()
    });
    return handleResponse(res);
}

// Delivery boy updates delivery status for an order
// POST /api/delivery/update-status
async function apiUpdateDeliveryStatus(orderId, deliveryBoyId, status) {
    const res = await fetch(`${API_BASE}/delivery/update-status`, {
        method:  'POST',
        headers: getHeaders(),
        body:    JSON.stringify({ orderId, deliveryBoyId, status })
    });
    return handleResponse(res);
}

/* ══════════════════════════════════════════════════════
   ADMIN
   ══════════════════════════════════════════════════════ */
async function apiGetDashboardStats() {
    const res = await fetch(`${API_BASE}/admin/dashboard`, { headers: getHeaders() });
    return handleResponse(res);
}

/* ══════════════════════════════════════════════════════
   WHATSAPP
   ══════════════════════════════════════════════════════ */
function openWhatsAppGreeting() {
    const user = isLoggedIn() ? getUser() : null;
    const name = user ? user.name : 'Customer';
    const msg  = encodeURIComponent(
        `🌾 Hello Orivya Rice!\n\nI am ${name}. I want to know more about your rice products.\nPlease help me. 🙏`
    );
    window.open(`https://wa.me/916304212346?text=${msg}`, '_blank');
}

/* ══════════════════════════════════════════════════════
   UI HELPERS
   ══════════════════════════════════════════════════════ */
function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    if (!toast) return;
    toast.textContent = message;
    toast.className   = 'toast show ' + type;
    setTimeout(() => toast.classList.remove('show'), 3500);
}

function showLoading(show) {
    const el = document.getElementById('loadingSpinner');
    if (el) el.style.display = show ? 'flex' : 'none';
}

function formatPrice(amount) {
    return '₹' + Number(amount).toLocaleString('en-IN');
}

function getImageUrl(imageUrl) {

    if (!imageUrl) {
        return '';
    }

    // If already full URL
    if (imageUrl.startsWith('http')) {
        return imageUrl;
    }

    // Production backend
    return 'https://orivya-fullstack-4.onrender.com' + imageUrl;
}

function escapeStr(str) {
    return (str || '').replace(/'/g, "\\'").replace(/"/g, '\\"');
}