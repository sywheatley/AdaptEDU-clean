// AdaptEDU – Rebuilt Script
// Features: week/month/day views · click-to-open detail popover
//           archive (completed items move to archive tab, not disappear)
//           distinct category colors · Apple Calendar UX

class Task {
    constructor(name, category, dueDate, urgency, userPriority, estimatedTime, description = '', completed = false) {
        this.id = `task_${Date.now()}_${Math.random().toString(36).slice(2)}`;
        this.type = 'task';
        this.name = name;
        this.category = (category || 'other').toLowerCase();
        this.dueDate = new Date(dueDate);
        this.urgency = parseInt(urgency) || 5;
        this.userPriority = parseInt(userPriority) || 5;
        this.estimatedTime = parseInt(estimatedTime) || 60;
        this.description = description;
        this.completed = completed;
        this.archived = false;
        this.minutesSpent = 0;
        this.archivedAt = null;
    }
    getHoursUntilDue() { return (this.dueDate - new Date()) / 3600000; }
    isOverdue() { return !this.completed && new Date() > this.dueDate; }
    getPriorityScore() {
        if (this.isOverdue()) return Infinity;
        if (this.completed) return -1;
        const tp = 10.0 / (this.getHoursUntilDue() + 1);
        return this.urgency + this.userPriority + tp;
    }
    getMinutesRemaining() { return Math.max(0, this.estimatedTime - this.minutesSpent); }
}

class CalEvent {
    constructor(name, startTime, endTime, location, status, category) {
        this.id = `event_${Date.now()}_${Math.random().toString(36).slice(2)}`;
        this.type = 'event';
        this.name = name;
        this.startTime = new Date(startTime);
        this.endTime = new Date(endTime);
        this.location = location || '';
        this.status = status || 'FIXED';
        this.category = (category || 'other').toLowerCase();
        this.archived = false;
        this.archivedAt = null;
    }
    getDurationMins() { return (this.endTime - this.startTime) / 60000; }
}

// ── State ──────────────────────────────────────────────────────────────────
let currentDate = new Date();          // anchor date for all views
let currentView = 'week';              // 'week' | 'month' | 'day'
let tasks  = [];
let events = [];
const START_HOUR = 7;
const END_HOUR   = 23;

// ── Category colors (must match CSS) ──────────────────────────────────────
const CAT_COLORS = {
    school:          '#4f8ef7',
    work:            '#34c759',
    personal:        '#bf5af2',
    extracurricular: '#ff9f0a',
    extra:           '#ff9f0a',
    other:           '#636366',
};
function catColor(cat) { return CAT_COLORS[cat] || CAT_COLORS.other; }

// ── Init ───────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    snapToMonday(currentDate);
    setupListeners();
    seedDemoData();
    refreshAll();
});

function snapToMonday(d) {
    const day = d.getDay();
    const diff = day === 0 ? -6 : 1 - day;
    d.setDate(d.getDate() + diff);
    d.setHours(0, 0, 0, 0);
}

