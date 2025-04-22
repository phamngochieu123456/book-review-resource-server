package com.hieupn.book_review.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for Book entity summary (used in listing)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookSummaryDTO {

    private Long id;

    private String title;

    private String coverImageUrl;

    private Integer publicationYear;

    private BigDecimal averageRating;

    private Integer reviewCount;

    private List<AuthorSummaryDTO> authors;

    private LocalDateTime createdAt;
}
