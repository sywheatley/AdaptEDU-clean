import java.util.ArrayList;
import java.util.List;

public class TaskManager {

    private ArrayList<Task> tasks; // List of tasks
    private ArrayList<Event> events; // List of events

    public TaskManager() {
        tasks = new ArrayList<>();
        events = new ArrayList<>();
    }

    // Task methods
    public void addTask(Task task) {
        tasks.add(task);
    }

    public void removeTask(Task task) {
        tasks.remove(task);
    }

    public List<Task> getTasks() {
        return tasks;
    }

    // Event methods
    public void addEvent(Event event) {
        events.add(event);
    }

    public void removeEvent(Event event) {
        events.remove(event);
    }

    public List<Event> getEvents() {
        return events;
    }

    // Additional utility methods
    public void printTasksByUrgency() {
        tasks.sort((t1, t2) -> Integer.compare(t2.getUrgency(), t1.getUrgency()));
        for (Task task : tasks) {
            System.out.println(task.getName() + " - Urgency: " + task.getUrgency());
        }
    }

    public void printTasksByDueDate() {
        tasks.sort((t1, t2) -> t1.getDueDate().compareTo(t2.getDueDate()));
        for (Task task : tasks) {
            System.out.println(task.getName() + " - Due: " + task.getDueDate());
        }
    }

    public Task getMostUrgentTask() {
        if (tasks.isEmpty()) return null;
        Task urgent = tasks.get(0);
        for (Task task : tasks) {
            if (task.getUrgency() > urgent.getUrgency()) {
                urgent = task;
            }
        }
        return urgent;
    }



}