// ── Event Listeners ────────────────────────────────────────────────────────
function setupListeners() {
    // Navigation
    document.getElementById('prev-btn').addEventListener('click', () => {
        if (currentView === 'week')  currentDate.setDate(currentDate.getDate() - 7);
        if (currentView === 'month') currentDate.setMonth(currentDate.getMonth() - 1);
        if (currentView === 'day')   currentDate.setDate(currentDate.getDate() - 1);
        refreshAll();
    });
    document.getElementById('next-btn').addEventListener('click', () => {
        if (currentView === 'week')  currentDate.setDate(currentDate.getDate() + 7);
        if (currentView === 'month') currentDate.setMonth(currentDate.getMonth() + 1);
        if (currentView === 'day')   currentDate.setDate(currentDate.getDate() + 1);
        refreshAll();
    });
    document.getElementById('today-btn').addEventListener('click', () => {
        currentDate = new Date();
        if (currentView === 'week' || currentView === 'day') snapToMonday(currentDate);
        refreshAll();
    });

    // View toggle
    document.querySelectorAll('.view-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.view-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            currentView = btn.dataset.view;
            if (currentView === 'week' || currentView === 'day') {
                // make sure currentDate is a Monday for week, or actual today for day
                if (currentView === 'week') snapToMonday(currentDate);
            }
            refreshAll();
        });
    });

    // Task/Archive tabs
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            renderTaskList(btn.dataset.tab);
        });
    });

    // Modals
    const taskModal  = document.getElementById('add-task-modal');
    const eventModal = document.getElementById('add-event-modal');

    document.getElementById('add-task-btn').addEventListener('click', () => taskModal.classList.remove('hidden'));
    document.getElementById('add-event-btn').addEventListener('click', () => eventModal.classList.remove('hidden'));

    [
        document.getElementById('close-task-modal'),
        document.getElementById('cancel-task-btn'),
    ].forEach(el => el.addEventListener('click', () => taskModal.classList.add('hidden')));

    [
        document.getElementById('close-event-modal'),
        document.getElementById('cancel-event-btn'),
    ].forEach(el => el.addEventListener('click', () => eventModal.classList.add('hidden')));

    // Form: add task
    document.getElementById('task-form').addEventListener('submit', e => {
        e.preventDefault();
        const f = e.target;
        tasks.push(new Task(
            f['task-name'].value,
            f['task-category'].value,
            f['task-due-date'].value,
            f['task-urgency'].value,
            f['task-priority'].value,
            f['task-estimated-time'].value,
            f['task-description'].value
        ));
        refreshAll();
        taskModal.classList.add('hidden');
        f.reset();
    });

    // Form: add event
    document.getElementById('event-form').addEventListener('submit', e => {
        e.preventDefault();
        const f = e.target;
        events.push(new CalEvent(
            f['event-name'].value,
            f['event-start-time'].value,
            f['event-end-time'].value,
            f['event-location'].value,
            f['event-status'].value,
            f['event-category'].value
        ));
        refreshAll();
        eventModal.classList.add('hidden');
        f.reset();
    });

    // Close popover on outside click
    document.addEventListener('click', e => {
        const pop = document.getElementById('detail-popover');
        if (!pop.classList.contains('hidden') && !pop.contains(e.target) && !e.target.closest('.cal-block') && !e.target.closest('.task-card') && !e.target.closest('.event-card') && !e.target.closest('.month-event-pill')) {
            pop.classList.add('hidden');
        }
    });

    document.getElementById('close-popover').addEventListener('click', () => {
        document.getElementById('detail-popover').classList.add('hidden');
    });
}

// ── Refresh ────────────────────────────────────────────────────────────────
function refreshAll() {
    updateLabel();
    renderCalendar();
    const activeTab = document.querySelector('.tab-btn.active')?.dataset.tab || 'tasks';
    renderTaskList(activeTab);
    updateStats();
}

function updateLabel() {
    const el = document.getElementById('week-label');
    if (currentView === 'week') {
        const end = new Date(currentDate);
        end.setDate(currentDate.getDate() + 6);
        const fmt = { month: 'short', day: 'numeric' };
        el.textContent = `${currentDate.toLocaleDateString('en-US', fmt)} – ${end.toLocaleDateString('en-US', fmt)}, ${currentDate.getFullYear()}`;
    } else if (currentView === 'month') {
        el.textContent = currentDate.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
    } else {
        el.textContent = currentDate.toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' });
    }
}

function updateStats() {
    const active = tasks.filter(t => !t.archived);
    document.getElementById('stat-pending').textContent = active.filter(t => !t.completed).length;
    document.getElementById('stat-done').textContent    = active.filter(t => t.completed).length;
    document.getElementById('stat-overdue').textContent = active.filter(t => t.isOverdue()).length;
    document.getElementById('stat-events').textContent  = events.filter(e => !e.archived).length;
}

// ══════════════════════════════════════════
// CALENDAR RENDERING
// ══════════════════════════════════════════
function renderCalendar() {
    if (currentView === 'month') renderMonthView();
    else renderTimeGrid(currentView === 'day' ? 1 : 7);
}

