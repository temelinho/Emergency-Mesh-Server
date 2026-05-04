// ═══════════════════════════════════════════════════════════════
// EmergencyMesh — Command Center Admin JS  (v2 — Full Features)
// ═══════════════════════════════════════════════════════════════

let stats = { active: 0, users: 0, resolved: 0, responders: 0, respondersActive: 0 };
let emergencies = [];
let users = [];
let responders = [];
let map;
let markers = {};
let showHeatmap = false;
let showResponders = true;
let showEmergencies = true;
let heatmapLayer;
let charts = {};

// Filters
let statusFilter = 'ALL';
let typeFilter = 'ALL';

// Feed filter & sort
let feedFilter = 'ALL';
let feedSort = 'PRIORITY';

const PRIORITY_ORDER = { EMERGENCY: 4, HIGH: 3, MEDIUM: 2, LOW: 1 };

// Map style
let mapStyleIndex = 0;
const MAP_TILES = [
    { name: 'Dark', url: 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', attr: '© CartoDB' },
    { name: 'Satellite', url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', attr: '© Esri' },
    { name: 'Street', url: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', attr: '© OSM' }
];
let currentTileLayer;

// Sound / notification
let previousEmergencyIds = new Set();
let isFirstLoad = true;

// Modal mini-map
let miniMap = null;
let miniMarker = null;

const API_BASE = '/api/v1';

// fetchAllData debounce (çok sayıda mesaj geldiğinde API fırtınasını önler)
let fetchAllDataTimer = null;
const FETCH_DEBOUNCE_MS = 800;

function scheduleFetchAllData() {
    if (fetchAllDataTimer) clearTimeout(fetchAllDataTimer);
    fetchAllDataTimer = setTimeout(() => {
        fetchAllDataTimer = null;
        fetchAllData();
    }, FETCH_DEBOUNCE_MS);
}

// ─── INIT ───────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    initMap();
    initIcons();
    initNavigation();
    initClock();
    initCharts();
    initFilters();
    fetchAllData();
    initWebSocket();

    setInterval(fetchAllData, 60000); // Polling'i 60 saniyeye düşürdüm, WebSocket ana kaynak olacak

    document.getElementById('refresh-btn')?.addEventListener('click', fetchAllData);
    document.getElementById('test-emergency-btn')?.addEventListener('click', createTestEmergency);
    document.getElementById('toggle-heatmap')?.addEventListener('click', toggleHeatmap);
    document.getElementById('toggle-responders')?.addEventListener('click', toggleResponders);
    document.getElementById('toggle-emergencies')?.addEventListener('click', toggleEmergencies);
    document.getElementById('btn-fit-bounds')?.addEventListener('click', fitAllBounds);
    document.getElementById('btn-map-style')?.addEventListener('click', cycleMapStyle);
    document.getElementById('btn-export-csv')?.addEventListener('click', exportCSV);
    document.getElementById('btn-open-add-team')?.addEventListener('click', openAddTeamModal);
});

// ─── CLOCK ──────────────────────────────────────────────────
function initClock() {
    const tick = () => {
        const el = document.getElementById('system-clock');
        if (el) el.textContent = new Date().toLocaleTimeString('tr-TR');
    };
    tick();
    setInterval(tick, 1000);
}

// ─── ICONS ──────────────────────────────────────────────────
function initIcons() { if (typeof lucide !== 'undefined') lucide.createIcons(); }

// ─── NAVIGATION ─────────────────────────────────────────────
function initNavigation() {
    document.querySelectorAll('.nav-item').forEach(item => {
        item.addEventListener('click', () => {
            const view = item.dataset.view;
            if (!view) return;
            switchView(view);
            document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
            item.classList.add('active');
        });
    });
}

function switchView(viewId) {
    ['dashboard', 'map', 'requests', 'users', 'responders'].forEach(s => {
        const el = document.getElementById(`section-${s}`);
        if (el) el.style.display = (s === viewId) ? 'block' : 'none';
    });

    const titles = {
        dashboard: { t: 'Command Center', s: 'Stratejik Acil Durum Analitik Paneli' },
        map: { t: 'Canlı Saha Takibi', s: 'Tüm birimlerin ve vakaların gerçek zamanlı lokasyonları' },
        responders: { t: 'Saha Ekipleri', s: 'Aktif müdahale ekiplerinin durum ve lokasyon yönetimi' },
        requests: { t: 'Vaka Yönetimi', s: 'Gelen acil durum taleplerinin sevk ve idaresi' },
        users: { t: 'Kullanıcı Kayıtları', s: 'Sisteme kayıtlı vatandaşların güvenli veri tabanı' }
    };

    const info = titles[viewId];
    if (info) {
        document.getElementById('view-title').textContent = info.t;
        document.getElementById('view-subtitle').textContent = info.s;
    }

    if (viewId === 'map') setTimeout(() => map?.invalidateSize(), 250);
}

// ─── FILTERS ────────────────────────────────────────────────
function initFilters() {
    document.querySelectorAll('#status-filters .filter-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('#status-filters .filter-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            statusFilter = btn.dataset.status;
            updateMap();
            renderRequestsTable();
        });
    });

    document.querySelectorAll('#type-filters .filter-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('#type-filters .filter-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            typeFilter = btn.dataset.type;
            updateMap();
            renderRequestsTable();
        });
    });

    const priorityFilterEl = document.getElementById('filter-priority');
    if (priorityFilterEl) {
        priorityFilterEl.addEventListener('change', () => {
            updateMap();
            renderRequestsTable();
        });
    }

    const searchInput = document.getElementById('filter-search');
    if (searchInput) {
        searchInput.addEventListener('input', () => {
            updateMap();
            renderRequestsTable();
        });
    }

    const timeFilter = document.getElementById('filter-time');
    if (timeFilter) {
        timeFilter.addEventListener('change', () => {
            updateMap();
            renderRequestsTable();
        });
    }

    // Responder Filters
    const respSearch = document.getElementById('responder-search');
    if (respSearch) {
        respSearch.addEventListener('input', () => renderRespondersTable());
    }
    const respStatusFilter = document.getElementById('responder-filter-status');
    if (respStatusFilter) {
        respStatusFilter.addEventListener('change', () => renderRespondersTable());
    }
    const respTypeFilter = document.getElementById('responder-filter-type');
    if (respTypeFilter) {
        respTypeFilter.addEventListener('change', () => renderRespondersTable());
    }
}

function getFilteredEmergencies() {
    const timeVal = document.getElementById('filter-time') ? document.getElementById('filter-time').value : 'ALL';
    const prioVal = document.getElementById('filter-priority') ? document.getElementById('filter-priority').value : 'ALL';
    const searchVal = document.getElementById('filter-search') ? document.getElementById('filter-search').value.toLowerCase().trim() : '';
    const now = new Date();

    return emergencies.filter(e => {
        if (statusFilter !== 'ALL' && e.status !== statusFilter) return false;
        if (typeFilter !== 'ALL' && e.type !== typeFilter) return false;
        if (prioVal !== 'ALL' && (e.priority || 'MEDIUM').toUpperCase() !== prioVal) return false;

        if (timeVal === '1H') {
            const hAgo = new Date(now.getTime() - 60 * 60 * 1000);
            if (new Date(e.createdAt) < hAgo) return false;
        } else if (timeVal === '24H') {
            const dAgo = new Date(now.getTime() - 24 * 60 * 60 * 1000);
            if (new Date(e.createdAt) < dAgo) return false;
        }

        if (searchVal) {
            const userName = `${e.userFirstName || ''} ${e.userLastName || ''}`.toLowerCase();
            const str = `${e.id} ${e.description} ${getTypeLabel(e.type)} ${e.status} ${e.priority} ${userName}`.toLowerCase();
            if (!str.includes(searchVal)) return false;
        }

        return true;
    });
}

