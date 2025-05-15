package app.api.diagnosticruntime.patient.casemanagment.comment;

public class CommentMapper {

    public static CommentDTO toCommentDTO(Comment comment) {
        CommentDTO commentDTO = new CommentDTO();
        commentDTO.setId(comment.getId());
        commentDTO.setCaseId(comment.getCaseId());
        commentDTO.setCreationDate(comment.getCreationDate());
        commentDTO.setCreationUser(comment.getCreationUser());
        commentDTO.setCommentText(comment.getCommentText());
        commentDTO.setModificationDate(comment.getModificationDate());
        commentDTO.setModificationUser(comment.getModificationUser());
        return commentDTO;
    }
}