// ── Time Grid (Week & Day) ─────────────────────────────────────────────────
function renderTimeGrid(numDays) {
    const cal = document.getElementById('calendar');
    cal.innerHTML = '';
    const wrapper = document.createElement('div');
    wrapper.className = 'calendar-wrapper';

    // Header row
    const header = document.createElement('div');
    header.className = `cal-header${numDays === 1 ? ' day-view' : ''}`;
    header.innerHTML = '<div class="cal-header-spacer"></div>';

    const today = new Date();
    const DAYS  = ['Mon','Tue','Wed','Thu','Fri','Sat','Sun'];

    for (let i = 0; i < numDays; i++) {
        const d = new Date(currentDate);
        d.setDate(currentDate.getDate() + i);
        const isToday = d.toDateString() === today.toDateString();
        header.innerHTML += `
            <div class="cal-day-header ${isToday ? 'today' : ''}">
                <div class="cal-day-name">${DAYS[i]}</div>
                <div class="cal-day-date">${d.getDate()}</div>
            </div>`;
    }
    wrapper.appendChild(header);

    // Body
    const body = document.createElement('div');
    body.className = `cal-body${numDays === 1 ? ' day-view' : ''}`;

    // Time column
    let timeHTML = '<div class="cal-time-col">';
    for (let h = START_HOUR; h <= END_HOUR; h++) {
        const label = h < END_HOUR
            ? `<span class="cal-time-label">${h % 12 || 12}${h < 12 ? 'AM' : 'PM'}</span>`
            : '';
        timeHTML += `<div class="cal-time-slot">${label}</div>`;
    }
    timeHTML += '</div>';
    body.innerHTML = timeHTML;

    for (let i = 0; i < numDays; i++) {
        const d = new Date(currentDate);
        d.setDate(currentDate.getDate() + i);
        const isToday = d.toDateString() === today.toDateString();
        const col = document.createElement('div');
        col.className = `cal-day-col${isToday ? ' today-col' : ''}`;
        col.id = `day-col-${i}`;
        body.appendChild(col);
    }
    wrapper.appendChild(body);
    cal.appendChild(wrapper);

    // Current time line
    const endOfRange = new Date(currentDate);
    endOfRange.setDate(currentDate.getDate() + numDays);
    if (today >= currentDate && today < endOfRange) {
        const h = today.getHours(), m = today.getMinutes();
        if (h >= START_HOUR && h < END_HOUR) {
            const dayIndex = numDays === 1 ? 0 : (today.getDay() === 0 ? 6 : today.getDay() - 1);
            const col = document.getElementById(`day-col-${dayIndex}`);
            if (col) {
                const top = ((h - START_HOUR) + m / 60) * 60;
                const line = document.createElement('div');
                line.className = 'current-time-line';
                line.style.top = `${top}px`;
                line.innerHTML = '<div class="current-time-dot"></div>';
                col.appendChild(line);
            }
        }
    }

    // Place blocks
    const weekEnd = new Date(currentDate);
    weekEnd.setDate(currentDate.getDate() + numDays);

    const placeBlock = (item, colIdx, startFrac, endFrac, isTask) => {
        const col = document.getElementById(`day-col-${colIdx}`);
        if (!col || endFrac <= 0 || startFrac >= (END_HOUR - START_HOUR)) return;
        const top    = Math.max(0, startFrac) * 60;
        const height = Math.max(18, (Math.min(END_HOUR - START_HOUR, endFrac) - Math.max(0, startFrac)) * 60 - 1);
        const block  = document.createElement('div');
        const catCls = normCat(item.category);
        block.className = `cal-block ${catCls}${isTask ? ' task-block' : ''}`;
        block.style.cssText = `top:${top}px;height:${height}px`;
        const timeStr = isTask
            ? `Due ${fmtTime(item.dueDate)}`
            : `${fmtTime(item.startTime)} – ${fmtTime(item.endTime)}`;
        block.innerHTML = `<div class="block-title">${item.name}</div><div class="block-time">${timeStr}</div>`;
        block.addEventListener('click', e => { e.stopPropagation(); showPopover(item, e); });
        col.appendChild(block);
    };

    events.filter(ev => !ev.archived && ev.startTime >= currentDate && ev.startTime < weekEnd).forEach(ev => {
        const colIdx   = numDays === 1 ? 0 : dayIndex(ev.startTime);
        const startFrac = timeFrac(ev.startTime);
        const endFrac   = timeFrac(ev.endTime);
        placeBlock(ev, colIdx, startFrac, endFrac, false);
    });

    tasks.filter(t => !t.archived && t.dueDate >= currentDate && t.dueDate < weekEnd).forEach(t => {
        const colIdx   = numDays === 1 ? 0 : dayIndex(t.dueDate);
        const endFrac   = timeFrac(t.dueDate);
        const startFrac = endFrac - t.estimatedTime / 60;
        placeBlock(t, colIdx, startFrac, endFrac, true);
    });
}

