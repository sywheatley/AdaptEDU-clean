import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class AppController {

    TaskManager manager = new TaskManager();

    @GetMapping("/tasks")
    public List<Task> getTasks() {
        return manager.getTasks();
    }

    @PostMapping("/tasks")
    public void addTask(@RequestBody Task task) {
        manager.addTask(task);
    }

    @GetMapping("/events")
    public List<Event> getEvents() {
        return manager.getEvents();
    }

    @PostMapping("/events")
    public void addEvent(@RequestBody Event event) {
        manager.addEvent(event);
    }
}