// ─── DATA FETCHING ──────────────────────────────────────────
async function fetchAllData() {
    console.log('[CMD] Syncing data (V3)...');
    try {
        await Promise.all([
            fetchStatsAndEmergencies(),
            fetchUsers(),
            fetchResponders()
        ]);
        renderAll();
        console.log(`[CMD] Sync OK (V3) — ${emergencies.length} vakalar`);
    } catch (err) {
        console.error('[CMD] Sync failed:', err);
    }
}

async function fetchStatsAndEmergencies() {
    console.log("[DATA] Fetching emergencies from API...");
    try {
        const res = await fetch(`${API_BASE}/emergency?size=200`);
        const data = await res.json();

        const apiContent = data.content || [];
        const oldCount = emergencies.length;
        emergencies = mergeEmergencies(apiContent, emergencies);
        console.log(`[DATA] Fetched ${apiContent.length} from API, merged to ${emergencies.length} (Previous: ${oldCount})`);
        if (emergencies.length > 0) {
            console.log("[DATA] Latest emergency sample:", emergencies[0]);
        }

        // İstatistikleri hesapla
        stats.active = emergencies.filter(e => e.status !== 'RESOLVED').length;
        stats.resolved = emergencies.filter(e => e.status === 'RESOLVED').length;

        const el = document.getElementById('stat-active');
        if (el) el.textContent = stats.active;
        const elR = document.getElementById('stat-resolved');
        if (elR) elR.textContent = stats.resolved;

        checkNewEmergencies();
        renderAll();
    } catch (err) {
        console.error("[DATA] Error fetching emergencies:", err);
    }
}

async function fetchResponders() {
    try {
        const res = await fetch(`${API_BASE}/responders`);
        if (!res.ok) { responders = []; return; }
        responders = await res.json();
    } catch { responders = []; }
    stats.responders = responders.length;
    stats.respondersActive = responders.filter(r => r.status === 'BUSY').length;
    const el = document.getElementById('stat-responders');
    if (el) el.textContent = `${stats.respondersActive} / ${stats.responders}`;
}

async function fetchUsers() {
    try {
        const res = await fetch(`${API_BASE}/users/all`);
        if (!res.ok) { users = []; return; }
        users = await res.json();
    } catch { users = []; }
    stats.users = users.length;
    const el = document.getElementById('stat-users');
    if (el) el.textContent = stats.users;
}

// ─── NEW EMERGENCY DETECTION & NOTIFICATION ─────────────────
// String ID kullan (UUID format farkları için)
function toIdStr(id) {
    return id != null ? String(id) : null;
}

// API + WebSocket merge (race: yeni vaka henüz API'de olmayabilir, grafikte kaybolmasın)
function mergeEmergencies(apiContent, currentList) {
    const apiIds = new Set((apiContent || []).map(e => toIdStr(e.id)).filter(Boolean));
    const localOnly = (currentList || []).filter(e => {
        const id = toIdStr(e.id);
        return id && !apiIds.has(id);
    });
    const merged = [...(apiContent || []), ...localOnly];
    const seen = new Set();
    const deduped = merged.filter(e => {
        const id = toIdStr(e.id);
        if (!id || seen.has(id)) return false;
        seen.add(id);
        return true;
    });
    return deduped.sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0));
}

function checkNewEmergencies() {
    const currentIds = new Set(emergencies.map(e => toIdStr(e.id)).filter(Boolean));

    if (isFirstLoad) {
        previousEmergencyIds = currentIds;
        isFirstLoad = false;
        return;
    }

    emergencies.forEach(e => {
        const idStr = toIdStr(e.id);
        if (!idStr) return;
        if (!previousEmergencyIds.has(idStr)) {
            // Gerçekten yeni - bildir (WebSocket'ten zaten bildirdiysek previousEmergencyIds'te olacak)
            playAlertSound(e.priority);
            showToast(e);
            showBrowserNotification(e);
        }
    });

    previousEmergencyIds = currentIds;
}

function playAlertSound(priority) {
    try {
        const ctx = new (window.AudioContext || window.webkitAudioContext)();
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();

        osc.connect(gain);
        gain.connect(ctx.destination);

        const isUrgent = priority === 'EMERGENCY' || priority === 'HIGH';
        osc.frequency.value = isUrgent ? 880 : 523;
        osc.type = isUrgent ? 'sawtooth' : 'sine';
        gain.gain.value = 0.15;

        osc.start();
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + (isUrgent ? 0.8 : 0.4));
        osc.stop(ctx.currentTime + (isUrgent ? 0.8 : 0.4));

        if (isUrgent) {
            setTimeout(() => {
                const osc2 = ctx.createOscillator();
                const gain2 = ctx.createGain();
                osc2.connect(gain2);
                gain2.connect(ctx.destination);
                osc2.frequency.value = 1100;
                osc2.type = 'sawtooth';
                gain2.gain.value = 0.15;
                osc2.start();
                gain2.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 1.2);
                osc2.stop(ctx.currentTime + 1.2);
            }, 300);
        }
    } catch (err) {
        console.warn('Audio alert failed:', err);
    }
}

function showBrowserNotification(e) {
    if (!('Notification' in window)) return;
    if (Notification.permission === 'granted') {
        new Notification(`🚨 Yeni ${getTypeLabel(e.type)} Vakası`, {
            body: `${e.description}\nÖncelik: ${e.priority}`,
            icon: '🚨',
            tag: e.id
        });
    } else if (Notification.permission !== 'denied') {
        Notification.requestPermission();
    }
}

// ─── TOAST NOTIFICATIONS ────────────────────────────────────
function showToast(titleOrEmergency, desc, type = "info") {
    const container = document.getElementById("toast-container");
    if (!container) return;

    let title, description, toastType = type;

    // Handle both signatures: showToast(emergencyObject) and showToast(title, desc, type)
    if (typeof titleOrEmergency === 'object') {
        const e = titleOrEmergency;
        title = `🚨 YENİ ${getTypeLabel(e.type).toUpperCase()}`;
        description = `${getTypeEmoji(e.type)} ${e.description || "Acil yardım talebi"}`;
        toastType = e.priority === 'EMERGENCY' ? 'warning' : 'info';
    } else {
        title = titleOrEmergency;
        description = desc;
    }

    const cleanTitle = title && title !== "undefined" ? title : "Sistem Uyarısı";
    const cleanDesc = description && description !== "undefined" ? description : "Yeni bildirim alındı";

    const toast = document.createElement("div");
    toast.className = `toast toast-${toastType}`;
    toast.innerHTML = `
        <div class="toast-icon">${toastType === "warning" ? "🚨" : "📡"}</div>
        <div class="toast-body">
            <div class="toast-title">${cleanTitle}</div>
            <div class="toast-desc">${cleanDesc}</div>
        </div>
        <button class="toast-close" onclick="this.parentElement.remove()">&times;</button>
    `;

    container.appendChild(toast);
    setTimeout(() => toast.classList.add('show'), 100);

    // Auto remove
    setTimeout(() => {
        if (toast.parentNode) {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 400);
        }
    }, 8000);
}