function timeFrac(d) { return (d.getHours() - START_HOUR) + d.getMinutes() / 60; }
function dayIndex(d) { return d.getDay() === 0 ? 6 : d.getDay() - 1; }

// ── Month View ─────────────────────────────────────────────────────────────
function renderMonthView() {
    const cal = document.getElementById('calendar');
    cal.innerHTML = '';
    const wrap = document.createElement('div');
    wrap.className = 'month-wrapper';

    // Day name header
    const hrow = document.createElement('div');
    hrow.className = 'month-header-row';
    ['Mon','Tue','Wed','Thu','Fri','Sat','Sun'].forEach(d => {
        hrow.innerHTML += `<div class="month-day-name">${d}</div>`;
    });
    wrap.appendChild(hrow);

    // Grid
    const grid = document.createElement('div');
    grid.className = 'month-grid';

    const year  = currentDate.getFullYear();
    const month = currentDate.getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay  = new Date(year, month + 1, 0);
    const today    = new Date();

    // Start on Monday
    let startOffset = firstDay.getDay() === 0 ? 6 : firstDay.getDay() - 1;
    const gridStart = new Date(firstDay);
    gridStart.setDate(1 - startOffset);

    const totalCells = Math.ceil((startOffset + lastDay.getDate()) / 7) * 7;

    for (let i = 0; i < totalCells; i++) {
        const cellDate = new Date(gridStart);
        cellDate.setDate(gridStart.getDate() + i);
        const isToday      = cellDate.toDateString() === today.toDateString();
        const isOtherMonth = cellDate.getMonth() !== month;

        const cell = document.createElement('div');
        cell.className = `month-cell${isOtherMonth ? ' other-month' : ''}${isToday ? ' today-cell' : ''}`;

        const dateEl = document.createElement('div');
        dateEl.className = 'month-cell-date';
        dateEl.textContent = cellDate.getDate();
        dateEl.addEventListener('click', () => {
            currentDate = new Date(cellDate);
            currentView = 'day';
            document.querySelectorAll('.view-btn').forEach(b => b.classList.toggle('active', b.dataset.view === 'day'));
            refreshAll();
        });
        cell.appendChild(dateEl);

        // Items on this day
        const cellStart = new Date(cellDate); cellStart.setHours(0,0,0,0);
        const cellEnd   = new Date(cellDate); cellEnd.setHours(23,59,59,999);

        const dayEvents = events.filter(ev => !ev.archived && ev.startTime >= cellStart && ev.startTime <= cellEnd);
        const dayTasks  = tasks.filter(t  => !t.archived  && t.dueDate  >= cellStart && t.dueDate  <= cellEnd);

        const allItems = [...dayEvents, ...dayTasks];
        const MAX_SHOW = 3;
        allItems.slice(0, MAX_SHOW).forEach(item => {
            const pill = document.createElement('div');
            const catCls = normCat(item.category);
            pill.className = `month-event-pill ${catCls}${item.type === 'task' ? ' task-pill' : ''}`;
            pill.textContent = item.name;
            pill.addEventListener('click', e => { e.stopPropagation(); showPopover(item, e); });
            cell.appendChild(pill);
        });
        if (allItems.length > MAX_SHOW) {
            const more = document.createElement('div');
            more.className = 'month-more';
            more.textContent = `+${allItems.length - MAX_SHOW} more`;
            cell.appendChild(more);
        }
        grid.appendChild(cell);
    }
    wrap.appendChild(grid);
    cal.appendChild(wrap);
}

