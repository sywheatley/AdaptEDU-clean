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
//BIG PROBLEM - cannot handle tasks that cant be properly scheduled!

public class Scheduler {

    private TaskManager tasks;

    public Scheduler(TaskManager taskManager) {
        this.tasks = taskManager;
    }

    public Scheduler() {
        this.tasks = new TaskManager();
    }

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
        LocalDateTime scheduleStart, LocalDateTime scheduleEnd) {
    return generateSchedule(fixedEvents, scheduleStart, scheduleEnd, null);
    }
    
    public List<Event> generateSchedule(List<Event> fixedEvents,
            LocalDateTime scheduleStart, LocalDateTime scheduleEnd, String filePath) {

        if (filePath != null) tasks.insertTaskList(filePath);
        tasks.sortByDueDate();
        // fixedEvents.add(new Event("Latest Night 1", LocalDateTime.MAX));
        // fixedEvents.add(new Event("Latest Night 2",
        // tasks.getTasks().get(0).getDueDate().plusDays(12)));

        // 1. Find all available time slots
        List<TimeSlot> freeSlots = findFreeTimeSlots(fixedEvents, scheduleStart, scheduleEnd);
        // freeSlots.add(new TimeSlot(fixedEvents.get(fixedEvents.size() -
        // 1).getEndTime(),
        // fixedEvents.get(fixedEvents.size() - 1).getEndTime().plusDays(10)));

        // 2. Prioritize tasks to schedule the most important ones first
        // CHANGE TO USE TASKMANAGER v
        tasks.sortByUrgency();
        /*
         * List<Task> sortedTasks = tasksToSchedule.stream()
         * .filter(t -> !t.isCompleted()) // Don't schedule completed tasks
         * .sorted(Comparator.comparing(Task::getPriorityScore).reversed())
         * .collect(Collectors.toList());
         */
        List<Event> scheduledTaskEvents = new ArrayList<>();
        Map<Task, Double> remainingTimes = new LinkedHashMap<>();
        // CHANGE TO USE TASKMANAGER ^

        // 3. Fit tasks into free slots
        long timeToWait = 250;
        System.out.println("Starting fitting");
        /*
         * try {
         * Thread.sleep(timeToWait);
         * } catch (InterruptedException Ex) {
         * Ex.printStackTrace();
         * }
         */

        tasks.procrastinate();
        while (tasks.getTasks().size() > 0) {
            tasks.sortByUrgency();
            System.out.println("Sorted: ");
            System.out.println(tasks);
            Task task = tasks.getTasks().get(0);

            System.out.println("got task: " + task.getName() + " " + task.getSession());
            /*
             * try {
             * Thread.sleep(timeToWait);
             * } catch (InterruptedException Ex) {
             * Ex.printStackTrace();
             * }
             */

            // Adjust estimated time for a more realistic duration using the procrastination
            // model
            // double remainingDuration =
            // ProcrastinationAlgorithm.getRealisticTimeInMinutes(task.getEstimatedTime());
            int remainingDuration = task.getEstimatedTime();
            List<Event> taskSessions = new ArrayList<>();

            // Find slots for the task, splitting if necessary

            System.out.println("Starting timeslots");
            /*
             * try {
             * Thread.sleep(timeToWait);
             * } catch (InterruptedException Ex) {
             * Ex.printStackTrace();
             * }
             */

            for (TimeSlot slot : freeSlots) {

                System.out.println("Started freeslots: " + slot + slot.getDurationInMinutes());
                /*
                 * try {
                 * Thread.sleep(timeToWait);
                 * } catch (InterruptedException Ex) {
                 * Ex.printStackTrace();
                 * }
                 */

                long slotDuration = slot.getDurationInMinutes();
                // Makes sure it accounts for used up free slots
                if (slotDuration <= 5) {

                    System.out.println("Continued");
                    /*
                     * try {
                     * Thread.sleep(timeToWait / 2);
                     * } catch (InterruptedException Ex) {
                     * Ex.printStackTrace();
                     * }
                     */

                    continue;

                }

                // Take as much time as possible from the current slot
                int timeToTake = (int) Math.min(slotDuration, remainingDuration);
                System.out.println("Time to take: " + timeToTake);

                LocalDateTime taskStart = slot.start;
                LocalDateTime taskEnd = taskStart.plusMinutes(timeToTake);
                System.out.println("New end: " + taskEnd);

                // Create a new Event to represent the scheduled task session

                System.out.println("Making Event: " + task.getName() + " " + task.getSession());
                /*
                 * try {
                 * Thread.sleep(timeToWait);
                 * } catch (InterruptedException Ex) {
                 * Ex.printStackTrace();
                 * }
                 */

                Event taskEvent = new Event(
                        task.getName() + " (Session " + task.getSession() + ")",
                        taskStart, // The 'Date' field in Event is a bit redundant, but we use start time
                        taskStart,
                        taskEnd,
                        task.getPriorityScore());
                taskEvent.setStatus("SCHEDULED_TASK");
                taskEvent.setDescription("Scheduled block for task: " + task.getName());
                if (task.getCategory() != null) {
                    taskEvent.setCategory(task.getCategory());
                }

                taskSessions.add(taskEvent);

                System.out.println("Removing Task: " + task.getName() + " " + task.getSession());
                /*
                 * try {
                 * Thread.sleep(timeToWait);
                 * } catch (InterruptedException Ex) {
                 * Ex.printStackTrace();
                 * }
                 */

                tasks.removeTask(task);

                // Update the free slot by moving its start time forward
                slot.start = taskEnd;
                System.out.println("New Timeslot Time:" + slot);
                remainingDuration -= timeToTake;
                System.out.println("Remaining Duration: " + remainingDuration);
                if (remainingDuration > 0.1) {
                    task.setSession(task.getSession() + 1);
                    task.setEstimatedTime(remainingDuration);
                    tasks.addTask(task);

                    System.out.println(
                            "Added Task: " + task.getName() + " " + task.getSession() + " " + task.getEstimatedTime());
                    /*
                     * try {
                     * Thread.sleep(timeToWait);
                     * } catch (InterruptedException Ex) {
                     * Ex.printStackTrace();
                     * }
                     */

                    tasks.sortByUrgency();
                    System.out.println("Sorted: ");
                    System.out.println(tasks);
                    break;

                } else
                    break;

            }

            // If the task was split into multiple sessions, label them
            // if (taskSessions.size() > 1) {
            // for (int i = 0; i < taskSessions.size(); i++) {
            // taskSessions.get(i).setName(task.getName() + " (Session " + (i + 1) + ")");
            // taskSessions.get(i).setSession(i);
            // task.setSession(i);
            // tasks.addTask(task);
            // }
            // }

            scheduledTaskEvents.addAll(taskSessions);

            // if (remainingDuration > 0.1) {
            // remainingTimes.put(task, remainingDuration);
            // }
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
            // If there's a gap between the current time and the start of the next event,
            // it's a free slot
            if (currentTime.isBefore(event.getStartTime())) {
                freeSlots.add(new TimeSlot(currentTime, event.getStartTime()));
            }
            // Move the current time to the end of the event, effectively handling overlaps
            if (event.getEndTime().isAfter(currentTime)) {
                currentTime = event.getEndTime();
            }
        }

        // Add a final 7 free slots
        if (currentTime.isBefore(windowEnd)) {
            freeSlots.add(new TimeSlot(currentTime, windowEnd));
        }
        for (int i = 70; i > 0; i--) {
            currentTime = windowEnd.plusHours(10);
            freeSlots.add(new TimeSlot(currentTime, currentTime.plusHours(14)));
        }

        return freeSlots;
    }
    /*
     * private static List<Task> loadTasksFromCSV(String filePath) {
     * List<Task> loadedTasks = new ArrayList<>();
     * try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
     * String line = br.readLine(); // Skip header
     * while ((line = br.readLine()) != null) {
     * String[] values = line.split(",", -1);
     * if (values.length >= 7) {
     * try {
     * loadedTasks.add(new Task(
     * values[0], // name
     * values[1], // category
     * LocalDateTime.parse(values[2]), // dueDate
     * Integer.parseInt(values[3]), // userPriority
     * Integer.parseInt(values[4]), // estimatedTime
     * Boolean.parseBoolean(values[5]), // completed
     * values[6] // description
     * ));
     * } catch (Exception e) {
     * System.err.println("Skipping invalid task row: " + line);
     * }
     * }
     * }
     * } catch (IOException e) {
     * System.err.println("Warning: Could not load tasks from " + filePath + " (" +
     * e.getMessage() + ")");
     * }
     * return loadedTasks;
     * }
     */

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
        List<Event> fixedEvents = loadEventsFromCSV("Main_Algorithm/events.csv");

        /*
         * if (fixedEvents.isEmpty() && tasks.isEmpty()) {
         * System.out.println(
         * "No data found in CSVs. Please make sure events.csv and tasks.csv exist in the running directory."
         * );
         * }
         */

        // --- 2. Define the scheduling window ---
        // The test data in the CSV spans from May 19 to May 25, 2024.
        // We test the schedule for a specific day from the dataset (e.g., Monday, May
        // 20)
        LocalDate testDate = LocalDate.of(2024, 5, 20);
        LocalDateTime scheduleStart = testDate.atTime(8, 0);
        LocalDateTime scheduleEnd = testDate.atTime(22, 0);

        // COPY THIS PART IN V
        // --- 3. Generate and print the schedule ---
        System.out.println("Generating Schedule for " + testDate + "...\n");
        List<Event> fullSchedule = scheduler.generateSchedule(fixedEvents, scheduleStart, scheduleEnd,
                "Main_Algorithm/tasks.csv");

        System.out.println("--- Final Daily Schedule ---");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

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

                // Find the original task to get the user's estimate
                /*
                 * Task originalTask = tasks.stream()
                 * .filter(t -> e.getName().equals(t.getName())
                 * || e.getName().startsWith(t.getName() + " (Session"))
                 * .findFirst().orElse(null);
                 * if (originalTask != null) {
                 * long actualScheduledMins = Duration.between(e.getStartTime(),
                 * e.getEndTime()).toMinutes();
                 * extraInfo = String.format(" (User Est: %d mins | Alg Calculated: %d mins)",
                 * originalTask.getEstimatedTime(), actualScheduledMins);
                 * }
                 */
            }

            System.out.printf("[%s] %s to %s - %s %s%s\n",
                    type, e.getStartTime().format(timeFormatter), e.getEndTime().format(timeFormatter), e.getName(),
                    e.getPriorityScore(),
                    extraInfo);
        }
        // COPY THIS PART IN ^
    }
}