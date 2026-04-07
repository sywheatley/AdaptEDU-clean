// AdaptEDU – Rebuilt Script
// Features: week/month/day views · click-to-open detail popover
//           archive (completed items move to archive tab, not disappear)
//           distinct category colors · Apple Calendar UX

/**
 * Represents a task with properties like name, category, due date, urgency, etc.
 * Includes methods to calculate priority score and check if overdue.
 */
class Task {
    /**
     * Creates a new Task instance.
     * @param {string} name - The name of the task.
     * @param {string} category - The category of the task (e.g., 'school', 'work').
     * @param {string|Date} dueDate - The due date and time.
     * @param {number} urgency - Urgency level (1-10).
     * @param {number} userPriority - User's priority level (1-10).
     * @param {number} estimatedTime - Estimated time in minutes.
     * @param {string} [description=''] - Optional description.
     * @param {boolean} [completed=false] - Whether the task is completed.
     */
    constructor(name, category, dueDate, urgency, userPriority, estimatedTime, description = '', completed = false) {
        this.id = `task_${Date.now()}_${Math.random().toString(36).slice(2)}`; // Unique ID for the task
        this.type = 'task'; // Type identifier
        this.name = name; // Task name
        this.category = (category || 'other').toLowerCase(); // Normalized category
        this.dueDate = new Date(dueDate); // Due date as Date object
        this.urgency = parseInt(urgency) || 5; // Urgency score
        this.userPriority = parseInt(userPriority) || 5; // User priority score
        this.estimatedTime = parseInt(estimatedTime) || 60; // Estimated time in minutes
        this.description = description; // Task description
        this.completed = completed; // Completion status
        this.archived = false; // Archive status
        this.minutesSpent = 0; // Time spent on task
        this.archivedAt = null; // Archive timestamp
    }

    /**
     * Calculates hours until due date.
     * @returns {number} Hours until due.
     */
    getHoursUntilDue() { return (this.dueDate - new Date()) / 3600000; }

    /**
     * Checks if the task is overdue.
     * @returns {boolean} True if overdue.
     */
    isOverdue() { return !this.completed && new Date() > this.dueDate; }

    /**
     * Calculates the priority score based on urgency, priority, and time until due.
     * @returns {number} Priority score (higher is more urgent).
     */
    getPriorityScore() {
        if (this.isOverdue()) return Infinity; // Overdue tasks have highest priority
        if (this.completed) return -1; // Completed tasks have lowest priority
        const tp = 10.0 / (this.getHoursUntilDue() + 1); // Time pressure factor
        return this.urgency + this.userPriority + tp; // Total score
    }

    /**
     * Gets remaining minutes to complete the task.
     * @returns {number} Minutes remaining.
     */
    getMinutesRemaining() { return Math.max(0, this.estimatedTime - this.minutesSpent); }
}

/**
 * Represents a calendar event with start/end times, location, etc.
 */
class CalEvent {
    /**
     * Creates a new CalEvent instance.
     * @param {string} name - Event name.
     * @param {string|Date} startTime - Start time.
     * @param {string|Date} endTime - End time.
     * @param {string} location - Event location.
     * @param {string} status - Event status ('FIXED' or 'OPTIONAL').
     * @param {string} category - Event category.
     */
    constructor(name, startTime, endTime, location, status, category) {
        this.id = `event_${Date.now()}_${Math.random().toString(36).slice(2)}`; // Unique ID
        this.type = 'event'; // Type identifier
        this.name = name; // Event name
        this.startTime = new Date(startTime); // Start time as Date
        this.endTime = new Date(endTime); // End time as Date
        this.location = location || ''; // Location
        this.status = status || 'FIXED'; // Status
        this.category = (category || 'other').toLowerCase(); // Normalized category
        this.archived = false; // Archive status
        this.archivedAt = null; // Archive timestamp
    }

    /**
     * Calculates event duration in minutes.
     * @returns {number} Duration in minutes.
     */
    getDurationMins() { return (this.endTime - this.startTime) / 60000; }
}