// ══════════════════════════════════════════
// TASK / ARCHIVE PANEL
// ══════════════════════════════════════════
function renderTaskList(tab = 'tasks') {
    const el      = document.getElementById('task-list');
    const subhead = document.getElementById('task-list-subheader');
    el.innerHTML  = '';

    if (tab === 'archive') {
        subhead.textContent = 'Completed & Archived ↓';
        const archivedTasks  = tasks.filter(t  => t.archived);
        const archivedEvents = events.filter(ev => ev.archived);
        const all = [
            ...archivedTasks.map(t  => ({ item: t,  archivedAt: t.archivedAt })),
            ...archivedEvents.map(ev => ({ item: ev, archivedAt: ev.archivedAt }))
        ].sort((a, b) => (b.archivedAt || 0) - (a.archivedAt || 0));

        if (all.length === 0) {
            el.innerHTML = '<div class="empty-state">Nothing archived yet.<br>Complete or archive tasks to see them here.</div>';
            return;
        }
        all.forEach(({ item }) => renderItem(item, el, true));
        return;
    }

    // Tasks tab
    subhead.textContent = 'Priority Score ↓';
    const activeTasks  = tasks.filter(t  => !t.archived).sort((a, b) => b.getPriorityScore() - a.getPriorityScore());
    const activeEvents = events.filter(ev => !ev.archived).sort((a, b) => a.startTime - b.startTime);

    if (activeTasks.length === 0 && activeEvents.length === 0) {
        el.innerHTML = '<div class="empty-state">All clear!<br>Add a task or event to get started.</div>';
        return;
    }

    if (activeTasks.length > 0) {
        activeTasks.forEach(t => renderItem(t, el, false));
    }
    if (activeEvents.length > 0) {
        const div = document.createElement('div');
        div.className = 'section-divider';
        div.textContent = 'UPCOMING EVENTS';
        el.appendChild(div);
        activeEvents.forEach(ev => renderItem(ev, el, false));
    }
}

function renderItem(item, container, isArchive) {
    const card = document.createElement('div');
    if (item.type === 'task') {
        const t = item;
        const score = t.getPriorityScore();
        let scoreColor = 'var(--accent)';
        if (score === Infinity) scoreColor = 'var(--accent-red)';
        else if (score > 15)   scoreColor = 'var(--accent-orange)';

        card.className = `task-card ${t.isOverdue() ? 'overdue' : ''} ${t.completed ? 'completed' : ''}`;
        card.dataset.category = t.category;

        const chk = document.createElement('div');
        chk.className = `card-checkbox${t.completed ? ' checked' : ''}`;
        chk.addEventListener('click', e => {
            e.stopPropagation();
            if (!t.completed) {
                t.completed = true;
                t.archived  = true;
                t.archivedAt = Date.now();
            } else {
                t.completed  = false;
                t.archived   = false;
                t.archivedAt = null;
            }
            refreshAll();
        });

        card.innerHTML = `
            <div class="card-info">
                <div class="card-name">${t.name}</div>
                <div class="card-meta">${capFirst(t.category)} · Due ${t.dueDate.toLocaleDateString('en-US', {weekday:'short',month:'short',day:'numeric'})}</div>
                <div class="card-detail">Urgency ${t.urgency} · Priority ${t.userPriority} · ${t.getMinutesRemaining()}m left</div>
            </div>
            <div class="card-score" style="color:${scoreColor}">${score === Infinity ? '∞' : score === -1 ? '✓' : score.toFixed(1)}</div>
        `;
        card.insertBefore(chk, card.firstChild);
        card.addEventListener('click', e => { if (!e.target.classList.contains('card-checkbox')) showPopover(t, e); });

    } else {
        const ev = item;
        card.className = 'event-card';
        card.style.position = 'relative';
        card.style.paddingLeft = '14px';
        const bar = document.createElement('div');
        bar.className = 'event-color-bar';
        bar.style.background = catColor(ev.category);
        card.appendChild(bar);
        card.innerHTML += `
            <div class="card-info">
                <div class="card-name">${ev.name}</div>
                <div class="card-meta">${ev.startTime.toLocaleDateString('en-US',{weekday:'short',month:'short',day:'numeric'})} · ${fmtTime(ev.startTime)}–${fmtTime(ev.endTime)}</div>
                <div class="card-detail">${capFirst(ev.category)} · ${ev.status}</div>
            </div>
        `;
        card.addEventListener('click', e => showPopover(ev, e));
    }
    container.appendChild(card);
}