// ─── RENDER ALL ─────────────────────────────────────────────
function renderAll() {
    renderRequestsTable();
    renderRespondersTable();
    renderUsersTable();
    renderActivityFeed();
    updateMap();
    updateMapStats();
    updateTicker();
    updateCharts();
    updateResponderStats();
}

// ─── TICKER ─────────────────────────────────────────────────
function updateTicker() {
    const el = document.getElementById('incident-ticker');
    if (!el) return;
    const pending = emergencies.filter(e => e.status === 'PENDING').slice(0, 5);
    const text = pending.length === 0
        ? 'Tüm bölgeler sakin — anlık aktif vaka bulunmuyor.'
        : pending.map(e => `⚠ [${e.priority}] ${e.type}: ${e.description}`).join('  ·  ');
    el.innerHTML = `<span>${text}</span>`;
}

// ─── MAP STATS OVERLAY ──────────────────────────────────────
function updateMapStats() {
    const elA = document.getElementById('map-stat-active');
    const elR = document.getElementById('map-stat-responders');
    const elS = document.getElementById('map-stat-resolved');
    if (elA) elA.textContent = stats.active;
    if (elR) elR.textContent = responders.filter(r => r.status === 'AVAILABLE').length;
    if (elS) elS.textContent = stats.resolved;
}

// ─── CHARTS ─────────────────────────────────────────────────
function initCharts() {
    Chart.defaults.color = '#94a3b8';
    Chart.defaults.borderColor = 'rgba(255,255,255,0.04)';

    const ctxPeak = document.getElementById('peakHourChart');
    if (ctxPeak) {
        charts.peak = new Chart(ctxPeak.getContext('2d'), {
            type: 'bar',
            data: {
                labels: Array.from({ length: 24 }, (_, i) => `${String(i).padStart(2, '0')}:00`),
                datasets: [{
                    label: 'Vaka',
                    data: new Array(24).fill(0),
                    backgroundColor: 'rgba(59,130,246,0.5)',
                    borderColor: '#3b82f6',
                    borderWidth: 1,
                    borderRadius: 4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    y: { beginAtZero: true, ticks: { stepSize: 1 }, grid: { color: 'rgba(255,255,255,0.04)' } },
                    x: { grid: { display: false }, ticks: { maxRotation: 0, autoSkip: true, maxTicksLimit: 12 } }
                }
            }
        });
    }

    const ctxPri = document.getElementById('priorityChart');
    if (ctxPri) {
        charts.priority = new Chart(ctxPri.getContext('2d'), {
            type: 'doughnut',
            data: {
                labels: ['Düşük', 'Orta', 'Yüksek', 'Acil'],
                datasets: [{
                    data: [1, 1, 1, 1],
                    backgroundColor: ['#10b981', '#f59e0b', '#f97316', '#ef4444'],
                    borderWidth: 0,
                    hoverOffset: 15
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '65%',
                plugins: { legend: { position: 'bottom', labels: { padding: 16, usePointStyle: true, pointStyleWidth: 10 } } }
            }
        });
    }
}

function updateCharts() {
    if (!emergencies.length) return;

    if (charts.priority) {
        const c = { LOW: 0, MEDIUM: 0, HIGH: 0, EMERGENCY: 0 };
        emergencies.forEach(e => {
            const p = (e.priority || 'MEDIUM').toUpperCase();
            if (p in c) c[p]++;
        });
        charts.priority.data.datasets[0].data = [c.LOW, c.MEDIUM, c.HIGH, c.EMERGENCY];
        charts.priority.update('none');
    }

    if (charts.peak) {
        const hours = new Array(24).fill(0);
        emergencies.forEach(e => {
            const d = new Date(e.createdAt);
            if (!isNaN(d.getTime())) hours[d.getHours()]++;
        });
        charts.peak.data.datasets[0].data = hours;
        charts.peak.update('none');
    }
}

// ─── ACTIVITY FEED ──────────────────────────────────────────
function setFeedFilter(value) {
    feedFilter = value;
    document.querySelectorAll('.feed-filter-btn').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.feedStatus === value);
    });
    renderActivityFeed();
}

function setFeedSort(value) {
    feedSort = value;
    document.querySelectorAll('.feed-sort-btn').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.feedSort === value);
    });
    renderActivityFeed();
}

function renderActivityFeed() {
    const el = document.getElementById('activity-feed');
    if (!el) return;

    // Apply filter
    let filtered = feedFilter === 'ALL'
        ? [...emergencies]
        : emergencies.filter(e => e.status === feedFilter);

    // Apply sort
    if (feedSort === 'PRIORITY') {
        filtered.sort((a, b) => {
            const pa = PRIORITY_ORDER[(a.priority || 'MEDIUM').toUpperCase()] || 2;
            const pb = PRIORITY_ORDER[(b.priority || 'MEDIUM').toUpperCase()] || 2;
            return pb - pa; // HIGH → LOW
        });
    } else if (feedSort === 'PRIORITY_ASC') {
        filtered.sort((a, b) => {
            const pa = PRIORITY_ORDER[(a.priority || 'MEDIUM').toUpperCase()] || 2;
            const pb = PRIORITY_ORDER[(b.priority || 'MEDIUM').toUpperCase()] || 2;
            return pa - pb; // LOW → HIGH
        });
    } else {
        // TIME (default): newest first
        filtered.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    }

    filtered = filtered.slice(0, 20);

    if (!filtered.length) {
        el.innerHTML = '<li style="color:var(--text-dim);padding:1rem;text-align:center;">Seçili filtrede vaka bulunmuyor.</li>';
        return;
    }

    const priorityColors = { EMERGENCY: '#ef4444', HIGH: '#f97316', MEDIUM: '#f59e0b', LOW: '#10b981' };

    el.innerHTML = filtered.map(e => `
        <li class="feed-item" onclick="openDetailModal('${e.id}')" style="cursor:pointer;">
            <div class="feed-icon ${(e.type || '').toLowerCase()}">${getTypeEmoji(e.type)}</div>
            <div class="feed-body">
                <div class="feed-title">${getTypeLabel(e.type)} — ${e.status}</div>
                <div class="feed-desc">${e.description}</div>
            </div>
            <div class="feed-meta">
                <div class="feed-time">${timeAgo(e.createdAt)}</div>
                <span class="feed-priority" style="background:${priorityColors[e.priority] || '#64748b'};"></span>
            </div>
        </li>
    `).join('');
}

