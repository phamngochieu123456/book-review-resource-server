package com.hieupn.book_review.service;

import com.hieupn.book_review.exception.ResourceNotFoundException;
import com.hieupn.book_review.mapper.AuthorMapper;
import com.hieupn.book_review.model.dto.AuthorDetailDTO;
import com.hieupn.book_review.model.entity.Author;
import com.hieupn.book_review.repository.AuthorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * Service for author-related operations
 */
@Service
@RequiredArgsConstructor
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final AuthorMapper authorMapper;

    /**
     * Get an author by ID
     *
     * @param id The author ID
     * @return AuthorDTO
     * @throws ResourceNotFoundException if author not found
     */
    public AuthorDetailDTO getAuthorById(Long id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author", "id", id));

        // Convert to DTO
        AuthorDetailDTO authorDTO = authorMapper.toAuthorDTO(author);

        // Initialize empty books list
        // Books will be loaded separately via pagination through the /authors/{id}/books endpoint
        authorDTO.setBooks(new ArrayList<>());

        return authorDTO;
    }
}