// ══════════════════════════════════════════
// DETAIL POPOVER
// ══════════════════════════════════════════
function showPopover(item, e) {
    const pop     = document.getElementById('detail-popover');
    const dot     = document.getElementById('popover-dot');
    const title   = document.getElementById('popover-title');
    const body    = document.getElementById('popover-body');
    const footer  = document.getElementById('popover-footer');

    dot.style.background = catColor(item.category);
    title.textContent    = item.name;
    body.innerHTML       = '';
    footer.innerHTML     = '';

    const row = (icon, label, val) => {
        if (!val) return;
        body.innerHTML += `<div class="popover-row"><span class="popover-icon">${icon}</span><span>${label}: <span class="popover-val">${val}</span></span></div>`;
    };

    if (item.type === 'task') {
        const t = item;
        row('📅', 'Due',        t.dueDate.toLocaleDateString('en-US',{weekday:'long',month:'long',day:'numeric',year:'numeric'}));
        row('⏰', 'Due time',   fmtTime(t.dueDate));
        row('🏷', 'Category',   capFirst(t.category));
        row('🔥', 'Urgency',    `${t.urgency}/10`);
        row('⭐', 'Priority',   `${t.userPriority}/10`);
        row('⏱', 'Est. time',  `${t.estimatedTime} min`);
        row('📝', 'Notes',      t.description);
        row('📊', 'Score',      t.isOverdue() ? 'OVERDUE' : t.getPriorityScore().toFixed(2));

        if (!t.archived) {
            const btnComplete = btn('btn-complete', t.completed ? 'Mark Incomplete' : 'Mark Complete', () => {
                t.completed  = !t.completed;
                t.archived   = t.completed;
                t.archivedAt = t.completed ? Date.now() : null;
                pop.classList.add('hidden');
                refreshAll();
            });
            const btnArchive = btn('btn-archive', 'Archive', () => {
                t.archived   = true;
                t.archivedAt = Date.now();
                pop.classList.add('hidden');
                refreshAll();
            });
            const btnDel = btn('btn-delete', 'Delete', () => {
                tasks = tasks.filter(x => x.id !== t.id);
                pop.classList.add('hidden');
                refreshAll();
            });
            footer.appendChild(btnComplete);
            footer.appendChild(btnArchive);
            footer.appendChild(btnDel);
        } else {
            const btnRestore = btn('btn-archive', 'Restore', () => {
                t.archived   = false;
                t.completed  = false;
                t.archivedAt = null;
                pop.classList.add('hidden');
                refreshAll();
            });
            const btnDel = btn('btn-delete', 'Delete', () => {
                tasks = tasks.filter(x => x.id !== t.id);
                pop.classList.add('hidden');
                refreshAll();
            });
            footer.appendChild(btnRestore);
            footer.appendChild(btnDel);
        }
    } else {
        const ev = item;
        row('📅', 'Date',     ev.startTime.toLocaleDateString('en-US',{weekday:'long',month:'long',day:'numeric',year:'numeric'}));
        row('🕐', 'Time',     `${fmtTime(ev.startTime)} – ${fmtTime(ev.endTime)}`);
        row('⏱', 'Duration', `${ev.getDurationMins()} min`);
        row('📍', 'Location', ev.location);
        row('🏷', 'Category', capFirst(ev.category));
        row('📌', 'Status',   ev.status);

        if (!ev.archived) {
            const btnArc = btn('btn-archive', 'Archive', () => {
                ev.archived   = true;
                ev.archivedAt = Date.now();
                pop.classList.add('hidden');
                refreshAll();
            });
            const btnDel = btn('btn-delete', 'Delete', () => {
                events = events.filter(x => x.id !== ev.id);
                pop.classList.add('hidden');
                refreshAll();
            });
            footer.appendChild(btnArc);
            footer.appendChild(btnDel);
        } else {
            const btnRestore = btn('btn-archive', 'Restore', () => {
                ev.archived   = false;
                ev.archivedAt = null;
                pop.classList.add('hidden');
                refreshAll();
            });
            const btnDel = btn('btn-delete', 'Delete', () => {
                events = events.filter(x => x.id !== ev.id);
                pop.classList.add('hidden');
                refreshAll();
            });
            footer.appendChild(btnRestore);
            footer.appendChild(btnDel);
        }
    }

    // Position popover near click
    pop.classList.remove('hidden');
    const popW = 300, popH = 320;
    let left = e.clientX + 12, top = e.clientY - 20;
    if (left + popW > window.innerWidth - 10)  left = e.clientX - popW - 12;
    if (top  + popH > window.innerHeight - 10) top  = window.innerHeight - popH - 10;
    if (top < 10) top = 10;
    pop.style.left = `${left}px`;
    pop.style.top  = `${top}px`;
}

