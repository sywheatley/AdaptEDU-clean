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

### Backend:

1. Gemini:
   + to creat algorithm (python and josse code to attain the coefficients for the algorithms) code

2. Data Set:
   + https://zenodo.org/records/7022735?preview_file=JOSSE_Dataset.zip


---


### Debugging: 

1. VS Code Debugger
2. VS Code CoPilot
