package com.hieupn.book_review.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReactionDTO {
    @NotNull(message = "Reactable ID is required")
    private Long reactableId;

    @NotBlank(message = "Reactable type is required")
    private String reactableType;

    @NotBlank(message = "Reaction type is required")
    private String reactionType;
}
