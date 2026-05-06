package procrastination_alg;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.LinkedHashMap;

// Assuming Task, Event, and the procrastination algorithm are in the classpath.
// You may need to ensure your project is set up to compile/access these files.

public class Scheduler {

    private TaskManager tasks = new TaskManager();

    /**
     * A simple private class to represent a block of free time.
     */
    private static class TimeSlot {
        LocalDateTime start;
        LocalDateTime end;

        TimeSlot(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }

        public long getDurationInMinutes() {
            return Duration.between(start, end).toMinutes();
        }

        @Override
        public String toString() {
            return "TimeSlot{" +
                    "start=" + start +
                    ", end=" + end +
                    ", duration=" + getDurationInMinutes() + "min" +
                    '}';
        }
    }

    public LocalDateTime getFirstFreeTime(List<TimeSlot> freeSlots) {
        for (TimeSlot time : freeSlots) {
            if (time.getDurationInMinutes() > 0) {
                return time.start;
            }
        }
        return LocalDateTime.now();
    }

    /**
     * Generates a schedule by placing tasks into the free time between fixed
     * events.
     *
     * @param fixedEvents     A list of events that are already scheduled and cannot
     *                        be moved.
     * @param tasksToSchedule A list of tasks that need to be scheduled.
     * @param scheduleStart   The start of the time window for scheduling (e.g.,
     *                        beginning of the day).
     * @param scheduleEnd     The end of the time window for scheduling (e.g., end
     *                        of the day).
     * @return A list of Event objects, including the original fixed events and new
     *         events for the scheduled tasks.
     */
    public List<Event> generateSchedule(List<Event> fixedEvents,
            LocalDateTime scheduleStart, LocalDateTime scheduleEnd, String filePath) {
        return generateSchedule(fixedEvents, scheduleStart, scheduleEnd, filePath, 8, 22);
    }

    public List<Event> generateSchedule(List<Event> fixedEvents,
            LocalDateTime scheduleStart, LocalDateTime scheduleEnd, String filePath, int startH, int endH) {

        tasks.getTasks().clear(); // Ensure we don't accumulate duplicates from previous requests
        tasks.insertTaskList(filePath);
        
        // Filter out completed tasks so they don't invisibly block out your free time
        tasks.getTasks().removeIf(Task::isCompleted);
        
        tasks.sortByDueDate();

        // 1. Find all available time slots
        List<TimeSlot> freeSlots = findFreeTimeSlots(fixedEvents, scheduleStart, scheduleEnd, startH, endH);

        // 2. Prioritize tasks to schedule the most important ones first

        tasks.sortByUrgency();

        List<Event> scheduledTaskEvents = new ArrayList<>();
        Map<Task, Double> remainingTimes = new LinkedHashMap<>();

        // 3. Fit tasks into free slots

        tasks.procrastinate();
        int sessionInDay = 0;
        while (!tasks.getTasks().isEmpty()) {
            LocalDateTime time = getFirstFreeTime(freeSlots);
            tasks.sortByUrgency(time);

            Task originalTask = tasks.getTasks().get(0);
            Task taskToSchedule = originalTask;
            
            if (sessionInDay >= 3 && originalTask.getPriorityScore() < -10) {
                taskToSchedule = new Task("Break", "BREAK", LocalDateTime.MAX, 0, 15, false, 15, "Break");
                sessionInDay = 0;
            }

            int remainingDuration = taskToSchedule.getEstimatedTime();
            List<Event> taskSessions = new ArrayList<>();

            // Find slots for the task, splitting if necessary
            boolean scheduledAny = false;
            for (TimeSlot slot : freeSlots) {

                // Snap the slot start time to the next 15-minute boundary for clean blocks
                LocalDateTime taskStart = slot.start;
                int startMod = taskStart.getMinute() % 15;
                if (startMod != 0) {
                    taskStart = taskStart.plusMinutes(15 - startMod);
                }

                long slotDuration = Duration.between(taskStart, slot.end).toMinutes();
                // Require at least 15 minutes of free time to schedule a chunk
                if (slotDuration < 15) {
                    continue;
                }
                scheduledAny = true;
            
            int maxSession = taskToSchedule.getMaxSessionLength();
            if (maxSession == -1) {
                maxSession = Integer.MAX_VALUE; // No limit (No Breaks)
            }
                // Take as much time as possible from the current slot
            int timeToTake = (int) Math.min(maxSession, Math.min(slotDuration, remainingDuration));

                // Lock chunks into 15-minute increments unless it's the final tiny piece
                if (timeToTake >= 15) {
                    timeToTake = (timeToTake / 15) * 15;
                }

                LocalDateTime taskEnd = taskStart.plusMinutes(timeToTake);
                sessionInDay++;
                // Create a new Event to represent the scheduled task session

                if (taskEnd.getHour() >= endH)
                    sessionInDay = 0;

                Event taskEvent = new Event(
                        taskToSchedule.getName() + (taskToSchedule.getCategory().equals("BREAK") ? "" : " (Session " + taskToSchedule.getSession() + ")"),
                        taskStart, // The 'Date' field in Event is a bit redundant, but we use start time
                        taskStart,
                        taskEnd,
                        taskToSchedule.getDueDate(),
                        taskToSchedule.getPriorityScore());
                taskEvent.setStatus("SCHEDULED_TASK");
                taskEvent.setDescription("Scheduled block for task: " + taskToSchedule.getName());
                if (taskToSchedule.getCategory() != null) {
                    taskEvent.setCategory(taskToSchedule.getCategory());
                }

                taskSessions.add(taskEvent);
                tasks.removeTask(taskToSchedule); // If break, it skips. If originalTask, removes it.

                // Update the free slot by moving its start time forward
                slot.start = taskEnd;
                remainingDuration -= timeToTake;
                if (remainingDuration > 0.1 && !taskToSchedule.getCategory().equals("BREAK")) {
                    taskToSchedule.setSession(taskToSchedule.getSession() + 1);
                    taskToSchedule.setEstimatedTime(remainingDuration);
                    tasks.addTask(taskToSchedule);

                    tasks.sortByUrgency(time);

                    break;

                } else
                    break;

            }

            if (!scheduledAny) {
                remainingTimes.put(originalTask, (double) originalTask.getEstimatedTime());
                tasks.removeTask(originalTask); // Must remove originalTask to prevent infinite loop
            }
            scheduledTaskEvents.addAll(taskSessions);
        }

        // 4. Combine fixed events with newly scheduled task events
        List<Event> fullSchedule = new ArrayList<>(fixedEvents);
        fullSchedule.addAll(scheduledTaskEvents);
        fullSchedule.sort(Comparator.comparing(Event::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())));