// ── State ──────────────────────────────────────────────────────────────────
let currentDate = new Date();          // anchor date for all views (current week/month/day start)
let currentView = 'week';              // current view mode: 'week', 'month', or 'day'
let tasks  = [];                       // array of Task objects
let events = [];                       // array of CalEvent objects
const START_HOUR = 7;                  // earliest hour to display in time grid (7 AM)
const END_HOUR   = 23;                 // latest hour to display in time grid (11 PM)

// ── Persistence (localStorage) ─────────────────────────────────────────────

const TASKS_KEY  = 'adaptedu_tasks';
const EVENTS_KEY = 'adaptedu_events';

/** Serialize and save current state to localStorage */
function saveData() {
    localStorage.setItem(TASKS_KEY,  JSON.stringify(tasks));
    localStorage.setItem(EVENTS_KEY, JSON.stringify(events));
}

/** Load saved data from localStorage, rehydrate class instances */
function loadData() {
    const rawTasks  = JSON.parse(localStorage.getItem(TASKS_KEY)  || '[]');
    const rawEvents = JSON.parse(localStorage.getItem(EVENTS_KEY) || '[]');

    tasks = rawTasks.map(t => {
        const task = new Task(t.name, t.category, t.dueDate, t.urgency, t.userPriority, t.estimatedTime, t.description, t.completed);
        task.id         = t.id;
        task.archived   = t.archived;
        task.archivedAt = t.archivedAt;
        task.minutesSpent = t.minutesSpent || 0;
        return task;
    });

    events = rawEvents.map(e => {
        const ev = new CalEvent(e.name, e.startTime, e.endTime, e.location, e.status, e.category);
        ev.id         = e.id;
        ev.archived   = e.archived;
        ev.archivedAt = e.archivedAt;
        return ev;
    });
}

/** Parse a tasks CSV string and add to tasks array */
function importTasksCSV(text) {
    const lines = text.trim().split('\n');
    const headers = lines[0].split(',');
    lines.slice(1).forEach(line => {
        const cols = line.split(',');
        const row  = Object.fromEntries(headers.map((h, i) => [h.trim(), cols[i]?.trim()]));
        tasks.push(new Task(row.name, row.category, row.dueDate, 5, row.userPriority, row.estimatedTime, row.description, row.completed === 'true'));
    });
}

/** Parse an events CSV string and add to events array */
function importEventsCSV(text) {
    const lines = text.trim().split('\n');
    const headers = lines[0].split(',');
    lines.slice(1).forEach(line => {
        const cols = line.split(',');
        const row  = Object.fromEntries(headers.map((h, i) => [h.trim(), cols[i]?.trim()]));
        // Normalize status: 'FIXED_EVENT' → 'FIXED'
        const status = row.status?.includes('FIXED') ? 'FIXED' : 'OPTIONAL';
        events.push(new CalEvent(row.name, row.startTime, row.endTime, row.location, status, row.category));
    });
}

/** Export current tasks and events as downloadable CSV files */
function exportToCSV() {
    const taskHeaders = 'name,category,dueDate,userPriority,estimatedTime,completed,description';
    const taskRows    = tasks.map(t =>
        `${t.name},${t.category},${t.dueDate.toISOString()},${t.userPriority},${t.estimatedTime},${t.completed},${t.description}`
    );
    downloadCSV('tasks_export.csv', [taskHeaders, ...taskRows].join('\n'));

    const eventHeaders = 'name,startTime,endTime,location,status,category';
    const eventRows    = events.map(e =>
        `${e.name},${e.startTime.toISOString()},${e.endTime.toISOString()},${e.location},${e.status},${e.category}`
    );
    downloadCSV('events_export.csv', [eventHeaders, ...eventRows].join('\n'));
}

function downloadCSV(filename, content) {
    const a    = document.createElement('a');
    a.href     = URL.createObjectURL(new Blob([content], { type: 'text/csv' }));
    a.download = filename;
    a.click();
}

// ── Category colors (must match CSS) ──────────────────────────────────────
const CAT_COLORS = {                   // color mapping for categories
    school:          '#4f8ef7',        // blue for school
    work:            '#34c759',        // green for work
    personal:        '#bf5af2',        // purple for personal
    extracurricular: '#ff9f0a',        // orange for extracurricular
    extra:           '#ff9f0a',        // alias for extracurricular
    other:           '#636366',        // gray for other
};

