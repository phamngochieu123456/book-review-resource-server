package com.hieupn.book_review.controller;

import com.hieupn.book_review.model.dto.BookDetailDTO;
import com.hieupn.book_review.model.dto.BookSummaryDTO;
import com.hieupn.book_review.model.dto.CreateBookRequestDTO;
import com.hieupn.book_review.model.dto.UpdateBookRequestDTO;
import com.hieupn.book_review.model.response.PagedResponse;
import com.hieupn.book_review.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * REST controller for book-related operations
 */
@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    /**
     * Get all books with optional filtering, pagination, and sorting
     *
     * @param page Page number (zero-based)
     * @param size Items per page
     * @param sortBy Field to sort by (title, averageRating, publicationYear, createdAt)
     * @param sortDir Sort direction (asc or desc)
     * @param categoryId Optional category ID to filter by
     * @param authorId Optional author ID to filter by
     * @param searchTerm Optional search term to filter title or description
     * @return PagedResponse of BookSummaryDTO objects
     */
    @GetMapping
    public ResponseEntity<PagedResponse<BookSummaryDTO>> getAllBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String searchTerm) {

        // Validate sortBy parameter
        if (!isValidSortField(sortBy)) {
            sortBy = "createdAt";
        }

        // Create sort and pageable objects
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        // Get books from service
        Page<BookSummaryDTO> books = bookService.getAllBooks(categoryId, authorId, searchTerm, pageable);

        // Wrap the Page in our custom PagedResponse
        PagedResponse<BookSummaryDTO> response = new PagedResponse<>(books);

        return ResponseEntity.ok(response);
    }

    /**
     * Get a book by its ID
     *
     * @param id The book ID
     * @return BookDetailDTO for the book
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookDetailDTO> getBookById(@PathVariable Long id) {
        BookDetailDTO book = bookService.getBookById(id);
        return ResponseEntity.ok(book);
    }

    /**
     * Create a new book
     *
     * @param createBookRequestDTO DTO containing book information
     * @return BookDetailDTO for the created book
     */
    @PostMapping
    public ResponseEntity<BookDetailDTO> createBook(@Valid @RequestBody CreateBookRequestDTO createBookRequestDTO) {
        // TODO: Get current user ID from security context
        Integer currentUserId = 1; // For demonstration purposes

        BookDetailDTO createdBook = bookService.createBook(createBookRequestDTO, currentUserId);

        // Create location URI for the new resource
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdBook.getId())
                .toUri();

        return ResponseEntity.created(location).body(createdBook);
    }

    /**
     * Update an existing book
     *
     * @param id The book ID
     * @param updateBookRequestDTO DTO containing updated book information
     * @return BookDetailDTO for the updated book
     */
    @PutMapping("/{id}")
    public ResponseEntity<BookDetailDTO> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookRequestDTO updateBookRequestDTO) {

        // TODO: Get current user ID from security context
        Integer currentUserId = 1; // For demonstration purposes

        BookDetailDTO updatedBook = bookService.updateBook(id, updateBookRequestDTO, currentUserId);

        return ResponseEntity.ok(updatedBook);
    }

    /**
     * Soft delete a book
     *
     * @param id The book ID
     * @return No content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Check if the sort field is valid
     *
     * @param field The field to check
     * @return true if valid, false otherwise
     */
    private boolean isValidSortField(String field) {
        return field.equals("title") ||
                field.equals("averageRating") ||
                field.equals("publicationYear") ||
                field.equals("createdAt");
    }
}