        if (!remainingTimes.isEmpty()) {
            System.out.println("\nWarning: Could not fully schedule all tasks. Unscheduled remaining time:");
            for (Map.Entry<Task, Double> entry : remainingTimes.entrySet()) {
                System.out.printf("- %s (Remaining: %.0f min)\n", entry.getKey().getName(), entry.getValue());
            }
        }

        return fullSchedule;
    }

    /**
     * Identifies blocks of free time between a given start and end time, avoiding a
     * list of busy events.
     */
    private List<TimeSlot> findFreeTimeSlots(List<Event> events, LocalDateTime windowStart, LocalDateTime windowEnd, int startH, int endH) {
        List<TimeSlot> freeSlots = new ArrayList<>();

        List<Event> sortedEvents = events.stream()
                .filter(e -> e.getStartTime() != null && e.getEndTime() != null)
                .sorted(Comparator.comparing(Event::getStartTime))
                .collect(Collectors.toList());

        LocalDateTime currentTime = windowStart;

        for (Event event : sortedEvents) {
            if (currentTime.getHour() >= endH) currentTime = currentTime.plusDays(1).withHour(startH).withMinute(0).withSecond(0).withNano(0);
            if (currentTime.getHour() < startH) currentTime = currentTime.withHour(startH).withMinute(0).withSecond(0).withNano(0);

            if (!currentTime.isBefore(event.getEndTime())) continue; // Skip events in the past

            while (currentTime.isBefore(event.getStartTime())) {
                if (currentTime.getHour() >= endH) currentTime = currentTime.plusDays(1).withHour(startH).withMinute(0).withSecond(0).withNano(0);
                if (currentTime.getHour() < startH) currentTime = currentTime.withHour(startH).withMinute(0).withSecond(0).withNano(0);
                
                if (!currentTime.isBefore(event.getStartTime())) break;

                LocalDateTime endOfDay = currentTime.withHour(endH).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime slotEnd = event.getStartTime().isBefore(endOfDay) ? event.getStartTime() : endOfDay;
                
                if (currentTime.isBefore(slotEnd)) {
                    freeSlots.add(new TimeSlot(currentTime, slotEnd));
                }
                currentTime = slotEnd;
            }

            if (currentTime.isBefore(event.getEndTime())) {
                currentTime = event.getEndTime();
            }
        }

        // Fill the rest of the days into the future to ensure enough space
        for (int i = 0; i < 70; i++) {
            if (currentTime.getHour() >= endH) currentTime = currentTime.plusDays(1).withHour(startH).withMinute(0).withSecond(0).withNano(0);
            if (currentTime.getHour() < startH) currentTime = currentTime.withHour(startH).withMinute(0).withSecond(0).withNano(0);

            LocalDateTime endOfDay = currentTime.withHour(endH).withMinute(0).withSecond(0).withNano(0);
            if (currentTime.isBefore(endOfDay)) {
                freeSlots.add(new TimeSlot(currentTime, endOfDay));
            }
            currentTime = currentTime.plusDays(1).withHour(startH).withMinute(0).withSecond(0).withNano(0);
        }

        return freeSlots;
    }

    public static List<Event> loadEventsFromCSV(String filePath) {
        List<Event> loadedEvents = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] values = TaskManager.parseCsvLine(line);
                if (values.length >= 7) {
                    try {
                        LocalDateTime start = LocalDateTime.parse(values[1]);
                        Event e = new Event(
                                values[0], // name
                                start, // Date
                                start, // startTime
                                LocalDateTime.parse(values[2]), // endTime
                                Integer.parseInt(values[3]), // duration
                                values[4], // location
                                Integer.parseInt(values[5]), // travelTime
                                values[6] // status
                        );
                        if (values.length > 7 && !values[7].isEmpty())
                            e.setCategory(values[7]);
                        if (values.length > 8 && !values[8].isEmpty())
                            e.setDescription(values[8]);
                        loadedEvents.add(e);
                    } catch (Exception e) {
                        System.err.println("Skipping invalid event row: " + line);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load events from " + filePath + " (" + e.getMessage() + ")");
        }
        return loadedEvents;
    }

    public static void main(String[] args) {
        Scheduler scheduler = new Scheduler();

        // --- 1. Load Data From CSV ---
        List<Event> fixedEvents = loadEventsFromCSV("src/main/java/procrastination_alg/events.csv");
        // --- 2. Define the scheduling window ---
        // The test data in the CSV spans from May 19 to May 25, 2024.
        // We test the schedule for a specific day from the dataset (e.g., Monday, May
        // 20)
        LocalDate testDate = LocalDate.of(2024, 5, 20);
        LocalDateTime scheduleStart = testDate.atTime(8, 0);
        LocalDateTime scheduleEnd = testDate.atTime(22, 0);

        // --- 3. Generate and print the schedule ---
        System.out.println("Generating Schedule for " + testDate + "...\n");
        List<Event> fullSchedule = scheduler.generateSchedule(fixedEvents, scheduleStart, scheduleEnd,
                "src/main/java/procrastination_alg/tasks.csv");

        System.out.println("--- Final Daily Schedule ---");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

        // to detect overlaps:
        boolean overlap = false;
        boolean sleepLoss = false;
        boolean overDue = false;
        int index = 0;
        Event overlapped = new Event("Placeholder", LocalDateTime.now());

        LocalDateTime date = LocalDateTime.MIN;
        for (Event e : fullSchedule) {
            if (e.getDate().toLocalDate().isAfter(date.toLocalDate())) {
                date = e.getDate();
                System.out.println(date.toLocalDate().format(dateFormatter) + " - " + date.getDayOfWeek());
            }
            String type = "FIXED_EVENT";
            String extraInfo = "";

            if ("SCHEDULED_TASK".equals(e.getStatus())) {
                type = "SCHEDULED_TASK";
            }

            System.out.printf("[%s] %s to %s - %s %s%s\n",
                    type, e.getStartTime().format(timeFormatter), e.getEndTime().format(timeFormatter), e.getName(),
                    e.getPriorityScore(),
                    extraInfo);
            if (index > 0) {
                if (/* e.getStartTime().isBefore(fullSchedule.get(index - 1).getEndTime()) */ e.getStartTime()
                        .getDayOfYear() <= fullSchedule.get(index - 1).getEndTime().getDayOfYear()
                        && e.getStartTime().getHour() < fullSchedule.get(index - 1).getEndTime().getHour()) {
                    if (e.getStatus().equals("SCHEDULED_TASK")
                            || fullSchedule.get(index - 1).getStatus().equals("SCHEDULED_TASK")) {
                        overlap = true;
                        overlapped = e;
                    }
                    System.out.println("^^^^^ OVERLAP ^^^^^");
                }

            }
            if (e.getDueDate().isBefore(e.getEndTime())) {
                overDue = true;
                System.out.println("!!! PREDICTED OVERDUE !!!");
            }
            if (e.getStartTime().getHour() < 8 || e.getEndTime().getHour() > 22) {
                sleepLoss = true;
                System.out.println("~~~ PREDICTED LOSS OF SLEEP ~~~ " + e.getStartTime().getHour() + " - "
                        + e.getEndTime().getHour());
            }
            index++;
        }
        if (overlap) {
            System.out.println("OVERLAP DETECTED AT " + overlapped.getName());
        }
        if (overDue) {
            System.out.println("OVERDUE ASSIGNMENT DETECTED");
        }
        if (sleepLoss) {
            System.out.println("LOSS OF SLEEP DETECTED");
        }
        index = 0;
    }
}
