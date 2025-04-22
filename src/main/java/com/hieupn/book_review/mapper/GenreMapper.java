package com.hieupn.book_review.mapper;

import com.hieupn.book_review.model.dto.GenreDTO;
import com.hieupn.book_review.model.entity.Genre;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Mapper interface for converting between Genre entity and GenreDTO
 */
@Mapper(componentModel = "spring")
public interface GenreMapper {

    GenreMapper INSTANCE = Mappers.getMapper(GenreMapper.class);

    /**
     * Convert Genre entity to GenreDTO
     * Map the description from the associated category
     *
     * @param genre The Genre entity
     * @return The corresponding GenreDTO
     */
    GenreDTO toGenreDTO(Genre genre);

    /**
     * Convert a list of Genre entities to a list of GenreDTOs
     *
     * @param genres The list of Genre entities
     * @return The list of corresponding GenreDTOs
     */
    List<GenreDTO> toGenreDTOs(List<Genre> genres);
}
