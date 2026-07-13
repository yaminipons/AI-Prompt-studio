/* ==========================================================
   AI PROMPT ENGINEERING STUDIO — DASHBOARD SHELL LOGIC
   Auth guard, sidebar navigation, theme toggle, mobile drawer,
   logout, session bootstrap, and Dashboard Home stats loading.
   Depends on: api.js (loaded before this file).
   Exposes globally: switchModule(), refreshDashboardHome()
   ========================================================== */

const MODULE_META = {
    home:        { title: 'Dashboard',        subtitle: 'Welcome back' },
    generator:   { title: 'Prompt Generator', subtitle: 'Craft a new prompt from a task description' },
    optimizer:   { title: 'Prompt Optimizer', subtitle: 'Improve an existing prompt' },
    analyzer:    { title: 'Prompt Analyzer',  subtitle: 'Score a prompt across five quality dimensions' },
    battle:      { title: 'Battle Arena',     subtitle: 'Compare prompt styles head-to-head' },
    library:     { title: 'Prompt Library',   subtitle: 'Your saved prompts' },
    collections: { title: 'Collections',      subtitle: 'Organize prompts into groups' },
    'collection-detail': { title: 'Collection', subtitle: '' },
    history:     { title: 'Prompt History',   subtitle: 'Everything you have generated' },
    chat:        { title: 'AI Chat',          subtitle: 'Conversational AI assistant' },
    profile:     { title: 'Profile',          subtitle: 'Manage your account' },
    admin:       { title: 'Admin Dashboard',  subtitle: 'Platform-wide management' }
};

let currentModule = 'home';

/* ---------- BOOTSTRAP ---------- */
document.addEventListener('DOMContentLoaded', async () => {
    requireAuth();
    initTheme();
    bindThemeToggle();
    bindSidebarNav();
    bindMobileDrawer();
    bindLogout();
    bindQuickActions();
    populateSidebarUser();

    if (Session.isAdmin()) {
        const adminNavItem = document.getElementById('adminNavItem');
        if (adminNavItem) adminNavItem.style.display = '';
    }

    await refreshDashboardHome();

    hidePageLoader();
});

function hidePageLoader() {
    const loader = document.getElementById('pageLoader');
    if (loader) {
        setTimeout(() => loader.classList.add('hidden'), 200);
    }
}

/* ---------- THEME ---------- */
function initTheme() {
    const saved = localStorage.getItem(STORAGE_KEYS.THEME) || 'light';
    document.documentElement.setAttribute('data-theme', saved);
}

function bindThemeToggle() {
    const toggle = document.getElementById('themeToggle');
    if (!toggle) return;
    toggle.addEventListener('click', () => {
        const current = document.documentElement.getAttribute('data-theme');
        const next = current === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-theme', next);
        localStorage.setItem(STORAGE_KEYS.THEME, next);
    });
}

/* ---------- SIDEBAR USER ---------- */
function populateSidebarUser() {
    const nameEl = document.getElementById('sidebarUserName');
    const emailEl = document.getElementById('sidebarUserEmail');
    const avatarEl = document.getElementById('sidebarAvatar');

    if (nameEl) nameEl.textContent = Session.getFullName();
    if (emailEl) emailEl.textContent = Session.getEmail();
    if (avatarEl) avatarEl.textContent = Session.getInitials();
}

/* ---------- LOGOUT ---------- */
function bindLogout() {
    const logoutBtn = document.getElementById('logoutBtn');
    if (!logoutBtn) return;
    logoutBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        Session.clear();
        window.location.href = 'login.html';
    });
}

/* ---------- SIDEBAR NAVIGATION / MODULE SWITCHING ---------- */
function bindSidebarNav() {
    document.querySelectorAll('.nav-item[data-module]').forEach(item => {
        item.addEventListener('click', () => {
            switchModule(item.getAttribute('data-module'));
            closeMobileDrawer();
        });
    });
}

function bindQuickActions() {
    document.querySelectorAll('.quick-action[data-module]').forEach(btn => {
        btn.addEventListener('click', () => switchModule(btn.getAttribute('data-module')));
    });
}

/**
 * Switches the visible dashboard module, updates the sidebar active
 * state and topbar title, and triggers a lazy-load hook for the
 * target module if one has been registered via onModuleActivated().
 * Exposed globally so modules.js / workspace.js can navigate
 * programmatically (e.g. a "View in Library" link).
 * @param {string} moduleName - key matching a #module-{name} section id
 */
function switchModule(moduleName) {
    if (!MODULE_META[moduleName]) return;

    document.querySelectorAll('.module-view').forEach(view => view.classList.remove('active'));
    const targetView = document.getElementById(`module-${moduleName}`);
    if (targetView) targetView.classList.add('active');

    document.querySelectorAll('.nav-item[data-module]').forEach(item => {
        item.classList.toggle('active', item.getAttribute('data-module') === moduleName);
    });

    const meta = MODULE_META[moduleName];
    const titleEl = document.getElementById('topbarTitle');
    const subtitleEl = document.getElementById('topbarSubtitle');
    if (titleEl) titleEl.textContent = meta.title;
    if (subtitleEl) subtitleEl.textContent = meta.subtitle;

    currentModule = moduleName;
    window.scrollTo({ top: 0, behavior: 'smooth' });

    if (typeof moduleActivationHandlers !== 'undefined' && moduleActivationHandlers[moduleName]) {
        moduleActivationHandlers[moduleName]();
    }
}

