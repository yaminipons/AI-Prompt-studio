/* ==========================================================
   AI PROMPT ENGINEERING STUDIO — WORKSPACE MODULE LOGIC
   Library, Collections, History, AI Chat, Profile, Admin.
   Depends on: api.js, app.js, modules.js (loaded before this file).
   ========================================================== */

/* ---------- STATE ---------- */
let libraryState = { page: 0, size: 12, filter: 'all', keyword: '' };
let historyState = { page: 0, size: 10, action: 'ALL' };
let adminUsersState = { page: 0, size: 10 };
let adminPromptsState = { page: 0, size: 10 };
let currentCollectionId = null;
let currentChatSessionId = null;
let libraryLoaded = false;
let collectionsLoaded = false;
let historyLoaded = false;
let chatLoaded = false;
let profileLoaded = false;
let adminLoaded = false;

/* Extend the shared registry from modules.js */
Object.assign(moduleActivationHandlers, {
    library: () => { if (!libraryLoaded) { libraryLoaded = true; loadLibrary(); } },
    collections: () => { if (!collectionsLoaded) { collectionsLoaded = true; loadCollections(); } },
    history: () => { if (!historyLoaded) { historyLoaded = true; loadHistory(); } },
    chat: () => { if (!chatLoaded) { chatLoaded = true; loadChatSessions(); } },
    profile: () => { if (!profileLoaded) { profileLoaded = true; loadProfile(); } },
    admin: () => { if (!adminLoaded) { adminLoaded = true; loadAdmin(); } }
});

document.addEventListener('DOMContentLoaded', () => {
    bindLibraryToolbar();
    bindHistoryToolbar();
    bindChatComposer();
    bindProfileForms();
    bindAdminTabs();
    bindCollectionDetailBack();
});

/* ================================================================
   PROMPT LIBRARY
   ================================================================ */
function bindLibraryToolbar() {
    const search = document.getElementById('librarySearch');
    if (search) {
        search.addEventListener('input', debounce(() => {
            libraryState.keyword = search.value.trim();
            libraryState.page = 0;
            loadLibrary();
        }, 400));
    }

    document.querySelectorAll('#module-library .filter-chip').forEach(chip => {
        chip.addEventListener('click', () => {
            document.querySelectorAll('#module-library .filter-chip').forEach(c => c.classList.remove('active'));
            chip.classList.add('active');
            libraryState.filter = chip.getAttribute('data-filter');
            libraryState.page = 0;
            loadLibrary();
        });
    });
}

async function loadLibrary() {
    const grid = document.getElementById('libraryGrid');
    grid.innerHTML = renderSkeletonCards(6);

    try {
        let pageData;
        if (libraryState.keyword) {
            pageData = await API.prompts.searchLibrary(libraryState.keyword, libraryState.page, libraryState.size);
        } else if (libraryState.filter === 'favorites') {
            pageData = await API.prompts.getFavorites(libraryState.page, libraryState.size);
        } else {
            pageData = await API.prompts.getLibrary(libraryState.page, libraryState.size);
        }
        renderLibraryGrid(pageData);
        renderPagination('libraryPagination', pageData, (p) => { libraryState.page = p; loadLibrary(); });
    } catch (error) {
        toastError(error.message || 'Failed to load library');
        grid.innerHTML = renderErrorState('Failed to load your library');
    }
}

function renderLibraryGrid(pageData) {
    const grid = document.getElementById('libraryGrid');
    const items = pageData.content;

    if (!items || items.length === 0) {
        grid.innerHTML = renderEmptyState('📚', 'Your library is empty', 'Save prompts from the Generator, Optimizer, Analyzer, or Battle Arena to see them here.');
        return;
    }

    grid.innerHTML = items.map(p => `
        <div class="prompt-card card fade-in-up" data-id="${p.id}">
            <div class="prompt-card-header">
                <h4>${escapeHtml(p.title)}</h4>
                <button class="favorite-btn ${p.favorite ? 'active' : ''}" data-id="${p.id}" title="Toggle favorite">
                    ${p.favorite ? '★' : '☆'}
                </button>
            </div>
            <div class="prompt-card-text">${escapeHtml(p.generatedPrompt)}</div>
            <div class="prompt-card-tags">
                ${p.promptType ? `<span class="chip">${escapeHtml(promptTypeLabel(p.promptType))}</span>` : ''}
                ${(p.tags || []).map(t => `<span class="chip">${escapeHtml(t)}</span>`).join('')}
            </div>
            <div class="prompt-card-footer">
                <span class="date">${formatDate(p.createdAt)}</span>
                <div class="table-actions">
                    <button class="btn-icon" data-action="copy" data-id="${p.id}" title="Copy">📋</button>
                    <button class="btn-icon" data-action="export" data-id="${p.id}" title="Export PDF">⬇</button>
                    <button class="btn-icon" data-action="edit" data-id="${p.id}" title="Edit">✏️</button>
                    <button class="btn-icon" data-action="delete" data-id="${p.id}" title="Delete">🗑️</button>
                </div>
            </div>
        </div>
    `).join('');

    grid.querySelectorAll('.favorite-btn').forEach(btn => {
        btn.addEventListener('click', () => toggleLibraryFavorite(btn.getAttribute('data-id'), btn));
    });

    grid.querySelectorAll('[data-action="copy"]').forEach(btn => {
        btn.addEventListener('click', () => copyPromptText(items, btn.getAttribute('data-id')));
    });
    grid.querySelectorAll('[data-action="export"]').forEach(btn => {
        btn.addEventListener('click', () => handleExportPrompt(btn, btn.getAttribute('data-id'), 'PDF'));
    });
    grid.querySelectorAll('[data-action="edit"]').forEach(btn => {
        btn.addEventListener('click', () => openEditPromptModal(items.find(i => i.id === btn.getAttribute('data-id'))));
    });
    grid.querySelectorAll('[data-action="delete"]').forEach(btn => {
        btn.addEventListener('click', () => confirmDeletePrompt(btn.getAttribute('data-id')));
    });
}

