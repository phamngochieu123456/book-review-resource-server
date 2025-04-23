package com.hieupn.book_review.controller;

import com.hieupn.book_review.exception.UnauthorizedException;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    @PreAuthorize("hasAuthority('READ_BOOK_OVERVIEW')")
    public ResponseEntity<PagedResponse<BookSummaryDTO>> getAllBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) Long authorId) {

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

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_BOOK_INFO')")
    public ResponseEntity<BookDetailDTO> getBookById(@PathVariable Long id) {
        BookDetailDTO book = bookService.getBookById(id);
        return ResponseEntity.ok(book);
    }

    @GetMapping("/by-author")
    @PreAuthorize("hasAuthority('READ_BOOK_OVERVIEW')")
    public ResponseEntity<PagedResponse<BookSummaryDTO>> getBooksByAuthor(
            @RequestParam Long authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "publicationYear") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        // Create pageable object
        Pageable pageable = PageRequest.of(page, size, Sort.by(
                sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC,
                sortBy));

        // Get books by author
        Page<BookSummaryDTO> books = bookService.getBooksByAuthor(authorId, pageable);

        // Wrap the Page in our custom PagedResponse
        PagedResponse<BookSummaryDTO> response = new PagedResponse<>(books);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WRITE_BOOK')")
    public ResponseEntity<BookDetailDTO> createBook(@Valid @RequestBody CreateBookRequestDTO createBookRequestDTO) {
        // Get currentUserId from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();

        // Extract user_id from JWT token safely using Number.intValue()
        Number userIdNumber = jwt.getClaim("user_id");
        if (userIdNumber == null) {
            throw new UnauthorizedException("User ID not found in token");
        }

        Integer currentUserId = userIdNumber.intValue();

        BookDetailDTO createdBook = bookService.createBook(createBookRequestDTO, currentUserId);

        // Create location URI for the new resource
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdBook.getId())
                .toUri();

        return ResponseEntity.created(location).body(createdBook);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WRITE_BOOK')")
    public ResponseEntity<BookDetailDTO> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookRequestDTO updateBookRequestDTO) {

        // Get currentUserId from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();

        // Extract user_id from JWT token safely using Number.intValue()
        Number userIdNumber = jwt.getClaim("user_id");
        if (userIdNumber == null) {
            throw new UnauthorizedException("User ID not found in token");
        }

        Integer currentUserId = userIdNumber.intValue();

        BookDetailDTO updatedBook = bookService.updateBook(id, updateBookRequestDTO, currentUserId);

        return ResponseEntity.ok(updatedBook);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_BOOK')")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isValidSortField(String field) {
        return field.equals("title") ||
                field.equals("averageRating") ||
                field.equals("publicationYear") ||
                field.equals("createdAt");
    }
}
