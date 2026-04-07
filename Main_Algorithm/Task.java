package Main_Algorithm;
import java.time.LocalDateTime;

public class Task {
    private String name; // Task name
    private String category; // Category for the task
    private LocalDateTime dueDate; // Task due date and time
    private int userPriority; //
    private int estimatedTime; // Estimated time to complete in minutes
    private boolean completed; // Whether the task is completed
    private String description; // Optional description of the task
    private int minutesSpent; // Time spent on the task in minutes

    // Constructor for basic task with name and due date
    public Task(String name, LocalDateTime dueDate) {
        this.name = name;
        this.dueDate = dueDate;
    }

    // Constructor for task with all attributes
    public Task(String name, LocalDateTime dueDate, int userPriority, int estimatedTime, boolean completed) {
        this.name = name;
        this.dueDate = dueDate;
        this.userPriority = userPriority;
        this.estimatedTime = estimatedTime;
        this.completed = completed;
    }

    // Constructor for task with all attributes including description
    public Task(String name, String category, LocalDateTime dueDate, int userPriority, int estimatedTime,
            boolean completed, String description) {
        this.name = name;
        this.category = category;
        this.dueDate = dueDate;
        this.userPriority = userPriority;
        this.estimatedTime = estimatedTime;
        this.completed = completed;
        this.description = description;
    }

    // Getters and setters for task attributes
    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public int getUserPriority() {
        return userPriority;
    }

    public int getEstimatedTime() {
        return estimatedTime;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getDescription() {
        return description;
    }

    // Returns the number of minutes until the task is due
    public long getMinutesTillDue() {
        LocalDateTime now = LocalDateTime.now();
        return java.time.Duration.between(now, dueDate).toMinutes();
    }

    // Returns the number of hours until the task is due
    public long getHoursUntilDue() {
        LocalDateTime now = LocalDateTime.now();
        return java.time.Duration.between(now, dueDate).toHours();
    }

    // Calculates a priority score based on urgency, user priority, and time until
    // due date
    public double getTimePressure() {
        long hours = getHoursUntilDue();
        if (hours <= 0) {
            return 10;
        } else {
            return 10.0 / (hours + 1);
        }
    }

    public double getPriorityScore() {
        long hoursUntilDue = getHoursUntilDue();
        if (hoursUntilDue <= 0) {
            return Double.POSITIVE_INFINITY; // Overdue tasks have highest priority
        }

        return (userPriority + 2 * getTimePressure()); // Higher user priority and closer due date increases priority
    }

    // Marks the task as completed
    public void markAsCompleted() {
        this.completed = true;
    }

    // progress tracking
    public void addTimeSpent(int minutes) {
        this.minutesSpent += minutes;
    }

    public int getMinutesRemaining() {
        return Math.max(0, estimatedTime - minutesSpent);
    }

    public void updateUserPriority(int userPriority) {
        this.userPriority = Math.max(1, Math.min(10, userPriority)); // Ensure user priority is between 1 and 10
    }

    public void updateDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public void updateEstimatedTime(int estimatedTime) {
        this.estimatedTime = Math.max(0, estimatedTime); // Ensure estimated time is non-negative
    }

    // overdo
    public boolean isOverdue() {
        return LocalDateTime.now().isAfter(dueDate);
    }

}