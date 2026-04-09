package com.example.controller;

import org.springframework.web.bind.annotation.*;
import procrastination_alg.main_alg;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

// DTO for Task (for API communication)
class TaskDTO {
    public String name;
    public String category;
    public String dueDate; // ISO string
    public int userPriority;
    public int estimatedTime;
    public boolean completed;
    public String description;
    public int minutesSpent;
    public boolean archived;
    public Long archivedAt;
}

class EventDTO {
    public String name;
    public String startTime;
    public String endTime;
    public String location;
    public String status;
    public String category;
    public boolean archived;
    public Long archivedAt;
}

class CsvSyncRequest {
    public List<TaskDTO> tasks;
    public List<EventDTO> events;
}

@RestController
@RequestMapping("/api")
@CrossOrigin(allowedOriginPatterns = "*")
public class AdaptEDUController {
    @GetMapping("/courses")
    public String getCourses() {
        return "List of courses";
    }

    @GetMapping("/students")
    public String getStudents() {
        return "List of students";
    }

    @PostMapping("/enroll")
    public String enrollStudent(@RequestBody String studentId) {
        return "Student " + studentId + " enrolled successfully";
    }

    @PostMapping("/submit-assignment")
    public String submitAssignment(@RequestBody String assignmentData) {
        return "Assignment submitted successfully";
    }

    @PostMapping("/task-time-adjust")
    public TaskDTO adjustTaskTime(@RequestBody TaskDTO task) {
        // Adjust estimated time using the main algorithm
        double adjusted = main_alg.getRealisticTimeInMinutes(task.estimatedTime);
        task.estimatedTime = (int) Math.round(adjusted);
        return task;
    }

    @PostMapping("/state/save-csv")
    public Map<String, Object> saveStateCsv(@RequestBody CsvSyncRequest request) throws IOException {
        List<TaskDTO> tasks = request.tasks == null ? List.of() : request.tasks;
        List<EventDTO> events = request.events == null ? List.of() : request.events;

        writeTasksCsv(tasks, resolveResourcePath("tasks.csv"));
        writeEventsCsv(events, resolveResourcePath("events.csv"));

        return Map.of(
                "status", "ok",
                "tasksSaved", tasks.size(),
                "eventsSaved", events.size()
        );
    }

    private static Path resolveResourcePath(String fileName) {
        Path inModule = Paths.get("src", "main", "resources", fileName);
        if (Files.exists(inModule.getParent())) return inModule;

        Path fromRepoRoot = Paths.get("SpringBootTest", "src", "main", "resources", fileName);
        if (Files.exists(fromRepoRoot.getParent())) return fromRepoRoot;

        return inModule;
    }

    private static void writeTasksCsv(List<TaskDTO> tasks, Path path) throws IOException {
        StringBuilder out = new StringBuilder();
        out.append("name,category,dueDate,userPriority,estimatedTime,completed,description,minutesSpent,archived,archivedAt\n");
        for (TaskDTO task : tasks) {
            out.append(csv(task.name)).append(',')
                    .append(csv(task.category)).append(',')
                    .append(csv(task.dueDate)).append(',')
                    .append(task.userPriority).append(',')
                    .append(task.estimatedTime).append(',')
                    .append(task.completed).append(',')
                    .append(csv(task.description)).append(',')
                    .append(task.minutesSpent).append(',')
                    .append(task.archived).append(',')
                    .append(task.archivedAt == null ? "" : task.archivedAt)
                    .append('\n');
        }
        Files.writeString(path, out.toString(), StandardCharsets.UTF_8);
    }

    private static void writeEventsCsv(List<EventDTO> events, Path path) throws IOException {
        StringBuilder out = new StringBuilder();
        out.append("name,startTime,endTime,location,status,category,archived,archivedAt\n");
        for (EventDTO event : events) {
            out.append(csv(event.name)).append(',')
                    .append(csv(event.startTime)).append(',')
                    .append(csv(event.endTime)).append(',')
                    .append(csv(event.location)).append(',')
                    .append(csv(event.status)).append(',')
                    .append(csv(event.category)).append(',')
                    .append(event.archived).append(',')
                    .append(event.archivedAt == null ? "" : event.archivedAt)
                    .append('\n');
        }
        Files.writeString(path, out.toString(), StandardCharsets.UTF_8);
    }

    private static String csv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}

