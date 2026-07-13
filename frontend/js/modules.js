/* ==========================================================
   AI PROMPT ENGINEERING STUDIO — AI TOOLS MODULE LOGIC
   Prompt Generator, Optimizer, Analyzer, Battle Arena.
   Depends on: api.js, app.js (loaded before this file).
   Registers into global moduleActivationHandlers.
   ========================================================== */

const PROMPT_STYLES = [
    { type: 'ZERO_SHOT', name: 'Zero-Shot', desc: 'Direct instruction with no examples' },
    { type: 'FEW_SHOT', name: 'Few-Shot', desc: 'Includes examples to guide the response' },
    { type: 'CHAIN_OF_THOUGHT', name: 'Chain-of-Thought', desc: 'Step-by-step reasoning before the answer' },
    { type: 'ROLE_BASED', name: 'Role-Based', desc: 'Assigns an expert persona to the model' },
    { type: 'STEP_BY_STEP', name: 'Step-by-Step', desc: 'Breaks the task into sequential steps' },
    { type: 'INSTRUCTION', name: 'Instruction', desc: 'Clear structured instructions with constraints' }
];

/* Global registry consulted by app.js's switchModule() */
const moduleActivationHandlers = {
    generator: initGeneratorStyleGrid,
    battle: initBattleStyleGrid
};

let selectedGenStyle = 'ZERO_SHOT';
let selectedBattleStyles = new Set();
let generatorGridInitialized = false;
let battleGridInitialized = false;

/* ---------- BOOTSTRAP ---------- */
document.addEventListener('DOMContentLoaded', () => {
    bindGenerator();
    bindOptimizer();
    bindAnalyzer();
    bindBattle();
});

/* ================================================================
   PROMPT GENERATOR
   ================================================================ */
function initGeneratorStyleGrid() {
    if (generatorGridInitialized) return;
    generatorGridInitialized = true;

    const grid = document.getElementById('genStyleGrid');
    if (!grid) return;

    grid.innerHTML = PROMPT_STYLES.map((style, index) => `
        <div class="style-option ${index === 0 ? 'selected' : ''}" data-type="${style.type}">
            <div class="style-name">${escapeHtml(style.name)}</div>
            <div class="style-desc">${escapeHtml(style.desc)}</div>
        </div>
    `).join('');

    grid.querySelectorAll('.style-option').forEach(option => {
        option.addEventListener('click', () => {
            grid.querySelectorAll('.style-option').forEach(o => o.classList.remove('selected'));
            option.classList.add('selected');
            selectedGenStyle = option.getAttribute('data-type');
        });
    });
}

function bindGenerator() {
    const submitBtn = document.getElementById('genSubmitBtn');
    if (!submitBtn) return;

    submitBtn.addEventListener('click', async () => {
        const task = document.getElementById('genTask').value.trim();
        const context = document.getElementById('genContext').value.trim();

        if (!task) {
            toastWarning('Please describe the task you want a prompt for');
            return;
        }

        try {
            await withButtonLoading(submitBtn, async () => {
                const result = await API.prompts.generate({
                    task,
                    promptType: selectedGenStyle,
                    context: context || null
                });
                renderToolResult('genResultPanel', result, 'generator');
                toastSuccess('Prompt generated successfully');
            });
        } catch (error) {
            toastError(error.message || 'Failed to generate prompt');
        }
    });
}

/* ================================================================
   PROMPT OPTIMIZER
   ================================================================ */
function bindOptimizer() {
    const submitBtn = document.getElementById('optSubmitBtn');
    if (!submitBtn) return;

    submitBtn.addEventListener('click', async () => {
        const originalPrompt = document.getElementById('optOriginal').value.trim();

        if (!originalPrompt) {
            toastWarning('Please paste a prompt to optimize');
            return;
        }

        try {
            await withButtonLoading(submitBtn, async () => {
                const result = await API.prompts.optimize({ originalPrompt });
                renderToolResult('optResultPanel', result, 'optimizer');
                toastSuccess('Prompt optimized successfully');
            });
        } catch (error) {
            toastError(error.message || 'Failed to optimize prompt');
        }
    });
}

