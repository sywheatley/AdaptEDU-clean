import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;

public class AdaptEDUApp extends JFrame {

    // ── Colour palette ──────────────────────────────────────────────
    static final Color BG_DARK       = new Color(18,  18,  30);
    static final Color BG_PANEL      = new Color(26,  26,  42);
    static final Color BG_CARD       = new Color(34,  34,  54);
    static final Color ACCENT_BLUE   = new Color(99,  102, 241);
    static final Color ACCENT_PURPLE = new Color(139, 92,  246);
    static final Color ACCENT_GREEN  = new Color(52,  211, 153);
    static final Color ACCENT_ORANGE = new Color(251, 146, 60);
    static final Color ACCENT_RED    = new Color(248, 113, 113);
    static final Color TEXT_PRIMARY  = new Color(236, 236, 255);
    static final Color TEXT_MUTED    = new Color(120, 120, 160);
    static final Color BORDER_COLOR  = new Color(50,  50,  75);

    // ── State ────────────────────────────────────────────────────────
    LocalDate currentWeekStart;
    List<Task> tasks = new ArrayList<>();
    WeeklyCalendarPanel calendarPanel;
    TaskListPanel taskListPanel;
    JLabel weekLabel;

    // ── Entry point ──────────────────────────────────────────────────
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new AdaptEDUApp().setVisible(true));
    }

    // ── Constructor ──────────────────────────────────────────────────
    public AdaptEDUApp() {
        super("AdaptEDU");
        currentWeekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        seedDemoTasks();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(0, 0));

        add(buildTopBar(),    BorderLayout.NORTH);
        add(buildSidebar(),   BorderLayout.WEST);
        add(buildMainArea(),  BorderLayout.CENTER);
    }

    // ════════════════════════════════════════════════════════════════
    // TOP BAR
    // ════════════════════════════════════════════════════════════════
    JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_PANEL);
        bar.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_COLOR));
        bar.setPreferredSize(new Dimension(0, 56));

        // Logo
        JLabel logo = new JLabel("  AdaptEDU");
        logo.setFont(new Font("SansSerif", Font.BOLD, 20));
        logo.setForeground(ACCENT_BLUE);
        logo.setPreferredSize(new Dimension(220, 56));
        bar.add(logo, BorderLayout.WEST);

        // Week navigation
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 12));
        nav.setOpaque(false);

        JButton prev = iconButton("◀");
        weekLabel = new JLabel(weekRangeLabel(), SwingConstants.CENTER);
        weekLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        weekLabel.setForeground(TEXT_PRIMARY);
        weekLabel.setPreferredSize(new Dimension(230, 30));
        JButton next = iconButton("▶");
        JButton todayBtn = pillButton("Today");

        prev.addActionListener(e -> { currentWeekStart = currentWeekStart.minusWeeks(1); refreshCalendar(); });
        next.addActionListener(e -> { currentWeekStart = currentWeekStart.plusWeeks(1);  refreshCalendar(); });
        todayBtn.addActionListener(e -> {
            currentWeekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
            refreshCalendar();
        });

        nav.add(prev); nav.add(todayBtn); nav.add(weekLabel); nav.add(next);
        bar.add(nav, BorderLayout.CENTER);

        // Add task button
        JButton addBtn = new JButton("＋  New Task");
        addBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        addBtn.setForeground(Color.WHITE);
        addBtn.setBackground(ACCENT_BLUE);
        addBtn.setBorder(new EmptyBorder(8, 18, 8, 18));
        addBtn.setFocusPainted(false);
        addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addBtn.addActionListener(e -> showAddTaskDialog());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 12));
        right.setOpaque(false);
        right.add(addBtn);
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    // ════════════════════════════════════════════════════════════════
    // SIDEBAR
    // ════════════════════════════════════════════════════════════════
    JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setBackground(BG_PANEL);
        side.setBorder(new MatteBorder(0, 0, 0, 1, BORDER_COLOR));
        side.setPreferredSize(new Dimension(220, 0));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 0, 1, BORDER_COLOR),
            new EmptyBorder(20, 16, 20, 16)
        ));

        side.add(sectionLabel("VIEWS"));
        side.add(Box.createVerticalStrut(6));
        side.add(navItem("📅  Week View", true));
        side.add(navItem("📆  Month View", false));
        side.add(navItem("📋  Day View", false));
        side.add(Box.createVerticalStrut(20));

        side.add(sectionLabel("CATEGORIES"));
        side.add(Box.createVerticalStrut(6));
        side.add(categoryItem("School",       ACCENT_BLUE));
        side.add(categoryItem("Work",         ACCENT_GREEN));
        side.add(categoryItem("Personal",     ACCENT_PURPLE));
        side.add(categoryItem("Extracurricular", ACCENT_ORANGE));
        side.add(Box.createVerticalStrut(20));

        side.add(sectionLabel("PRIORITY"));
        side.add(Box.createVerticalStrut(6));
        side.add(priorityBadge("Do Today",  ACCENT_RED));
        side.add(priorityBadge("High",      ACCENT_ORANGE));
        side.add(priorityBadge("Medium",    ACCENT_BLUE));
        side.add(priorityBadge("Low",       TEXT_MUTED));

        side.add(Box.createVerticalGlue());

        // Mini stats
        side.add(buildMiniStats());

        return side;
    }

    JPanel buildMiniStats() {
        JPanel p = new JPanel(new GridLayout(2, 2, 8, 8));
        p.setOpaque(false);
        long done    = tasks.stream().filter(t -> t.done).count();
        long pending = tasks.stream().filter(t -> !t.done).count();
        p.add(statBox(String.valueOf(pending), "Pending",   ACCENT_BLUE));
        p.add(statBox(String.valueOf(done),    "Completed", ACCENT_GREEN));
        p.add(statBox(String.valueOf(tasks.stream().filter(t -> t.priority.equals("Do Today")).count()), "Urgent", ACCENT_RED));
        p.add(statBox(String.valueOf(tasks.size()), "Total", ACCENT_PURPLE));
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
    // MAIN AREA (calendar + task list)
    // ════════════════════════════════════════════════════════════════
    JPanel buildMainArea() {
        JPanel main = new JPanel(new BorderLayout(0, 0));
        main.setBackground(BG_DARK);

        calendarPanel  = new WeeklyCalendarPanel();
        taskListPanel  = new TaskListPanel();

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            new JScrollPane(calendarPanel) {{ getViewport().setBackground(BG_DARK); setBorder(null); }},
            taskListPanel
        );
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
        static final int HOUR_H    = 64;   // px per hour
        static final int TIME_W    = 56;   // left time-label column
        static final int HEADER_H  = 52;   // day-name header height
        static final int START_H   = 7;    // first hour shown
        static final int END_H     = 23;   // last  hour shown
        static final int HOURS     = END_H - START_H;

        WeeklyCalendarPanel() {
            setBackground(BG_DARK);
            setPreferredSize(new Dimension(820, HEADER_H + HOURS * HOUR_H + 20));
        }

        int colWidth() { return (getWidth() - TIME_W) / 7; }

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

            String[] dayNames = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
            for (int d = 0; d < 7; d++) {
                LocalDate date = currentWeekStart.plusDays(d);
                int x = TIME_W + d * cw;
                boolean isToday = date.equals(today);

                g.setColor(BORDER_COLOR);
                g.drawLine(x, 0, x, HEADER_H);

                String dayStr = dayNames[d];
                String numStr = String.valueOf(date.getDayOfMonth());

                if (isToday) {
                    g.setColor(ACCENT_BLUE);
                    g.fillOval(x + cw/2 - 16, HEADER_H/2 - 2, 32, 32);
                    g.setColor(Color.WHITE);
                } else {
                    g.setColor(TEXT_MUTED);
                }
                g.setFont(new Font("SansSerif", Font.PLAIN, 11));
                drawCenteredString(g, dayStr, x, 10, cw, 16);

                if (!isToday) g.setColor(TEXT_PRIMARY);
                g.setFont(new Font("SansSerif", Font.BOLD, 15));
                drawCenteredString(g, numStr, x, HEADER_H/2, cw, 20);
            }

            // ── Hour grid ────────────────────────────────────────────
            for (int h = 0; h <= HOURS; h++) {
                int y = HEADER_H + h * HOUR_H;
                g.setColor(BORDER_COLOR);
                g.drawLine(TIME_W, y, getWidth(), y);

                if (h < HOURS) {
                    int hour = START_H + h;
                    String label = (hour % 12 == 0 ? 12 : hour % 12) + (hour < 12 ? " AM" : " PM");
                    g.setColor(TEXT_MUTED);
                    g.setFont(new Font("SansSerif", Font.PLAIN, 10));
                    g.drawString(label, 4, y + 14);
                }
            }

            // Vertical column separators
            for (int d = 0; d < 7; d++) {
                int x = TIME_W + d * cw;
                g.setColor(BORDER_COLOR);
                g.drawLine(x, HEADER_H, x, HEADER_H + HOURS * HOUR_H);
            }

            // ── Current time line ────────────────────────────────────
            LocalTime now = LocalTime.now();
            if (now.getHour() >= START_H && now.getHour() < END_H) {
                double fraction = (now.getHour() - START_H + now.getMinute() / 60.0);
                int ty = HEADER_H + (int)(fraction * HOUR_H);
                g.setColor(ACCENT_RED);
                g.setStroke(new BasicStroke(2));
                g.fillOval(TIME_W - 5, ty - 5, 10, 10);
                g.drawLine(TIME_W, ty, getWidth(), ty);
                g.setStroke(new BasicStroke(1));
            }

            // ── Task blocks ──────────────────────────────────────────
            for (Task t : tasks) {
                int dayOffset = (int) currentWeekStart.until(t.date, java.time.temporal.ChronoUnit.DAYS);
                if (dayOffset < 0 || dayOffset > 6) continue;

                double startFrac = (t.startHour - START_H) + t.startMin / 60.0;
                double endFrac   = startFrac + t.durationMins / 60.0;
                if (endFrac <= 0 || startFrac >= HOURS) continue;

                int x  = TIME_W + dayOffset * cw + 3;
                int y  = HEADER_H + (int)(Math.max(0, startFrac) * HOUR_H);
                int w  = cw - 6;
                int h  = (int)((Math.min(HOURS, endFrac) - Math.max(0, startFrac)) * HOUR_H) - 2;
                if (h < 4) h = 4;

                Color blockColor = taskColor(t);
                Color dimmed = t.done ? dimColor(blockColor) : blockColor;

                // Shadow
                g.setColor(new Color(0,0,0,60));
                g.fillRoundRect(x+2, y+2, w, h, 8, 8);

                // Block
                g.setColor(new Color(dimmed.getRed(), dimmed.getGreen(), dimmed.getBlue(), t.done ? 100 : 220));
                g.fillRoundRect(x, y, w, h, 8, 8);

                // Left accent stripe
                g.setColor(dimmed);
                g.fillRoundRect(x, y, 4, h, 4, 4);

                // Text
                if (h >= 18) {
                    g.setColor(t.done ? TEXT_MUTED : Color.WHITE);
                    g.setFont(new Font("SansSerif", Font.BOLD, 11));
                    String title = t.done ? "✓ " + t.name : t.name;
                    drawClippedString(g, title, x + 8, y + 14, w - 12);
                }
                if (h >= 32) {
                    g.setColor(new Color(255,255,255,160));
                    g.setFont(new Font("SansSerif", Font.PLAIN, 10));
                    String timeStr = formatHour(t.startHour, t.startMin) + " · " + t.durationMins + "m";
                    drawClippedString(g, timeStr, x + 8, y + 27, w - 12);
                }
            }
        }

        void drawCenteredString(Graphics2D g, String s, int x, int y, int w, int h) {
            FontMetrics fm = g.getFontMetrics();
            int tx = x + (w - fm.stringWidth(s)) / 2;
            int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;
            g.drawString(s, tx, ty);
        }

        void drawClippedString(Graphics2D g, String s, int x, int y, int maxW) {
            FontMetrics fm = g.getFontMetrics();
            while (fm.stringWidth(s) > maxW && s.length() > 3)
                s = s.substring(0, s.length() - 4) + "…";
            g.drawString(s, x, y);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // TASK LIST PANEL (right sidebar)
    // ════════════════════════════════════════════════════════════════
    class TaskListPanel extends JPanel {
        JPanel listContainer;

        TaskListPanel() {
            setBackground(BG_PANEL);
            setLayout(new BorderLayout());
            setBorder(new MatteBorder(0, 1, 0, 0, BORDER_COLOR));
            setPreferredSize(new Dimension(300, 0));

            // Header
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(BG_PANEL);
            header.setBorder(new CompoundBorder(
                new MatteBorder(0,0,1,0, BORDER_COLOR),
                new EmptyBorder(14,16,14,16)
            ));
            JLabel title = new JLabel("Tasks by Priority");
            title.setFont(new Font("SansSerif", Font.BOLD, 14));
            title.setForeground(TEXT_PRIMARY);
            header.add(title, BorderLayout.WEST);
            add(header, BorderLayout.NORTH);

            // Scrollable list
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

            String[] groups = {"Do Today", "High", "Medium", "Low"};
            Color[]  gColors = {ACCENT_RED, ACCENT_ORANGE, ACCENT_BLUE, TEXT_MUTED};

            for (int gi = 0; gi < groups.length; gi++) {
                String group = groups[gi];
                Color  color = gColors[gi];
                List<Task> grouped = tasks.stream()
                    .filter(t -> t.priority.equals(group))
                    .sorted(Comparator.comparing((Task t) -> t.date))
                    .collect(java.util.stream.Collectors.toList());
                if (grouped.isEmpty()) continue;

                // Group label
                JLabel lbl = new JLabel("  " + group.toUpperCase());
                lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
                lbl.setForeground(color);
                lbl.setAlignmentX(LEFT_ALIGNMENT);
                lbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
                listContainer.add(lbl);
                listContainer.add(Box.createVerticalStrut(4));

                for (Task t : grouped) {
                    listContainer.add(buildTaskCard(t, color));
                    listContainer.add(Box.createVerticalStrut(6));
                }
                listContainer.add(Box.createVerticalStrut(8));
            }

            listContainer.revalidate();
            listContainer.repaint();
        }

        JPanel buildTaskCard(Task t, Color accentColor) {
            JPanel card = new JPanel(new BorderLayout(8, 0));
            card.setBackground(BG_CARD);
            card.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(10, 12, 10, 12)
            ));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
            card.setAlignmentX(LEFT_ALIGNMENT);
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Checkbox
            JCheckBox cb = new JCheckBox();
            cb.setSelected(t.done);
            cb.setOpaque(false);
            cb.setFocusPainted(false);
            cb.addActionListener(e -> {
                t.done = cb.isSelected();
                calendarPanel.repaint();
                refresh();
            });
            card.add(cb, BorderLayout.WEST);

            // Info
            JPanel info = new JPanel(new GridLayout(2, 1, 0, 2));
            info.setOpaque(false);
            JLabel name = new JLabel(t.name);
            name.setFont(new Font("SansSerif", Font.BOLD, 13));
            name.setForeground(t.done ? TEXT_MUTED : TEXT_PRIMARY);
            JLabel meta = new JLabel(t.category + "  ·  " +
                t.date.format(DateTimeFormatter.ofPattern("EEE MMM d")) + "  ·  " +
                formatHour(t.startHour, t.startMin));
            meta.setFont(new Font("SansSerif", Font.PLAIN, 11));
            meta.setForeground(TEXT_MUTED);
            info.add(name);
            info.add(meta);
            card.add(info, BorderLayout.CENTER);

            // Score badge
            JLabel score = new JLabel(String.valueOf(t.priorityScore()));
            score.setFont(new Font("SansSerif", Font.BOLD, 13));
            score.setForeground(accentColor);
            score.setHorizontalAlignment(SwingConstants.RIGHT);
            card.add(score, BorderLayout.EAST);

            // Hover highlight
            card.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { card.setBackground(new Color(44,44,66)); card.repaint(); }
                public void mouseExited (MouseEvent e) { card.setBackground(BG_CARD);            card.repaint(); }
            });

            return card;
        }
    }

    // ════════════════════════════════════════════════════════════════
    // ADD TASK DIALOG
    // ════════════════════════════════════════════════════════════════
    void showAddTaskDialog() {
        JDialog dlg = new JDialog(this, "New Task", true);
        dlg.setSize(420, 460);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(BG_PANEL);
        dlg.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_PANEL);
        form.setBorder(new EmptyBorder(24, 28, 8, 28));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 0, 6, 0);
        c.weightx = 1;

        // Task name
        JTextField nameField = styledField("Task name…");
        c.gridx=0; c.gridy=0; form.add(formLabel("Task Name"), c);
        c.gridy=1; form.add(nameField, c);

        // Category
        JComboBox<String> catBox = styledCombo("School","Work","Personal","Extracurricular");
        c.gridy=2; form.add(formLabel("Category"), c);
        c.gridy=3; form.add(catBox, c);

        // Priority
        JComboBox<String> prioBox = styledCombo("Do Today","High","Medium","Low");
        c.gridy=4; form.add(formLabel("Priority"), c);
        c.gridy=5; form.add(prioBox, c);

        // Date
        JTextField dateField = styledField(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        c.gridy=6; form.add(formLabel("Due Date (YYYY-MM-DD)"), c);
        c.gridy=7; form.add(dateField, c);

        // Start time
        JTextField timeField = styledField("09:00");
        c.gridy=8; form.add(formLabel("Start Time (HH:MM)"), c);
        c.gridy=9; form.add(timeField, c);

        // Duration
        JSpinner durSpinner = new JSpinner(new SpinnerNumberModel(60, 15, 480, 15));
        styleSpinner(durSpinner);
        c.gridy=10; form.add(formLabel("Duration (minutes)"), c);
        c.gridy=11; form.add(durSpinner, c);

        dlg.add(form, BorderLayout.CENTER);

        // Buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        btns.setBackground(BG_PANEL);
        btns.setBorder(new MatteBorder(1,0,0,0, BORDER_COLOR));

        JButton cancel = pillButton("Cancel");
        cancel.addActionListener(e -> dlg.dispose());

        JButton save = new JButton("Add Task");
        save.setFont(new Font("SansSerif", Font.BOLD, 13));
        save.setForeground(Color.WHITE);
        save.setBackground(ACCENT_BLUE);
        save.setBorder(new EmptyBorder(8,20,8,20));
        save.setFocusPainted(false);
        save.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        save.addActionListener(e -> {
            try {
                String name = nameField.getText().trim();
                if (name.isEmpty()) { nameField.setBorder(new LineBorder(ACCENT_RED)); return; }
                LocalDate date = LocalDate.parse(dateField.getText().trim());
                String[] tp = timeField.getText().trim().split(":");
                int sh = Integer.parseInt(tp[0]);
                int sm = Integer.parseInt(tp[1]);
                tasks.add(new Task(name,
                    (String) catBox.getSelectedItem(),
                    (String) prioBox.getSelectedItem(),
                    date, sh, sm,
                    (Integer) durSpinner.getValue()));
                tasks.sort(Comparator.comparingInt(Task::priorityScore).reversed());
                refreshCalendar();
                dlg.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Please check date (YYYY-MM-DD) and time (HH:MM) formats.");
            }
        });

        btns.add(cancel);
        btns.add(save);
        dlg.add(btns, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // ════════════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════════════
    void refreshCalendar() {
        weekLabel.setText(weekRangeLabel());
        calendarPanel.repaint();
        taskListPanel.refresh();
    }

    String weekRangeLabel() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d");
        LocalDate end = currentWeekStart.plusDays(6);
        return currentWeekStart.format(fmt) + " – " + end.format(fmt) + ", " + currentWeekStart.getYear();
    }

    Color taskColor(Task t) {
        return switch (t.category) {
            case "School"          -> ACCENT_BLUE;
            case "Work"            -> ACCENT_GREEN;
            case "Personal"        -> ACCENT_PURPLE;
            case "Extracurricular" -> ACCENT_ORANGE;
            default                -> ACCENT_BLUE;
        };
    }

    Color dimColor(Color c) {
        return new Color(c.getRed()/2, c.getGreen()/2, c.getBlue()/2);
    }

    String formatHour(int h, int m) {
        int h12 = h % 12 == 0 ? 12 : h % 12;
        String ampm = h < 12 ? "AM" : "PM";
        return String.format("%d:%02d %s", h12, m, ampm);
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

    JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 10));
        l.setForeground(TEXT_MUTED);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    JPanel navItem(String text, boolean active) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(active ? new Color(99,102,241,40) : BG_PANEL);
        p.setBorder(new CompoundBorder(
            active ? new LineBorder(new Color(99,102,241,80), 1, true) : new EmptyBorder(0,0,0,0),
            new EmptyBorder(8, 10, 8, 10)
        ));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 13));
        l.setForeground(active ? ACCENT_BLUE : TEXT_PRIMARY);
        p.add(l);
        return p;
    }

    JPanel categoryItem(String name, Color color) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        JPanel dot = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(color);
                g.fillOval(0,2,10,10);
            }
        };
        dot.setPreferredSize(new Dimension(10,14));
        dot.setOpaque(false);
        JLabel l = new JLabel(name);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l.setForeground(TEXT_PRIMARY);
        p.add(dot); p.add(l);
        return p;
    }

    JPanel priorityBadge(String name, Color color) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JLabel l = new JLabel(name);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
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
            new EmptyBorder(8, 10, 8, 10)
        ));
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
        s.setForeground(TEXT_PRIMARY);
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
    // TASK MODEL
    // ════════════════════════════════════════════════════════════════
    static class Task {
        String    name, category, priority;
        LocalDate date;
        int       startHour, startMin, durationMins;
        boolean   done;

        Task(String name, String category, String priority,
             LocalDate date, int startHour, int startMin, int durationMins) {
            this.name         = name;
            this.category     = category;
            this.priority     = priority;
            this.date         = date;
            this.startHour    = startHour;
            this.startMin     = startMin;
            this.durationMins = durationMins;
        }

        int priorityScore() {
            int base = switch (priority) {
                case "Do Today" -> 100;
                case "High"     -> 75;
                case "Medium"   -> 50;
                default         -> 25;
            };
            long daysUntil = LocalDate.now().until(date, java.time.temporal.ChronoUnit.DAYS);
            int urgency = daysUntil <= 0 ? 20 : daysUntil <= 2 ? 15 : daysUntil <= 7 ? 5 : 0;
            return base + urgency;
        }
    }

    // ════════════════════════════════════════════════════════════════
    // DEMO DATA
    // ════════════════════════════════════════════════════════════════
    void seedDemoTasks() {
        LocalDate today = LocalDate.now();
        LocalDate mon   = today.with(java.time.DayOfWeek.MONDAY);
        tasks.addAll(List.of(
            new Task("Math Homework",       "School",          "Do Today",  mon,              9,  0, 90),
            new Task("History Essay Draft", "School",          "High",      mon.plusDays(1), 13,  0, 120),
            new Task("Soccer Practice",     "Extracurricular", "Medium",    mon.plusDays(1), 16, 30, 90),
            new Task("Physics Lab Report",  "School",          "High",      mon.plusDays(2), 10,  0, 60),
            new Task("Work Shift",          "Work",            "Do Today",  mon.plusDays(2), 15,  0, 180),
            new Task("College App Essay",   "School",          "Do Today",  mon.plusDays(3),  9,  0, 120),
            new Task("Gym",                 "Personal",        "Low",       mon.plusDays(3), 17,  0, 60),
            new Task("Study for SAT",       "School",          "High",      mon.plusDays(4),  8,  0, 120),
            new Task("Club Meeting",        "Extracurricular", "Medium",    mon.plusDays(4), 15,  0, 60),
            new Task("Weekend Reading",     "Personal",        "Low",       mon.plusDays(5), 11,  0, 60)
        ));
        tasks.sort(Comparator.comparingInt(Task::priorityScore).reversed());
    }
}