function btn(cls, label, handler) {
    const b = document.createElement('button');
    b.className = `popover-action-btn ${cls}`;
    b.textContent = label;
    b.addEventListener('click', handler);
    return b;
}

// ── Demo Data ──────────────────────────────────────────────────────────────
function seedDemoData() {
    const mon = new Date();
    snapToMonday(mon);

    const d = (offset, h, m = 0) => {
        const x = new Date(mon);
        x.setDate(mon.getDate() + offset);
        x.setHours(h, m, 0, 0);
        return x;
    };

    tasks.push(new Task("Math Homework",       "School",          d(0, 17),  9, 8, 90,  "Chapter 5 exercises"));
    tasks.push(new Task("Physics Lab Report",  "School",          d(2, 12),  6, 6, 60,  "Include all graphs"));
    tasks.push(new Task("Team Presentation",   "Work",            d(3, 15),  8, 9, 120, "Slides + script"));
    tasks.push(new Task("Journal Entry",       "Personal",        d(1, 20),  3, 4, 20,  ""));
    tasks.push(new Task("Overdue Assignment",  "School",          d(-1, 12), 8, 8, 45,  "Submit on portal"));

    events.push(new CalEvent("School",          d(0,  8), d(0, 15), "Main Building",    "FIXED",    "School"));
    events.push(new CalEvent("School",          d(1,  8), d(1, 15), "Main Building",    "FIXED",    "School"));
    events.push(new CalEvent("School",          d(2,  8), d(2, 15), "Main Building",    "FIXED",    "School"));
    events.push(new CalEvent("School",          d(3,  8), d(3, 15), "Main Building",    "FIXED",    "School"));
    events.push(new CalEvent("School",          d(4,  8), d(4, 15), "Main Building",    "FIXED",    "School"));
    events.push(new CalEvent("Soccer Practice", d(1, 16, 30), d(1, 18), "Sports Field", "FIXED",    "Extracurricular"));
    events.push(new CalEvent("Club Meeting",    d(3, 15), d(3, 16),  "Room 204",        "OPTIONAL", "Extracurricular"));
    events.push(new CalEvent("Work Shift",      d(2, 16), d(2, 20),  "Office",          "FIXED",    "Work"));
}

// ── Helpers ────────────────────────────────────────────────────────────────
function fmtTime(d) {
    return d.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', hour12: true });
}
function capFirst(s) { return s ? s.charAt(0).toUpperCase() + s.slice(1) : ''; }
function normCat(cat) {
    if (!cat) return 'other';
    const c = cat.toLowerCase();
    if (c === 'extra') return 'extracurricular';
    return c;
}
