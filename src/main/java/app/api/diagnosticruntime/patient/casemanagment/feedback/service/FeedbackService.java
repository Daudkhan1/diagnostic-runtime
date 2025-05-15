package app.api.diagnosticruntime.patient.casemanagment.feedback.service;

import app.api.diagnosticruntime.patient.casemanagment.feedback.dto.FeedbackDTO;
import app.api.diagnosticruntime.patient.casemanagment.feedback.dto.FeedbackUpdateDTO;
import app.api.diagnosticruntime.patient.casemanagment.feedback.mapper.FeedbackMapper;
import app.api.diagnosticruntime.patient.casemanagment.feedback.model.Feedback;
import app.api.diagnosticruntime.patient.casemanagment.feedback.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Transactional
    public boolean hasFeedback(String userId, String caseId) {
        return feedbackRepository.findByUserIdAndCaseIdAndIsDeleted(userId, caseId, false).size() > 0;
    }

    @Transactional(readOnly = true)
    public List<FeedbackDTO> getAllFeedbacks() {
        List<Feedback> feedbacks = feedbackRepository.findByIsDeleted(false);
        return feedbacks.stream()
                .map(FeedbackMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FeedbackDTO getFeedbackById(String id) {
        Feedback feedback = feedbackRepository.findById(id).orElseThrow(() -> new RuntimeException("Feedback not found"));
        return FeedbackMapper.toDTO(feedback);
    }

    @Transactional(readOnly = true)
    public List<FeedbackDTO> getFeedbacksByUserIdAndCaseId(String userId, String caseId) {
        List<Feedback> feedbacks = feedbackRepository.findByUserIdAndCaseIdAndIsDeleted(userId, caseId, false);
        return feedbacks.stream()
                .map(FeedbackMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FeedbackDTO> getFeedbacksByCaseId(String caseId) {
        List<Feedback> feedbacks = feedbackRepository.findByCaseIdAndIsDeleted(caseId, false);
        return feedbacks.stream()
                .map(FeedbackMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public FeedbackDTO createFeedback(FeedbackDTO feedbackDTO) {
        Feedback feedback = FeedbackMapper.toEntity(feedbackDTO);
        feedback.setDeleted(false);
        Feedback savedFeedback = feedbackRepository.save(feedback);
        return FeedbackMapper.toDTO(savedFeedback);
    }

    @Transactional
    public FeedbackDTO updateFeedback(String id, FeedbackUpdateDTO feedbackUpdateDTO) {
        Feedback existingFeedback = feedbackRepository.findById(id).orElseThrow(() -> new RuntimeException("Feedback not found"));
        existingFeedback.setCaseId(feedbackUpdateDTO.getCaseId());
        existingFeedback.setDifficultyLevel(feedbackUpdateDTO.getDifficultyLevel());
        existingFeedback.setFeedback(feedbackUpdateDTO.getFeedback());
        Feedback updatedFeedback = feedbackRepository.save(existingFeedback);
        return FeedbackMapper.toDTO(updatedFeedback);
    }

    @Transactional
    public void deleteFeedback(String id) {
        Feedback feedback = feedbackRepository.findById(id).orElseThrow(() -> new RuntimeException("Feedback not found"));
        feedback.setDeleted(true);
        feedbackRepository.save(feedback);
    }
}