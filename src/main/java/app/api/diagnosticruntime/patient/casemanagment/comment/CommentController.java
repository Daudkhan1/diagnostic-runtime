package app.api.diagnosticruntime.patient.casemanagment.comment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<List<CommentDTO>> getComments(
            @RequestParam(required = false) String caseId,
            @RequestParam(required = false) String userEmail) {

        // Call the service to fetch the filtered comments
        List<CommentDTO> comments = commentService.getAllComments(caseId, userEmail);
        return ResponseEntity.status(HttpStatus.OK).body(comments);
    }

    @PostMapping
    public ResponseEntity<CommentDTO> addComment(@RequestBody CommentModifyDTO commentDTO, @AuthenticationPrincipal UserDetails userDetails) {
        // Get the employee's name from the token
        String userEmail = userDetails.getUsername();

        // Call the service to store the comment
        CommentDTO comment = commentService.addComment(commentDTO, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommentDTO> updateComment(@PathVariable String id, @RequestBody CommentModifyDTO commentDTO, @AuthenticationPrincipal UserDetails userDetails) {
        // Get the employee's name from the token
        String userEmail = userDetails.getUsername();

        // Call the service to update the comment
        CommentDTO updatedComment = commentService.updateComment(id, commentDTO, userEmail);
        return ResponseEntity.ok(updatedComment);
    }
}