/**
 * Gets the color for a given category.
 * @param {string} cat - Category name.
 * @returns {string} Hex color code.
 */
function catColor(cat) { return CAT_COLORS[cat] || CAT_COLORS.other; }

// ── Init ───────────────────────────────────────────────────────────────────
/**
 * Initializes the application when DOM is loaded.
 */
document.addEventListener('DOMContentLoaded', () => {
    snapToMonday(currentDate);         // Align currentDate to Monday
    setupListeners();                  // Set up event listeners
    loadData();
    if (tasks.length === 0 && events.length === 0) seedDemoData();
    refreshAll();                      // Render initial UI
});

/**
 * Snaps a date to the start of the week (Monday).
 * @param {Date} d - Date to snap.
 */
function snapToMonday(d) {
    const day = d.getDay();            // 0 = Sunday, 1 = Monday, etc.
    const diff = day === 0 ? -6 : 1 - day; // Calculate days to subtract
    d.setDate(d.getDate() + diff);     // Adjust date
    d.setHours(0, 0, 0, 0);           // Set to midnight
}

// ── Event Listeners ────────────────────────────────────────────────────────
/**
 * Sets up all event listeners for UI interactions.
 */
function setupListeners() {
    // Navigation buttons
    document.getElementById('prev-btn').addEventListener('click', () => {
        if (currentView === 'week')  currentDate.setDate(currentDate.getDate() - 7); // Previous week
        if (currentView === 'month') currentDate.setMonth(currentDate.getMonth() - 1); // Previous month
        if (currentView === 'day')   currentDate.setDate(currentDate.getDate() - 1); // Previous day
        refreshAll(); // Update UI
    });
    document.getElementById('next-btn').addEventListener('click', () => {
        if (currentView === 'week')  currentDate.setDate(currentDate.getDate() + 7); // Next week
        if (currentView === 'month') currentDate.setMonth(currentDate.getMonth() + 1); // Next month
        if (currentView === 'day')   currentDate.setDate(currentDate.getDate() + 1); // Next day
        refreshAll(); // Update UI
    });
    document.getElementById('today-btn').addEventListener('click', () => {
        currentDate = new Date(); // Reset to today
        if (currentView === 'week' || currentView === 'day') snapToMonday(currentDate); // Snap if needed
        refreshAll(); // Update UI
    });

    // View toggle buttons
    document.querySelectorAll('.view-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.view-btn').forEach(b => b.classList.remove('active')); // Remove active class from all
            btn.classList.add('active'); // Add active to clicked
            currentView = btn.dataset.view; // Update current view
            if (currentView === 'week' || currentView === 'day') {
                // Ensure currentDate is aligned for week/day views
                if (currentView === 'week') snapToMonday(currentDate);
            }
            refreshAll(); // Refresh UI
        });
    });

    // Task/Archive tabs
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active')); // Remove active from all tabs
            btn.classList.add('active'); // Add active to clicked tab
            renderTaskList(btn.dataset.tab); // Render task list for the tab
        });
    });

    // Modal handling
    const taskModal  = document.getElementById('add-task-modal'); // Task modal element
    const eventModal = document.getElementById('add-event-modal'); // Event modal element

    document.getElementById('add-task-btn').addEventListener('click', () => taskModal.classList.remove('hidden')); // Show task modal
    document.getElementById('add-event-btn').addEventListener('click', () => eventModal.classList.remove('hidden')); // Show event modal

    // Close task modal
    [
        document.getElementById('close-task-modal'),
        document.getElementById('cancel-task-btn'),
    ].forEach(el => el.addEventListener('click', () => taskModal.classList.add('hidden')));

    // Close event modal
    [
        document.getElementById('close-event-modal'),
        document.getElementById('cancel-event-btn'),
    ].forEach(el => el.addEventListener('click', () => eventModal.classList.add('hidden')));

    // Form submission: add task
    document.getElementById('task-form').addEventListener('submit', e => {
        e.preventDefault(); // Prevent default form submission
        const f = e.target; // Form element
        tasks.push(new Task( // Create and add new task
            f['task-name'].value,
            f['task-category'].value,
            f['task-due-date'].value,
            f['task-urgency'].value,
            f['task-priority'].value,
            f['task-estimated-time'].value,
            f['task-description'].value
        ));
        refreshAll(); // Refresh UI after adding task
        saveData();
        taskModal.classList.add('hidden'); // Hide modal
        f.reset(); // Reset form
    });

    // Form submission: add event
    document.getElementById('event-form').addEventListener('submit', e => {
        e.preventDefault(); // Prevent default submission
        const f = e.target; // Form element
        events.push(new CalEvent( // Create and add new event
            f['event-name'].value,
            f['event-start-time'].value,
            f['event-end-time'].value,
            f['event-location'].value,
            f['event-status'].value,
            f['event-category'].value
        ));
        refreshAll(); // Refresh UI
        saveData();
        eventModal.classList.add('hidden'); // Hide modal
        f.reset(); // Reset form
    });

    // Close popover on outside click
    document.addEventListener('click', e => {
        const pop = document.getElementById('detail-popover'); // Popover element
        if (!pop.classList.contains('hidden') && !pop.contains(e.target) && !e.target.closest('.cal-block') && !e.target.closest('.task-card') && !e.target.closest('.event-card') && !e.target.closest('.month-event-pill')) {
            pop.classList.add('hidden'); // Hide if clicked outside
        }
    });

    document.getElementById('close-popover').addEventListener('click', () => {
        document.getElementById('detail-popover').classList.add('hidden'); // Close popover
    });

    // CSV Import
    document.getElementById('import-csv-input').addEventListener('change', async (e) => {
        for (const file of e.target.files) {
            const text = await file.text();
            if (file.name.toLowerCase().includes('task')) importTasksCSV(text);
            else if (file.name.toLowerCase().includes('event')) importEventsCSV(text);
        }
        saveData();
        refreshAll();
    });

    // CSV Export
    document.getElementById('export-csv-btn').addEventListener('click', exportToCSV);
}

