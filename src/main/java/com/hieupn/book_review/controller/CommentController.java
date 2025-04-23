package com.hieupn.book_review.controller;

import com.hieupn.book_review.exception.UnauthorizedException;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/books/{bookId}/comments")
    @PreAuthorize("hasAuthority('READ_COMMENT')")
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

    @GetMapping("/comments/{parentCommentId}/replies")
    @PreAuthorize("hasAuthority('READ_COMMENT')")
    public ResponseEntity<List<CommentDTO>> getCommentReplies(@PathVariable Long parentCommentId) {
        List<CommentDTO> replies = commentService.getChildComments(parentCommentId);
        return ResponseEntity.ok(replies);
    }

    @GetMapping("/comments/{commentId}")
    @PreAuthorize("hasAuthority('READ_COMMENT')")
    public ResponseEntity<CommentDTO> getCommentById(@PathVariable Long commentId) {
        CommentDTO comment = commentService.getCommentById(commentId);
        return ResponseEntity.ok(comment);
    }

    @PostMapping("/comments")
    @PreAuthorize("hasAuthority('WRITE_COMMENT')")
    public ResponseEntity<CommentDTO> createComment(@Valid @RequestBody CreateCommentDTO createCommentDTO) {
        // Get currentUserId from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();

        // Extract user_id from JWT token safely using Number.intValue()
        Number userIdNumber = jwt.getClaim("user_id");
        if (userIdNumber == null) {
            throw new UnauthorizedException("User ID not found in token");
        }

        Integer currentUserId = userIdNumber.intValue();

        CommentDTO createdComment = commentService.createComment(createCommentDTO, currentUserId);

        // Create location URI for the new resource
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdComment.getId())
                .toUri();

        return ResponseEntity.created(location).body(createdComment);
    }

    @PutMapping("/comments/{commentId}")
    @PreAuthorize("hasAuthority('WRITE_COMMENT')")
    public ResponseEntity<CommentDTO> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentDTO updateCommentDTO) {

        // Get currentUserId from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();

        // Extract user_id from JWT token safely using Number.intValue()
        Number userIdNumber = jwt.getClaim("user_id");
        if (userIdNumber == null) {
            throw new UnauthorizedException("User ID not found in token");
        }

        Integer currentUserId = userIdNumber.intValue();

        CommentDTO updatedComment = commentService.updateComment(commentId, updateCommentDTO, currentUserId);

        return ResponseEntity.ok(updatedComment);
    }

    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("hasAuthority('WRITE_COMMENT')")
    public ResponseEntity<Void> softDeleteComment(@PathVariable Long commentId) {
        // Get currentUserId from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();

        // Extract user_id from JWT token safely using Number.intValue()
        Number userIdNumber = jwt.getClaim("user_id");
        if (userIdNumber == null) {
            throw new UnauthorizedException("User ID not found in token");
        }

        Integer currentUserId = userIdNumber.intValue();

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        commentService.softDeleteComment(commentId, currentUserId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/comments/{commentId}/hide")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> adminSoftDeleteComment(@PathVariable Long commentId) {
        // Get currentUserId from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();

        // Extract user_id from JWT token safely using Number.intValue()
        Number userIdNumber = jwt.getClaim("user_id");
        if (userIdNumber == null) {
            throw new UnauthorizedException("User ID not found in token");
        }

        Integer currentUserId = userIdNumber.intValue();

        boolean isAdmin = true;

        commentService.softDeleteComment(commentId, currentUserId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/comments/{commentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> adminHardDeleteComment(@PathVariable Long commentId) {
        commentService.hardDeleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
