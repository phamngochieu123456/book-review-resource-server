package com.hieupn.book_review.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCommentDTO {
    @NotBlank(message = "Comment content cannot be blank")
    @Size(max = 2000, message = "Comment content cannot exceed 2000 characters")
    private String content;
}
