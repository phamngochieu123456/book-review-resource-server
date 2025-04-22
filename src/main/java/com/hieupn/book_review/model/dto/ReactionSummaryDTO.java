package com.hieupn.book_review.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReactionSummaryDTO {
    private Long reactableId;
    private String reactableType;
    private Map<String, Long> countsByType; // Map of reaction type to count
    private Long totalCount;
}
