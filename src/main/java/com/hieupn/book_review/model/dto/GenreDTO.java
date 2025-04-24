// src/main/java/com/hieupn/book_review/model/dto/GenreDTO.java
package com.hieupn.book_review.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Genre entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenreDTO {

    private Long id;

    @NotBlank(message = "Genre name cannot be blank")
    @Size(max = 100, message = "Genre name cannot exceed 100 characters")
    private String name;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
