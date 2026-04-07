import javax.swing.*;
import javax.swing.border.*;

import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class AdaptEDUApp2 extends JFrame {

    // ── Colour palette ───────────────────────────────────────────────
    static final Color BG_DARK = new Color(18, 18, 30);
    static final Color BG_PANEL = new Color(26, 26, 42);
    static final Color BG_CARD = new Color(34, 34, 54);
    static final Color ACCENT_BLUE = new Color(99, 102, 241);
    static final Color ACCENT_PURPLE = new Color(139, 92, 246);
    static final Color ACCENT_GREEN = new Color(52, 211, 153);
    static final Color ACCENT_ORANGE = new Color(251, 146, 60);
    static final Color ACCENT_RED = new Color(248, 113, 113);
    static final Color TEXT_PRIMARY = new Color(236, 236, 255);
    static final Color TEXT_MUTED = new Color(120, 120, 160);
    static final Color BORDER_COLOR = new Color(50, 50, 75);

    // ── State ────────────────────────────────────────────────────────
    LocalDate currentWeekStart;
    List<Task> tasks = new ArrayList<>();
    List<Event> events = new ArrayList<>();

    WeeklyCalendarPanel calendarPanel;
    TaskListPanel taskListPanel;
    JLabel weekLabel;

    // ── Entry point ──────────────────────────────────────────────────
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new AdaptEDUApp2().setVisible(true));
    }

    // ── Constructor ──────────────────────────────────────────────────
    public AdaptEDUApp2() {
        super("AdaptEDU");
        currentWeekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        seedDemoData();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(0, 0));

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);
        add(buildMainArea(), BorderLayout.CENTER);
    }

    // ════════════════════════════════════════════════════════════════
    // TOP BAR
    // ════════════════════════════════════════════════════════════════
    JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_PANEL);
        bar.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_COLOR));
        bar.setPreferredSize(new Dimension(0, 56));

        JLabel logo = new JLabel("  AdaptEDU");
        logo.setFont(new Font("SansSerif", Font.BOLD, 20));
        logo.setForeground(ACCENT_BLUE);
        logo.setPreferredSize(new Dimension(220, 56));
        bar.add(logo, BorderLayout.WEST);

        // Week navigation
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 12));
        nav.setOpaque(false);
        JButton prev = iconButton("◀");
        JButton todayBtn = pillButton("Today");
        weekLabel = new JLabel(weekRangeLabel(), SwingConstants.CENTER);
        weekLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        weekLabel.setForeground(TEXT_PRIMARY);
        weekLabel.setPreferredSize(new Dimension(230, 30));
        JButton next = iconButton("▶");

        prev.addActionListener(e -> {
            currentWeekStart = currentWeekStart.minusWeeks(1);
            refreshAll();
        });
        next.addActionListener(e -> {
            currentWeekStart = currentWeekStart.plusWeeks(1);
            refreshAll();
        });
        todayBtn.addActionListener(e -> {
            currentWeekStart = LocalDate.now().with(DayOfWeek.MONDAY);
            refreshAll();
        });

        nav.add(prev);
        nav.add(todayBtn);
        nav.add(weekLabel);
        nav.add(next);
        bar.add(nav, BorderLayout.CENTER);

        // Action buttons
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        right.setOpaque(false);
        JButton addEvent = pillButton("＋  Event");
        JButton addTask = accentButton("＋  Task");
        addEvent.addActionListener(e -> showAddEventDialog());
        addTask.addActionListener(e -> showAddTaskDialog());
        right.add(addEvent);
        right.add(addTask);
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    // ════════════════════════════════════════════════════════════════
    // SIDEBAR
    // ════════════════════════════════════════════════════════════════
    JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setBackground(BG_PANEL);
        side.setPreferredSize(new Dimension(220, 0));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 0, 1, BORDER_COLOR),
                new EmptyBorder(20, 16, 20, 16)));

        side.add(sectionLabel("VIEWS"));
        side.add(Box.createVerticalStrut(6));
        side.add(navItem("📅  Week View", true));
        side.add(navItem("📆  Month View", false));
        side.add(navItem("📋  Day View", false));
        side.add(Box.createVerticalStrut(20));

        side.add(sectionLabel("TASK CATEGORIES"));
        side.add(Box.createVerticalStrut(6));
        side.add(categoryItem("School", ACCENT_BLUE));
        side.add(categoryItem("Work", ACCENT_GREEN));
        side.add(categoryItem("Personal", ACCENT_PURPLE));
        side.add(categoryItem("Extracurricular", ACCENT_ORANGE));
        side.add(Box.createVerticalStrut(12));

        side.add(sectionLabel("EVENTS"));
        side.add(Box.createVerticalStrut(6));
        side.add(categoryItem("Fixed Event", ACCENT_RED));
        side.add(categoryItem("Optional Event", new Color(180, 180, 200)));
        side.add(Box.createVerticalStrut(20));

        side.add(sectionLabel("PRIORITY SCORE"));
        side.add(Box.createVerticalStrut(6));
        side.add(priorityBadge("■  Overdue / ∞", ACCENT_RED));
        side.add(priorityBadge("■  Score > 15", ACCENT_ORANGE));
        side.add(priorityBadge("■  Score ≤ 15", ACCENT_BLUE));

        side.add(Box.createVerticalGlue());
        side.add(buildMiniStats());
        return side;
    }

    JPanel buildMiniStats() {
        JPanel p = new JPanel(new GridLayout(2, 2, 8, 8));
        p.setOpaque(false);
        long done = tasks.stream().filter(Task::isCompleted).count();
        long pending = tasks.stream().filter(t -> !t.isCompleted()).count();
        long overdue = tasks.stream().filter(t -> t.isOverdue() && !t.isCompleted()).count();
        p.add(statBox(String.valueOf(pending), "Pending", ACCENT_BLUE));
        p.add(statBox(String.valueOf(done), "Done", ACCENT_GREEN));
        p.add(statBox(String.valueOf(overdue), "Overdue", ACCENT_RED));
        p.add(statBox(String.valueOf(events.size()), "Events", ACCENT_PURPLE));
        return p;
    }

    JPanel statBox(String value, String label, Color color) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setBackground(BG_CARD);
        p.setBorder(new EmptyBorder(8, 10, 8, 10));
        JLabel v = new JLabel(value, SwingConstants.CENTER);
        v.setFont(new Font("SansSerif", Font.BOLD, 20));
        v.setForeground(color);
        JLabel l = new JLabel(label, SwingConstants.CENTER);
        l.setFont(new Font("SansSerif", Font.PLAIN, 10));
        l.setForeground(TEXT_MUTED);
        p.add(v, BorderLayout.CENTER);
        p.add(l, BorderLayout.SOUTH);
        return p;
    }

    // ════════════════════════════════════════════════════════════════
    // MAIN AREA
    // ════════════════════════════════════════════════════════════════
    JPanel buildMainArea() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BG_DARK);

        calendarPanel = new WeeklyCalendarPanel();
        taskListPanel = new TaskListPanel();

        JScrollPane calScroll = new JScrollPane(calendarPanel);
        calScroll.getViewport().setBackground(BG_DARK);
        calScroll.setBorder(null);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, calScroll, taskListPanel);
        split.setDividerLocation(820);
        split.setDividerSize(1);
        split.setBorder(null);
        split.setBackground(BG_DARK);

        main.add(split, BorderLayout.CENTER);
        return main;
    }

    // ════════════════════════════════════════════════════════════════
    // WEEKLY CALENDAR PANEL
    // ════════════════════════════════════════════════════════════════
    class WeeklyCalendarPanel extends JPanel {
        static final int HOUR_H = 64;
        static final int TIME_W = 56;
        static final int HEADER_H = 52;
        static final int START_H = 7;
        static final int END_H = 23;
        static final int HOURS = END_H - START_H;

        WeeklyCalendarPanel() {
            setBackground(BG_DARK);
            setPreferredSize(new Dimension(820, HEADER_H + HOURS * HOUR_H + 20));
        }

        int colWidth() {
            return Math.max(1, (getWidth() - TIME_W) / 7);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int cw = colWidth();
            LocalDate today = LocalDate.now();

            // ── Day headers ──────────────────────────────────────────
            g.setColor(BG_PANEL);
            g.fillRect(0, 0, getWidth(), HEADER_H);
            g.setColor(BORDER_COLOR);
            g.drawLine(0, HEADER_H - 1, getWidth(), HEADER_H - 1);

            String[] dayNames = { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };
            for (int d = 0; d < 7; d++) {
                LocalDate date = currentWeekStart.plusDays(d);
                int x = TIME_W + d * cw;
                boolean isToday = date.equals(today);

                g.setColor(BORDER_COLOR);
                g.drawLine(x, 0, x, HEADER_H);

                if (isToday) {
                    g.setColor(ACCENT_BLUE);
                    g.fillOval(x + cw / 2 - 16, HEADER_H / 2 - 2, 32, 32);
                    g.setColor(Color.WHITE);
                } else {
                    g.setColor(TEXT_MUTED);
                }
                g.setFont(new Font("SansSerif", Font.PLAIN, 11));
                drawCentered(g, dayNames[d], x, 10, cw, 16);

                if (!isToday)
                    g.setColor(TEXT_PRIMARY);
                g.setFont(new Font("SansSerif", Font.BOLD, 15));
                drawCentered(g, String.valueOf(date.getDayOfMonth()), x, HEADER_H / 2, cw, 20);
            }

            // ── Hour grid ────────────────────────────────────────────
            for (int h = 0; h <= HOURS; h++) {
                int y = HEADER_H + h * HOUR_H;
                g.setColor(BORDER_COLOR);
                g.drawLine(TIME_W, y, getWidth(), y);
                if (h < HOURS) {
                    int hour = START_H + h;
                    String lbl = (hour % 12 == 0 ? 12 : hour % 12) + (hour < 12 ? " AM" : " PM");
                    g.setColor(TEXT_MUTED);
                    g.setFont(new Font("SansSerif", Font.PLAIN, 10));
                    g.drawString(lbl, 4, y + 14);
                }
            }
            for (int d = 0; d < 7; d++) {
                g.setColor(BORDER_COLOR);
                g.drawLine(TIME_W + d * cw, HEADER_H, TIME_W + d * cw, HEADER_H + HOURS * HOUR_H);
            }

            // ── Current time line ────────────────────────────────────
            LocalTime now = LocalTime.now();
            if (now.getHour() >= START_H && now.getHour() < END_H) {
                double frac = (now.getHour() - START_H) + now.getMinute() / 60.0;
                int ty = HEADER_H + (int) (frac * HOUR_H);
                g.setColor(ACCENT_RED);
                g.setStroke(new BasicStroke(2));
                g.fillOval(TIME_W - 5, ty - 5, 10, 10);
                g.drawLine(TIME_W, ty, getWidth(), ty);
                g.setStroke(new BasicStroke(1));
            }

            // ── Events (drawn first, behind tasks) ───────────────────
            for (Event ev : events) {
                LocalDateTime evStart = ev.getStartTime();
                if (evStart == null)
                    continue;

                LocalDate evDate = evStart.toLocalDate();
                int dayOffset = (int) currentWeekStart.until(evDate, java.time.temporal.ChronoUnit.DAYS);
                if (dayOffset < 0 || dayOffset > 6)
                    continue;

                int startH = evStart.getHour();
                int startM = evStart.getMinute();
                int durMins = ev.getDuration() > 0 ? ev.getDuration()
                        : (ev.getEndTime() != null
                                ? (int) Duration.between(evStart, ev.getEndTime()).toMinutes()
                                : 60);

                double startFrac = (startH - START_H) + startM / 60.0;
                double endFrac = startFrac + durMins / 60.0;
                if (endFrac <= 0 || startFrac >= HOURS)
                    continue;

                int x = TIME_W + dayOffset * cw + 3;
                int y = HEADER_H + (int) (Math.max(0, startFrac) * HOUR_H);
                int w = cw - 6;
                int h = Math.max(4, (int) ((Math.min(HOURS, endFrac) - Math.max(0, startFrac)) * HOUR_H) - 2);

                boolean isFixed = "FIXED".equalsIgnoreCase(ev.getStatus());
                Color evColor = isFixed ? ACCENT_RED : new Color(180, 180, 200);

                // Subtle fill + border style to distinguish from tasks
                g.setColor(new Color(evColor.getRed(), evColor.getGreen(), evColor.getBlue(), 35));
                g.fillRoundRect(x, y, w, h, 8, 8);
                g.setColor(evColor);
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(x, y, w, h, 8, 8);
                g.setStroke(new BasicStroke(1));
                g.fillRoundRect(x, y, 4, h, 4, 4);

                if (h >= 18) {
                    g.setColor(TEXT_PRIMARY);
                    g.setFont(new Font("SansSerif", Font.BOLD, 11));
                    drawClipped(g, ev.getName(), x + 8, y + 14, w - 12);
                }
                if (h >= 32 && ev.getLocation() != null && !ev.getLocation().isEmpty()) {
                    g.setColor(TEXT_MUTED);
                    g.setFont(new Font("SansSerif", Font.PLAIN, 10));
                    drawClipped(g, "📍 " + ev.getLocation(), x + 8, y + 27, w - 12);
                }
            }

            // ── Tasks ────────────────────────────────────────────────
            for (Task t : tasks) {
                if (t.getDueDate() == null)
                    continue;

                LocalDate taskDate = t.getDueDate().toLocalDate();
                int dayOffset = (int) currentWeekStart.until(taskDate, java.time.temporal.ChronoUnit.DAYS);
                if (dayOffset < 0 || dayOffset > 6)
                    continue;

                // Block ends at due time; length = estimatedTime
                int dueH = t.getDueDate().getHour();
                int dueM = t.getDueDate().getMinute();
                int durMins = t.getEstimatedTime() > 0 ? t.getEstimatedTime() : 60;

                double endFrac = (dueH - START_H) + dueM / 60.0;
                double startFrac = endFrac - durMins / 60.0;
                if (endFrac <= 0 || startFrac >= HOURS)
                    continue;

                int x = TIME_W + dayOffset * cw + 3;
                int y = HEADER_H + (int) (Math.max(0, startFrac) * HOUR_H);
                int w = cw - 6;
                int h = Math.max(4, (int) ((Math.min(HOURS, endFrac) - Math.max(0, startFrac)) * HOUR_H) - 2);

                Color blockColor = taskColor(t);
                int alpha = t.isCompleted() ? 70 : 200;

                // Shadow
                g.setColor(new Color(0, 0, 0, 50));
                g.fillRoundRect(x + 2, y + 2, w, h, 8, 8);

                // Fill
                g.setColor(new Color(blockColor.getRed(), blockColor.getGreen(), blockColor.getBlue(), alpha));
                g.fillRoundRect(x, y, w, h, 8, 8);

                // Left accent stripe
                g.setColor(blockColor);
                g.fillRoundRect(x, y, 4, h, 4, 4);

                // Overdue "!" badge
                if (t.isOverdue() && !t.isCompleted()) {
                    g.setColor(ACCENT_RED);
                    g.setFont(new Font("SansSerif", Font.BOLD, 12));
                    g.drawString("!", x + w - 12, y + 13);
                }

                if (h >= 18) {
                    g.setColor(t.isCompleted() ? TEXT_MUTED : Color.WHITE);
                    g.setFont(new Font("SansSerif", Font.BOLD, 11));
                    String title = t.isCompleted() ? "✓ " + t.getName() : t.getName();
                    drawClipped(g, title, x + 8, y + 14, w - 18);
                }
                if (h >= 32) {
                    g.setColor(new Color(255, 255, 255, 150));
                    g.setFont(new Font("SansSerif", Font.PLAIN, 10));
                    String meta = "Due " + formatTime(dueH, dueM)
                            + "  ·  " + t.getMinutesRemaining() + "m left";
                    drawClipped(g, meta, x + 8, y + 27, w - 12);
                }
            }
        }

        void drawCentered(Graphics2D g, String s, int x, int y, int w, int h) {
            FontMetrics fm = g.getFontMetrics();
            g.drawString(s, x + (w - fm.stringWidth(s)) / 2,
                    y + (h + fm.getAscent() - fm.getDescent()) / 2);
        }

        void drawClipped(Graphics2D g, String s, int x, int y, int maxW) {
            FontMetrics fm = g.getFontMetrics();
            while (fm.stringWidth(s) > maxW && s.length() > 3)
                s = s.substring(0, s.length() - 4) + "…";
            g.drawString(s, x, y);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // TASK LIST PANEL
    // ════════════════════════════════════════════════════════════════
    class TaskListPanel extends JPanel {
        JPanel listContainer;

        TaskListPanel() {
            setBackground(BG_PANEL);
            setLayout(new BorderLayout());
            setBorder(new MatteBorder(0, 1, 0, 0, BORDER_COLOR));
            setPreferredSize(new Dimension(300, 0));

            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(BG_PANEL);
            header.setBorder(new CompoundBorder(
                    new MatteBorder(0, 0, 1, 0, BORDER_COLOR),
                    new EmptyBorder(14, 16, 14, 16)));
            JLabel title = new JLabel("Tasks  ·  Priority Score ↓");
            title.setFont(new Font("SansSerif", Font.BOLD, 14));
            title.setForeground(TEXT_PRIMARY);
            header.add(title, BorderLayout.WEST);
            add(header, BorderLayout.NORTH);

            listContainer = new JPanel();
            listContainer.setBackground(BG_PANEL);
            listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
            JScrollPane scroll = new JScrollPane(listContainer);
            scroll.setBorder(null);
            scroll.getViewport().setBackground(BG_PANEL);
            scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            add(scroll, BorderLayout.CENTER);

            refresh();
        }

        void refresh() {
            listContainer.removeAll();
            listContainer.add(Box.createVerticalStrut(8));

            // Sort by Task.getPriorityScore() — overdue tasks (∞) float to top
            List<Task> sorted = tasks.stream()
                    .sorted((a, b) -> Double.compare(b.getPriorityScore(), a.getPriorityScore()))
                    .collect(Collectors.toList());

            if (sorted.isEmpty()) {
                JLabel empty = new JLabel("  No tasks yet — add one above!");
                empty.setFont(new Font("SansSerif", Font.ITALIC, 13));
                empty.setForeground(TEXT_MUTED);
                listContainer.add(empty);
            } else {
                for (Task t : sorted) {
                    listContainer.add(buildTaskCard(t));
                    listContainer.add(Box.createVerticalStrut(6));
                }
            }

            // Events this week
            List<Event> weekEvents = events.stream()
                    .filter(ev -> ev.getStartTime() != null)
                    .filter(ev -> {
                        LocalDate d = ev.getStartTime().toLocalDate();
                        return !d.isBefore(currentWeekStart) && !d.isAfter(currentWeekStart.plusDays(6));
                    })
                    .sorted(Comparator.comparing(Event::getStartTime))
                    .collect(Collectors.toList());

            if (!weekEvents.isEmpty()) {
                listContainer.add(Box.createVerticalStrut(10));
                JLabel evHeader = new JLabel("  EVENTS THIS WEEK");
                evHeader.setFont(new Font("SansSerif", Font.BOLD, 10));
                evHeader.setForeground(ACCENT_PURPLE);
                evHeader.setAlignmentX(LEFT_ALIGNMENT);
                evHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
                listContainer.add(evHeader);
                listContainer.add(Box.createVerticalStrut(4));
                for (Event ev : weekEvents) {
                    listContainer.add(buildEventCard(ev));
                    listContainer.add(Box.createVerticalStrut(6));
                }
            }

            listContainer.revalidate();
            listContainer.repaint();
        }

        JPanel buildTaskCard(Task t) {
            double score = t.getPriorityScore();
            Color accent = (score == Double.POSITIVE_INFINITY || score > 15)
                    ? ACCENT_RED
                    : (score > 8 ? ACCENT_ORANGE : ACCENT_BLUE);

            JPanel card = new JPanel(new BorderLayout(8, 0));
            card.setBackground(BG_CARD);
            card.setBorder(new CompoundBorder(
                    new LineBorder(
                            (t.isOverdue() && !t.isCompleted()) ? new Color(248, 113, 113, 100) : BORDER_COLOR,
                            1, true),
                    new EmptyBorder(10, 12, 10, 12)));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
            card.setAlignmentX(LEFT_ALIGNMENT);
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Checkbox → calls Task.markAsCompleted()
            JCheckBox cb = new JCheckBox();
            cb.setSelected(t.isCompleted());
            cb.setOpaque(false);
            cb.setFocusPainted(false);
            cb.addActionListener(e -> {
                if (cb.isSelected())
                    t.markAsCompleted();
                calendarPanel.repaint();
                refresh();
            });
            card.add(cb, BorderLayout.WEST);

            // Text info
            JPanel info = new JPanel(new GridLayout(3, 1, 0, 1));
            info.setOpaque(false);

            JLabel name = new JLabel(t.isCompleted() ? "✓  " + t.getName() : t.getName());
            name.setFont(new Font("SansSerif", Font.BOLD, 13));
            name.setForeground(t.isCompleted() ? TEXT_MUTED : TEXT_PRIMARY);

            String catStr = (t.getCategory() != null && !t.getCategory().isEmpty())
                    ? t.getCategory() + "  ·  "
                    : "";
            String dueStr = t.getDueDate() != null
                    ? t.getDueDate().format(DateTimeFormatter.ofPattern("EEE MMM d, h:mm a"))
                    : "No due date";
            JLabel meta = new JLabel(catStr + dueStr);
            meta.setFont(new Font("SansSerif", Font.PLAIN, 11));
            meta.setForeground(TEXT_MUTED);

            // Exposes urgency, userPriority, minutesRemaining from Task
            JLabel detail = new JLabel(
                    "Urgency: " + t.getPriorityScore()
                            + "  ·  Priority: " + t.getUserPriority()
                            + "  ·  " + t.getMinutesRemaining() + "m left");
            detail.setFont(new Font("SansSerif", Font.PLAIN, 10));
            detail.setForeground(new Color(140, 140, 180));

            info.add(name);
            info.add(meta);
            info.add(detail);
            card.add(info, BorderLayout.CENTER);

            // Score badge — uses Task.getPriorityScore()
            String scoreStr = (score == Double.POSITIVE_INFINITY) ? "∞" : String.format("%.1f", score);
            JLabel scoreLbl = new JLabel(scoreStr, SwingConstants.RIGHT);
            scoreLbl.setFont(new Font("SansSerif", Font.BOLD, 14));
            scoreLbl.setForeground(accent);
            scoreLbl.setToolTipText("Priority score = urgency + userPriority + timePressure");
            card.add(scoreLbl, BorderLayout.EAST);

            card.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    card.setBackground(new Color(44, 44, 66));
                    card.repaint();
                }

                public void mouseExited(MouseEvent e) {
                    card.setBackground(BG_CARD);
                    card.repaint();
                }
            });
            return card;
        }

        JPanel buildEventCard(Event ev) {
            boolean isFixed = "FIXED".equalsIgnoreCase(ev.getStatus());
            Color accent = isFixed ? ACCENT_RED : new Color(180, 180, 200);

            JPanel card = new JPanel(new BorderLayout(10, 0));
            card.setBackground(BG_CARD);
            card.setBorder(new CompoundBorder(
                    new LineBorder(BORDER_COLOR, 1, true),
                    new EmptyBorder(10, 12, 10, 12)));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
            card.setAlignmentX(LEFT_ALIGNMENT);

            // Color dot
            JPanel dot = new JPanel() {
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(accent);
                    g.fillOval(0, 4, 10, 10);
                }
            };
            dot.setPreferredSize(new Dimension(14, 18));
            dot.setOpaque(false);
            card.add(dot, BorderLayout.WEST);

            JPanel info = new JPanel(new GridLayout(2, 1, 0, 2));
            info.setOpaque(false);

            JLabel name = new JLabel(ev.getName());
            name.setFont(new Font("SansSerif", Font.BOLD, 13));
            name.setForeground(TEXT_PRIMARY);

            // Uses Event.getStartTime(), Event.getLocation()
            String timeStr = ev.getStartTime().format(DateTimeFormatter.ofPattern("EEE MMM d, h:mm a"));
            String locStr = (ev.getLocation() != null && !ev.getLocation().isEmpty())
                    ? "  📍 " + ev.getLocation()
                    : "";
            JLabel meta = new JLabel(timeStr + locStr);
            meta.setFont(new Font("SansSerif", Font.PLAIN, 11));
            meta.setForeground(TEXT_MUTED);

            info.add(name);
            info.add(meta);
            card.add(info, BorderLayout.CENTER);

            // Uses Event.getStatus()
            JLabel statusLbl = new JLabel(isFixed ? "FIXED" : "OPT", SwingConstants.RIGHT);
            statusLbl.setFont(new Font("SansSerif", Font.BOLD, 10));
            statusLbl.setForeground(accent);
            card.add(statusLbl, BorderLayout.EAST);

            card.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    card.setBackground(new Color(44, 44, 66));
                    card.repaint();
                }

                public void mouseExited(MouseEvent e) {
                    card.setBackground(BG_CARD);
                    card.repaint();
                }
            });
            return card;
        }
    }

    // ════════════════════════════════════════════════════════════════
    // ADD TASK DIALOG
    // ════════════════════════════════════════════════════════════════
    void showAddTaskDialog() {
        JDialog dlg = new JDialog(this, "New Task", true);
        dlg.setSize(440, 530);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(BG_PANEL);
        dlg.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_PANEL);
        form.setBorder(new EmptyBorder(24, 28, 8, 28));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 0, 4, 0);
        c.weightx = 1;
        c.gridx = 0;

        JTextField nameField = styledField("Task name…");
        JTextField catField = styledField("e.g. School, Work, Personal…");
        JTextField descField = styledField("Optional description…");
        JTextField dateField = styledField(
                LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + " 17:00");
        JSpinner urgSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 10, 1));
        JSpinner uprSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 10, 1));
        JSpinner estSpinner = new JSpinner(new SpinnerNumberModel(60, 5, 480, 5));
        styleSpinner(urgSpinner);
        styleSpinner(uprSpinner);
        styleSpinner(estSpinner);

        int row = 0;
        c.gridy = row++;
        form.add(formLabel("Task Name *"), c);
        c.gridy = row++;
        form.add(nameField, c);
        c.gridy = row++;
        form.add(formLabel("Category"), c);
        c.gridy = row++;
        form.add(catField, c);
        c.gridy = row++;
        form.add(formLabel("Description"), c);
        c.gridy = row++;
        form.add(descField, c);
        c.gridy = row++;
        form.add(formLabel("Due Date & Time  (YYYY-MM-DD HH:MM) *"), c);
        c.gridy = row++;
        form.add(dateField, c);
        c.gridy = row++;
        form.add(formLabel("Urgency  (1 = low, 10 = critical)"), c);
        c.gridy = row++;
        form.add(urgSpinner, c);
        c.gridy = row++;
        form.add(formLabel("Your Priority  (1 = low, 10 = must do)"), c);
        c.gridy = row++;
        form.add(uprSpinner, c);
        c.gridy = row++;
        form.add(formLabel("Estimated Time  (minutes)"), c);
        c.gridy = row++;
        form.add(estSpinner, c);

        dlg.add(form, BorderLayout.CENTER);
        dlg.add(buildDialogButtons(dlg, () -> {
            try {
                String taskName = nameField.getText().trim();
                if (taskName.isEmpty()) {
                    nameField.setBorder(new LineBorder(ACCENT_RED));
                    return;
                }
                LocalDateTime due = LocalDateTime.parse(
                        dateField.getText().trim(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                // Full Task constructor: name, category, dueDate, userPriority,
                // estimatedTime, completed, description
                Task t = new Task(
                        taskName,
                        catField.getText().trim().isEmpty() ? null : catField.getText().trim(),
                        due,
                        (Integer) urgSpinner.getValue(),
                        (Integer) estSpinner.getValue(),
                        false,
                        descField.getText().trim().isEmpty() ? null : descField.getText().trim());
                tasks.add(t);
                refreshAll();
                dlg.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg,
                        "Check date format: YYYY-MM-DD HH:MM\ne.g.  2025-06-15 14:30");
            }
        }), BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    // ════════════════════════════════════════════════════════════════
    // ADD EVENT DIALOG
    // ════════════════════════════════════════════════════════════════
    void showAddEventDialog() {
        JDialog dlg = new JDialog(this, "New Event", true);
        dlg.setSize(440, 560);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(BG_PANEL);
        dlg.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_PANEL);
        form.setBorder(new EmptyBorder(24, 28, 8, 28));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 0, 4, 0);
        c.weightx = 1;
        c.gridx = 0;

        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        JTextField nameField = styledField("Event name…");
        JTextField startField = styledField(today + " 09:00");
        JTextField endField = styledField(today + " 10:00");
        JTextField locationField = styledField("Optional location…");
        JTextField descField = styledField("Optional notes…");
        JComboBox<String> statusBox = styledCombo("FIXED", "OPTIONAL");
        JComboBox<String> catBox = styledCombo("School", "Work", "Personal", "Extracurricular", "Other");
        JSpinner travelSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 120, 5));
        styleSpinner(travelSpinner);

        int row = 0;
        c.gridy = row++;
        form.add(formLabel("Event Name *"), c);
        c.gridy = row++;
        form.add(nameField, c);
        c.gridy = row++;
        form.add(formLabel("Start  (YYYY-MM-DD HH:MM) *"), c);
        c.gridy = row++;
        form.add(startField, c);
        c.gridy = row++;
        form.add(formLabel("End    (YYYY-MM-DD HH:MM) *"), c);
        c.gridy = row++;
        form.add(endField, c);
        c.gridy = row++;
        form.add(formLabel("Location"), c);
        c.gridy = row++;
        form.add(locationField, c);
        c.gridy = row++;
        form.add(formLabel("Notes"), c);
        c.gridy = row++;
        form.add(descField, c);
        c.gridy = row++;
        form.add(formLabel("Status"), c);
        c.gridy = row++;
        form.add(statusBox, c);
        c.gridy = row++;
        form.add(formLabel("Category"), c);
        c.gridy = row++;
        form.add(catBox, c);
        c.gridy = row++;
        form.add(formLabel("Travel Time  (minutes)"), c);
        c.gridy = row++;
        form.add(travelSpinner, c);

        dlg.add(form, BorderLayout.CENTER);
        dlg.add(buildDialogButtons(dlg, () -> {
            try {
                String evName = nameField.getText().trim();
                if (evName.isEmpty()) {
                    nameField.setBorder(new LineBorder(ACCENT_RED));
                    return;
                }
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                LocalDateTime start = LocalDateTime.parse(startField.getText().trim(), fmt);
                LocalDateTime end = LocalDateTime.parse(endField.getText().trim(), fmt);
                int dur = (int) Duration.between(start, end).toMinutes();

                // Full Event constructor: name, date, startTime, endTime, duration,
                // location, travelTime, status
                Event ev = new Event(
                        evName,
                        start, // date = start date
                        start,
                        end,
                        Math.max(0, dur),
                        locationField.getText().trim().isEmpty() ? null : locationField.getText().trim(),
                        (Integer) travelSpinner.getValue(),
                        (String) statusBox.getSelectedItem());
                ev.setCategory((String) catBox.getSelectedItem());
                if (!descField.getText().trim().isEmpty())
                    ev.setDescription(descField.getText().trim());

                events.add(ev);
                refreshAll();
                dlg.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg,
                        "Check date format: YYYY-MM-DD HH:MM\ne.g.  2025-06-15 14:30");
            }
        }), BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    // ════════════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════════════
    void refreshAll() {
        weekLabel.setText(weekRangeLabel());
        calendarPanel.repaint();
        taskListPanel.refresh();
        revalidate();
        repaint();
    }

    String weekRangeLabel() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d");
        return currentWeekStart.format(fmt) + " – "
                + currentWeekStart.plusDays(6).format(fmt)
                + ", " + currentWeekStart.getYear();
    }

    Color taskColor(Task t) {
        if (t.getCategory() == null)
            return ACCENT_BLUE;
        return switch (t.getCategory().toLowerCase()) {
            case "school" -> ACCENT_BLUE;
            case "work" -> ACCENT_GREEN;
            case "personal" -> ACCENT_PURPLE;
            case "extracurricular" -> ACCENT_ORANGE;
            default -> ACCENT_BLUE;
        };
    }

    String formatTime(int h, int m) {
        return String.format("%d:%02d %s", h % 12 == 0 ? 12 : h % 12, m, h < 12 ? "AM" : "PM");
    }

    // ── Widget helpers ───────────────────────────────────────────────
    JButton accentButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setForeground(Color.WHITE);
        b.setBackground(ACCENT_BLUE);
        b.setBorder(new EmptyBorder(8, 18, 8, 18));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    JButton iconButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setForeground(TEXT_MUTED);
        b.setBackground(BG_CARD);
        b.setBorder(new EmptyBorder(6, 12, 6, 12));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    JButton pillButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
        b.setForeground(TEXT_PRIMARY);
        b.setBackground(BG_CARD);
        b.setBorder(new EmptyBorder(6, 14, 6, 14));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    JPanel buildDialogButtons(JDialog dlg, Runnable onSave) {
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        btns.setBackground(BG_PANEL);
        btns.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_COLOR));
        JButton cancel = pillButton("Cancel");
        cancel.addActionListener(e -> dlg.dispose());
        JButton save = accentButton("Save");
        save.addActionListener(e -> onSave.run());
        btns.add(cancel);
        btns.add(save);
        return btns;
    }

    JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 10));
        l.setForeground(TEXT_MUTED);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    JPanel navItem(String text, boolean active) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(active ? new Color(99, 102, 241, 40) : BG_PANEL);
        p.setBorder(new CompoundBorder(
                active ? new LineBorder(new Color(99, 102, 241, 80), 1, true) : new EmptyBorder(0, 0, 0, 0),
                new EmptyBorder(8, 10, 8, 10)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 13));
        l.setForeground(active ? ACCENT_BLUE : TEXT_PRIMARY);
        p.add(l);
        return p;
    }

    JPanel categoryItem(String name, Color color) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JPanel dot = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(color);
                g.fillOval(0, 3, 10, 10);
            }
        };
        dot.setPreferredSize(new Dimension(10, 16));
        dot.setOpaque(false);
        JLabel l = new JLabel(name);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l.setForeground(TEXT_PRIMARY);
        p.add(dot);
        p.add(l);
        return p;
    }

    JPanel priorityBadge(String name, Color color) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JLabel l = new JLabel(name);
        l.setFont(new Font("SansSerif", Font.PLAIN, 11));
        l.setForeground(color);
        p.add(l);
        return p;
    }

    JTextField styledField(String placeholder) {
        JTextField f = new JTextField(placeholder);
        f.setBackground(BG_CARD);
        f.setForeground(TEXT_PRIMARY);
        f.setCaretColor(TEXT_PRIMARY);
        f.setFont(new Font("SansSerif", Font.PLAIN, 13));
        f.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(8, 10, 8, 10)));
        return f;
    }

    JComboBox<String> styledCombo(String... items) {
        JComboBox<String> b = new JComboBox<>(items);
        b.setBackground(BG_CARD);
        b.setForeground(TEXT_PRIMARY);
        b.setFont(new Font("SansSerif", Font.PLAIN, 13));
        return b;
    }

    void styleSpinner(JSpinner s) {
        s.setBackground(BG_CARD);
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setBackground(BG_CARD);
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setForeground(TEXT_PRIMARY);
    }

    JLabel formLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 11));
        l.setForeground(TEXT_MUTED);
        return l;
    }

    // ════════════════════════════════════════════════════════════════
    // DEMO DATA — uses your exact Task and Event constructors
    // ════════════════════════════════════════════════════════════════
    void seedDemoData() {
        LocalDate mon = LocalDate.now().with(DayOfWeek.MONDAY);

        // Task(name, category, dueDate, userPriority, estimatedTime, completed,
        // description)
        tasks.add(new Task("Math Homework", "School", mon.atTime(17, 0), 8, 90, false, "Chapter 5 problems 1–20"));
        tasks.add(new Task("History Essay Draft", "School", mon.plusDays(1).atTime(23, 59), 7, 120, false,
                "Thesis + 3 body paragraphs"));
        tasks.add(new Task("Physics Lab Report", "School", mon.plusDays(2).atTime(12, 0), 6, 60, false, null));
        tasks.add(new Task("College App Essay", "School", mon.plusDays(3).atTime(23, 59), 9, 120, false,
                "Common App personal statement"));
        tasks.add(new Task("Work Report", "Work", mon.plusDays(4).atTime(17, 0), 5, 90, false, null));
        tasks.add(new Task("Weekend Reading", "Personal", mon.plusDays(5).atTime(20, 0), 3, 60, false, null));
        tasks.add(new Task("Overdue Assignment", "School", mon.minusDays(1).atTime(12, 0), 8, 45, false,
                "This is overdue!"));

        // Event(name, date, startTime, endTime, duration, location, travelTime, status)
        LocalDateTime schoolS = mon.atTime(8, 0), schoolE = mon.atTime(15, 0);
        LocalDateTime pracS = mon.plusDays(1).atTime(16, 30), pracE = mon.plusDays(1).atTime(18, 0);
        LocalDateTime shiftS = mon.plusDays(2).atTime(15, 0), shiftE = mon.plusDays(2).atTime(19, 0);
        LocalDateTime clubS = mon.plusDays(3).atTime(15, 0), clubE = mon.plusDays(3).atTime(16, 0);

        events.add(new Event("School", schoolS, schoolS, schoolE, 420, "High School", 15, "FIXED"));
        events.add(new Event("Soccer Practice", pracS, pracS, pracE, 90, "Sports Field", 10, "FIXED"));
        events.add(new Event("Work Shift", shiftS, shiftS, shiftE, 240, "Part-time Job", 20, "FIXED"));
        events.add(new Event("Club Meeting", clubS, clubS, clubE, 60, "Room 204", 0, "OPTIONAL"));
    }
}