function copyPromptText(items, id) {
    const item = items.find(i => i.id === id);
    if (!item) return;
    navigator.clipboard.writeText(item.generatedPrompt)
        .then(() => toastSuccess('Copied to clipboard'))
        .catch(() => toastError('Failed to copy'));
}

async function toggleLibraryFavorite(id, button) {
    try {
        const updated = await API.prompts.toggleFavorite(id);
        button.classList.toggle('active', updated.favorite);
        button.textContent = updated.favorite ? '★' : '☆';
        toastSuccess(updated.favorite ? 'Added to favorites' : 'Removed from favorites');
    } catch (error) {
        toastError(error.message || 'Failed to update favorite');
    }
}

function confirmDeletePrompt(id) {
    openModal(`
        <div class="modal-header"><h3>Delete Prompt</h3></div>
        <p class="text-secondary">This will permanently delete this prompt. This action cannot be undone.</p>
        <div class="modal-footer">
            <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
            <button class="btn btn-danger" id="confirmDeleteBtn">Delete</button>
        </div>
    `);
    document.getElementById('confirmDeleteBtn').addEventListener('click', async (e) => {
        try {
            await withButtonLoading(e.target, async () => {
                await API.prompts.remove(id);
                closeModal();
                toastSuccess('Prompt deleted');
                loadLibrary();
            });
        } catch (error) {
            toastError(error.message || 'Failed to delete prompt');
        }
    });
}

function openEditPromptModal(prompt) {
    if (!prompt) return;
    openModal(`
        <div class="modal-header"><h3>Edit Prompt</h3></div>
        <form id="editPromptForm">
            <div class="form-group">
                <label class="form-label">Title</label>
                <input type="text" id="editTitle" class="form-input" value="${escapeHtml(prompt.title)}" required>
            </div>
            <div class="form-group">
                <label class="form-label">Prompt Text</label>
                <textarea id="editPromptText" class="form-textarea" style="min-height:160px;" required>${escapeHtml(prompt.generatedPrompt)}</textarea>
            </div>
            <div class="form-group">
                <label class="form-label">Tags (comma-separated)</label>
                <input type="text" id="editTags" class="form-input" value="${escapeHtml((prompt.tags || []).join(', '))}">
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                <button type="submit" class="btn btn-primary" id="editSaveBtn">
                    <span class="btn-label">Save Changes</span>
                    <span class="spinner"></span>
                </button>
            </div>
        </form>
    `);

    document.getElementById('editPromptForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const saveBtn = document.getElementById('editSaveBtn');
        const tags = document.getElementById('editTags').value.split(',').map(t => t.trim()).filter(Boolean);

        try {
            await withButtonLoading(saveBtn, async () => {
                await API.prompts.update(prompt.id, {
                    title: document.getElementById('editTitle').value.trim(),
                    generatedPrompt: document.getElementById('editPromptText').value.trim(),
                    tags,
                    collectionId: prompt.collectionId || null,
                    favorite: prompt.favorite
                });
                closeModal();
                toastSuccess('Prompt updated');
                loadLibrary();
            });
        } catch (error) {
            toastError(error.message || 'Failed to update prompt');
        }
    });
}

/* ================================================================
   COLLECTIONS
   ================================================================ */
function bindCollectionDetailBack() {
    const backBtn = document.getElementById('backToCollectionsBtn');
    if (backBtn) {
        backBtn.addEventListener('click', () => switchModule('collections'));
    }
}