// ─── TABLES ─────────────────────────────────────────────────
function renderRequestsTable() {
    const tbody = document.getElementById('requests-body');
    if (!tbody) return;

    // Use filtered data so the table respects UI filters
    const filtered = getFilteredEmergencies();
    console.log(`[RENDER] Table: showing ${filtered.length} of ${emergencies.length} total emergencies (Filters: Status=${statusFilter}, Type=${typeFilter})`);

    if (!filtered.length) {
        tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:var(--text-dim);padding:2rem;">Kriterlere uygun talep bulunmuyor.</td></tr>';
        return;
    }

    const SLA_MINUTES = 30;

    try {
        tbody.innerHTML = filtered.map(r => {
            const status = r.status || 'UNKNOWN';
            const priority = r.priority || 'MEDIUM';
            const type = r.type || 'OTHER';
            const elapsed = timeAgo(r.createdAt);
            const minutesElapsed = r.createdAt ? (Date.now() - parseUTCDate(r.createdAt).getTime()) / 60000 : 0;
            const isSLA = status !== 'RESOLVED' && minutesElapsed > SLA_MINUTES;

            const fullName = (r.userFirstName || r.userLastName)
                ? `${r.userFirstName || ''} ${r.userLastName || ''}`.trim()
                : (r.userId || '').substring(0, 8);

            const userDisplay = `<div class="user-info-cell" title="TEL: ${r.userPhoneNumber || '—'}">
                <span class="user-name">${fullName}</span>
                <span class="user-id-small">${(r.userId || '').substring(0, 8)}</span>
            </div>`;

            return `
            <tr class="${isSLA ? 'sla-warning' : ''}">
                <td><span class="badge priority-${priority.toLowerCase()}">${priority}</span></td>
                <td><span class="type-indicator" title="${type}">${getTypeEmoji(type)} ${getTypeLabel(type)}</span></td>
                <td>${userDisplay}</td>
                <td style="cursor:pointer;text-decoration:underline dotted;color:var(--primary);" onclick="openDetailModal('${r.id}')">${r.description || '—'}</td>
                <td><span class="status-badge badge-${status.toLowerCase()}">${status}</span></td>
                <td><span class="time-ago ${isSLA ? 'sla-critical' : ''}">${elapsed}</span></td>
                <td>
                    <div style="display:flex;gap:4px;">
                        ${status !== 'RESOLVED' ? `<button class="btn btn-success btn-sm" onclick="resolveEmergency('${r.id}')">✓ Çöz</button>` : ''}
                        ${(status === 'PENDING' || status === 'IN_PROGRESS') ? `<button class="btn btn-primary btn-sm" onclick="showAssignModal('${r.id}')">Ata</button>` : ''}
                    </div>
                </td>
            </tr>
        `}).join('');
    } catch (err) {
        console.error("[RENDER] Error in requests table mapping:", err);
    }
}

function renderRespondersTable() {
    const grid = document.getElementById('responders-grid');
    if (!grid) return;

    const searchTerm = document.getElementById('responder-search')?.value.toLowerCase().trim() || '';
    const statusFilterVal = document.getElementById('responder-filter-status')?.value || 'ALL';
    const typeFilterVal = document.getElementById('responder-filter-type')?.value || 'ALL';

    const filtered = responders.filter(r => {
        if (statusFilterVal !== 'ALL' && r.status !== statusFilterVal) return false;
        if (typeFilterVal !== 'ALL' && r.type !== typeFilterVal) return false;
        if (searchTerm) {
            const str = `${r.name} ${r.type} ${r.status}`.toLowerCase();
            if (!str.includes(searchTerm)) return false;
        }
        return true;
    });

    if (!filtered.length) {
        grid.innerHTML = '<div class="loading-placeholder">Kriterlere uygun ekip bulunamadı.</div>';
        return;
    }

    grid.innerHTML = filtered.map(r => {
        const statusClass = `status-${(r.status || 'offline').toLowerCase()}`;
        const typeLabel = getResponderTypeLabel(r.type);
        const typeEmoji = getResponderEmoji(r.type);
        const lastSeen = r.updatedAt ? timeAgo(r.updatedAt) : 'Bilinmiyor';
        const regionText = (r.city || r.district) ? `${r.city || '—'} / ${r.district || '—'}` : '—';

        return `
            <div class="responder-card ${statusClass}">
                <div class="resp-header">
                    <div class="resp-identity">
                        <span class="resp-type-tag">${typeLabel}</span>
                        <h3 class="resp-name">${r.name}</h3>
                        <div class="resp-status-row">
                            <span class="resp-status-dot"></span>
                            <span class="resp-status-text">${getStatusLabel(r.status)}</span>
                        </div>
                    </div>
                    <div class="resp-icon-box">
                        ${typeEmoji}
                    </div>
                </div>
                
                <div class="resp-info-grid">
                    <div class="info-item">
                        <span class="info-label">Konum</span>
                        <span class="info-value">${r.latitude ? `${r.latitude.toFixed(4)}, ${r.longitude.toFixed(4)}` : '—'}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">Son Görülme</span>
                        <span class="info-value">${lastSeen}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">Bölge</span>
                        <span class="info-value">${regionText}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">Ekip ID</span>
                        <span class="info-value">#${(r.id || '').substring(0, 5)}</span>
                    </div>
                </div>

                <div class="resp-actions">
                    <button class="btn btn-secondary btn-sm" onclick="focusOnResponder('${r.id}')">
                        <i data-lucide="map-pin" style="width:12px;height:12px;display:inline-block;margin-right:4px;"></i> Harita
                    </button>
                    <button class="btn btn-primary btn-sm" onclick="openMessageModal('${r.id}')">
                        <i data-lucide="message-square" style="width:12px;height:12px;display:inline-block;margin-right:4px;"></i> Mesaj
                    </button>
                </div>
            </div>
        `;
    }).join('');

    // Re-init icons for dynamic content
    if (typeof lucide !== 'undefined') lucide.createIcons();
}

function getResponderEmoji(type) {
    const mapping = {
        'RESCUE_TEAM': '🔍',
        'AMBULANCE': '🚑',
        'FIRE_TRUCK': '🚒',
        'POLICE_CAR': '🚔',
        'OTHER': '🏃'
    };
    return mapping[type] || '🏃';
}

function getResponderTypeLabel(type) {
    const mapping = {
        'RESCUE_TEAM': 'Arama Kurtarma',
        'AMBULANCE': 'Ambulans / Sağlık',
        'FIRE_TRUCK': 'İtfaiye',
        'POLICE_CAR': 'Polis / Asayiş',
        'OTHER': 'Diğer'
    };
    return mapping[type] || type || 'Saha Ekibi';
}

function getStatusLabel(status) {
    const mapping = {
        'AVAILABLE': 'Müsait',
        'BUSY': 'Görevde',
        'OFFLINE': 'Çevrimdışı'
    };
    return mapping[status] || 'Bilinmiyor';
}

function updateResponderStats() {
    const counts = { TOTAL: responders.length, AVAILABLE: 0, BUSY: 0, OFFLINE: 0 };
    responders.forEach(r => {
        if (counts[r.status] !== undefined) counts[r.status]++;
        else counts.OFFLINE++;
    });

    const elTotal = document.getElementById('res-total-count');
    const elAvail = document.getElementById('res-available-count');
    const elBusy = document.getElementById('res-busy-count');
    const elOff = document.getElementById('res-offline-count');

    if (elTotal) elTotal.textContent = counts.TOTAL;
    if (elAvail) elAvail.textContent = counts.AVAILABLE;
    if (elBusy) elBusy.textContent = counts.BUSY;
    if (elOff) elOff.textContent = counts.OFFLINE;
}

function focusOnResponder(id) {
    const r = responders.find(res => res.id === id);
    if (!r || !r.latitude) {
        showToast("Hata", "Ekip konumu bulunamadı.", "error");
        return;
    }
    switchView('map');
    map.setView([r.latitude, r.longitude], 16);
    // Find marker and open popup
    const m = markers[`res-${r.id}`];
    if (m) m.openPopup();
}

function openMessageModal(id) {
    const r = responders.find(res => res.id === id);
    showToast("Bilgi", `${r ? r.name : 'Ekip'} ile iletişim kanalı açılıyor...`, "info");
}