/* ================================================================
   SHARED RESULT RENDERER (Generator + Optimizer)
   ================================================================ */
function renderToolResult(panelId, result, sourceModule) {
    const panel = document.getElementById(panelId);
    if (!panel) return;

    panel.dataset.promptId = result.id;

    panel.innerHTML = `
        <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:14px;">
            <h3 style="font-size:1rem; margin:0;">${sourceModule === 'generator' ? '✨ Generated Prompt' : '✨ Optimized Prompt'}</h3>
            ${result.promptType ? `<span class="chip">${escapeHtml(promptTypeLabel(result.promptType))}</span>` : ''}
        </div>
        <div class="result-output fade-in">${escapeHtml(result.generatedPrompt)}</div>
        <div class="result-actions">
            <button class="btn btn-secondary btn-sm" onclick="copyToClipboard(this)" data-copy-target="${result.id}">📋 Copy</button>
            <button class="btn btn-primary btn-sm save-to-library-btn" data-prompt-id="${result.id}">💾 Save to Library</button>
            <button class="btn btn-secondary btn-sm export-btn" data-prompt-id="${result.id}" data-format="PDF">⬇ Export PDF</button>
        </div>
    `;

    panel.querySelector('.result-output').dataset.rawText = result.generatedPrompt;

    const copyBtn = panel.querySelector('[data-copy-target]');
    if (copyBtn) {
        copyBtn.addEventListener('click', () => {
            navigator.clipboard.writeText(result.generatedPrompt)
                .then(() => toastSuccess('Copied to clipboard'))
                .catch(() => toastError('Failed to copy'));
        });
    }

    const saveBtn = panel.querySelector('.save-to-library-btn');
    if (saveBtn) {
        saveBtn.addEventListener('click', () => handleSaveExistingPrompt(saveBtn, result.id));
    }

    const exportBtn = panel.querySelector('.export-btn');
    if (exportBtn) {
        exportBtn.addEventListener('click', () => handleExportPrompt(exportBtn, result.id, 'PDF'));
    }
}

/**
 * Saves an already-created prompt record (Generator/Optimizer/Analyzer
 * result) to the Library via the lightweight PATCH endpoint, updating
 * the triggering button's state on success.
 */
async function handleSaveExistingPrompt(button, promptId) {
    try {
        await withButtonLoading(button, async () => {
            await API.prompts.saveExisting(promptId);
            button.textContent = '✓ Saved to Library';
            button.disabled = true;
            toastSuccess('Prompt saved to your library');
        });
    } catch (error) {
        toastError(error.message || 'Failed to save prompt');
    }
}

/**
 * Triggers a file download for the given prompt via the Export API.
 * Shared by every module that shows a result with export actions.
 */
async function handleExportPrompt(button, promptId, format) {
    try {
        await withButtonLoading(button, async () => {
            await API.export.download(promptId, format);
            toastSuccess(`Exported as ${format}`);
        });
    } catch (error) {
        toastError(error.message || 'Export failed. Save the prompt to your library first if you haven\'t already.');
    }
}

/* ================================================================
   PROMPT ANALYZER
   ================================================================ */
function bindAnalyzer() {
    const submitBtn = document.getElementById('anzSubmitBtn');
    if (!submitBtn) return;

    submitBtn.addEventListener('click', async () => {
        const promptText = document.getElementById('anzText').value.trim();

        if (!promptText) {
            toastWarning('Please paste a prompt to analyze');
            return;
        }

        try {
            await withButtonLoading(submitBtn, async () => {
                const result = await API.prompts.analyze({ promptText });
                renderAnalysisResult(result);
                toastSuccess('Analysis complete');
            });
        } catch (error) {
            toastError(error.message || 'Failed to analyze prompt');
        }
    });
}