async function loadCollections() {
    const grid = document.getElementById('collectionsGrid');
    grid.innerHTML = renderSkeletonCards(4);

    try {
        const collections = await API.prompts.getCollections();
        renderCollectionsGrid(collections);
    } catch (error) {
        toastError(error.message || 'Failed to load collections');
        grid.innerHTML = renderErrorState('Failed to load collections');
    }
}

function renderCollectionsGrid(collections) {
    const grid = document.getElementById('collectionsGrid');

    const cardsHtml = collections.map(c => `
        <div class="collection-card card fade-in-up" data-id="${c.id}">
            <div class="collection-color-bar" style="background:${escapeHtml(c.color || '#6366f1')}"></div>
            <h4>${escapeHtml(c.name)}</h4>
            <p>${escapeHtml(c.description || 'No description')}</p>
            <div class="collection-meta">
                <span>${c.promptCount} prompt${c.promptCount === 1 ? '' : 's'}</span>
                <div class="table-actions">
                    <button class="btn-icon" data-action="edit-collection" data-id="${c.id}" title="Edit">✏️</button>
                    <button class="btn-icon" data-action="delete-collection" data-id="${c.id}" title="Delete">🗑️</button>
                </div>
            </div>
        </div>
    `).join('');

    const addCardHtml = `
        <div class="add-collection-card" id="addCollectionCard">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 5v14M5 12h14"/></svg>
            <span>New Collection</span>
        </div>
    `;

    grid.innerHTML = cardsHtml + addCardHtml;

    document.getElementById('addCollectionCard').addEventListener('click', openCreateCollectionModal);

    grid.querySelectorAll('.collection-card').forEach(card => {
        card.addEventListener('click', (e) => {
            if (e.target.closest('[data-action]')) return;
            openCollectionDetail(card.getAttribute('data-id'), collections);
        });
    });

    grid.querySelectorAll('[data-action="edit-collection"]').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            openEditCollectionModal(collections.find(c => c.id === btn.getAttribute('data-id')));
        });
    });

    grid.querySelectorAll('[data-action="delete-collection"]').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            confirmDeleteCollection(btn.getAttribute('data-id'));
        });
    });
}

function openCreateCollectionModal() {
    openModal(`
        <div class="modal-header"><h3>New Collection</h3></div>
        <form id="createCollectionForm">
            <div class="form-group">
                <label class="form-label">Name</label>
                <input type="text" id="collectionName" class="form-input" placeholder="e.g. Marketing Copy" required>
            </div>
            <div class="form-group">
                <label class="form-label">Description (optional)</label>
                <textarea id="collectionDesc" class="form-textarea" style="min-height:80px;"></textarea>
            </div>
            <div class="form-group">
                <label class="form-label">Color</label>
                <input type="color" id="collectionColor" class="form-input" value="#6366f1" style="height:48px; padding:6px;">
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                <button type="submit" class="btn btn-primary" id="createCollectionBtn">
                    <span class="btn-label">Create</span>
                    <span class="spinner"></span>
                </button>
            </div>
        </form>
    `);

    document.getElementById('createCollectionForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn = document.getElementById('createCollectionBtn');
        try {
            await withButtonLoading(btn, async () => {
                await API.prompts.createCollection({
                    name: document.getElementById('collectionName').value.trim(),
                    description: document.getElementById('collectionDesc').value.trim() || null,
                    color: document.getElementById('collectionColor').value
                });
                closeModal();
                toastSuccess('Collection created');
                loadCollections();
            });
        } catch (error) {
            toastError(error.message || 'Failed to create collection');
        }
    });
}

function openEditCollectionModal(collection) {
    if (!collection) return;
    openModal(`
        <div class="modal-header"><h3>Edit Collection</h3></div>
        <form id="editCollectionForm">
            <div class="form-group">
                <label class="form-label">Name</label>
                <input type="text" id="collectionName" class="form-input" value="${escapeHtml(collection.name)}" required>
            </div>
            <div class="form-group">
                <label class="form-label">Description</label>
                <textarea id="collectionDesc" class="form-textarea" style="min-height:80px;">${escapeHtml(collection.description || '')}</textarea>
            </div>
            <div class="form-group">
                <label class="form-label">Color</label>
                <input type="color" id="collectionColor" class="form-input" value="${collection.color || '#6366f1'}" style="height:48px; padding:6px;">
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                <button type="submit" class="btn btn-primary" id="editCollectionBtn">
                    <span class="btn-label">Save Changes</span>
                    <span class="spinner"></span>
                </button>
            </div>
        </form>
    `);

    document.getElementById('editCollectionForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn = document.getElementById('editCollectionBtn');
        try {
            await withButtonLoading(btn, async () => {
                await API.prompts.updateCollection(collection.id, {
                    name: document.getElementById('collectionName').value.trim(),
                    description: document.getElementById('collectionDesc').value.trim() || null,
                    color: document.getElementById('collectionColor').value
                });
                closeModal();
                toastSuccess('Collection updated');
                loadCollections();
            });
        } catch (error) {
            toastError(error.message || 'Failed to update collection');
        }
    });
}

