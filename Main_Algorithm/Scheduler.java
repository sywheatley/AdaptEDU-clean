package Main_Algorithm;
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

        long getDurationInMinutes() {
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

    /**
     * Generates a schedule by placing tasks into the free time between fixed events.
     *
     * @param fixedEvents       A list of events that are already scheduled and cannot be moved.
     * @param tasksToSchedule   A list of tasks that need to be scheduled.
     * @param scheduleStart     The start of the time window for scheduling (e.g., beginning of the day).
     * @param scheduleEnd       The end of the time window for scheduling (e.g., end of the day).
     * @return A list of Event objects, including the original fixed events and new events for the scheduled tasks.
     */
    public List<Event> generateSchedule(List<Event> fixedEvents, List<Task> tasksToSchedule, LocalDateTime scheduleStart, LocalDateTime scheduleEnd) {
        // 1. Find all available time slots
        List<TimeSlot> freeSlots = findFreeTimeSlots(fixedEvents, scheduleStart, scheduleEnd);

        // 2. Prioritize tasks to schedule the most important ones first
        List<Task> sortedTasks = tasksToSchedule.stream()
                .filter(t -> !t.isCompleted()) // Don't schedule completed tasks
                .sorted(Comparator.comparing(Task::getPriorityScore).reversed())
                .collect(Collectors.toList());

        List<Event> scheduledTaskEvents = new ArrayList<>();
        Map<Task, Double> remainingTimes = new LinkedHashMap<>();

        // 3. Fit tasks into free slots
        for (Task task : sortedTasks) {
            // Adjust estimated time for a more realistic duration using the procrastination model
            double remainingDuration = procrastination_alg.main_alg.getRealisticTimeInMinutes(task.getEstimatedTime());
            List<Event> taskSessions = new ArrayList<>();
            
            // Find slots for the task, splitting if necessary
            for (TimeSlot slot : freeSlots) {
                long slotDuration = slot.getDurationInMinutes();
                if (slotDuration <= 0) continue;

                // Take as much time as possible from the current slot
                double timeToTake = Math.min((double) slotDuration, remainingDuration);

                LocalDateTime taskStart = slot.start;
                LocalDateTime taskEnd = taskStart.plusMinutes((long) timeToTake);

                // Create a new Event to represent the scheduled task session
                Event taskEvent = new Event(
                    task.getName(),
                    taskStart, // The 'Date' field in Event is a bit redundant, but we use start time
                    taskStart,
                    taskEnd
                );
                taskEvent.setStatus("SCHEDULED_TASK");
                taskEvent.setDescription("Scheduled block for task: " + task.getName());
                if (task.getCategory() != null) {
                    taskEvent.setCategory(task.getCategory());
                }
                
                taskSessions.add(taskEvent);

                // Update the free slot by moving its start time forward
                slot.start = taskEnd;
                remainingDuration -= timeToTake;

                // Move to the next task if we've fulfilled the required time
                if (remainingDuration <= 0.1) {
                    break;
                }
            }

            // If the task was split into multiple sessions, label them
            if (taskSessions.size() > 1) {
                for (int i = 0; i < taskSessions.size(); i++) {
                    taskSessions.get(i).setName(task.getName() + " (Session " + (i + 1) + ")");
                }
            }

            scheduledTaskEvents.addAll(taskSessions);

            if (remainingDuration > 0.1) {
                remainingTimes.put(task, remainingDuration);
            }
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
     * Identifies blocks of free time between a given start and end time, avoiding a list of busy events.
     */
    private List<TimeSlot> findFreeTimeSlots(List<Event> events, LocalDateTime windowStart, LocalDateTime windowEnd) {
        List<TimeSlot> freeSlots = new ArrayList<>();

        // Filter events to be within our scheduling window and sort them
        List<Event> sortedEvents = events.stream()
                .filter(e -> e.getStartTime() != null && e.getEndTime() != null)
                .filter(e -> e.getEndTime().isAfter(windowStart) && e.getStartTime().isBefore(windowEnd))
                .sorted(Comparator.comparing(Event::getStartTime))
                .collect(Collectors.toList());

        LocalDateTime currentTime = windowStart;

        for (Event event : sortedEvents) {
            // If there's a gap between the current time and the start of the next event, it's a free slot
            if (currentTime.isBefore(event.getStartTime())) {
                freeSlots.add(new TimeSlot(currentTime, event.getStartTime()));
            }
            // Move the current time to the end of the event, effectively handling overlaps
            if (event.getEndTime().isAfter(currentTime)) {
                currentTime = event.getEndTime();
            }
        }

        // Add the final free slot from the end of the last event to the end of the window
        if (currentTime.isBefore(windowEnd)) {
            freeSlots.add(new TimeSlot(currentTime, windowEnd));
        }

        return freeSlots;
    }

    private static List<Task> loadTasksFromCSV(String filePath) {
        List<Task> loadedTasks = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",", -1);
                if (values.length >= 7) {
                    try {
                        loadedTasks.add(new Task(
                            values[0], // name
                            values[1], // category
                            LocalDateTime.parse(values[2]), // dueDate
                            Integer.parseInt(values[3]), // userPriority
                            Integer.parseInt(values[4]), // estimatedTime
                            Boolean.parseBoolean(values[5]), // completed
                            values[6] // description
                        ));
                    } catch (Exception e) {
                        System.err.println("Skipping invalid task row: " + line);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load tasks from " + filePath + " (" + e.getMessage() + ")");
        }
        return loadedTasks;
    }

    private static List<Event> loadEventsFromCSV(String filePath) {
        List<Event> loadedEvents = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",", -1);
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
                        if (values.length > 7 && !values[7].isEmpty()) e.setCategory(values[7]);
                        if (values.length > 8 && !values[8].isEmpty()) e.setDescription(values[8]);
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
        List<Event> fixedEvents = loadEventsFromCSV("Main_Algorithm/events.csv");
        List<Task> tasks = loadTasksFromCSV("Main_Algorithm/tasks.csv");
        
        if (fixedEvents.isEmpty() && tasks.isEmpty()) {
            System.out.println("No data found in CSVs. Please make sure events.csv and tasks.csv exist in the running directory.");
        }

        // --- 2. Define the scheduling window ---
        // The test data in the CSV spans from May 19 to May 25, 2024.
        // We test the schedule for a specific day from the dataset (e.g., Monday, May 20)
        LocalDate testDate = LocalDate.of(2024, 5, 20);
        LocalDateTime scheduleStart = testDate.atTime(8, 0);
        LocalDateTime scheduleEnd = testDate.atTime(22, 0);

        // --- 3. Generate and print the schedule ---
        System.out.println("Generating Schedule for " + testDate + "...\n");
        List<Event> fullSchedule = scheduler.generateSchedule(fixedEvents, tasks, scheduleStart, scheduleEnd);
        
        System.out.println("--- Final Daily Schedule ---");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        
        for (Event e : fullSchedule) {
            String type = "FIXED_EVENT";
            String extraInfo = "";
            
            if ("SCHEDULED_TASK".equals(e.getStatus())) {
                type = "SCHEDULED_TASK";
                
                // Find the original task to get the user's estimate
                Task originalTask = tasks.stream()
                        .filter(t -> e.getName().equals(t.getName()) || e.getName().startsWith(t.getName() + " (Session"))
                        .findFirst().orElse(null);
                if (originalTask != null) {
                    long actualScheduledMins = Duration.between(e.getStartTime(), e.getEndTime()).toMinutes();
                    extraInfo = String.format(" (User Est: %d mins | Alg Calculated: %d mins)", originalTask.getEstimatedTime(), actualScheduledMins);
                }
            }
            
            System.out.printf("[%s] %s to %s - %s%s\n", 
                type, e.getStartTime().format(timeFormatter), e.getEndTime().format(timeFormatter), e.getName(), extraInfo);
        }
    }
}