function renderUsersTable() {
    const tbody = document.getElementById('users-body');
    if (!tbody) return;
    if (!users.length) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;color:var(--text-dim);padding:2rem;">Kullanıcı bulunamadı.</td></tr>';
        return;
    }
    tbody.innerHTML = users.map(u => `
        <tr>
            <td><code style="color:var(--primary)">${u.id.substring(0, 8)}</code></td>
            <td>${u.firstName || '—'}</td>
            <td>${u.lastName || '—'}</td>
            <td>${u.phoneNumber || '—'}</td>
            <td>${u.city || '—'} / ${u.district || '—'}</td>
            <td>${parseUTCDate(u.createdAt).toLocaleDateString('tr-TR')}</td>
        </tr>
    `).join('');
}

// ─── MAP ────────────────────────────────────────────────────
function initMap() {
    map = L.map('map').setView([41.0122, 28.9764], 12);
    currentTileLayer = L.tileLayer(MAP_TILES[0].url, { attribution: MAP_TILES[0].attr }).addTo(map);

    // Realistic thermal heatmap overlay (heatmap.js)
    heatmapLayer = new HeatmapOverlay({
        radius: 30,
        maxOpacity: 0.75,
        minOpacity: 0.05,
        blur: 0.85,
        scaleRadius: false,
        useLocalExtrema: false,
        latField: 'lat',
        lngField: 'lng',
        valueField: 'count',
        gradient: {
            0.05: '#0000ff',
            0.15: '#0040ff',
            0.25: '#00bfff',
            0.35: '#00ffbf',
            0.45: '#40ff00',
            0.55: '#bfff00',
            0.65: '#ffff00',
            0.75: '#ffbf00',
            0.85: '#ff8000',
            0.95: '#ff4000',
            1.0: '#ff0000'
        }
    });

    // Request notification permission
    if ('Notification' in window && Notification.permission === 'default') {
        Notification.requestPermission();
    }
}

function updateMap() {
    // Clear all existing markers
    Object.values(markers).forEach(m => { try { map.removeLayer(m); } catch (e) { } });
    markers = {};

    const filtered = getFilteredEmergencies();

    // Emergency markers with pulsating effect
    if (showEmergencies) {
        filtered.forEach(e => {
            if (!e.latitude || !e.longitude) return;
            const color = e.status === 'RESOLVED' ? '#3b82f6'
                : e.priority === 'EMERGENCY' ? '#ef4444'
                    : e.priority === 'HIGH' ? '#f97316'
                        : e.priority === 'MEDIUM' ? '#f59e0b'
                            : '#10b981';

            // Add pulse ring for active EMERGENCY/HIGH
            if (e.status !== 'RESOLVED' && (e.priority === 'EMERGENCY' || e.priority === 'HIGH')) {
                const pulseRing = L.circleMarker([e.latitude, e.longitude], {
                    radius: 18,
                    fillColor: color,
                    color: color,
                    weight: 2,
                    fillOpacity: 0.15,
                    opacity: 0.4,
                    className: 'pulse-marker'
                }).addTo(map);
                markers[`pulse-${e.id}`] = pulseRing;
            }

            const m = L.circleMarker([e.latitude, e.longitude], {
                radius: 9, fillColor: color, color: '#fff', weight: 1.5, fillOpacity: 0.85
            }).addTo(map);

            // Rich popup with action buttons
            m.bindPopup(buildEmergencyPopup(e), { maxWidth: 280 });
            markers[`em-${e.id}`] = m;
        });
    }

    // Responder markers
    if (showResponders) {
        responders.forEach(r => {
            if (!r.latitude || !r.longitude) return;
            const color = r.status === 'AVAILABLE' ? '#10b981' : '#f59e0b';
            const m = L.circleMarker([r.latitude, r.longitude], {
                radius: 7, fillColor: color, color: '#fff', weight: 2, fillOpacity: 0.9
            }).addTo(map);
            m.bindPopup(`<div class="popup-header"><span class="popup-type-icon">🚑</span><span class="popup-type-label">EKİP: ${r.name}</span></div>
                <div class="popup-desc">Tür: ${r.type}<br>Durum: ${r.status === 'AVAILABLE' ? 'MÜSAİT' : 'MEŞGUL'}</div>`);
            markers[`res-${r.id}`] = m;
        });
    }

    // Realistic thermal heatmap (heatmap.js)
    if (showHeatmap && heatmapLayer) {
        if (!map.hasLayer(heatmapLayer)) map.addLayer(heatmapLayer);

        const priorityWeight = { EMERGENCY: 10, HIGH: 6, MEDIUM: 3, LOW: 1 };
        const pts = filtered
            .filter(e => e.latitude && e.longitude)
            .map(e => ({
                lat: e.latitude,
                lng: e.longitude,
                count: priorityWeight[(e.priority || 'MEDIUM').toUpperCase()] || 2
            }));

        heatmapLayer.setData({ max: 10, data: pts });
    } else if (heatmapLayer && map.hasLayer(heatmapLayer)) {
        map.removeLayer(heatmapLayer);
    }

    drawClusters(filtered);
}

// ─── MAP CLUSTERING (Kriz Merkezi Algoritması) ──────────────
function getDistanceFromLatLonInKm(lat1, lon1, lat2, lon2) {
    const R = 6371;
    const dLat = (lat2 - lat1) * (Math.PI / 180);
    const dLon = (lon2 - lon1) * (Math.PI / 180);
    const a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(lat1 * (Math.PI / 180)) * Math.cos(lat2 * (Math.PI / 180)) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}

function drawClusters(filtered) {
    const clusters = [];
    const thresholdKm = 0.5; // Kümeler arası max 500 metre

    filtered.forEach(e => {
        if (!e.latitude || !e.longitude) return;
        let added = false;
        for (let c of clusters) {
            const d = getDistanceFromLatLonInKm(c.lat, c.lng, e.latitude, e.longitude);
            if (d < thresholdKm) {
                // Merkez koordinatı mevcutlarla ortalayarak kaydır
                c.lat = ((c.lat * c.count) + e.latitude) / (c.count + 1);
                c.lng = ((c.lng * c.count) + e.longitude) / (c.count + 1);
                c.count++;
                c.items.push(e);
                added = true;
                break;
            }
        }
        if (!added) {
            clusters.push({ lat: e.latitude, lng: e.longitude, count: 1, items: [e] });
        }
    });

    clusters.filter(c => c.count >= 5).forEach((c, idx) => {
        // Yoğunluğa göre yarıçap (Max 80 ile sınırlandırıldı - çok büyümesini önler)
        const radiusAnim = Math.min(40 + (c.count * 3), 80);

        const circle = L.circleMarker([c.lat, c.lng], {
            radius: radiusAnim,
            color: '#ef4444',
            fillColor: '#ef4444',
            fillOpacity: 0.25,
            weight: 2,
            dashArray: '5, 5',
            className: 'cluster-marker' // Animation eklenebilir
        }).addTo(map);

        circle.bindPopup(`
            <div style="text-align:center;">
                <b style="color:var(--danger);font-size:1.1rem;display:block;margin-bottom:8px;">🚨 KRİZ MERKEZİ</b>
                <span class="badge" style="background:#ef444420;color:#ef4444;border:1px solid #ef4444;">
                    Yoğunluk: ${c.count} Vaka
                </span>
                <p style="margin-top:10px;font-size:0.9rem;color:var(--text-dim)">
                   Merkez Koordinat:<br> ${c.lat.toFixed(4)}, ${c.lng.toFixed(4)}
                </p>
            </div>
        `);
        markers[`cluster-${idx}`] = circle;
    });
}