function confirmDeleteCollection(id) {
    openModal(`
        <div class="modal-header"><h3>Delete Collection</h3></div>
        <p class="text-secondary">Prompts inside this collection will remain in your library, unlinked from the collection. This action cannot be undone.</p>
        <div class="modal-footer">
            <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
            <button class="btn btn-danger" id="confirmDeleteCollectionBtn">Delete</button>
        </div>
    `);
    document.getElementById('confirmDeleteCollectionBtn').addEventListener('click', async (e) => {
        try {
            await withButtonLoading(e.target, async () => {
                await API.prompts.deleteCollection(id);
                closeModal();
                toastSuccess('Collection deleted');
                loadCollections();
            });
        } catch (error) {
            toastError(error.message || 'Failed to delete collection');
        }
    });
}

async function openCollectionDetail(collectionId, collections) {
    currentCollectionId = collectionId;
    const collection = collections.find(c => c.id === collectionId);

    document.getElementById('collectionDetailName').textContent = collection ? collection.name : 'Collection';
    document.getElementById('collectionDetailDesc').textContent = collection ? (collection.description || '') : '';

    switchModule('collection-detail');

    const grid = document.getElementById('collectionDetailGrid');
    grid.innerHTML = renderSkeletonCards(4);

    try {
        const prompts = await API.prompts.getPromptsInCollection(collectionId);
        renderCollectionDetailGrid(prompts, collectionId);
    } catch (error) {
        toastError(error.message || 'Failed to load collection prompts');
        grid.innerHTML = renderErrorState('Failed to load prompts');
    }
}

function renderCollectionDetailGrid(items, collectionId) {
    const grid = document.getElementById('collectionDetailGrid');

    if (!items || items.length === 0) {
        grid.innerHTML = renderEmptyState('📁', 'This collection is empty', 'Add prompts to this collection from your Library.');
        return;
    }

    grid.innerHTML = items.map(p => `
        <div class="prompt-card card fade-in-up">
            <div class="prompt-card-header">
                <h4>${escapeHtml(p.title)}</h4>
            </div>
            <div class="prompt-card-text">${escapeHtml(p.generatedPrompt)}</div>
            <div class="prompt-card-footer">
                <span class="date">${formatDate(p.createdAt)}</span>
                <button class="btn-icon" data-action="remove" data-id="${p.id}" title="Remove from collection">✕</button>
            </div>
        </div>
    `).join('');

    grid.querySelectorAll('[data-action="remove"]').forEach(btn => {
        btn.addEventListener('click', async () => {
            try {
                await API.prompts.removeFromCollection(collectionId, btn.getAttribute('data-id'));
                toastSuccess('Removed from collection');
                const prompts = await API.prompts.getPromptsInCollection(collectionId);
                renderCollectionDetailGrid(prompts, collectionId);
            } catch (error) {
                toastError(error.message || 'Failed to remove prompt');
            }
        });
    });
}

/* ================================================================
   PROMPT HISTORY
   ================================================================ */
function bindHistoryToolbar() {
    document.querySelectorAll('#historyFilters .filter-chip').forEach(chip => {
        chip.addEventListener('click', () => {
            document.querySelectorAll('#historyFilters .filter-chip').forEach(c => c.classList.remove('active'));
            chip.classList.add('active');
            historyState.action = chip.getAttribute('data-action');
            historyState.page = 0;
            loadHistory();
        });
    });

    const clearBtn = document.getElementById('clearHistoryBtn');
    if (clearBtn) {
        clearBtn.addEventListener('click', () => {
            openModal(`
                <div class="modal-header"><h3>Clear History</h3></div>
                <p class="text-secondary">This permanently deletes all unsaved history records. Prompts saved to your Library are not affected.</p>
                <div class="modal-footer">
                    <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                    <button class="btn btn-danger" id="confirmClearHistoryBtn">Clear History</button>
                </div>
            `);
            document.getElementById('confirmClearHistoryBtn').addEventListener('click', async (e) => {
                try {
                    await withButtonLoading(e.target, async () => {
                        await API.prompts.clearHistory();
                        closeModal();
                        toastSuccess('History cleared');
                        loadHistory();
                    });
                } catch (error) {
                    toastError(error.message || 'Failed to clear history');
                }
            });
        });
    }
}

