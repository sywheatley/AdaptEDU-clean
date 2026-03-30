// AdaptEDU App Framework Script

class Task {
    constructor(name, category, dueDate, urgency, userPriority, estimatedTime, description = '', completed = false) {
        this.id = `task_${Date.now()}`;
        this.name = name;
        this.category = category.toLowerCase();
        this.dueDate = new Date(dueDate);
        this.urgency = parseInt(urgency);
        this.userPriority = parseInt(userPriority);
        this.estimatedTime = parseInt(estimatedTime);
        this.description = description;
        this.completed = completed;
        this.minutesSpent = 0;
    }
    getHoursUntilDue() { return (this.dueDate.getTime() - new Date().getTime()) / (1000 * 60 * 60); }
    isOverdue() { return !this.completed && new Date() > this.dueDate; }
    getPriorityScore() {
        if (this.isOverdue()) return Infinity;
        if (this.completed) return -1;
        const timePressure = 10.0 / (this.getHoursUntilDue() + 1);
        return this.urgency + this.userPriority + timePressure;
    }
    getMinutesRemaining() { return Math.max(0, this.estimatedTime - this.minutesSpent); }
}

class Event {
    constructor(name, startTime, endTime, location, status, category) {
        this.id = `event_${Date.now()}`;
        this.name = name;
        this.startTime = new Date(startTime);
        this.endTime = new Date(endTime);
        this.location = location;
        this.status = status; // 'FIXED' or 'OPTIONAL'
        this.category = category.toLowerCase();
    }
    getDuration() { return (this.endTime.getTime() - this.startTime.getTime()) / (1000 * 60); }
}