// ─── RICH POPUP BUILDER ─────────────────────────────────────
function buildEmergencyPopup(e) {
    const priorityClass = (e.priority || 'MEDIUM').toLowerCase();
    const emoji = getTypeEmoji(e.type);
    const label = getTypeLabel(e.type);
    const elapsed = timeAgo(e.createdAt);

    let actions = '';
    if (e.status !== 'RESOLVED') {
        actions += `<button class="btn btn-success btn-sm" onclick="resolveEmergency('${e.id}')">✓ Çöz</button>`;
    }
    if (e.status === 'PENDING' || e.status === 'IN_PROGRESS') {
        actions += `<button class="btn btn-primary btn-sm" onclick="showAssignModal('${e.id}')">Ekip Ata</button>`;
    }
    actions += `<button class="btn btn-secondary btn-sm" onclick="openDetailModal('${e.id}')">Detay</button>`;

    return `
        <div class="popup-header">
            <span class="popup-type-icon">${emoji}</span>
            <span class="popup-type-label">${label}</span>
        </div>
        <div class="popup-badges">
            <span class="badge priority-${priorityClass}">${e.priority}</span>
            <span class="status-badge badge-${e.status.toLowerCase()}">${e.status}</span>
        </div>
        <div class="popup-desc">${e.description}</div>
        <div class="popup-meta">📍 ${e.latitude.toFixed(4)}, ${e.longitude.toFixed(4)}<br>⏱ ${elapsed}</div>
        <div class="popup-actions">${actions}</div>
    `;
}

// ─── MAP TOGGLES ────────────────────────────────────────────
function toggleHeatmap() {
    showHeatmap = !showHeatmap;
    const btn = document.getElementById('toggle-heatmap');
    if (btn) {
        btn.textContent = showHeatmap ? 'Isı Haritası: Açık' : 'Isı Haritası: Kapalı';
        btn.classList.toggle('active', showHeatmap);
    }
    updateMap();
}

function toggleResponders() {
    showResponders = !showResponders;
    const btn = document.getElementById('toggle-responders');
    if (btn) {
        btn.textContent = showResponders ? 'Ekipleri Göster' : 'Ekipleri Gizle';
        btn.classList.toggle('active', showResponders);
    }
    updateMap();
}

function toggleEmergencies() {
    showEmergencies = !showEmergencies;
    const btn = document.getElementById('toggle-emergencies');
    if (btn) {
        btn.textContent = showEmergencies ? 'Acil Durumlar: Açık' : 'Acil Durumlar: Kapalı';
        btn.classList.toggle('active', showEmergencies);
    }
    updateMap();
}

// ─── FIT ALL BOUNDS ─────────────────────────────────────────
function fitAllBounds() {
    const latlngs = [];
    emergencies.forEach(e => { if (e.latitude && e.longitude) latlngs.push([e.latitude, e.longitude]); });
    responders.forEach(r => { if (r.latitude && r.longitude) latlngs.push([r.latitude, r.longitude]); });

    if (latlngs.length) {
        map.fitBounds(L.latLngBounds(latlngs).pad(0.1));
    }
}

// ─── MAP STYLE CYCLE ────────────────────────────────────────
function cycleMapStyle() {
    mapStyleIndex = (mapStyleIndex + 1) % MAP_TILES.length;
    const tile = MAP_TILES[mapStyleIndex];

    map.removeLayer(currentTileLayer);
    currentTileLayer = L.tileLayer(tile.url, { attribution: tile.attr }).addTo(map);

    const btn = document.getElementById('btn-map-style');
    if (btn) btn.textContent = `🗺️ ${tile.name}`;
}