function renderAnalysisResult(result) {
    const panel = document.getElementById('anzResultPanel');
    if (!panel) return;

    const a = result.analysis;
    if (!a) {
        panel.innerHTML = `<div class="empty-state"><div class="empty-icon">⚠️</div><h4>No analysis data returned</h4></div>`;
        return;
    }

    const scores = [
        { label: 'Grammar', value: a.grammarScore },
        { label: 'Clarity', value: a.clarityScore },
        { label: 'Context', value: a.contextScore },
        { label: 'Hallucination Risk', value: a.hallucinationRisk },
        { label: 'Complexity', value: a.complexityScore }
    ];

    panel.innerHTML = `
        <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:18px;">
            <h3 style="font-size:1rem; margin:0;">📊 Analysis Results</h3>
            <span class="badge ${overallScoreBadgeClass(a.overallScore)}">Overall: ${a.overallScore}/100</span>
        </div>

        <div class="score-grid">
            ${scores.map(s => `
                <div class="score-ring-card">
                    <div class="score-ring" style="--pct:${s.value}">
                        <span>${s.value}</span>
                    </div>
                    <div class="score-ring-label">${escapeHtml(s.label)}</div>
                </div>
            `).join('')}
        </div>

        <h4 style="font-size:0.9rem; margin-bottom:12px;">Suggestions</h4>
        <ul class="suggestions-list">
            ${a.suggestions.map(s => `<li>${escapeHtml(s)}</li>`).join('')}
        </ul>

        <div class="result-actions" style="margin-top:20px;">
            <button class="btn btn-primary btn-sm save-to-library-btn" data-prompt-id="${result.id}">💾 Save to Library</button>
            <button class="btn btn-secondary btn-sm export-btn" data-prompt-id="${result.id}" data-format="PDF">⬇ Export PDF</button>
        </div>
    `;

    const saveBtn = panel.querySelector('.save-to-library-btn');
    if (saveBtn) saveBtn.addEventListener('click', () => handleSaveExistingPrompt(saveBtn, result.id));

    const exportBtn = panel.querySelector('.export-btn');
    if (exportBtn) exportBtn.addEventListener('click', () => handleExportPrompt(exportBtn, result.id, 'PDF'));
}

function overallScoreBadgeClass(score) {
    if (score >= 75) return 'badge-success';
    if (score >= 50) return 'badge-warning';
    return 'badge-danger';
}

/* ================================================================
   BATTLE ARENA
   ================================================================ */
function initBattleStyleGrid() {
    if (battleGridInitialized) return;
    battleGridInitialized = true;

    const grid = document.getElementById('battleStyleGrid');
    if (!grid) return;

    grid.innerHTML = PROMPT_STYLES.map(style => `
        <div class="style-option" data-type="${style.type}">
            <div class="style-name">${escapeHtml(style.name)}</div>
            <div class="style-desc">${escapeHtml(style.desc)}</div>
        </div>
    `).join('');

    grid.querySelectorAll('.style-option').forEach(option => {
        option.addEventListener('click', () => {
            const type = option.getAttribute('data-type');
            if (selectedBattleStyles.has(type)) {
                selectedBattleStyles.delete(type);
                option.classList.remove('selected');
            } else {
                selectedBattleStyles.add(type);
                option.classList.add('selected');
            }
        });
    });
}