async function loadHistory() {
    const tbody = document.getElementById('historyTableBody');
    tbody.innerHTML = `<tr><td colspan="5"><div class="skeleton skeleton-line"></div></td></tr>`;

    try {
        const pageData = historyState.action === 'ALL'
            ? await API.prompts.getHistory(historyState.page, historyState.size)
            : await API.prompts.getHistoryByAction(historyState.action, historyState.page, historyState.size);

        renderHistoryTable(pageData);
        renderPagination('historyPagination', pageData, (p) => { historyState.page = p; loadHistory(); });
    } catch (error) {
        toastError(error.message || 'Failed to load history');
        tbody.innerHTML = `<tr><td colspan="5">${renderErrorState('Failed to load history')}</td></tr>`;
    }
}

function renderHistoryTable(pageData) {
    const tbody = document.getElementById('historyTableBody');
    const items = pageData.content;

    if (!items || items.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5">${renderEmptyState('🕓', 'No history yet', 'Use the AI tools to start building your history.')}</td></tr>`;
        return;
    }

    tbody.innerHTML = items.map(p => `
        <tr>
            <td>
                <div class="table-title-cell">
                    <span class="title">${escapeHtml(p.title)}</span>
                    <span class="subtitle">${escapeHtml(truncate(p.generatedPrompt || p.originalInput, 70))}</span>
                </div>
            </td>
            <td>${p.promptType ? `<span class="chip">${escapeHtml(promptTypeLabel(p.promptType))}</span>` : '—'}</td>
            <td><span class="badge badge-muted">${escapeHtml(actionLabel(p.action))}</span></td>
            <td>${formatDateTime(p.createdAt)}</td>
            <td>
                <div class="table-actions">
                    ${!p.saved ? `<button class="btn-icon" data-action="save" data-id="${p.id}" title="Save to Library">💾</button>` : `<span class="badge badge-success">Saved</span>`}
                    <button class="btn-icon" data-action="delete" data-id="${p.id}" title="Delete">🗑️</button>
                </div>
            </td>
        </tr>
    `).join('');

    tbody.querySelectorAll('[data-action="save"]').forEach(btn => {
        btn.addEventListener('click', async () => {
            try {
                await withButtonLoading(btn, async () => {
                    await API.prompts.saveExisting(btn.getAttribute('data-id'));
                    toastSuccess('Saved to library');
                    loadHistory();
                });
            } catch (error) {
                toastError(error.message || 'Failed to save prompt');
            }
        });
    });

    tbody.querySelectorAll('[data-action="delete"]').forEach(btn => {
        btn.addEventListener('click', () => {
            openModal(`
                <div class="modal-header"><h3>Delete Record</h3></div>
                <p class="text-secondary">This permanently deletes this history record. This action cannot be undone.</p>
                <div class="modal-footer">
                    <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                    <button class="btn btn-danger" id="confirmDeleteHistoryBtn">Delete</button>
                </div>
            `);
            document.getElementById('confirmDeleteHistoryBtn').addEventListener('click', async (e) => {
                try {
                    await withButtonLoading(e.target, async () => {
                        await API.prompts.remove(btn.getAttribute('data-id'));
                        closeModal();
                        toastSuccess('Record deleted');
                        loadHistory();
                    });
                } catch (error) {
                    toastError(error.message || 'Failed to delete record');
                }
            });
        });
    });
}

/* ================================================================
   AI CHAT
   ================================================================ */
function bindChatComposer() {
    const newChatBtn = document.getElementById('newChatBtn');
    if (newChatBtn) {
        newChatBtn.addEventListener('click', async () => {
            try {
                await withButtonLoading(newChatBtn, async () => {
                    const session = await API.chat.createSession();
                    await loadChatSessions();
                    openChatSession(session.id);
                });
            } catch (error) {
                toastError(error.message || 'Failed to create chat session');
            }
        });
    }

    const sendBtn = document.getElementById('chatSendBtn');
    const input = document.getElementById('chatInput');

    if (sendBtn) sendBtn.addEventListener('click', sendChatMessage);
    if (input) {
        input.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendChatMessage();
            }
        });
    }
}

async function loadChatSessions() {
    const list = document.getElementById('chatSessionList');
    list.innerHTML = `<div class="skeleton skeleton-line"></div><div class="skeleton skeleton-line"></div>`;

    try {
        const sessions = await API.chat.getSessions();
        renderChatSessionList(sessions);
    } catch (error) {
        toastError(error.message || 'Failed to load chat sessions');
        list.innerHTML = renderErrorState('Failed to load sessions');
    }
}

