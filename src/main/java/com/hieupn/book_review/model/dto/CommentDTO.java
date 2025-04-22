package com.hieupn.book_review.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {
    private Long id;
    private String content;
    private Integer userId;
    private String username; // Username of commenter
    private Long bookId;
    private String bookTitle; // Title of the book being commented on
    private Long parentCommentId; // ID of parent comment if this is a reply
    private List<CommentDTO> replies; // Child comments/replies
    private ReactionSummaryDTO reactions; // Summary of reactions to this comment
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;
}
