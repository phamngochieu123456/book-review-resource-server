package com.hieupn.book_review.controller;

import com.hieupn.book_review.model.dto.CommentDTO;
import com.hieupn.book_review.model.dto.CreateCommentDTO;
import com.hieupn.book_review.model.dto.UpdateCommentDTO;
import com.hieupn.book_review.model.response.PagedResponse;
import com.hieupn.book_review.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST controller for comment-related operations
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * Get top-level comments for a book
     *
     * @param bookId   Book ID
     * @param page     Page number (zero-based)
     * @param size     Items per page
     * @param sortBy   Field to sort by (createdAt)
     * @param sortDir  Sort direction (asc or desc)
     * @return PagedResponse of CommentDTOs
     */
    @GetMapping("/books/{bookId}/comments")
    public ResponseEntity<PagedResponse<CommentDTO>> getBookComments(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        // Create sort and pageable objects
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        // Get comments from service
        Page<CommentDTO> comments = commentService.getBookComments(bookId, pageable);

        // Wrap the Page in our custom PagedResponse
        PagedResponse<CommentDTO> response = new PagedResponse<>(comments);

        return ResponseEntity.ok(response);
    }

    /**
     * Get child comments (replies) for a parent comment
     *
     * @param parentCommentId Parent comment ID
     * @return List of CommentDTOs
     */
    @GetMapping("/comments/{parentCommentId}/replies")
    public ResponseEntity<List<CommentDTO>> getCommentReplies(@PathVariable Long parentCommentId) {
        List<CommentDTO> replies = commentService.getChildComments(parentCommentId);
        return ResponseEntity.ok(replies);
    }

    /**
     * Get a comment by its ID
     *
     * @param commentId Comment ID
     * @return CommentDTO
     */
    @GetMapping("/comments/{commentId}")
    public ResponseEntity<CommentDTO> getCommentById(@PathVariable Long commentId) {
        CommentDTO comment = commentService.getCommentById(commentId);
        return ResponseEntity.ok(comment);
    }

    /**
     * Create a new comment
     *
     * @param createCommentDTO Comment creation data
     * @return CommentDTO for the created comment
     */
    @PostMapping("/comments")
    public ResponseEntity<CommentDTO> createComment(@Valid @RequestBody CreateCommentDTO createCommentDTO) {
        // TODO: Get current user ID from security context
        Integer currentUserId = 1; // For demonstration purposes

        CommentDTO createdComment = commentService.createComment(createCommentDTO, currentUserId);

        // Create location URI for the new resource
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdComment.getId())
                .toUri();

        return ResponseEntity.created(location).body(createdComment);
    }

    /**
     * Update an existing comment
     *
     * @param commentId        Comment ID
     * @param updateCommentDTO Comment update data
     * @return CommentDTO for the updated comment
     */
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<CommentDTO> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentDTO updateCommentDTO) {

        // TODO: Get current user ID from security context
        Integer currentUserId = 1; // For demonstration purposes

        CommentDTO updatedComment = commentService.updateComment(commentId, updateCommentDTO, currentUserId);

        return ResponseEntity.ok(updatedComment);
    }

    /**
     * Soft delete a comment (mark as deleted)
     *
     * @param commentId Comment ID
     * @return No content response
     */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> softDeleteComment(@PathVariable Long commentId) {
        // TODO: Get current user ID and roles from security context
        Integer currentUserId = 1; // For demonstration purposes
        boolean isAdmin = false; // For demonstration purposes

        commentService.softDeleteComment(commentId, currentUserId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    /**
     * Admin: Soft delete any comment
     *
     * @param commentId Comment ID
     * @return No content response
     */
    @DeleteMapping("/admin/comments/{commentId}/hide")
    public ResponseEntity<Void> adminSoftDeleteComment(@PathVariable Long commentId) {
        // TODO: Get current user ID from security context and check if admin
        Integer currentUserId = 1; // For demonstration purposes
        boolean isAdmin = true; // Admin role is required for this endpoint

        commentService.softDeleteComment(commentId, currentUserId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    /**
     * Admin: Hard delete a comment (permanent deletion)
     *
     * @param commentId Comment ID
     * @return No content response
     */
    @DeleteMapping("/admin/comments/{commentId}")
    public ResponseEntity<Void> adminHardDeleteComment(@PathVariable Long commentId) {
        // TODO: Check if admin role is present

        commentService.hardDeleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