// ─── CSV EXPORT ─────────────────────────────────────────────
function exportCSV() {
    if (!emergencies.length) return;

    const headers = ['ID', 'Tür', 'Öncelik', 'Durum', 'Açıklama', 'Latitude', 'Longitude', 'Tarih'];
    const rows = emergencies.map(e => [
        e.id,
        e.type,
        e.priority,
        e.status,
        `"${(e.description || '').replace(/"/g, '""')}"`,
        e.latitude || '',
        e.longitude || '',
        e.createdAt || ''
    ]);

    const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);

    const a = document.createElement('a');
    a.href = url;
    a.download = `emergencymesh_vakalar_${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
}

// ─── DETAIL MODAL ───────────────────────────────────────────
function openDetailModal(id) {
    const e = emergencies.find(em => em.id === id);
    if (!e) return;

    const modal = document.getElementById('detail-modal');
    const title = document.getElementById('modal-title');
    const body = document.getElementById('modal-body');
    const actions = document.getElementById('modal-actions');

    title.innerHTML = `${getTypeEmoji(e.type)} ${getTypeLabel(e.type)} Vakası`;

    const elapsed = timeAgo(e.createdAt);
    const minutesElapsed = (Date.now() - parseUTCDate(e.createdAt).getTime()) / 60000;
    const isSLA = e.status !== 'RESOLVED' && minutesElapsed > 30;

    let cleanDesc = e.description || '';
    let healthData = '';
    const healthRegex = /\[SAĞLIK:\s*(.*?)\]/i;
    const match = cleanDesc.match(healthRegex);
    if (match) {
        healthData = match[1];
        cleanDesc = cleanDesc.replace(healthRegex, '').trim();
    }

    body.innerHTML = `
        <div class="modal-field">
            <div class="modal-field-label">Öncelik</div>
            <div class="modal-field-value"><span class="badge priority-${(e.priority || 'MEDIUM').toLowerCase()}">${e.priority}</span></div>
        </div>
        <div class="modal-field">
            <div class="modal-field-label">Durum</div>
            <div class="modal-field-value"><span class="status-badge badge-${e.status.toLowerCase()}">${e.status}</span></div>
        </div>
        <div class="modal-field">
            <div class="modal-field-label">Vaka Türü</div>
            <div class="modal-field-value">${getTypeEmoji(e.type)} ${e.type}</div>
        </div>
        <div class="modal-field">
            <div class="modal-field-label">Geçen Süre</div>
            <div class="modal-field-value" style="${isSLA ? 'color:#ef4444;font-weight:700;' : ''}">${elapsed} ${isSLA ? '⚠ SLA AŞILDI' : ''}</div>
        </div>
        <div class="modal-field full-width">
            <div class="modal-field-label">Açıklama</div>
            <div class="modal-field-value">${cleanDesc}</div>
        </div>
        ${healthData ? `
        <div class="modal-field full-width" style="background: rgba(239, 68, 68, 0.1); border-left: 4px solid #ef4444; padding: 12px; border-radius: 4px; margin-top: 8px;">
            <div class="modal-field-label" style="color: #ef4444; display:flex; align-items:center; gap:6px;">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
                TIBBİ BİLGİ (Kritik)
            </div>
            <div class="modal-field-value" style="color: #fca5a5; font-weight:600; margin-top:4px;">${healthData}</div>
        </div>
        ` : ''}
        <div class="modal-field">
            <div class="modal-field-label">Konum</div>
            <div class="modal-field-value">${e.latitude ? `${e.latitude.toFixed(5)}, ${e.longitude.toFixed(5)}` : 'Bilinmiyor'}</div>
        </div>
        <div class="modal-field">
            <div class="modal-field-label">Oluşturulma</div>
            <div class="modal-field-value">${new Date(e.createdAt).toLocaleString('tr-TR')}</div>
        </div>
        <div class="modal-field">
            <div class="modal-field-label">Kullanıcı ID</div>
            <div class="modal-field-value"><code style="color:var(--primary)">${e.userId || '—'}</code></div>
        </div>
        <div class="modal-field">
            <div class="modal-field-label">Vaka ID</div>
            <div class="modal-field-value"><code style="color:var(--text-dim);font-size:0.75rem;">${e.id}</code></div>
        </div>
    `;

    let actionHtml = '<button class="btn btn-secondary" onclick="closeModal()">Kapat</button>';
    if (e.status !== 'RESOLVED') {
        actionHtml = `<button class="btn btn-success" onclick="resolveEmergency('${e.id}');closeModal();">✓ Vakayı Çöz</button>` + actionHtml;
    }
    if (e.status === 'PENDING' || e.status === 'IN_PROGRESS') {
        actionHtml = `<button class="btn btn-primary" onclick="showAssignModal('${e.id}');closeModal();">Ekip Ata</button>` + actionHtml;
    }
    actions.innerHTML = actionHtml;

    modal.classList.add('visible');

    // Mini map
    setTimeout(() => {
        if (miniMap) { miniMap.remove(); miniMap = null; }
        if (e.latitude && e.longitude) {
            miniMap = L.map('modal-mini-map', { zoomControl: false, attributionControl: false }).setView([e.latitude, e.longitude], 14);
            L.tileLayer(MAP_TILES[0].url, { attribution: '' }).addTo(miniMap);
            L.circleMarker([e.latitude, e.longitude], {
                radius: 10, fillColor: '#ef4444', color: '#fff', weight: 2, fillOpacity: 0.9
            }).addTo(miniMap);
        }
    }, 200);
}

function closeModal() {
    const modal = document.getElementById('detail-modal');
    modal.classList.remove('visible');
    if (miniMap) { miniMap.remove(); miniMap = null; }
}

// Close modal on backdrop click
document.addEventListener('click', (ev) => {
    if (ev.target.id === 'detail-modal') closeModal();
    if (ev.target.id === 'assign-modal') closeAssignModal();
    if (ev.target.id === 'add-team-modal') closeAddTeamModal();
});

// Close modal on Escape
document.addEventListener('keydown', (ev) => {
    if (ev.key === 'Escape') { closeModal(); closeAssignModal(); closeAddTeamModal(); }
});

// ─── ACTIONS ────────────────────────────────────────────────
async function resolveEmergency(id) {
    try {
        await fetch(`${API_BASE}/emergency/${id}/status`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: 'RESOLVED' })
        });
        // Close any open popups
        map.closePopup();
        fetchAllData();
    } catch (e) { console.error('Resolve failed:', e); }
}

async function createTestEmergency() {
    if (!users.length) { alert('Kullanıcı bulunamadı!'); return; }
    const u = users[Math.floor(Math.random() * users.length)];
    const types = ['MEDICAL', 'FIRE', 'POLICE', 'ACCIDENT'];
    const pris = ['LOW', 'MEDIUM', 'HIGH', 'EMERGENCY'];
    try {
        await fetch(`${API_BASE}/emergency`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                userId: u.id,
                type: types[Math.floor(Math.random() * types.length)],
                description: 'Admin paneli test vakası',
                latitude: 41.005 + Math.random() * 0.03,
                longitude: 28.960 + Math.random() * 0.04,
                priority: pris[Math.floor(Math.random() * pris.length)]
            })
        });
        fetchAllData();
    } catch (e) { console.error('Test vaka oluşturulamadı:', e); }
}

function calculateDistance(lat1, lon1, lat2, lon2) {
    if (!lat1 || !lon1 || !lat2 || !lon2) return Infinity;
    const R = 6371; // km
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
              Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
              Math.sin(dLon/2) * Math.sin(dLon/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    return R * c;
}

function showAssignModal(requestId) {
    if (!responders.length) { alert('Hiç ekip bulunamadı!'); return; }

    const modal = document.getElementById('assign-modal');
    const body = document.getElementById('assign-modal-body');

    const emergency = emergencies.find(e => e.id === requestId);
    const title = document.getElementById('assign-modal-title');
    title.innerHTML = `🚑 Ekip Seç ${emergency ? `— <span style="font-size:0.8rem;color:var(--text-dim);">${getTypeEmoji(emergency.type)} ${emergency.type}</span>` : ''}`;

    // Sort responders: AVAILABLE first, then by distance to emergency
    const sortedResponders = [...responders].sort((a, b) => {
        if (a.status === 'AVAILABLE' && b.status !== 'AVAILABLE') return -1;
        if (a.status !== 'AVAILABLE' && b.status === 'AVAILABLE') return 1;
        
        if (emergency && emergency.latitude && emergency.longitude) {
            const distA = calculateDistance(emergency.latitude, emergency.longitude, a.latitude, a.longitude);
            const distB = calculateDistance(emergency.latitude, emergency.longitude, b.latitude, b.longitude);
            return distA - distB;
        }
        return 0;
    });

    let bestMatchFound = false;

    body.innerHTML = sortedResponders.map(r => {
        const isAvailable = r.status === 'AVAILABLE';
        let distanceHtml = '';
        let isBestMatch = false;

        if (emergency && emergency.latitude && r.latitude) {
            const dist = calculateDistance(emergency.latitude, emergency.longitude, r.latitude, r.longitude);
            if (dist !== Infinity) {
                // Varsayılan hız: Şehir içi ortalama 40 km/s -> Dakikada 0.66 km -> 1 km = 1.5 dk
                const etaMinutes = Math.max(1, Math.round(dist * 1.5));
                distanceHtml = `<div style="font-size:0.8rem; color:var(--primary); margin-top:8px; display:flex; align-items:center; gap:6px;">
                                    <span style="background:rgba(59,130,246,0.1); padding:2px 6px; border-radius:4px;">📍 ${dist.toFixed(1)} km</span>
                                    <span style="background:rgba(16,185,129,0.1); color:#10b981; padding:2px 6px; border-radius:4px;">⏳ ~${etaMinutes} dk (Tahmini)</span>
                                </div>`;
                
                // Eğer ekip müsaitse ve henüz bir "en iyi eşleşme" bulunamadıysa (liste zaten sıralı olduğu için ilk müsait olan en yakınıdır)
                if (isAvailable && !bestMatchFound) {
                    isBestMatch = true;
                    bestMatchFound = true;
                }
            }
        }

        const borderStyle = isBestMatch ? 'border: 2px solid #10b981; box-shadow: 0 0 10px rgba(16,185,129,0.2);' : '';

        return `
            <div class="responder-card ${isAvailable ? 'available' : 'busy'}" 
                 ${isAvailable ? `onclick="confirmAssign('${requestId}', '${r.id}', '${r.name}')"` : ''}
                 style="cursor:${isAvailable ? 'pointer' : 'not-allowed'}; position:relative; ${borderStyle}">
                
                ${isBestMatch ? `<div style="position:absolute; top:-10px; right:10px; background:#10b981; color:#fff; font-size:0.65rem; font-weight:bold; padding:2px 8px; border-radius:10px; box-shadow:0 2px 4px rgba(0,0,0,0.3);">✨ EN İYİ ROTA</div>` : ''}
                
                <div class="responder-card-header">
                    <div class="responder-card-icon">${r.type === 'AMBULANCE' ? '🚑' : r.type === 'FIRE_TRUCK' ? '🚒' : r.type === 'POLICE' ? '🚔' : '🚐'}</div>
                    <div class="responder-card-info">
                        <div class="responder-card-name">${r.name}</div>
                        <div class="responder-card-type">${getResponderTypeLabel(r.type)}</div>
                    </div>
                    <span class="status-badge ${isAvailable ? 'badge-resolved' : 'badge-pending'}">${isAvailable ? 'MÜSAİT' : 'MEŞGUL'}</span>
                </div>
                ${distanceHtml}
                ${r.city || r.district ? `<div class="responder-card-loc" style="margin-top: 6px; font-size:0.75rem; color:var(--text-dim);">🌍 ${r.city || '—'} / ${r.district || '—'}</div>` : ''}
            </div>
        `;
    }).join('');

    const availableCount = responders.filter(r => r.status === 'AVAILABLE').length;
    if (!availableCount) {
        body.innerHTML += `<div style="text-align:center;color:var(--warning);padding:1rem;font-size:0.85rem;">⚠ Tüm ekipler meşgul. Müsait ekip bekleniyor.</div>`;
    }

    modal.classList.add('visible');
}

function confirmAssign(requestId, responderId, responderName) {
    if (confirm(`${responderName} birimini bu vakaya atamak istiyor musunuz?`)) {
        closeAssignModal();
        assignResponder(requestId, responderId);
    }
}

function closeAssignModal() {
    const modal = document.getElementById('assign-modal');
    modal.classList.remove('visible');
}

async function assignResponder(reqId, resId) {
    try {
        const resp = await fetch(`${API_BASE}/emergency/${reqId}/assign/${resId}`, { method: 'PUT' });
        if (resp.ok) {
            map.closePopup();
            showToast('Başarılı', 'Ekip başarıyla atandı.', 'info');
            fetchAllData();
        } else {
            const errTxt = await resp.text();
            showToast('Atama Başarısız', errTxt || 'Sunucu hatası', 'warning');
        }
    } catch (e) { 
        console.error('Atama başarısız:', e); 
        showToast('Atama Başarısız', 'Ağ bağlantısı koptu', 'warning');
    }
}

// ─── UTILITIES ──────────────────────────────────────────────
function getTypeEmoji(type) {
    const m = {
        MEDICAL: '🏥',
        FIRE: '🔥',
        POLICE: '🚔',
        ACCIDENT: '💥',
        SAFE: '✔',
        RESCUE_DEBRIS: '🏠',
        SUPPLY_WATER: '💧',
        SHELTER: '⛺',
        DANGER: '🚨',
        POWER_OUTAGE: '🔌',
        OTHER: '❓',
        FOOD: '🍞',
        RESCUE: '🚁'
    };
    return m[type] || '⚠️';
}

function getTypeLabel(type) {
    const m = {
        MEDICAL: 'Tıbbi Yardım',
        FIRE: 'Yangın',
        POLICE: 'Polis/Güvenlik',
        ACCIDENT: 'Kaza',
        SAFE: 'Güvendeyim',
        RESCUE_DEBRIS: 'Enkaz Yardımı',
        SUPPLY_WATER: 'Lojistik/Gıda',
        SHELTER: 'Barınma',
        DANGER: 'Genel Tehlike',
        POWER_OUTAGE: 'Elektrik Kesintisi',
        OTHER: 'Diğer',
        FOOD: 'Gıda İhtiyacı',
        RESCUE: 'Kurtarma'
    };
    return m[type] || type;
}

// Parse dates safely assuming they are UTC (fixes GMT+3 offset issue)
function parseUTCDate(dateStr) {
    if (!dateStr) return new Date();
    // If it doesn't end with Z or timezone offset, explicitly tell JS it's UTC
    if (!dateStr.endsWith('Z') && !dateStr.includes('+')) {
        return new Date(dateStr + 'Z');
    }
    return new Date(dateStr);
}

function timeAgo(dateStr) {
    if (!dateStr) return '—';
    const diff = Date.now() - parseUTCDate(dateStr).getTime();
    if (diff < 0) return 'az önce';
    const secs = Math.floor(diff / 1000);
    if (secs < 60) return `${secs} sn önce`;
    const mins = Math.floor(secs / 60);
    if (mins < 60) return `${mins} dk önce`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs} sa önce`;
    const days = Math.floor(hrs / 24);
    return `${days} gün önce`;
}

