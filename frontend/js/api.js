/* ==========================================================
   AI PROMPT ENGINEERING STUDIO — CORE API LAYER
   Base config, JWT-aware fetch wrapper, toast notifications,
   and one method per backend REST endpoint.
   ========================================================== */

/* ---------- CONFIG ---------- */
const API_BASE_URL = 'https://ai-prompt-studio-8ki8.onrender.com/api';

const STORAGE_KEYS = {
    TOKEN: 'aps_token',
    USER_ID: 'aps_user_id',
    FULL_NAME: 'aps_full_name',
    EMAIL: 'aps_email',
    ROLE: 'aps_role',
    THEME: 'aps_theme'
};

/* ---------- TOKEN / SESSION HELPERS ---------- */
const Session = {
    getToken() {
        return localStorage.getItem(STORAGE_KEYS.TOKEN);
    },
    setSession(authResponse) {
        localStorage.setItem(STORAGE_KEYS.TOKEN, authResponse.token);
        localStorage.setItem(STORAGE_KEYS.USER_ID, authResponse.userId);
        localStorage.setItem(STORAGE_KEYS.FULL_NAME, authResponse.fullName);
        localStorage.setItem(STORAGE_KEYS.EMAIL, authResponse.email);
        localStorage.setItem(STORAGE_KEYS.ROLE, authResponse.role);
    },
    clear() {
        localStorage.removeItem(STORAGE_KEYS.TOKEN);
        localStorage.removeItem(STORAGE_KEYS.USER_ID);
        localStorage.removeItem(STORAGE_KEYS.FULL_NAME);
        localStorage.removeItem(STORAGE_KEYS.EMAIL);
        localStorage.removeItem(STORAGE_KEYS.ROLE);
    },
    isAuthenticated() {
        return !!this.getToken();
    },
    getFullName() {
        return localStorage.getItem(STORAGE_KEYS.FULL_NAME) || 'there';
    },
    getEmail() {
        return localStorage.getItem(STORAGE_KEYS.EMAIL) || '';
    },
    getRole() {
        return localStorage.getItem(STORAGE_KEYS.ROLE) || 'USER';
    },
    isAdmin() {
        return this.getRole() === 'ADMIN';
    },
    getInitials() {
        const name = this.getFullName();
        const parts = name.trim().split(/\s+/);
        if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
        return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
};

/* ---------- REDIRECT GUARDS ---------- */
function requireAuth() {
    if (!Session.isAuthenticated()) {
        window.location.href = 'login.html';
    }
}

function redirectIfAuthenticated() {
    if (Session.isAuthenticated()) {
        window.location.href = 'dashboard.html';
    }
}

/* ---------- CORE FETCH WRAPPER ---------- */
/**
 * Performs a request against the backend API, automatically attaching
 * the JWT Authorization header when a session exists, and unwrapping
 * the standard ApiResponse<T> envelope. On 401 from a PROTECTED
 * endpoint, clears the session and redirects to login, since that
 * means an existing token died mid-session. On 401 from the public
 * /auth/** endpoints (login/register), that status simply means
 * "invalid credentials" or "email already exists" — NOT an expired
 * session — so it is passed through as a normal error with the
 * backend's real message intact, and no session-clear/redirect happens.
 */
async function apiRequest(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;
    const headers = {
        'Content-Type': 'application/json',
        ...(options.headers || {})
    };

    const token = Session.getToken();
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    let response;
    try {
        response = await fetch(url, { ...options, headers });
    } catch (networkError) {
        throw new Error('Unable to reach the server. Please check your connection.');
    }

    const isAuthEndpoint = endpoint.startsWith('/auth/');

    let body;
    try {
        body = await response.json();
    } catch {
        body = null;
    }

    if (response.status === 401) {
        if (isAuthEndpoint) {
            // A 401 from /auth/login or /auth/register means invalid
            // credentials, not an expired session. Surface the real
            // backend message and do NOT clear/redirect anything.
            const message = (body && body.message) ? body.message : 'Invalid email or password';
            throw new Error(message);
        }

        // A 401 from any other (protected) endpoint means the token
        // that was attached is invalid or has expired.
        Session.clear();
        if (!window.location.pathname.endsWith('login.html') && !window.location.pathname.endsWith('index.html')) {
            window.location.href = 'login.html';
        }
        throw new Error('Your session has expired. Please log in again.');
    }

    if (!response.ok || (body && body.success === false)) {
        const message = (body && body.message) ? body.message : `Request failed (${response.status})`;
        throw new Error(message);
    }

    return body ? body.data : null;
}

/**
 * Performs a raw (non-JSON) fetch for binary downloads (Export feature).
 * Returns the raw Response so the caller can read blob() and headers.
 */
async function apiRequestRaw(endpoint) {
    const url = `${API_BASE_URL}${endpoint}`;
    const headers = {};
    const token = Session.getToken();
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(url, { headers });

    if (response.status === 401) {
        Session.clear();
        window.location.href = 'login.html';
        throw new Error('Your session has expired. Please log in again.');
    }

    if (response.status === 403) {
        const message = (body && body.message) ? body.message : 'You do not have permission to perform this action.';
        throw new Error(message);
    }

    if (!response.ok) {
        throw new Error(`Export failed (${response.status})`);
    }

    return response;
}

function buildQuery(params) {
    const query = Object.entries(params)
        .filter(([, v]) => v !== undefined && v !== null && v !== '')
        .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
        .join('&');
    return query ? `?${query}` : '';
}

/* ---------- API NAMESPACE ---------- */
const API = {

    /* ===== AUTH ===== */
    auth: {
        register(payload) {
            return apiRequest('/auth/register', { method: 'POST', body: JSON.stringify(payload) });
        },
        login(payload) {
            return apiRequest('/auth/login', { method: 'POST', body: JSON.stringify(payload) });
        }
    },

    /* ===== USER ===== */
    user: {
        getProfile() {
            return apiRequest('/users/me');
        },
        updateProfile(payload) {
            return apiRequest('/users/me', { method: 'PUT', body: JSON.stringify(payload) });
        },
        changePassword(payload) {
            return apiRequest('/users/me/password', { method: 'PUT', body: JSON.stringify(payload) });
        },
        getDashboardStats() {
            return apiRequest('/users/me/dashboard-stats');
        }
    },

    /* ===== PROMPTS: GENERATOR / OPTIMIZER / ANALYZER / BATTLE ===== */
    prompts: {
        generate(payload) {
            return apiRequest('/prompts/generate', { method: 'POST', body: JSON.stringify(payload) });
        },
        optimize(payload) {
            return apiRequest('/prompts/optimize', { method: 'POST', body: JSON.stringify(payload) });
        },
        analyze(payload) {
            return apiRequest('/prompts/analyze', { method: 'POST', body: JSON.stringify(payload) });
        },
        battle(payload) {
            return apiRequest('/prompts/battle', { method: 'POST', body: JSON.stringify(payload) });
        },

        /* ---- Library ---- */
        save(payload) {
            return apiRequest('/prompts/library', { method: 'POST', body: JSON.stringify(payload) });
        },
        saveExisting(promptId) {
            return apiRequest(`/prompts/${promptId}/save`, { method: 'PATCH' });
        },
        getLibrary(page = 0, size = 20) {
            return apiRequest(`/prompts/library${buildQuery({ page, size })}`);
        },
        searchLibrary(keyword, page = 0, size = 20) {
            return apiRequest(`/prompts/library/search${buildQuery({ keyword, page, size })}`);
        },
        getById(promptId) {
            return apiRequest(`/prompts/${promptId}`);
        },
        update(promptId, payload) {
            return apiRequest(`/prompts/${promptId}`, { method: 'PUT', body: JSON.stringify(payload) });
        },
        toggleFavorite(promptId) {
            return apiRequest(`/prompts/${promptId}/favorite`, { method: 'PATCH' });
        },
        getFavorites(page = 0, size = 20) {
            return apiRequest(`/prompts/favorites${buildQuery({ page, size })}`);
        },
        remove(promptId) {
            return apiRequest(`/prompts/${promptId}`, { method: 'DELETE' });
        },

        /* ---- History ---- */
        getHistory(page = 0, size = 20) {
            return apiRequest(`/prompts/history${buildQuery({ page, size })}`);
        },
        getHistoryByAction(action, page = 0, size = 20) {
            return apiRequest(`/prompts/history/${action}${buildQuery({ page, size })}`);
        },
        clearHistory() {
            return apiRequest('/prompts/history', { method: 'DELETE' });
        },

        /* ---- Collections ---- */
        createCollection(payload) {
            return apiRequest('/prompts/collections', { method: 'POST', body: JSON.stringify(payload) });
        },
        getCollections() {
            return apiRequest('/prompts/collections');
        },
        updateCollection(collectionId, payload) {
            return apiRequest(`/prompts/collections/${collectionId}`, { method: 'PUT', body: JSON.stringify(payload) });
        },
        deleteCollection(collectionId) {
            return apiRequest(`/prompts/collections/${collectionId}`, { method: 'DELETE' });
        },
        addToCollection(collectionId, promptId) {
            return apiRequest(`/prompts/collections/${collectionId}/prompts/${promptId}`, { method: 'POST' });
        },
        removeFromCollection(collectionId, promptId) {
            return apiRequest(`/prompts/collections/${collectionId}/prompts/${promptId}`, { method: 'DELETE' });
        },
        getPromptsInCollection(collectionId) {
            return apiRequest(`/prompts/collections/${collectionId}/prompts`);
        }
    },

    /* ===== CHAT ===== */
    chat: {
        createSession() {
            return apiRequest('/chat/sessions', { method: 'POST' });
        },
        getSessions() {
            return apiRequest('/chat/sessions');
        },
        getSession(sessionId) {
            return apiRequest(`/chat/sessions/${sessionId}`);
        },
        sendMessage(sessionId, message) {
            return apiRequest(`/chat/sessions/${sessionId}/messages`, {
                method: 'POST',
                body: JSON.stringify({ message })
            });
        },
        renameSession(sessionId, title) {
            return apiRequest(`/chat/sessions/${sessionId}/title`, {
                method: 'PATCH',
                body: JSON.stringify({ title })
            });
        },
        deleteSession(sessionId) {
            return apiRequest(`/chat/sessions/${sessionId}`, { method: 'DELETE' });
        }
    },

    /* ===== EXPORT ===== */
    export: {
        async download(promptId, format, suggestedName) {
            const response = await apiRequestRaw(`/export/${promptId}/${format.toLowerCase()}`);
            const blob = await response.blob();
            const disposition = response.headers.get('Content-Disposition') || '';
            const match = disposition.match(/filename="?([^";]+)"?/);
            const fileName = match ? match[1] : (suggestedName || `prompt.${format.toLowerCase()}`);

            const link = document.createElement('a');
            const objectUrl = URL.createObjectURL(blob);
            link.href = objectUrl;
            link.download = fileName;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            URL.revokeObjectURL(objectUrl);
        }
    },

    /* ===== ADMIN ===== */
    admin: {
        getUsers(page = 0, size = 20) {
            return apiRequest(`/admin/users${buildQuery({ page, size })}`);
        },
        getUserById(userId) {
            return apiRequest(`/admin/users/${userId}`);
        },
        toggleUserStatus(userId) {
            return apiRequest(`/admin/users/${userId}/status`, { method: 'PATCH' });
        },
        updateUserRole(userId, role) {
            return apiRequest(`/admin/users/${userId}/role`, {
                method: 'PATCH',
                body: JSON.stringify({ role })
            });
        },
        getAllPrompts(page = 0, size = 20) {
            return apiRequest(`/admin/prompts${buildQuery({ page, size })}`);
        },
        getStats() {
            return apiRequest('/admin/stats');
        }
    }
};