function bindBattle() {
    const submitBtn = document.getElementById('battleSubmitBtn');
    if (!submitBtn) return;

    submitBtn.addEventListener('click', async () => {
        const task = document.getElementById('battleTask').value.trim();
        const context = document.getElementById('battleContext').value.trim();

        if (!task) {
            toastWarning('Please describe the task for the battle');
            return;
        }

        const resultsContainer = document.getElementById('battleResults');
        resultsContainer.innerHTML = `
            <div class="empty-state">
                <div class="loader-ring" style="width:36px; height:36px; margin-bottom:16px;"></div>
                <h4>Running the battle...</h4>
                <p>Generating and scoring multiple prompt styles. This may take a moment.</p>
            </div>`;

        try {
            await withButtonLoading(submitBtn, async () => {
                const payload = {
                    task,
                    context: context || null,
                    styles: selectedBattleStyles.size > 0 ? Array.from(selectedBattleStyles) : null
                };
                const result = await API.prompts.battle(payload);
                renderBattleResult(result);
                toastSuccess('Battle arena comparison complete');
            });
        } catch (error) {
            resultsContainer.innerHTML = '';
            toastError(error.message || 'Battle arena failed');
        }
    });
}

function renderBattleResult(result) {
    const container = document.getElementById('battleResults');
    if (!container) return;

    const battle = result.battle;
    if (!battle || !battle.entries || battle.entries.length === 0) {
        container.innerHTML = `<div class="empty-state"><div class="empty-icon">⚠️</div><h4>No battle results returned</h4></div>`;
        return;
    }

    const sortedEntries = [...battle.entries].sort((a, b) => b.score - a.score);

    container.innerHTML = `
        <div class="battle-recommendation fade-in-up">
            <span class="icon">🏆</span>
            <div>
                <strong>Recommended: ${escapeHtml(promptTypeLabel(battle.recommendedType))}</strong>
                <p style="margin-top:4px; font-size:0.88rem; color:var(--text-secondary);">${escapeHtml(battle.recommendationReason)}</p>
            </div>
        </div>

        <div class="battle-grid">
            ${sortedEntries.map(entry => {
                const isWinner = entry.promptType === battle.recommendedType;
                return `
                    <div class="battle-card card fade-in-up ${isWinner ? 'winner' : ''}">
                        ${isWinner ? '<span class="battle-winner-badge">🏆 Winner</span>' : ''}
                        <div class="battle-card-header">
                            <span class="chip">${escapeHtml(promptTypeLabel(entry.promptType))}</span>
                            <span class="battle-score">${entry.score}</span>
                        </div>
                        <details>
                            <summary style="cursor:pointer; font-size:0.82rem; color:var(--brand-primary); font-weight:600; margin-bottom:8px;">View prompt used</summary>
                            <div class="battle-output" style="margin-bottom:10px;">${escapeHtml(entry.promptText)}</div>
                        </details>
                        <div>
                            <div style="font-size:0.78rem; font-weight:700; color:var(--text-muted); margin-bottom:6px;">AI OUTPUT</div>
                            <div class="battle-output">${escapeHtml(entry.aiOutput)}</div>
                        </div>
                    </div>
                `;
            }).join('')}
        </div>

        <div class="result-actions" style="margin-top:24px;">
            <button class="btn btn-primary btn-sm save-to-library-btn" data-prompt-id="${result.id}">💾 Save Battle to Library</button>
            <button class="btn btn-secondary btn-sm export-btn" data-prompt-id="${result.id}" data-format="PDF">⬇ Export PDF</button>
        </div>
    `;

    const saveBtn = container.querySelector('.save-to-library-btn');
    if (saveBtn) saveBtn.addEventListener('click', () => handleSaveExistingPrompt(saveBtn, result.id));

    const exportBtn = container.querySelector('.export-btn');
    if (exportBtn) exportBtn.addEventListener('click', () => handleExportPrompt(exportBtn, result.id, 'PDF'));
}

/* ---------- SHARED: COPY TO CLIPBOARD ---------- */
function copyToClipboard(button) {
    const panel = button.closest('.result-panel');
    const output = panel ? panel.querySelector('.result-output') : null;
    const text = output ? output.dataset.rawText : '';

    if (!text) return;

    navigator.clipboard.writeText(text)
        .then(() => toastSuccess('Copied to clipboard'))
        .catch(() => toastError('Failed to copy'));
}