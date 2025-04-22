package com.hieupn.book_review.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentDTO {
    @NotNull(message = "Book ID is required")
    private Long bookId;

    @NotBlank(message = "Comment content cannot be blank")
    @Size(max = 2000, message = "Comment content cannot exceed 2000 characters")
    private String content;

    // Optional, only present if this is a reply to another comment
    private Long parentCommentId;
}
