package com.example.controller;

import org.springframework.web.bind.annotation.*;
import procrastination_alg.main_alg;

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
}

@RestController
@RequestMapping("/api")
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
}