/* ---------- TOAST NOTIFICATIONS ---------- */
function ensureToastContainer() {
    let container = document.querySelector('.toast-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'toast-container';
        document.body.appendChild(container);
    }
    return container;
}

const TOAST_ICONS = {
    success: '✓',
    error: '✕',
    info: 'i',
    warning: '!'
};

/**
 * Shows a toast notification in the top-right corner.
 * @param {string} message - the text to display
 * @param {'success'|'error'|'info'|'warning'} type - visual style
 * @param {number} duration - ms before auto-dismiss (default 4000)
 */
function showToast(message, type = 'info', duration = 4000) {
    const container = ensureToastContainer();

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `
        <span class="toast-icon">${TOAST_ICONS[type] || TOAST_ICONS.info}</span>
        <span class="toast-message"></span>
        <button class="toast-close" aria-label="Dismiss">✕</button>
    `;
    toast.querySelector('.toast-message').textContent = message;

    const removeToast = () => {
        toast.classList.add('removing');
        setTimeout(() => toast.remove(), 300);
    };

    toast.querySelector('.toast-close').addEventListener('click', removeToast);
    container.appendChild(toast);

    setTimeout(removeToast, duration);
}

function toastSuccess(message) { showToast(message, 'success'); }
function toastError(message) { showToast(message, 'error'); }
function toastInfo(message) { showToast(message, 'info'); }
function toastWarning(message) { showToast(message, 'warning'); }

