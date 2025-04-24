// src/main/java/com/hieupn/book_review/mapper/GenreMapper.java
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
     *
     * @param genre The Genre entity
     * @return The corresponding GenreDTO
     */
    GenreDTO toGenreDTO(Genre genre);

    /**
     * Convert GenreDTO to Genre entity
     *
     * @param genreDTO The GenreDTO
     * @return The corresponding Genre entity
     */
    @Mapping(target = "bookGenres", ignore = true)
    Genre toGenre(GenreDTO genreDTO);

    /**
     * Convert a list of Genre entities to a list of GenreDTOs
     *
     * @param genres The list of Genre entities
     * @return The list of corresponding GenreDTOs
     */
    List<GenreDTO> toGenreDTOs(List<Genre> genres);
}