function renderChatSessionList(sessions) {
    const list = document.getElementById('chatSessionList');

    if (!sessions || sessions.length === 0) {
        list.innerHTML = `<div class="empty-state" style="padding:24px;"><div class="empty-icon">💬</div><p style="font-size:0.85rem;">No chats yet. Start a new one!</p></div>`;
        return;
    }

    list.innerHTML = sessions.map(s => `
        <div class="chat-session-item ${s.id === currentChatSessionId ? 'active' : ''}" data-id="${s.id}">
            <span class="title">${escapeHtml(s.title)}</span>
            <span class="meta">${s.messageCount} messages · ${formatDate(s.updatedAt)}</span>
        </div>
    `).join('');

    list.querySelectorAll('.chat-session-item').forEach(item => {
        item.addEventListener('click', () => openChatSession(item.getAttribute('data-id')));
    });
}

async function openChatSession(sessionId) {
    currentChatSessionId = sessionId;

    document.querySelectorAll('.chat-session-item').forEach(item => {
        item.classList.toggle('active', item.getAttribute('data-id') === sessionId);
    });

    const messagesContainer = document.getElementById('chatMessages');
    messagesContainer.innerHTML = `<div class="empty-state"><div class="loader-ring" style="width:32px;height:32px;"></div></div>`;

    document.getElementById('chatInput').disabled = false;
    document.getElementById('chatSendBtn').disabled = false;

    try {
        const session = await API.chat.getSession(sessionId);
        renderChatMessages(session.messages);
    } catch (error) {
        toastError(error.message || 'Failed to load chat');
    }
}

function renderChatMessages(messages) {
    const container = document.getElementById('chatMessages');

    if (!messages || messages.length === 0) {
        container.innerHTML = `<div class="empty-state"><div class="empty-icon">💬</div><h4>Say hello!</h4><p>Type a message below to start the conversation.</p></div>`;
        return;
    }

    container.innerHTML = messages.map(m => renderChatBubble(m)).join('');
    container.scrollTop = container.scrollHeight;
}

function renderChatBubble(message) {
    const isUser = message.role === 'USER';
    return `
        <div class="chat-bubble-row ${isUser ? 'user' : 'assistant'}">
            <div class="chat-bubble-avatar">${isUser ? Session.getInitials() : 'AI'}</div>
            <div class="chat-bubble">${escapeHtml(message.content)}</div>
        </div>
    `;
}

async function sendChatMessage() {
    if (!currentChatSessionId) {
        toastWarning('Please select or create a chat first');
        return;
    }

    const input = document.getElementById('chatInput');
    const message = input.value.trim();
    if (!message) return;

    const container = document.getElementById('chatMessages');
    const emptyState = container.querySelector('.empty-state');
    if (emptyState) container.innerHTML = '';

    container.insertAdjacentHTML('beforeend', renderChatBubble({ role: 'USER', content: message }));
    container.scrollTop = container.scrollHeight;
    input.value = '';

    const sendBtn = document.getElementById('chatSendBtn');
    sendBtn.disabled = true;
    input.disabled = true;

    container.insertAdjacentHTML('beforeend', `
        <div class="chat-bubble-row assistant" id="typingIndicatorRow">
            <div class="chat-bubble-avatar">AI</div>
            <div class="chat-bubble"><div class="chat-typing"><span></span><span></span><span></span></div></div>
        </div>
    `);
    container.scrollTop = container.scrollHeight;

    try {
        const session = await API.chat.sendMessage(currentChatSessionId, message);
        document.getElementById('typingIndicatorRow')?.remove();
        renderChatMessages(session.messages);
        loadChatSessions();
    } catch (error) {
        document.getElementById('typingIndicatorRow')?.remove();
        toastError(error.message || 'Failed to send message');
    } finally {
        sendBtn.disabled = false;
        input.disabled = false;
        input.focus();
    }
}

/* ================================================================
   PROFILE
   ================================================================ */
function bindProfileForms() {
    const profileForm = document.getElementById('profileForm');
    if (profileForm) {
        profileForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const btn = document.getElementById('profileSaveBtn');
            try {
                await withButtonLoading(btn, async () => {
                    const updated = await API.user.updateProfile({
                        fullName: document.getElementById('profileFullName').value.trim(),
                        bio: document.getElementById('profileBio').value.trim(),
                        profileImageUrl: null
                    });
                    localStorage.setItem(STORAGE_KEYS.FULL_NAME, updated.fullName);
                    populateSidebarUser();
                    populateProfileCard(updated);
                    toastSuccess('Profile updated successfully');
                });
            } catch (error) {
                toastError(error.message || 'Failed to update profile');
            }
        });
    }

    const passwordForm = document.getElementById('passwordForm');
    if (passwordForm) {
        passwordForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const btn = document.getElementById('passwordSaveBtn');
            const currentPassword = document.getElementById('currentPassword').value;
            const newPassword = document.getElementById('newPassword').value;

            try {
                await withButtonLoading(btn, async () => {
                    await API.user.changePassword({ currentPassword, newPassword });
                    passwordForm.reset();
                    toastSuccess('Password updated successfully');
                });
            } catch (error) {
                toastError(error.message || 'Failed to update password');
            }
        });
    }
}

