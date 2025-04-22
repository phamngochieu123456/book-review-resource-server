package com.hieupn.book_review.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReactionDTO {
    private Long id;
    private Integer userId;
    private String username; // Username of the reactor
    private String reactionType;
    private Long reactableId;
    private String reactableType;
    private LocalDateTime createdAt;
}
