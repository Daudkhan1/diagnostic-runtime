package app.api.diagnosticruntime.patient.casemanagment.history.service;

import app.api.diagnosticruntime.annotation.model.Annotation;
import app.api.diagnosticruntime.annotation.service.AnnotationService;
import app.api.diagnosticruntime.patient.casemanagment.comment.CommentService;
import app.api.diagnosticruntime.patient.casemanagment.diagnosis.service.DiagnosisService;
import app.api.diagnosticruntime.patient.casemanagment.dto.CaseInfoDTO;
import app.api.diagnosticruntime.patient.casemanagment.feedback.service.FeedbackService;
import app.api.diagnosticruntime.patient.casemanagment.history.dto.StatusCount;
import app.api.diagnosticruntime.patient.casemanagment.history.dto.TransferDetailsDTO;
import app.api.diagnosticruntime.patient.casemanagment.history.dto.TransferUserDetailsDTO;
import app.api.diagnosticruntime.patient.casemanagment.history.model.History;
import app.api.diagnosticruntime.patient.casemanagment.history.repository.HistoryRepository;
import app.api.diagnosticruntime.patient.casemanagment.model.Case;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseStatus;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import app.api.diagnosticruntime.patient.model.PatientSlide;
import app.api.diagnosticruntime.patient.casemanagment.service.CaseService;
import app.api.diagnosticruntime.patient.model.PatientSlideStatus;
import app.api.diagnosticruntime.patient.service.PatientSlideService;
import app.api.diagnosticruntime.userdetails.dto.UserInfoDTO;
import app.api.diagnosticruntime.userdetails.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static app.api.diagnosticruntime.patient.casemanagment.history.mapper.HistoryMapper.fromHistory;

@Service
@RequiredArgsConstructor
public class HistoryService {
    private final HistoryRepository historyRepository;
    private final CaseService caseService;
    private final MongoTemplate mongoTemplate;
    private final UserService userService;
    private final PatientSlideService patientSlideService;
    private final AnnotationService annotationService;
    private final DiagnosisService diagnosisService;
    private final FeedbackService feedbackService;
    private final CommentService commentService;

    @Transactional
    public History recordStateTransition(
            String caseId,
            CaseStatus previousStatus,
            CaseStatus newStatus,
            String actionByPathologistId,
            String transferredPathologistId,
            String note
    ) {
        History history = new History();
        history.setCaseId(caseId);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setActionByPathologistId(actionByPathologistId);
        history.setTransferredToPathologistId(transferredPathologistId);
        history.setNote(note);

        return historyRepository.save(history);
    }

    public boolean hasPermissionToAnnotate(String caseId, String username) {
        History latestHistory = historyRepository.findTopByCaseIdOrderByCreatedAtDesc(caseId);
        if (latestHistory == null) {
            return false;
        }

        String userId = userService.getUserIdByUsername(username);

        boolean isUserInvolved = userId.equals(latestHistory.getActionByPathologistId()) || userId.equals(latestHistory.getTransferredToPathologistId());
        boolean isCaseInCorrectState = latestHistory.getNewStatus() == CaseStatus.IN_PROGRESS || latestHistory.getNewStatus() == CaseStatus.REFERRED;

        return isUserInvolved && isCaseInCorrectState;
    }

    @Transactional(readOnly = true)
    public Page<History> getCaseHistory(String caseId, Pageable pageable) {
        return historyRepository.findByCaseIdOrderByCreatedAtDesc(caseId, pageable);
    }

    @Transactional(readOnly = true)
    public History getLatestHistoryForCase(String caseId) {
        return historyRepository.findFirstByCaseIdOrderByCreatedAtDesc(caseId)
            .orElse(null);
    }