/**
 * Wraps an async action with button loading state and standardized
 * error toasting. Disables the button, shows its inline spinner, runs
 * the action, and always restores the button state afterward.
 * @param {HTMLElement} button - the button element to toggle loading state on
 * @param {Function} action - async function to execute
 */
async function withButtonLoading(button, action) {
    if (!button) return action();
    button.classList.add('loading');
    button.disabled = true;
    try {
        return await action();
    } finally {
        button.classList.remove('loading');
        button.disabled = false;
    }
}

/* ---------- MISC HELPERS ---------- */
function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    const div = document.createElement('div');
    div.textContent = String(str);
    return div.innerHTML;
}

function formatDate(isoString) {
    if (!isoString) return '';
    const date = new Date(isoString);
    return date.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

function formatDateTime(isoString) {
    if (!isoString) return '';
    const date = new Date(isoString);
    return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' }) + ' · ' +
           date.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
}

function truncate(str, maxLength) {
    if (!str) return '';
    return str.length > maxLength ? str.substring(0, maxLength).trim() + '…' : str;
}

function debounce(fn, delay = 350) {
    let timer;
    return (...args) => {
        clearTimeout(timer);
        timer = setTimeout(() => fn(...args), delay);
    };
}

const PROMPT_TYPE_LABELS = {
    ZERO_SHOT: 'Zero-Shot',
    FEW_SHOT: 'Few-Shot',
    CHAIN_OF_THOUGHT: 'Chain-of-Thought',
    ROLE_BASED: 'Role-Based',
    STEP_BY_STEP: 'Step-by-Step',
    INSTRUCTION: 'Instruction'
};

const ACTION_LABELS = {
    GENERATE: 'Generator',
    OPTIMIZE: 'Optimizer',
    ANALYZE: 'Analyzer',
    BATTLE: 'Battle Arena',
    MANUAL: 'Manual Save'
};

function promptTypeLabel(type) {
    return PROMPT_TYPE_LABELS[type] || type || '—';
}

function actionLabel(action) {
    return ACTION_LABELS[action] || action || '—';
}