import java.util.ArrayList;

public class TaskManager {

    private ArrayList<Task> tasks; // List of tasks

    public TaskManager() {
        tasks = new ArrayList<>();
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public void removeTask(Task task) {
        tasks.remove(task);
    }

    public void printTasksByUrgency() {
        tasks.sort((t1, t2) -> Double.compare(t2.getPriorityScore(), t1.getPriorityScore()));
        for (Task task : tasks) {
            System.out.println(task.getName() + " - Urgency: " + task.getPriorityScore());
        }
    }

    public void printTasksByDueDate() {
        tasks.sort((t1, t2) -> t1.getDueDate().compareTo(t2.getDueDate()));
        for (Task task : tasks) {
            System.out.println(task.getName() + " - Due: " + task.getDueDate());
        }
    }

    public Task getMostUrgentTask() {
        Task urgent = tasks.get(0);
        for (Task task : tasks) {
            if (task.getPriorityScore() > urgent.getPriorityScore()) {
                urgent = task;
            }
        }
        return urgent;
    }

    public void sortByUrgency() {
        tasks.sort((t1, t2) -> Double.compare(t2.getPriorityScore(), t1.getPriorityScore()));
    }

    public void printTasks() {
        for (Task task : tasks) {
            System.out.println(task.getName() + " - Due: " + task.getDueDate());
        }
    }

}