    public TransferDetailsDTO getLatestTransferHistoryForCaseDisplay(String caseId) {
        Optional<History> history = historyRepository.findFirstByCaseIdAndNewStatusOrderByCreatedAtDesc(caseId, CaseStatus.REFERRED);
        if(history.isEmpty())
            return new TransferDetailsDTO();


        TransferDetailsDTO transferDetailsDTO = new TransferDetailsDTO();
        if(!StringUtils.isBlank(history.get().getActionByPathologistId()) && !history.get().getActionByPathologistId().equals("undefined")){
            TransferUserDetailsDTO transferredBy = getTwoEyesUserDetails(history.get().getActionByPathologistId(), history.get());
            transferDetailsDTO.setTransferredBy(transferredBy);
        }

        if(!StringUtils.isBlank(history.get().getTransferredToPathologistId()) && !history.get().getTransferredToPathologistId().equals("undefined")){
            TransferUserDetailsDTO transferredTo = getTwoEyesUserDetails(history.get().getTransferredToPathologistId(), history.get());
            transferDetailsDTO.setTransferredTo(transferredTo);
        }

        return transferDetailsDTO;
    }

    public TransferDetailsDTO getLatestTransferHistory(String caseId) {
        Optional<History> history = historyRepository.findFirstByCaseIdAndNewStatusOrderByCreatedAtDesc(caseId, CaseStatus.REFERRED);
        if(history.isEmpty())
            throw new IllegalArgumentException("This case has not been transferred to anyone");

        String userIdOfTransferee = history.get().getTransferredToPathologistId();
        Optional<UserInfoDTO> tranferee = userService.getUserById(userIdOfTransferee);
        if(tranferee.isEmpty())
            throw new IllegalArgumentException("This user of id: " + userIdOfTransferee + " doesn't exist");

        TransferDetailsDTO transferDetailsDTO = new TransferDetailsDTO();
        if(!StringUtils.isBlank(history.get().getActionByPathologistId())){
            TransferUserDetailsDTO transferredBy = getTwoEyesUserDetails(history.get().getActionByPathologistId(), history.get());
            transferDetailsDTO.setTransferredBy(transferredBy);
        }

        if(!StringUtils.isBlank(history.get().getTransferredToPathologistId())){
            TransferUserDetailsDTO transferredTo = getTwoEyesUserDetails(history.get().getTransferredToPathologistId(), history.get());
            transferDetailsDTO.setTransferredTo(transferredTo);
        }

        return transferDetailsDTO;
    }

    private TransferUserDetailsDTO getTwoEyesUserDetails(String userId, History history){
        Optional<UserInfoDTO> userInfo = userService.getUserById(userId);
        if(userInfo.isEmpty())
            throw new IllegalArgumentException("This user of id: " + userInfo + " doesn't exist");
        return fromHistory(history, userInfo.get());
    }

    @Transactional
    public void assignCase(String caseId, UserDetails userDetails) {

        History history = getLatestHistoryForCase(caseId);
        String currentUserId = userService.getUserIdByUsername(userDetails.getUsername());
        StringBuilder note = new StringBuilder();
        if(history == null)
            throw new IllegalArgumentException("Assigning a case with no previous record");
        else if(history.getNewStatus().equals(CaseStatus.NEW)) {

            note.append("User with id:").append(currentUserId).append(" and email:").append(userDetails.getUsername())
                    .append(" assigned the case to themselves");

        } else if(history.getNewStatus().equals(CaseStatus.COMPLETE)) {
            note.append("User with id:").append(currentUserId).append(" and email:").append(userDetails.getUsername())
                    .append(" assigned the case to themselves after removing it from state: ").append(CaseStatus.COMPLETE.toString());
        } else {
            throw new IllegalArgumentException("Cannot assign case if Previous case type isn't NEW or COMPLETE");
        }

        recordStateTransition(
                caseId,
                CaseStatus.NEW,
                CaseStatus.IN_PROGRESS,
                currentUserId,
                null,
                note.toString()
        );
        caseService.updateCaseStatus(caseId, CaseStatus.IN_PROGRESS.toString());

    }

