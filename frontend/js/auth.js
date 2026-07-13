/* ==========================================================
   AI PROMPT ENGINEERING STUDIO — AUTH PAGE LOGIC
   Handles login.html and register.html: validation, submission,
   password visibility toggle, and theme toggle on unauthenticated pages.
   ========================================================== */

(function initTheme() {
    const saved = localStorage.getItem(STORAGE_KEYS.THEME) || 'light';
    document.documentElement.setAttribute('data-theme', saved);
})();

document.addEventListener('DOMContentLoaded', () => {
    redirectIfAuthenticated();

    const themeToggle = document.getElementById('themeToggle');
    if (themeToggle) {
        themeToggle.addEventListener('click', () => {
            const current = document.documentElement.getAttribute('data-theme');
            const next = current === 'dark' ? 'light' : 'dark';
            document.documentElement.setAttribute('data-theme', next);
            localStorage.setItem(STORAGE_KEYS.THEME, next);
        });
    }

    const passwordToggle = document.getElementById('togglePassword');
    if (passwordToggle) {
        passwordToggle.addEventListener('click', () => {
            const passwordInput = document.getElementById('password');
            const isHidden = passwordInput.type === 'password';
            passwordInput.type = isHidden ? 'text' : 'password';
        });
    }

    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', handleLogin);
    }

    const registerForm = document.getElementById('registerForm');
    if (registerForm) {
        registerForm.addEventListener('submit', handleRegister);
    }
});

/* ---------- VALIDATION HELPERS ---------- */
function clearFieldError(inputId, errorId) {
    const input = document.getElementById(inputId);
    const error = document.getElementById(errorId);
    if (input) input.style.borderColor = '';
    if (error) {
        error.textContent = '';
        error.classList.remove('visible');
    }
}

function setFieldError(inputId, errorId, message) {
    const input = document.getElementById(inputId);
    const error = document.getElementById(errorId);
    if (input) input.style.borderColor = 'var(--danger)';
    if (error) {
        error.textContent = message;
        error.classList.add('visible');
    }
}

function isValidEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function isValidPassword(password) {
    return password.length >= 8 && /[A-Za-z]/.test(password) && /\d/.test(password);
}

/* ---------- LOGIN ---------- */
async function handleLogin(event) {
    event.preventDefault();

    clearFieldError('email', 'emailError');
    clearFieldError('password', 'passwordError');

    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;

    let hasError = false;

    if (!email || !isValidEmail(email)) {
        setFieldError('email', 'emailError', 'Please enter a valid email address');
        hasError = true;
    }

    if (!password) {
        setFieldError('password', 'passwordError', 'Password is required');
        hasError = true;
    }

    if (hasError) return;

    const loginBtn = document.getElementById('loginBtn');

    try {
        await withButtonLoading(loginBtn, async () => {
            const authResponse = await API.auth.login({ email, password });
            Session.setSession(authResponse);
            toastSuccess(`Welcome back, ${authResponse.fullName.split(' ')[0]}!`);
            setTimeout(() => { window.location.href = 'dashboard.html'; }, 500);
        });
    } catch (error) {
        toastError(error.message || 'Login failed. Please check your credentials.');
    }
}

/* ---------- REGISTER ---------- */
async function handleRegister(event) {
    event.preventDefault();

    clearFieldError('fullName', 'fullNameError');
    clearFieldError('email', 'emailError');
    clearFieldError('password', 'passwordError');

    const fullName = document.getElementById('fullName').value.trim();
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;

    let hasError = false;

    if (fullName.length < 2) {
        setFieldError('fullName', 'fullNameError', 'Please enter your full name');
        hasError = true;
    }

    if (!email || !isValidEmail(email)) {
        setFieldError('email', 'emailError', 'Please enter a valid email address');
        hasError = true;
    }

    if (!isValidPassword(password)) {
        setFieldError('password', 'passwordError', 'Password must be at least 8 characters with a letter and a number');
        hasError = true;
    }

    if (hasError) return;

    const registerBtn = document.getElementById('registerBtn');

    try {
        await withButtonLoading(registerBtn, async () => {
            const authResponse = await API.auth.register({ fullName, email, password });
            Session.setSession(authResponse);
            toastSuccess(`Welcome to AI Prompt Engineering Studio, ${authResponse.fullName.split(' ')[0]}!`);
            setTimeout(() => { window.location.href = 'dashboard.html'; }, 500);
        });
    } catch (error) {
        toastError(error.message || 'Registration failed. Please try again.');
    }
}