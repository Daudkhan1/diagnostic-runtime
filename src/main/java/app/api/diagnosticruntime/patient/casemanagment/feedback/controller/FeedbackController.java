package app.api.diagnosticruntime.patient.casemanagment.feedback.controller;

import app.api.diagnosticruntime.patient.casemanagment.feedback.dto.FeedbackDTO;
import app.api.diagnosticruntime.patient.casemanagment.feedback.dto.FeedbackUpdateDTO;
import app.api.diagnosticruntime.patient.casemanagment.feedback.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @GetMapping
    public ResponseEntity<List<FeedbackDTO>> getAllFeedbacks() {
        List<FeedbackDTO> feedbacks = feedbackService.getAllFeedbacks();
        return ResponseEntity.ok(feedbacks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeedbackDTO> getFeedbackById(@PathVariable String id) {
        FeedbackDTO feedback = feedbackService.getFeedbackById(id);
        return ResponseEntity.ok(feedback);
    }

    @GetMapping("/user/{userId}/case/{caseId}")
    public ResponseEntity<List<FeedbackDTO>> getFeedbacksByUserIdAndCaseId(@PathVariable String userId, @PathVariable String caseId) {
        List<FeedbackDTO> feedbacks = feedbackService.getFeedbacksByUserIdAndCaseId(userId, caseId);
        return ResponseEntity.ok(feedbacks);
    }

    @GetMapping("/case/{caseId}")
    public ResponseEntity<List<FeedbackDTO>> getFeedbacksByCaseId(@PathVariable String caseId) {
        List<FeedbackDTO> feedbacks = feedbackService.getFeedbacksByCaseId(caseId);
        return ResponseEntity.ok(feedbacks);
    }

    @PostMapping
    public ResponseEntity<FeedbackDTO> createFeedback(@RequestBody FeedbackDTO feedbackDTO) {
        FeedbackDTO createdFeedback = feedbackService.createFeedback(feedbackDTO);
        return ResponseEntity.ok(createdFeedback);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FeedbackDTO> updateFeedback(@PathVariable String id, @RequestBody FeedbackUpdateDTO feedbackUpdateDTO) {
        FeedbackDTO updatedFeedback = feedbackService.updateFeedback(id, feedbackUpdateDTO);
        return ResponseEntity.ok(updatedFeedback);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeedback(@PathVariable String id) {
        feedbackService.deleteFeedback(id);
        return ResponseEntity.noContent().build();
    }
}