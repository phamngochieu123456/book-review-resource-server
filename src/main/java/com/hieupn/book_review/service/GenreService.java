package com.hieupn.book_review.service;

import com.hieupn.book_review.mapper.GenreMapper;
import com.hieupn.book_review.model.dto.GenreDTO;
import com.hieupn.book_review.model.entity.Genre;
import com.hieupn.book_review.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service class for genre-related operations
 */
@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreRepository genreRepository;
    private final GenreMapper genreMapper;

    /**
     * Get all genres
     *
     * @return List of all GenreDTOs
     */
    public List<GenreDTO> getAllGenres() {
        List<Genre> genres = genreRepository.findAll();
        return genreMapper.toGenreDTOs(genres);
    }
}