    public void createNewCase(String caseId) {
        History history = getLatestHistoryForCase(caseId);
        StringBuilder note = new StringBuilder();
        if(history == null) {
            note.append("A new case has been created");
            recordStateTransition(
                    caseId,
                    null,
                    CaseStatus.NEW,
                    null,
                    null,
                    note.toString()
            );
        } else {
            throw new IllegalArgumentException("A new Case cannot have a previous history");
        }

    }

    @Transactional
    public void unassignCase(String caseId, UserDetails userDetails, boolean deleteAnnotationsAndComments) {
        History history = getLatestHistoryForCase(caseId);
        StringBuilder note = new StringBuilder();

        if(history == null) {
            throw new IllegalArgumentException("Case must have a previous state for it to be un-assigned");
        }

        else if(history.getNewStatus().equals(CaseStatus.IN_PROGRESS)) {
            String currentUserId = userService.getUserIdByUsername(userDetails.getUsername());
            note.append("User with id:").append(currentUserId).append(" and email:").append(userDetails.getUsername())
                    .append(" Un-assigned the case from themselves after removing it from state: ").append(CaseStatus.COMPLETE);

            if(deleteAnnotationsAndComments){
                patientSlideService.getAllByCaseId(caseId)
                        .forEach(p -> annotationService.deleteAllAnnotationsForSlide(p.getId(), userDetails.getUsername()));
                commentService.deletedCommentsByCaseId(caseId);
            }

            recordStateTransition(
                    caseId,
                    CaseStatus.IN_PROGRESS,
                    CaseStatus.NEW,
                    currentUserId,
                    null,
                    note.toString()
            );
        } else {
            throw new IllegalArgumentException("Cannot un-assign or create case, this state change from "+ history.getNewStatus()+" to IN_PROGRESS is not allowed");
        }


        caseService.updateCaseStatus(caseId, CaseStatus.NEW.toString());
        patientSlideService.updateStatusOfAllCaseSlides(caseId, PatientSlideStatus.NEW);
    }

    @Transactional
    public void transferCase(String caseId, UserDetails userDetails, String targetPathologistId) {
        History history = getLatestHistoryForCase(caseId);
        StringBuilder note = new StringBuilder();
        String currentUserId = userService.getUserIdByUsername(userDetails.getUsername());

        if(targetPathologistId.equals(currentUserId))
            throw new IllegalArgumentException("User cannot transfer the case to themselves");

        if(history == null) {
            throw new IllegalArgumentException("Case must have a previous state for it to be transferable");
        } else if(history.getNewStatus().equals(CaseStatus.IN_PROGRESS)){

            if(!patientSlideService.checkIfAllSlidesOfCaseAreComplete(caseId))
                throw new IllegalArgumentException("Case cannot be referred until all slides are completed");

            //check if the user has set a diagnosis for this case
            if (!diagnosisService.hasDiagnosis(currentUserId, caseId)) {
                throw new IllegalArgumentException("User must set a diagnosis for the case before completing it");
            }

            //check if the user has set the case difficulty
            if (!feedbackService.hasFeedback(currentUserId, caseId)) {
                throw new IllegalArgumentException("User must set the case difficulty before completing it");
            }

            patientSlideService.updateStatusOfAllCaseSlides(caseId, PatientSlideStatus.IN_PROGRESS);

            note.append("User with id:").append(currentUserId).append(" and email:").append(userDetails.getUsername())
                    .append(" transferred case to user with Id: ").append(targetPathologistId);
            recordStateTransition(
                    caseId,
                    CaseStatus.IN_PROGRESS,
                    CaseStatus.REFERRED,
                    currentUserId,
                    targetPathologistId,
                    note.toString()
            );
        }else {
            throw new IllegalArgumentException("Cannot transfer case, previous state must be IN_PROGRESS");
        }


        caseService.updateCaseStatus(caseId, CaseStatus.REFERRED.toString());
    }

