/* ══════════════════════════════════════════════════════
   api.js  —  All API calls + Auth helpers
   ══════════════════════════════════════════════════════ */

/* ── CONFIG ─────────────────────────────────────── */
const API_BASE = 'http://localhost:8080/api';

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

    if (data.success && data.data) {
        saveUserSession(data.data);
    }
    return data;
}

async function apiRegister(name, email, password, phone) {
    const res = await fetch(`${API_BASE}/auth/register`, {
        method:  'POST',
        headers: getHeaders(),
        body:    JSON.stringify({ name, email, password, phone })
    });
    const data = await handleResponse(res);

    if (data.success && data.data) {
        saveUserSession(data.data);
    }
    return data;
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
async function apiPlaceOrder(deliveryAddress, paymentMethod, transactionId = '') {
    const res = await fetch(`${API_BASE}/orders`, {
        method:  'POST',
        headers: getHeaders(),
        body:    JSON.stringify({ deliveryAddress, paymentMethod, transactionId })
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
    if (!imageUrl) return '';
    if (imageUrl.startsWith('/uploads/')) return 'http://localhost:8080' + imageUrl;
    return imageUrl;
}

function escapeStr(str) {
    return (str || '').replace(/'/g, "\\'").replace(/"/g, '\\"');
}
