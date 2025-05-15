package app.api.diagnosticruntime.patient.casemanagment.history.repository;

import app.api.diagnosticruntime.patient.casemanagment.history.dto.StatusCount;
import app.api.diagnosticruntime.patient.casemanagment.history.model.History;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseStatus;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HistoryRepository extends MongoRepository<History, String> {
    Page<History> findByCaseIdOrderByCreatedAtDesc(String caseId, Pageable pageable);
    Optional<History> findFirstByCaseIdOrderByCreatedAtDesc(String caseId);

    Optional<History> findFirstByCaseIdAndNewStatusOrderByCreatedAtDesc(String caseId, CaseStatus newStatus);

    History findTopByCaseIdOrderByCreatedAtDesc(String caseId);

    @Aggregation(pipeline = {
            // Start with cases collection and match by type
            "{ $match: { " +
                    "case_type: ?0, " +
                    "is_deleted: false " +
                    "} }",
            // Lookup history records for these cases
            "{ $lookup: { " +
                    "from: 'case_history', " +
                    "localField: '_id', " +
                    "foreignField: 'case_id', " +
                    "as: 'history' " +
                    "} }",
            // Unwind history array
            "{ $unwind: '$history' }",
            // Sort by created_at to get latest entries
            "{ $sort: { 'history.created_at': -1 } }",
            // Group by case to get latest status
            "{ $group: { " +
                    "_id: '$_id', " +
                    "latestStatus: { $first: '$history.new_status' }, " +
                    "latestActionBy: { $first: '$history.action_by' }, " +
                    "latestTransferredTo: { $first: '$history.transferred_to' } " +
                    "} }",
            // Match cases involving the user
            "{ $match: { " +
                    "$or: [ " +
                    "{ latestActionBy: ?1 }, " +
                    "{ latestTransferredTo: ?1 } " +
                    "] " +
                    "} }",
            // Group by status and user involvement
            "{ $group: { " +
                    "_id: { " +
                    "status: '$latestStatus', " +
                    "actionBy: '$latestActionBy', " +
                    "transferredTo: '$latestTransferredTo' " +
                    "}, " +
                    "count: { $sum: 1 } " +
                    "} }",
            // Final projection
            "{ $project: { " +
                    "status: '$_id.status', " +
                    "actionBy: '$_id.actionBy', " +
                    "transferredTo: '$_id.transferredTo', " +
                    "count: 1, " +
                    "_id: 0 " +
                    "} }"
    })
    List<StatusCount> getCaseCountsByUserAndStatusAndType(CaseType caseType, String userId);
}