document.addEventListener('DOMContentLoaded', () => {
    
    // ─── State ──────────────────────────────────────────────────────────────────
    let currentWeekStart = new Date();
    let tasks = [];
    let events = [];

    const START_HOUR = 7;
    const END_HOUR = 23;

    // ─── Initial Setup ──────────────────────────────────────────────────────────
    function initialize() {
        setWeekToMonday(currentWeekStart);
        setupEventListeners();
        seedDemoData();
        refreshAll();
    }

    function setWeekToMonday(date) {
    const day = currentWeekStart.getDay();
    const diff = currentWeekStart.getDate() - day + (day === 0 ? -6 : 1);
    currentWeekStart.setDate(diff);
    currentWeekStart.setHours(0, 0, 0, 0);

    const START_HOUR = 7;
    const END_HOUR = 23;
    }

    // ─── Event Listeners ────────────────────────────────────────────────────────
    function setupEventListeners() {
        // Navigation
        document.getElementById('prev-btn').addEventListener('click', () => { currentWeekStart.setDate(currentWeekStart.getDate() - 7); refreshAll(); });
        document.getElementById('next-btn').addEventListener('click', () => { currentWeekStart.setDate(currentWeekStart.getDate() + 7); refreshAll(); });
        document.getElementById('today-btn').addEventListener('click', () => { currentWeekStart = new Date(); setWeekToMonday(currentWeekStart); refreshAll(); });

        // Modals
        const taskModal = document.getElementById('add-task-modal');
        const eventModal = document.getElementById('add-event-modal');
        document.getElementById('add-task-btn').addEventListener('click', () => taskModal.classList.remove('hidden'));
        document.getElementById('add-event-btn').addEventListener('click', () => eventModal.classList.remove('hidden'));
        
        const closeButtons = [document.getElementById('close-task-modal'), document.getElementById('cancel-task-btn'), document.getElementById('close-event-modal'), document.getElementById('cancel-event-btn')];
        closeButtons.forEach(btn => btn.addEventListener('click', () => {
            taskModal.classList.add('hidden');
            eventModal.classList.add('hidden');
        }));

        // Form Submissions
        document.getElementById('task-form').addEventListener('submit', (e) => {
            e.preventDefault();
            const form = e.target;
            const newTask = new Task(
                form['task-name'].value,
                form['task-category'].value,
                form['task-due-date'].value,
                form['task-urgency'].value,
                form['task-priority'].value,
                form['task-estimated-time'].value,
                form['task-description'].value
            );
            tasks.push(newTask);
            refreshAll();
            taskModal.classList.add('hidden');
            form.reset();
        });

        document.getElementById('event-form').addEventListener('submit', (e) => {
            e.preventDefault();
            const form = e.target;
            const newEvent = new Event(
                form['event-name'].value,
                form['event-start-time'].value,
                form['event-end-time'].value,
                form['event-location'].value,
                form['event-status'].value,
                form['event-category'].value
            );
            events.push(newEvent);
            refreshAll();
            eventModal.classList.add('hidden');
            form.reset();
        });
    }

    // ─── Rendering Engine ───────────────────────────────────────────────────────
    function refreshAll() {
        updateWeekLabel();
        renderCalendarGrid();
        renderTaskList();
    }

    function updateWeekLabel() {
        const endOfWeek = new Date(currentWeekStart);
        endOfWeek.setDate(currentWeekStart.getDate() + 6);
        
        const fmt = { month: 'short', day: 'numeric' };
        const startLabel = currentWeekStart.toLocaleDateString('en-US', fmt);
        const endLabel = endOfWeek.toLocaleDateString('en-US', fmt);
        const year = currentWeekStart.getFullYear();
        
        document.getElementById('week-label').innerText = `${startLabel} – ${endLabel}, ${year}`;
    }

    function renderCalendarGrid() {
        const calendar = document.getElementById('calendar');
        calendar.innerHTML = ''; // Clear previous

        const wrapper = document.createElement('div');
        wrapper.className = 'calendar-wrapper';

        // 1. Build Header (Days)
        const header = document.createElement('div');
        header.className = 'cal-header';
        header.innerHTML = '<div class="cal-header-spacer"></div>'; // Time column space
        
        const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
        const today = new Date();
        
        for (let i = 0; i < 7; i++) {
            const dayDate = new Date(currentWeekStart);
            dayDate.setDate(currentWeekStart.getDate() + i);
            
            const isToday = dayDate.toDateString() === today.toDateString();

            header.innerHTML += `
                <div class="cal-day-header ${isToday ? 'today' : ''}">
                    <div class="cal-day-name">${days[i]}</div>
                    <div class="cal-day-date">${dayDate.getDate()}</div>
                </div>`;
        }
        wrapper.appendChild(header);

        // 2. Build Grid Body (Timeslots & Columns)
        const body = document.createElement('div');
        body.className = 'cal-body';

        // Time Labels Column
        let timeColHTML = '<div class="cal-time-col">';
        for (let i = START_HOUR; i <= END_HOUR; i++) {
            const hour12 = i % 12 === 0 ? 12 : i % 12;
            const ampm = i < 12 ? 'AM' : 'PM';
            timeColHTML += `<div class="cal-time-slot">${i < END_HOUR ? `<span class="cal-time-label">${hour12} ${ampm}</span>` : ''}</div>`;
        }
        timeColHTML += '</div>';
        body.innerHTML += timeColHTML;

        // Day Columns
        for (let i = 0; i < 7; i++) {
            body.innerHTML += `<div class="cal-day-col" id="day-col-${i}"></div>`;
        }
        
        wrapper.appendChild(body);
        calendar.appendChild(wrapper);

        renderCurrentTimeIndicator(today);
        renderAllBlocks();
    }

    function renderCurrentTimeIndicator(today) {
        const endOfWeek = new Date(currentWeekStart);
        endOfWeek.setDate(currentWeekStart.getDate() + 6);
        
        if (today >= currentWeekStart && today <= new Date(endOfWeek.setHours(23,59,59,999))) {
            const hour = today.getHours();
            if (hour >= START_HOUR && hour < END_HOUR) {
                const dayIndex = today.getDay() === 0 ? 6 : today.getDay() - 1;
                const col = document.getElementById(`day-col-${dayIndex}`);
                if (col) {
                    const topOffset = ((hour - START_HOUR) + (today.getMinutes() / 60)) * 64;
                    col.innerHTML += `<div class="current-time-line" style="top: ${topOffset}px"><div class="current-time-dot"></div></div>`;
                }
            }
        }
    }

    function renderAllBlocks() {
        const weekEnd = new Date(currentWeekStart);
        weekEnd.setDate(weekEnd.getDate() + 7);

        const addBlock = (item, dayIndex, startFrac, endFrac) => {
            const col = document.getElementById(`day-col-${dayIndex}`);
            if (!col || endFrac <= 0 || startFrac >= (END_HOUR - START_HOUR)) return;

            const top = Math.max(0, startFrac) * 64;
            const height = (Math.min(END_HOUR - START_HOUR, endFrac) - Math.max(0, startFrac)) * 64 - 2;
            
            const block = document.createElement('div');
            block.className = `cal-block ${item instanceof Task ? 'task' : 'event'} ${item.category}`;
            block.style.top = `${top}px`;
            block.style.height = `${height}px`;
            block.innerHTML = `<strong>${item.name}</strong>`;
            col.appendChild(block);
        };

        events.filter(ev => ev.startTime >= currentWeekStart && ev.startTime < weekEnd).forEach(ev => {
            const dayIndex = ev.startTime.getDay() === 0 ? 6 : ev.startTime.getDay() - 1;
            const startFrac = (ev.startTime.getHours() - START_HOUR) + (ev.startTime.getMinutes() / 60);
            const endFrac = (ev.endTime.getHours() - START_HOUR) + (ev.endTime.getMinutes() / 60);
            addBlock(ev, dayIndex, startFrac, endFrac);
        });

        tasks.filter(t => t.dueDate >= currentWeekStart && t.dueDate < weekEnd).forEach(t => {
            const dayIndex = t.dueDate.getDay() === 0 ? 6 : t.dueDate.getDay() - 1;
            const endFrac = (t.dueDate.getHours() - START_HOUR) + (t.dueDate.getMinutes() / 60);
            const startFrac = endFrac - (t.estimatedTime / 60);
            addBlock(t, dayIndex, startFrac, endFrac);
        });
    }

    function renderTaskList() {
        const taskList = document.getElementById('task-list');
        taskList.innerHTML = ''; // Clear list

        const sortedTasks = tasks.sort((a, b) => b.getPriorityScore() - a.getPriorityScore());

        if (sortedTasks.length === 0) {
            taskList.innerHTML = '<div style="padding: 20px; color: var(--text-muted); text-align: center;">No tasks yet!</div>';
            return;
        }

        sortedTasks.forEach(t => {
            const score = t.getPriorityScore();
            let scoreColor = 'var(--accent-blue)';
            if (score === Infinity) scoreColor = 'var(--accent-red)';
            else if (score > 15) scoreColor = 'var(--accent-orange)';

            const card = document.createElement('div');
            card.className = `task-card ${t.isOverdue() ? 'overdue' : ''} ${t.completed ? 'completed' : ''}`;
            card.innerHTML = `
                <input type="checkbox" ${t.completed ? 'checked' : ''}>
                <div class="card-info">
                    <div class="card-name">${t.name}</div>
                    <div class="card-meta">${t.category.charAt(0).toUpperCase() + t.category.slice(1)} · Due ${t.dueDate.toLocaleDateString([], {weekday: 'short', month: 'short', day: 'numeric'})}</div>
                    <div class="card-detail">Urgency: ${t.urgency} · Priority: ${t.userPriority} · ${t.getMinutesRemaining()}m left</div>
                </div>
                <div class="card-score" style="color: ${scoreColor};">${score === Infinity ? '∞' : score.toFixed(1)}</div>
            `;
            card.querySelector('input[type="checkbox"]').addEventListener('change', (e) => {
                t.completed = e.target.checked;
                refreshAll();
            });
            taskList.appendChild(card);
        });

        const weekEnd = new Date(currentWeekStart);
        weekEnd.setDate(weekEnd.getDate() + 7);
        const weekEvents = events
            .filter(ev => ev.startTime >= currentWeekStart && ev.startTime < weekEnd)
            .sort((a, b) => a.startTime - b.startTime);

        if (weekEvents.length > 0) {
            taskList.innerHTML += `<div class="section-label" style="margin: 16px 0 4px 4px; color: var(--accent-purple);">EVENTS THIS WEEK</div>`;
            weekEvents.forEach(ev => {
                const card = document.createElement('div');
                card.className = 'event-card';
                card.innerHTML = `
                    <div class="dot ${ev.status.toLowerCase()}"></div>
                    <div class="card-info">
                        <div class="card-name">${ev.name}</div>
                        <div class="card-meta">${ev.startTime.toLocaleDateString([], {weekday: 'short', hour: 'numeric', minute: '2-digit'})}</div>
                    </div>
                    <div class="status" style="color: ${ev.status === 'FIXED' ? 'var(--accent-red)' : 'var(--text-muted)'}">${ev.status}</div>
                `;
                taskList.appendChild(card);
            });
        }
    }

    // ─── Demo Data ──────────────────────────────────────────────────────────────
    function seedDemoData() {
        const mon = new Date();
        setWeekToMonday(mon);
        
        tasks.push(new Task("Math Homework", "School", new Date(mon.getTime()).setHours(17,0,0,0), 9, 8, 90));
        tasks.push(new Task("Physics Lab Report", "School", new Date(mon.getTime() + 2*86400000).setHours(12,0,0,0), 6, 6, 60));
        tasks.push(new Task("Overdue Assignment", "School", new Date(mon.getTime() - 86400000).setHours(12,0,0,0), 8, 8, 45));

        events.push(new Event("School", new Date(mon.getTime()).setHours(8,0,0,0), new Date(mon.getTime()).setHours(15,0,0,0), "High School", "FIXED", "School"));
        events.push(new Event("Soccer Practice", new Date(mon.getTime() + 1*86400000).setHours(16,30,0,0), new Date(mon.getTime() + 1*86400000).setHours(18,0,0,0), "Sports Field", "FIXED", "Extracurricular"));
        events.push(new Event("Club Meeting", new Date(mon.getTime() + 3*86400000).setHours(15,0,0,0), new Date(mon.getTime() + 3*86400000).setHours(16,0,0,0), "Room 204", "OPTIONAL", "Extracurricular"));
    }

    initialize();
});