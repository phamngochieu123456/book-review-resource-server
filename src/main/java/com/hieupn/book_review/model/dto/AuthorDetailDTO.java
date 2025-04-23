package com.hieupn.book_review.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for Author entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorDetailDTO {

    private Long id;

    @NotBlank(message = "Author name cannot be blank")
    @Size(max = 255, message = "Author name cannot exceed 255 characters")
    private String name;

    private String biography;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<BookSummaryDTO> books;
}
