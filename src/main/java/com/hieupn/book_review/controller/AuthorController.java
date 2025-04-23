package com.hieupn.book_review.controller;

import com.hieupn.book_review.model.dto.AuthorDetailDTO;
import com.hieupn.book_review.model.dto.BookSummaryDTO;
import com.hieupn.book_review.model.response.PagedResponse;
import com.hieupn.book_review.service.AuthorService;
import com.hieupn.book_review.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/authors")
@RequiredArgsConstructor
public class AuthorController {

    private final AuthorService authorService;
    private final BookService bookService;

    /**
     * Get author details by ID
     *
     * @param id The author ID
     * @return AuthorDTO with author details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_AUTHOR_INFO')")
    public ResponseEntity<AuthorDetailDTO> getAuthorById(@PathVariable Long id) {
        AuthorDetailDTO author = authorService.getAuthorById(id);
        return ResponseEntity.ok(author);
    }

    /**
     * Get paginated list of books by an author
     * This follows RESTful convention by using the author ID in the path
     *
     * @param authorId The author ID
     * @param page Page number (zero-based)
     * @param size Items per page
     * @param sortBy Field to sort by
     * @param sortDir Sort direction (asc/desc)
     * @return Paginated list of books by the author
     */
    @GetMapping("/{id}/books")
    @PreAuthorize("hasAuthority('READ_BOOK_OVERVIEW')")
    public ResponseEntity<PagedResponse<BookSummaryDTO>> getAuthorBooks(
            @PathVariable("id") Long authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "publicationYear") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        // Validate sortBy parameter
        if (!isValidSortField(sortBy)) {
            sortBy = "publicationYear";
        }

        // Create sort and pageable objects
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        // Get books by author with pagination
        Page<BookSummaryDTO> books = bookService.getBooksByAuthor(authorId, pageable);

        // Wrap the Page in our custom PagedResponse
        PagedResponse<BookSummaryDTO> response = new PagedResponse<>(books);

        return ResponseEntity.ok(response);
    }

    /**
     * Validate if the provided sort field is valid for book sorting
     *
     * @param field The field name to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidSortField(String field) {
        return field.equals("title") ||
                field.equals("averageRating") ||
                field.equals("publicationYear") ||
                field.equals("createdAt");
    }
}
