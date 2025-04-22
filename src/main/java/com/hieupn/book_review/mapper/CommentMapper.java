package com.hieupn.book_review.mapper;

import com.hieupn.book_review.model.dto.CommentDTO;
import com.hieupn.book_review.model.entity.Comment;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Mapper for converting between Comment entity and CommentDTO
 */
@Mapper(componentModel = "spring")
public interface CommentMapper {

    CommentMapper INSTANCE = Mappers.getMapper(CommentMapper.class);

    /**
     * Convert Comment entity to CommentDTO
     *
     * @param comment The Comment entity
     * @return The corresponding CommentDTO
     */
    @Mapping(target = "bookId", source = "book.id")
    @Mapping(target = "bookTitle", source = "book.title")
    @Mapping(target = "parentCommentId", source = "parentComment.id")
    @Mapping(target = "username", ignore = true) // Will be populated by UserService
    @Mapping(target = "replies", ignore = true) // Will be populated separately
    @Mapping(target = "reactions", ignore = true) // Will be populated separately
    CommentDTO toCommentDTO(Comment comment);

    /**
     * Convert a list of Comment entities to a list of CommentDTOs
     *
     * @param comments The list of Comment entities
     * @return The list of corresponding CommentDTOs
     */
    List<CommentDTO> toCommentDTOs(List<Comment> comments);
}
