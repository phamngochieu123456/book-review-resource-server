package com.hieupn.book_review.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object for updating an existing Book
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBookRequestDTO {

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

    private List<Long> authorIds;

    private List<Long> genreIds;
}