// ─── WEBSOCKET ──────────────────────────────────────────────
function initWebSocket() {
    console.log("[WS] Initializing WebSocket connection...");
    const socket = new SockJS("/ws");
    const stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, (frame) => {
        console.log("[WS] Connected: " + frame);

        // 1. Yeni mesaj akışını dinle (debounce ile API fırtınasını önle)
        stompClient.subscribe("/topic/messages", (message) => {
            console.log("[WS] New message on /topic/messages");
            scheduleFetchAllData();
        });

        // 2. Yeni acil durum (EmergencyRequest) akışını dinle
        stompClient.subscribe("/topic/emergencies", (message) => {
            console.log("[WS] !!! NEW EMERGENCY DETECTED !!!");
            try {
                const data = JSON.parse(message.body);
                console.log("[WS] Emergency object:", data);

                const dataId = toIdStr(data.id);
                if (dataId && !emergencies.some(e => e && toIdStr(e.id) === dataId)) {
                    emergencies.unshift(data);
                    renderAll();
                }

                // Hemen "bildirildi" olarak işaretle (fetchAllData sonrası tekrar bildirim önlenir)
                if (dataId) previousEmergencyIds.add(dataId);

                // Bildirim göster (bir kez)
                showToast(
                    `YENİ ${getTypeLabel(data.type || '').toUpperCase()}`,
                    `${getTypeEmoji(data.type || '')} ${data.description || "Acil yardım talebi"}`,
                    "warning"
                );

                // Sesli uyarı
                playAlertSound(data.priority || 'HIGH');

                // Transaction race: DB commit + API güncellemesi için yeterli gecikme (1200ms)
                // Grafikte görünüp hemen silinme sorununu önler
                setTimeout(() => fetchAllData(), 1200);
            } catch (e) {
                console.error("[WS] Parse error on emergency topic:", e);
                setTimeout(() => fetchAllData(), 300);
            }
        });

    }, (error) => {
        console.warn("[WS] Connection failed, retrying in 5s...", error);
        setTimeout(initWebSocket, 5000);
    });
}

// ─── WEBSOCKET ─── (Re-initialized logic simplified)


// ─── ADD TEAM MODAL ─────────────────────────────────────────
function openAddTeamModal() {
    const modal = document.getElementById('add-team-modal');
    if (modal) modal.classList.add('visible');
}

function closeAddTeamModal() {
    const modal = document.getElementById('add-team-modal');
    if (modal) modal.classList.remove('visible');
    const form = document.getElementById('add-team-form');
    if (form) form.reset();
}

async function submitNewTeam(event) {
    event.preventDefault();
    const name = document.getElementById('team-name').value;
    const type = document.getElementById('team-type').value;
    const status = document.getElementById('team-status').value;
    const city = document.getElementById('team-city')?.value || null;
    const district = document.getElementById('team-district')?.value || null;

    try {
        const response = await fetch('/api/v1/responders', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                name: name,
                type: type,
                status: status,
                city: city,
                district: district
            })
        });

        if (response.ok) {
            showToast('Başarılı', `Yeni ekip başarıyla eklendi: ${name}`, 'info');
            closeAddTeamModal();
            fetchResponders().then(renderAll);
        } else {
            showToast('Hata', 'Ekip eklenirken bir sunucu hatası oluştu.', 'warning');
        }
    } catch (error) {
        console.error('Add team error:', error);
        showToast('Bağlantı Hatası', 'Sunucuya ulaşılamıyor.', 'warning');
    }
}
