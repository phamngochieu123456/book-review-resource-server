// src/main/java/com/hieupn/book_review/model/dto/BookDetailDTO.java
package com.hieupn.book_review.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for detailed Book information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookDetailDTO {

    private Long id;

    private String title;

    private String description;

    private String isbn;

    private String coverImageUrl;

    private Integer publicationYear;

    private BigDecimal averageRating;

    private Integer reviewCount;

    // Author information
    @JsonIgnoreProperties("books")
    private List<AuthorSummaryDTO> authors;

    // Genres (replaced categories)
    private List<GenreDTO> genres;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}