async function loadProfile() {
    try {
        const [profile, stats] = await Promise.all([
            API.user.getProfile(),
            API.user.getDashboardStats()
        ]);
        populateProfileCard(profile);
        document.getElementById('profileFullName').value = profile.fullName;
        document.getElementById('profileBio').value = profile.bio || '';
        document.getElementById('profileStatPrompts').textContent = stats.totalPrompts;
        document.getElementById('profileStatCollections').textContent = stats.totalCollections;
        document.getElementById('profileStatChats').textContent = stats.totalChatSessions;
    } catch (error) {
        toastError(error.message || 'Failed to load profile');
    }
}

function populateProfileCard(profile) {
    document.getElementById('profileAvatarLg').textContent = Session.getInitials();
    document.getElementById('profileName').textContent = profile.fullName;
    document.getElementById('profileEmail').textContent = profile.email;
    const roleBadge = document.getElementById('profileRoleBadge');
    roleBadge.textContent = profile.role;
    roleBadge.className = `badge role-badge ${profile.role === 'ADMIN' ? 'badge-warning' : 'badge-primary'}`;
}

/* ================================================================
   ADMIN DASHBOARD
   ================================================================ */
function bindAdminTabs() {
    document.querySelectorAll('.admin-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            document.querySelectorAll('.admin-tab').forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            const target = tab.getAttribute('data-admin-tab');
            document.getElementById('adminUsersPanel').style.display = target === 'users' ? '' : 'none';
            document.getElementById('adminPromptsPanel').style.display = target === 'prompts' ? '' : 'none';
            if (target === 'prompts') loadAdminPrompts();
        });
    });
}

async function loadAdmin() {
    if (!Session.isAdmin()) return;

    const statsGrid = document.getElementById('adminStatsGrid');
    statsGrid.innerHTML = renderSkeletonCards(4);

    try {
        const stats = await API.admin.getStats();
        statsGrid.innerHTML = `
            <div class="stat-card card fade-in-up"><div class="stat-icon">👥</div><div class="stat-value">${stats.totalUsers}</div><div class="stat-label">Total Users</div></div>
            <div class="stat-card card fade-in-up"><div class="stat-icon">✅</div><div class="stat-value">${stats.activeUsers}</div><div class="stat-label">Active Users</div></div>
            <div class="stat-card card fade-in-up"><div class="stat-icon">📝</div><div class="stat-value">${stats.totalPrompts}</div><div class="stat-label">Total Prompts</div></div>
            <div class="stat-card card fade-in-up"><div class="stat-icon">💬</div><div class="stat-value">${stats.totalChatSessions}</div><div class="stat-label">Chat Sessions</div></div>
        `;
    } catch (error) {
        toastError(error.message || 'Failed to load admin stats');
        statsGrid.innerHTML = renderErrorState('Failed to load stats');
    }

    loadAdminUsers();
}

async function loadAdminUsers() {
    const tbody = document.getElementById('adminUsersTableBody');
    tbody.innerHTML = `<tr><td colspan="6"><div class="skeleton skeleton-line"></div></td></tr>`;

    try {
        const pageData = await API.admin.getUsers(adminUsersState.page, adminUsersState.size);
        renderAdminUsersTable(pageData);
        renderPagination('adminUsersPagination', pageData, (p) => { adminUsersState.page = p; loadAdminUsers(); });
    } catch (error) {
        toastError(error.message || 'Failed to load users');
        tbody.innerHTML = `<tr><td colspan="6">${renderErrorState('Failed to load users')}</td></tr>`;
    }
}

function renderAdminUsersTable(pageData) {
    const tbody = document.getElementById('adminUsersTableBody');
    const items = pageData.content;

    if (!items || items.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6">${renderEmptyState('👥', 'No users found', '')}</td></tr>`;
        return;
    }

    tbody.innerHTML = items.map(u => `
        <tr>
            <td>${escapeHtml(u.fullName)}</td>
            <td>${escapeHtml(u.email)}</td>
            <td>
                <select class="form-select role-select" data-id="${u.id}">
                    <option value="USER" ${u.role === 'USER' ? 'selected' : ''}>USER</option>
                    <option value="ADMIN" ${u.role === 'ADMIN' ? 'selected' : ''}>ADMIN</option>
                </select>
            </td>
            <td><span class="badge ${u.active ? 'badge-success' : 'badge-danger'}">${u.active ? 'Active' : 'Disabled'}</span></td>
            <td>${formatDate(u.createdAt)}</td>
            <td>
                <button class="btn btn-sm ${u.active ? 'btn-danger' : 'btn-secondary'}" data-action="toggle-status" data-id="${u.id}">
                    ${u.active ? 'Deactivate' : 'Activate'}
                </button>
            </td>
        </tr>
    `).join('');

    tbody.querySelectorAll('.role-select').forEach(select => {
        select.addEventListener('change', async () => {
            try {
                await API.admin.updateUserRole(select.getAttribute('data-id'), select.value);
                toastSuccess('User role updated');
            } catch (error) {
                toastError(error.message || 'Failed to update role');
                loadAdminUsers();
            }
        });
    });

    tbody.querySelectorAll('[data-action="toggle-status"]').forEach(btn => {
        btn.addEventListener('click', async () => {
            try {
                await withButtonLoading(btn, async () => {
                    await API.admin.toggleUserStatus(btn.getAttribute('data-id'));
                    toastSuccess('User status updated');
                    loadAdminUsers();
                });
            } catch (error) {
                toastError(error.message || 'Failed to update status');
            }
        });
    });
}