    @Transactional
    public void completeCase(String caseId, UserDetails userDetails) {
        String currentUserId = userService.getUserIdByUsername(userDetails.getUsername());
        History history = getLatestHistoryForCase(caseId);
        StringBuilder note = new StringBuilder();

        if(history == null)
            throw new IllegalArgumentException("Case must have a previous state for it to be transferable");
        else if(history.getNewStatus().equals(CaseStatus.REFERRED)) {

            // Check if the user has set a diagnosis for this case
            if (!diagnosisService.hasDiagnosis(currentUserId, caseId)) {
                throw new IllegalArgumentException("User must set a diagnosis for the case before completing it");
            }

            // Check if the user has set the case difficulty
            if (!feedbackService.hasFeedback(currentUserId, caseId)) {
                throw new IllegalArgumentException("User must set the case difficulty before completing it");
            }

            note.append("User with id:").append(currentUserId).append(" and email:").append(userDetails.getUsername())
                    .append(" has completed the case");
            recordStateTransition(
                    caseId,
                    CaseStatus.REFERRED,
                    CaseStatus.COMPLETE,
                    history.getActionByPathologistId(),
                    currentUserId,
                    note.toString()
            );
        }else {
            throw new IllegalArgumentException("Case must have a valid REFERRED state for it to be transferable");
        }

        caseService.updateCaseStatus(caseId, CaseStatus.COMPLETE.toString());
    }

    public CaseInfoDTO getCaseInfoByTypeAndUser(CaseType caseType, String loggedInUser) {
        String userId = userService.getUserIdByUsername(loggedInUser);

        // First get all non-deleted cases of the specified type
        Query caseQuery = new Query(Criteria.where("case_type").is(caseType)
                .and("is_deleted").is(false));
        List<String> caseIds = mongoTemplate.find(caseQuery, Case.class)
                .stream()
                .map(Case::getId)
                .collect(Collectors.toList());

        // Initialize counts
        long totalCases = 0;
        long newCases = 0;
        long inProgressCases = 0;
        long completedCases = 0;
        long referredCases = 0;
        long incomingCases = 0;

        if (!caseIds.isEmpty()) {
            // Get latest history entries for these cases
            Criteria historyCriteria = Criteria.where("case_id").in(caseIds);
            Query historyQuery = new Query(historyCriteria);
            List<History> allHistories = mongoTemplate.find(historyQuery, History.class);

            // Group histories by case_id and get the latest entry for each case
            Map<String, History> latestHistories = allHistories.stream()
                    .collect(Collectors.groupingBy(
                            History::getCaseId,
                            Collectors.collectingAndThen(
                                    Collectors.maxBy(Comparator.comparing(History::getCreatedAt)),
                                    optional -> optional.orElse(null)
                            )
                    ));

            // Count cases by status and user involvement
            for (History history : latestHistories.values()) {
                // For NEW cases, count regardless of user involvement
                if (history.getNewStatus() == CaseStatus.NEW) {
                    newCases++;
                    totalCases++;
                }
                // For other statuses, check user involvement
                else if (userId.equals(history.getActionByPathologistId()) ||
                        userId.equals(history.getTransferredToPathologistId())) {
                    totalCases++;

                    switch (history.getNewStatus()) {
                        case IN_PROGRESS:
                            inProgressCases++;
                            break;
                        case COMPLETE:
                            completedCases++;
                            break;
                        case REFERRED:
                            if (userId.equals(history.getActionByPathologistId())) {
                                referredCases++;
                            }
                            if (userId.equals(history.getTransferredToPathologistId())) {
                                incomingCases++;
                            }
                            break;
                    }
                }
            }
        }

        return new CaseInfoDTO(totalCases, newCases, inProgressCases, completedCases,
                referredCases, incomingCases);
    }
}
