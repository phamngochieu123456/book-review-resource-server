package com.hieupn.book_review.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object for creating a new Book
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookRequestDTO {

    @NotBlank(message = "Book title cannot be blank")
    @Size(max = 255, message = "Book title cannot exceed 255 characters")
    private String title;

    private String description;

    @Size(max = 20, message = "ISBN cannot exceed 20 characters")
    @Pattern(regexp = "^(97(8|9))?\\d{9}(\\d|X)$", message = "ISBN format is invalid")
    private String isbn;

    @Size(max = 500, message = "Cover image URL cannot exceed 500 characters")
    private String coverImageUrl;

    private Integer publicationYear;

    @NotNull(message = "At least one author must be provided")
    private List<Long> authorIds;

    @NotNull(message = "At least one category must be provided")
    private List<Long> categoryIds;
}
