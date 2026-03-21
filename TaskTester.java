import java.time.LocalDateTime;
// order: String name, String category, LocalDateTime dueDate, int urgency, int userPriority, int estimatedTime, boolean completed, String description

public class TaskTester {

    public static void main(String[] args) {
        TaskManager manager = new TaskManager();

        // Create some tasks
        Task task1 = new Task("Finish report", LocalDateTime.now().plusDays(2), 5, 3, 120, false);
        Task task2 = new Task("Buy groceries", LocalDateTime.now().plusHours(5), 3, 4, 30, false);
        Task task3 = new Task("Call mom", LocalDateTime.now().plusHours(1), 4, 5, 15, false);

        // Add tasks to manager
        manager.addTask(task1);
        manager.addTask(task2);
        manager.addTask(task3);

        // Print tasks by urgency
        System.out.println("Tasks by Urgency:");
        manager.printTasksByUrgency();

        // Print tasks by due date
        System.out.println("\nTasks by Due Date:");
        manager.printTasksByDueDate();

        // Get most urgent task
        Task mostUrgent = manager.getMostUrgentTask();
        System.out.println("\nMost Urgent Task: " + mostUrgent.getName());

        // 
    }


}
