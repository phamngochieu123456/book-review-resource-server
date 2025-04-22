package com.hieupn.book_review.mapper;

import com.hieupn.book_review.model.dto.ReviewDTO;
import com.hieupn.book_review.model.entity.Review;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Mapper for converting between Review entity and ReviewDTO
 */
@Mapper(componentModel = "spring")
public interface ReviewMapper {

    ReviewMapper INSTANCE = Mappers.getMapper(ReviewMapper.class);

    /**
     * Convert Review entity to ReviewDTO
     *
     * @param review The Review entity
     * @return The corresponding ReviewDTO
     */
    @Mapping(target = "bookId", source = "book.id")
    @Mapping(target = "bookTitle", source = "book.title")
    @Mapping(target = "username", ignore = true) // Will be populated by UserService
    ReviewDTO toReviewDTO(Review review);

    /**
     * Convert a list of Review entities to a list of ReviewDTOs
     *
     * @param reviews The list of Review entities
     * @return The list of corresponding ReviewDTOs
     */
    List<ReviewDTO> toReviewDTOs(List<Review> reviews);
}
