package com.example.controller;

import org.springframework.web.bind.annotation.*;

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
}