/* ---------- MOBILE DRAWER ---------- */
function bindMobileDrawer() {
    const hamburger = document.getElementById('hamburgerBtn');
    const sidebar = document.getElementById('sidebar');
    const overlay = document.getElementById('sidebarOverlay');

    if (hamburger) {
        hamburger.addEventListener('click', () => {
            sidebar.classList.add('open');
            overlay.classList.add('active');
        });
    }

    if (overlay) {
        overlay.addEventListener('click', closeMobileDrawer);
    }
}

function closeMobileDrawer() {
    const sidebar = document.getElementById('sidebar');
    const overlay = document.getElementById('sidebarOverlay');
    if (sidebar) sidebar.classList.remove('open');
    if (overlay) overlay.classList.remove('active');
}

/* ---------- DASHBOARD HOME ---------- */
/**
 * Loads and renders the Dashboard Home stat cards and usage breakdown
 * panel from /users/me/dashboard-stats. Exposed globally so it can be
 * re-triggered after actions elsewhere (e.g. after generating a prompt)
 * to keep the home view fresh next time the user visits it.
 */
async function refreshDashboardHome() {
    try {
        const stats = await API.user.getDashboardStats();
        renderStatsGrid(stats);
        renderActionBreakdown(stats.actionBreakdown);
    } catch (error) {
        toastError(error.message || 'Failed to load dashboard stats');
        const statsGrid = document.getElementById('statsGrid');
        if (statsGrid) {
            statsGrid.innerHTML = `
                <div class="empty-state" style="grid-column: 1 / -1;">
                    <div class="empty-icon">⚠️</div>
                    <h4>Could not load stats</h4>
                    <p>Please refresh the page to try again.</p>
                </div>`;
        }
    }
}

function renderStatsGrid(stats) {
    const grid = document.getElementById('statsGrid');
    if (!grid) return;

    const cards = [
        { icon: '📝', value: stats.totalPrompts, label: 'Total Prompts' },
        { icon: '📚', value: stats.savedPrompts, label: 'Saved to Library' },
        { icon: '⭐', value: stats.favoritePrompts, label: 'Favorites' },
        { icon: '📁', value: stats.totalCollections, label: 'Collections' },
        { icon: '💬', value: stats.totalChatSessions, label: 'Chat Sessions' },
        { icon: '📊', value: stats.averagePromptScore ? stats.averagePromptScore.toFixed(1) : '—', label: 'Avg. Analyzer Score' }
    ];

    grid.innerHTML = cards.map(card => `
        <div class="stat-card card fade-in-up">
            <div class="stat-card-top">
                <div class="stat-icon">${card.icon}</div>
            </div>
            <div class="stat-value">${escapeHtml(card.value)}</div>
            <div class="stat-label">${escapeHtml(card.label)}</div>
        </div>
    `).join('');
}

function renderActionBreakdown(breakdown) {
    const container = document.getElementById('actionBreakdown');
    if (!container) return;

    if (!breakdown) {
        container.innerHTML = `<p class="text-muted" style="font-size:0.88rem;">No activity yet.</p>`;
        return;
    }

    const entries = Object.entries(breakdown);
    const maxValue = Math.max(...entries.map(([, v]) => v), 1);

    if (entries.every(([, v]) => v === 0)) {
        container.innerHTML = `
            <div class="empty-state" style="padding: 24px;">
                <div class="empty-icon">📈</div>
                <h4>No activity yet</h4>
                <p>Start using the AI tools to see your usage breakdown here.</p>
            </div>`;
        return;
    }

    container.innerHTML = entries.map(([action, count]) => `
        <div class="breakdown-row">
            <div class="breakdown-label">${escapeHtml(actionLabel(action))}</div>
            <div class="breakdown-bar-track">
                <div class="breakdown-bar-fill" style="width:${(count / maxValue) * 100}%"></div>
            </div>
            <div class="breakdown-count">${count}</div>
        </div>
    `).join('');
}

/* ---------- MODAL HELPERS (shared across modules.js / workspace.js) ---------- */
/**
 * Opens the shared modal with the given inner HTML content.
 * @param {string} innerHtml - full modal content including header/body/footer
 */
function openModal(innerHtml) {
    const overlay = document.getElementById('modalOverlay');
    const box = document.getElementById('modalBox');
    if (!overlay || !box) return;
    box.innerHTML = innerHtml;
    overlay.classList.add('active');
}

function closeModal() {
    const overlay = document.getElementById('modalOverlay');
    const box = document.getElementById('modalBox');
    if (overlay) overlay.classList.remove('active');
    if (box) box.innerHTML = '';
}

document.addEventListener('click', (e) => {
    if (e.target && e.target.id === 'modalOverlay') {
        closeModal();
    }
});

document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        closeModal();
    }
});