// ── Refresh ────────────────────────────────────────────────────────────────
/**
 * Refreshes all UI components.
 */
function refreshAll() {
    updateLabel(); // Update date/week label
    renderCalendar(); // Render calendar view
    const activeTab = document.querySelector('.tab-btn.active')?.dataset.tab || 'tasks'; // Get active tab
    renderTaskList(activeTab); // Render task list
    updateStats(); // Update statistics
}

/**
 * Updates the date/week label based on current view.
 */
function updateLabel() {
    const el = document.getElementById('week-label'); // Label element
    if (currentView === 'week') {
        const end = new Date(currentDate); // End of week
        end.setDate(currentDate.getDate() + 6);
        const fmt = { month: 'short', day: 'numeric' }; // Format options
        el.textContent = `${currentDate.toLocaleDateString('en-US', fmt)} – ${end.toLocaleDateString('en-US', fmt)}, ${currentDate.getFullYear()}`; // Week range
    } else if (currentView === 'month') {
        el.textContent = currentDate.toLocaleDateString('en-US', { month: 'long', year: 'numeric' }); // Month and year
    } else {
        el.textContent = currentDate.toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' }); // Full date for day view
    }
}

/**
 * Updates the statistics in the sidebar.
 */
function updateStats() {
    const active = tasks.filter(t => !t.archived); // Active tasks
    document.getElementById('stat-pending').textContent = active.filter(t => !t.completed).length; // Pending tasks
    document.getElementById('stat-done').textContent    = active.filter(t => t.completed).length; // Completed tasks
    document.getElementById('stat-overdue').textContent = active.filter(t => t.isOverdue()).length; // Overdue tasks
    document.getElementById('stat-events').textContent  = events.filter(e => !e.archived).length; // Active events
}

// ══════════════════════════════════════════
// CALENDAR RENDERING
// ══════════════════════════════════════════
/**
 * Renders the calendar based on current view.
 */
function renderCalendar() {
    if (currentView === 'month') renderMonthView(); // Render month view
    else renderTimeGrid(currentView === 'day' ? 1 : 7); // Render time grid for week/day
}

