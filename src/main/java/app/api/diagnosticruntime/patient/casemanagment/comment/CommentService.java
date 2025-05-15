package app.api.diagnosticruntime.patient.casemanagment.comment;

import app.api.diagnosticruntime.userdetails.dto.UserInfoDTO;
import app.api.diagnosticruntime.userdetails.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static app.api.diagnosticruntime.patient.casemanagment.comment.CommentMapper.toCommentDTO;
@Transactional
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserService userService; // Repository to fetch user details
    private final MongoTemplate mongoTemplate;

    public List<CommentDTO> getAllComments(String caseId, String user) {
        // Create the base query
        Query query = new Query();

        // Add filters if provided
        if (caseId != null && !caseId.isEmpty()) {
            query.addCriteria(Criteria.where("case_id").is(caseId));
        }
        if (user != null && !user.isEmpty()) {
            query.addCriteria(Criteria.where("creation_user").is(user));
        }
        query.addCriteria(Criteria.where("is_deleted").is(false));
        // Execute the query
        List<Comment> comments = mongoTemplate.find(query, Comment.class);

        // Map comments to CommentDTOs and include the creationUserFullName
        return comments.stream()
                .map(comment -> {
                    String creationUserFullName = null;
                    if (comment.getCreationUser() != null) {
                        UserInfoDTO userEntity = userService.getUserByEmail(comment.getCreationUser()).orElse(null);
                        creationUserFullName = userEntity != null ? userEntity.getFullName() : null;
                    }

                    CommentDTO dto = CommentMapper.toCommentDTO(comment);
                    dto.setCreationUserFullName(creationUserFullName);
                    return dto;
                })
                .toList();
    }

    public CommentDTO addComment(CommentModifyDTO comment, String userEmail) {
        // Create a new Comment object
        Comment commentToUpload = new Comment();
        commentToUpload.setCaseId(comment.getCaseId());
        commentToUpload.setCommentText(comment.getCommentText());
        commentToUpload.setCreationUser(userEmail);
        commentToUpload.setCreationDate(LocalDate.now());
        commentToUpload.setDeleted(false);
        UserInfoDTO userEntity = userService.getUserByEmail(userEmail).orElse(null);
        String creationUserFullName = userEntity != null ? userEntity.getFullName() : null;
        // Save the comment
        Comment uploadedComment = commentRepository.save(commentToUpload);
        CommentDTO commentCreated = toCommentDTO(uploadedComment);
        commentCreated.setCreationUserFullName(creationUserFullName);
        return commentCreated;
    }

    public CommentDTO updateComment(String id, CommentModifyDTO comment, String userEmail) {
        // Find the existing comment
        Optional<Comment> existingComment = commentRepository.findByIdAndIsDeleted(id, false);
        if (existingComment.isEmpty()) {
            throw new IllegalArgumentException("Comment not found");
        }
        Comment commentToModify = existingComment.get();
        // Update the comment details
        commentToModify.setCommentText(comment.getCommentText());
        commentToModify.setModificationUser(userEmail);
        commentToModify.setModificationDate(LocalDate.now());
        Comment updatedComment = commentRepository.save(commentToModify);
        // Save the updated comment
        return toCommentDTO(updatedComment);
    }

    public List<Comment> getAllCommentsByCaseId(String caseId) {
        return commentRepository.findAllByCaseIdAndIsDeleted(caseId, false);
    }

    public void deletedCommentsByCaseId(String caseId) {
        commentRepository.markCommentsAsDeletedByCaseId(caseId);
    }
}