async function loadAdminPrompts() {
    const tbody = document.getElementById('adminPromptsTableBody');
    tbody.innerHTML = `<tr><td colspan="4"><div class="skeleton skeleton-line"></div></td></tr>`;

    try {
        const pageData = await API.admin.getAllPrompts(adminPromptsState.page, adminPromptsState.size);
        renderAdminPromptsTable(pageData);
        renderPagination('adminPromptsPagination', pageData, (p) => { adminPromptsState.page = p; loadAdminPrompts(); });
    } catch (error) {
        toastError(error.message || 'Failed to load prompts');
        tbody.innerHTML = `<tr><td colspan="4">${renderErrorState('Failed to load prompts')}</td></tr>`;
    }
}

function renderAdminPromptsTable(pageData) {
    const tbody = document.getElementById('adminPromptsTableBody');
    const items = pageData.content;

    if (!items || items.length === 0) {
        tbody.innerHTML = `<tr><td colspan="4">${renderEmptyState('📝', 'No prompts found', '')}</td></tr>`;
        return;
    }

    tbody.innerHTML = items.map(p => `
        <tr>
            <td>
                <div class="table-title-cell">
                    <span class="title">${escapeHtml(p.title)}</span>
                    <span class="subtitle">${escapeHtml(truncate(p.generatedPrompt || p.originalInput, 70))}</span>
                </div>
            </td>
            <td>${p.promptType ? escapeHtml(promptTypeLabel(p.promptType)) : '—'}</td>
            <td><span class="badge badge-muted">${escapeHtml(actionLabel(p.action))}</span></td>
            <td>${formatDateTime(p.createdAt)}</td>
        </tr>
    `).join('');
}

/* ================================================================
   SHARED RENDER HELPERS
   ================================================================ */
function renderSkeletonCards(count) {
    return Array(count).fill('<div class="card skeleton skeleton-card"></div>').join('');
}

function renderEmptyState(icon, title, description) {
    return `
        <div class="empty-state" style="grid-column: 1 / -1;">
            <div class="empty-icon">${icon}</div>
            <h4>${escapeHtml(title)}</h4>
            <p>${escapeHtml(description)}</p>
        </div>
    `;
}

function renderErrorState(message) {
    return `
        <div class="empty-state" style="grid-column: 1 / -1;">
            <div class="empty-icon">⚠️</div>
            <h4>Something went wrong</h4>
            <p>${escapeHtml(message)}</p>
        </div>
    `;
}

/**
 * Renders numbered pagination controls into the given container,
 * calling onPageClick(pageIndex) when a page button is clicked.
 * Shared across Library, History, Admin Users, and Admin Prompts.
 */
function renderPagination(containerId, pageData, onPageClick) {
    const container = document.getElementById(containerId);
    if (!container) return;

    const { pageNumber, totalPages } = pageData;

    if (totalPages <= 1) {
        container.innerHTML = '';
        return;
    }

    let buttons = '';
    buttons += `<button ${pageNumber === 0 ? 'disabled' : ''} data-page="${pageNumber - 1}">‹</button>`;

    const start = Math.max(0, pageNumber - 2);
    const end = Math.min(totalPages - 1, pageNumber + 2);

    for (let i = start; i <= end; i++) {
        buttons += `<button class="${i === pageNumber ? 'active' : ''}" data-page="${i}">${i + 1}</button>`;
    }

    buttons += `<button ${pageNumber >= totalPages - 1 ? 'disabled' : ''} data-page="${pageNumber + 1}">›</button>`;

    container.innerHTML = buttons;

    container.querySelectorAll('button[data-page]').forEach(btn => {
        btn.addEventListener('click', () => {
            const page = parseInt(btn.getAttribute('data-page'), 10);
            if (!isNaN(page) && page >= 0 && page < totalPages) {
                onPageClick(page);
            }
        });
    });
}