// ── Time Grid (Week & Day) ─────────────────────────────────────────────────
/**
 * Renders the time grid for week or day view.
 * @param {number} numDays - Number of days to display (1 for day, 7 for week).
 */
function renderTimeGrid(numDays) {
    const cal = document.getElementById('calendar'); // Calendar container
    cal.innerHTML = ''; // Clear previous content
    const wrapper = document.createElement('div'); // Wrapper div
    wrapper.className = 'calendar-wrapper';

    // Header row with day names
    const header = document.createElement('div'); // Header element
    header.className = `cal-header${numDays === 1 ? ' day-view' : ''}`; // Class based on view
    header.innerHTML = '<div class="cal-header-spacer"></div>'; // Spacer for time column

    const today = new Date(); // Current date
    const DAYS  = ['Mon','Tue','Wed','Thu','Fri','Sat','Sun']; // Day names

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
            saveData();
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
            saveData();
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
                saveData();
                refreshAll();
            });
            const btnArchive = btn('btn-archive', 'Archive', () => {
                t.archived   = true;
                t.archivedAt = Date.now();
                pop.classList.add('hidden');
                saveData();
                refreshAll();
            });
            const btnDel = btn('btn-delete', 'Delete', () => {
                tasks = tasks.filter(x => x.id !== t.id);
                pop.classList.add('hidden');
                saveData();
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
                saveData();
                refreshAll();
            });
            const btnDel = btn('btn-delete', 'Delete', () => {
                tasks = tasks.filter(x => x.id !== t.id);
                pop.classList.add('hidden');
                saveData();
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
                saveData();
                refreshAll();
            });
            const btnDel = btn('btn-delete', 'Delete', () => {
                events = events.filter(x => x.id !== ev.id);
                pop.classList.add('hidden');
                saveData();
                refreshAll();
            });
            footer.appendChild(btnArc);
            footer.appendChild(btnDel);
        } else {
            const btnRestore = btn('btn-archive', 'Restore', () => {
                ev.archived   = false;
                ev.archivedAt = null;
                pop.classList.add('hidden');
                saveData();
                refreshAll();
            });
            const btnDel = btn('btn-delete', 'Delete', () => {
                events = events.filter(x => x.id !== ev.id);
                pop.classList.add('hidden');
                saveData();
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
/**
 * Seeds the application with demo tasks and events.
 */
function seedDemoData() {
    const mon = new Date(); // Current date
    snapToMonday(mon); // Snap to Monday

    // Helper to create date with offset
    const d = (offset, h, m = 0) => {
        const x = new Date(mon); // Base date
        x.setDate(mon.getDate() + offset); // Add offset days
        x.setHours(h, m, 0, 0); // Set time
        return x;
    };

    // Add demo tasks
    tasks.push(new Task("Math Homework",       "School",          d(0, 17),  9, 8, 90,  "Chapter 5 exercises"));
    tasks.push(new Task("Physics Lab Report",  "School",          d(2, 12),  6, 6, 60,  "Include all graphs"));
    tasks.push(new Task("Team Presentation",   "Work",            d(3, 15),  8, 9, 120, "Slides + script"));
    tasks.push(new Task("Journal Entry",       "Personal",        d(1, 20),  3, 4, 20,  ""));
    tasks.push(new Task("Overdue Assignment",  "School",          d(-1, 12), 8, 8, 45,  "Submit on portal"));

    // Add demo events
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
/**
 * Formats a date to time string (e.g., 3:00 PM).
 * @param {Date} d - Date to format.
 * @returns {string} Formatted time.
 */
function fmtTime(d) {
    return d.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', hour12: true });
}

/**
 * Capitalizes the first letter of a string.
 * @param {string} s - String to capitalize.
 * @returns {string} Capitalized string.
 */
function capFirst(s) { return s ? s.charAt(0).toUpperCase() + s.slice(1) : ''; }

/**
 * Normalizes category name.
 * @param {string} cat - Category name.
 * @returns {string} Normalized category.
 */
function normCat(cat) {
    if (!cat) return 'other'; // Default to 'other'
    const c = cat.toLowerCase(); // Lowercase
    if (c === 'extra') return 'extracurricular'; // Alias
    return c; // Return normalized
}
