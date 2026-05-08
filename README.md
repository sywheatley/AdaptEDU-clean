# AdaptEDU

## Docker

Build the app image:

```sh
docker build -t adaptedu:local .
```

Run the app:

```sh
docker run --rm -p 8080:8080 adaptedu:local
```

Or use Docker Compose:

```sh
docker compose up --build
```

The app is served on `http://localhost:8080`.



## Resources Used

---

### UI:

1. w3schools:
   + html
   + css
   + JS
   + java classes
2. or the colors:
   + https://coolors.co/
3. Google Fonts
4. Apple Calendar Layout 
   + but heavily modified

---

### UI & Static components

This section documents the front-end static files and lightweight UI controllers used by AdaptEDU. The scheduling algorithm is implemented in `procrastination_alg` and is intentionally NOT modified here.

- `SpringBootTest/src/main/resources/static/index2.html`: Main HTML layout for the calendar UI. Contains DOM IDs and structure consumed by `script2.js` (modals, buttons, task/event lists, and the calendar panel).
- `SpringBootTest/src/main/resources/static/script2.js`: Main client-side script. Handles DOM interactions, modals, Pomodoro UI, local storage, and calls to backend endpoints (`/api/schedule`, `/api/task-time-adjust`, `/api/state/save-csv`). Contains only UI logic; scheduling is performed server-side.
- `SpringBootTest/src/main/resources/static/styles2.css`: CSS tokens, themes, and component styles. Category colors and theme overrides are defined as CSS variables in `:root`.
- `SpringBootTest/src/main/resources/static/manifest.json`: PWA manifest (icons, name, display modes) used when the app is installed as a progressive web app.
- `SpringBootTest/src/main/resources/static/service-worker.js`: Lightweight service worker to cache core assets for offline loading. Designed for simple offline UX; for production you may want a stronger caching strategy.

Suggested citations and references for the UI stack:

- Google Fonts – used for typography: https://fonts.google.com/
- MDN Web Docs – Service Worker and PWA guidance: https://developer.mozilla.org/en-US/docs/Web/API/Service_Worker_API
- PWA App Manifest spec: https://developer.mozilla.org/en-US/docs/Web/Manifest
- CSS design inspiration / palettes: https://coolors.co/ (palette generators)

If you want me to expand any file's inline comments or generate a documentation file per-component (e.g., `docs/ui.md`), tell me which files to prioritize.

---

### Developer notes — important methods & endpoints

This section gives quick summaries of key server endpoints and complex
client-side functions so contributors can find where important work happens.

- `GET /` (handled by `procrastination_alg.HomeController.home`): forwards to the
   single-page app `index2.html`.

- `POST /api/task-time-adjust` (`com.example.controller.AdaptEDUController.adjustTaskTime`):
   Accepts a task DTO and returns the adjusted estimated time using the
   `ProcrastinationAlgorithm` helper. Useful for the UI to preview realistic
   task durations without generating a full schedule.

- `POST /api/state/save-csv` (`com.example.controller.AdaptEDUController.saveStateCsv` and
   `procrastination_alg.ScheduleController.saveStateToCsv`): Writes the UI-provided
   tasks and events to CSV files under `src/main/resources`. The frontend calls
   this endpoint to persist edits; the scheduler and other endpoints read the
   CSV files as their source of truth.

- `POST /api/schedule` (`com.example.controller.AdaptEDUController.generateSchedule` and
   `procrastination_alg.ScheduleController.getSchedule`): Entrypoint to produce
   a schedule. The controller transforms DTOs into domain `Event` objects,
   prepares the requested scheduling window, then delegates to
   `procrastination_alg.Scheduler.generateSchedule(...)`. The response is a list
   of scheduled `Event` DTOs (including fixed events and generated scheduled
   task blocks). The scheduling algorithm itself lives in `Scheduler`.

Client-side key functions (see `SpringBootTest/src/main/resources/static/script2.js`):

- `setupListeners()` — wires all UI event handlers (navigation, modals,
   pomodoro controls, form submissions). Look here when element IDs are
   renamed or new controls are added.
- `refreshAll(fetchSchedule = false)` — orchestrates a full UI re-render and,
   when `fetchSchedule` is true, requests a new schedule from the backend.
- `renderCalendar()` — translates `scheduledBlocks` and `events` into DOM
   elements inside `#calendar`; responsible for drawing sessions and ensuring
   visual continuity.
- `renderTaskList(tab)` — builds the task/event/archived lists shown in the
   right-hand panel; each card contains dataset attributes used by the
   detail popover for edits and actions.
- Pomodoro helpers: `startPomodoro()`, `pausePomodoro()`, `switchPomoPhase()`
   handle the timer loop, UI updates, and phase transitions.

If you'd like, I can expand any of the above entries into a dedicated
`docs/*.md` file with examples, or insert more granular inline comments in
`script2.js` for every function (it's large, so I'd do that incrementally).

---

### Backend:

1. Gemini:
   + to creat algorithm (python and josse code to attain the coefficients for the algorithms) code

2. Data Set:
   + https://zenodo.org/records/7022735?preview_file=JOSSE_Dataset.zip


---


### Debugging: 

1. VS Code Debugger
2. VS Code CoPilot
