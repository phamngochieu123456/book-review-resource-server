package com.hieupn.book_review.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDTO {
    private Long id;
    private Byte rating;
    private String comment;
    private Integer userId;
    private String username; // Username of the reviewer
    private Long bookId;
    private String bookTitle; // Title of the book being reviewed
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
