package com.hieupn.book_review.mapper;

import com.hieupn.book_review.model.dto.AuthorDetailDTO;
import com.hieupn.book_review.model.dto.AuthorSummaryDTO;
import com.hieupn.book_review.model.entity.Author;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Mapper interface for converting between Author entity and AuthorDTO
 */
@Mapper(componentModel = "spring")
public interface AuthorMapper {

    AuthorMapper INSTANCE = Mappers.getMapper(AuthorMapper.class);

    /**
     * Convert Author entity to AuthorSummaryDTO
     *
     * @param author The Author entity
     * @return The corresponding AuthorSummaryDTO
     */
    AuthorSummaryDTO toAuthorSummaryDTO(Author author);

    /**
     * Convert Author entity to full AuthorDTO
     *
     * @param author The Author entity
     * @return The corresponding AuthorDTO with all details
     */
    @Mapping(target = "books", ignore = true) // Books will be loaded separately with pagination
    AuthorDetailDTO toAuthorDTO(Author author);

    /**
     * Convert AuthorDTO to Author entity
     *
     * @param authorSummaryDTO The AuthorDTO
     * @return The corresponding Author entity
     */
    @Mapping(target = "bookAuthors", ignore = true)
    Author toAuthor(AuthorSummaryDTO authorSummaryDTO);


    /**
     * Convert a list of Author entities to a list of AuthorDTOs
     *
     * @param authors The list of Author entities
     * @return The list of corresponding AuthorDTOs
     */
    List<AuthorSummaryDTO> toAuthorSummaryDTOs(List<Author> authors);
}
