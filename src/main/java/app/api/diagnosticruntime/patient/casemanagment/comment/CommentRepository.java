package app.api.diagnosticruntime.patient.casemanagment.comment;

import app.api.diagnosticruntime.patient.casemanagment.comment.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends MongoRepository<Comment, String> {
    Optional<Comment> findByIdAndIsDeleted(String s, boolean deleted);

    List<Comment> findAllByCaseIdAndIsDeleted(String caseId, boolean deleted);


    @Query("{'case_id' : ?0}")
    @Update("{ '$set': { 'isDeleted': true } }")
    void markCommentsAsDeletedByCaseId(@Param("caseId") String caseId);
}
