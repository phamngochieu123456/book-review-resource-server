package com.hieupn.book_review.service;

import com.hieupn.book_review.exception.DuplicateResourceException;
import com.hieupn.book_review.exception.ResourceNotFoundException;
import com.hieupn.book_review.mapper.BookMapper;
import com.hieupn.book_review.model.dto.BookDetailDTO;
import com.hieupn.book_review.model.dto.BookSummaryDTO;
import com.hieupn.book_review.model.dto.CreateBookRequestDTO;
import com.hieupn.book_review.model.dto.UpdateBookRequestDTO;
import com.hieupn.book_review.model.entity.*;
import com.hieupn.book_review.repository.*;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service class for book-related operations
 */
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final BookCategoryRepository bookCategoryRepository;
    private final BookAuthorRepository bookAuthorRepository;
    private final BookMapper bookMapper;

    /**
     * Get all non-deleted books with optional filtering
     *
     * @param categoryId Optional category ID to filter by
     * @param authorId Optional author ID to filter by
     * @param searchTerm Optional search term to filter title or description
     * @param pageable Pagination and sorting information
     * @return Page of BookSummaryDTO objects
     */
    public Page<BookSummaryDTO> getAllBooks(Long categoryId, Long authorId, String searchTerm, Pageable pageable) {
        Page<Book> books = bookRepository.findAllNonDeletedBooks(categoryId, authorId, searchTerm, pageable);
        return books.map(bookMapper::toBookSummaryDTO);
    }

    /**
     * Get a book by its ID
     *
     * @param id The book ID
     * @return BookDetailDTO for the book
     * @throws ResourceNotFoundException if the book is not found or is deleted
     */
    public BookDetailDTO getBookById(Long id) {
        Book book = bookRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", id));

        return bookMapper.toBookDetailDTO(book);
    }

    /**
     * Create a new book
     *
     * @param createBookRequestDTO DTO containing book information
     * @param currentUserId ID of the user creating the book (for tracking who assigned categories)
     * @return BookDetailDTO for the created book
     * @throws ResourceNotFoundException if author or category is not found
     * @throws DuplicateResourceException if ISBN already exists
     */
    @Transactional
    public BookDetailDTO createBook(CreateBookRequestDTO createBookRequestDTO, Integer currentUserId) {
        // Check if ISBN already exists
        if (createBookRequestDTO.getIsbn() != null && !createBookRequestDTO.getIsbn().isEmpty()) {
            Optional<Book> existingBook = bookRepository.findByIsbn(createBookRequestDTO.getIsbn());
            if (existingBook.isPresent()) {
                throw new DuplicateResourceException("Book", "isbn", createBookRequestDTO.getIsbn());
            }
        }

        // Create book entity
        Book book = Book.builder()
                .title(createBookRequestDTO.getTitle())
                .description(createBookRequestDTO.getDescription())
                .isbn(createBookRequestDTO.getIsbn())
                .coverImageUrl(createBookRequestDTO.getCoverImageUrl())
                .publicationYear(createBookRequestDTO.getPublicationYear())
                .build();

        // Save book to get an ID
        book = bookRepository.save(book);

        // Associate categories
        associateCategoriesWithBook(book, createBookRequestDTO.getCategoryIds(), currentUserId);

        // Associate authors
        associateAuthorsWithBook(book, createBookRequestDTO.getAuthorIds(), currentUserId);

        // Refresh book from database to get all relationships loaded
        book = bookRepository.findById(book.getId()).orElseThrow();

        return bookMapper.toBookDetailDTO(book);
    }

    /**
     * Update an existing book
     *
     * @param id The book ID
     * @param updateBookRequestDTO DTO containing updated book information
     * @param currentUserId ID of the user updating the book (for tracking who assigned categories)
     * @return BookDetailDTO for the updated book
     * @throws ResourceNotFoundException if book, author, or category is not found
     * @throws DuplicateResourceException if new ISBN conflicts with another book
     */
    @Transactional
    public BookDetailDTO updateBook(Long id, UpdateBookRequestDTO updateBookRequestDTO, Integer currentUserId) {
        // Find the book
        Book book = bookRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", id));

        // Check if new ISBN already exists for a different book
        if (updateBookRequestDTO.getIsbn() != null &&
                !updateBookRequestDTO.getIsbn().equals(book.getIsbn()) &&
                !updateBookRequestDTO.getIsbn().isEmpty()) {

            boolean existsWithIsbn = bookRepository.existsByIsbnAndIdNot(updateBookRequestDTO.getIsbn(), id);
            if (existsWithIsbn) {
                throw new DuplicateResourceException("Book", "isbn", updateBookRequestDTO.getIsbn());
            }
        }

        // Update authors if provided
        if (updateBookRequestDTO.getAuthorIds() != null) {
            // Remove existing authors
            bookAuthorRepository.deleteByBook(book);
            // Add new authors
            associateAuthorsWithBook(book, updateBookRequestDTO.getAuthorIds(), currentUserId);
        }

        // Update basic book properties
        book.setTitle(updateBookRequestDTO.getTitle());
        book.setDescription(updateBookRequestDTO.getDescription());
        book.setIsbn(updateBookRequestDTO.getIsbn());
        book.setCoverImageUrl(updateBookRequestDTO.getCoverImageUrl());
        book.setPublicationYear(updateBookRequestDTO.getPublicationYear());

        // Save book changes
        book = bookRepository.save(book);

        // Update categories if provided
        if (updateBookRequestDTO.getCategoryIds() != null) {
            // Remove existing categories
            bookCategoryRepository.deleteByBook(book);
            // Add new categories
            associateCategoriesWithBook(book, updateBookRequestDTO.getCategoryIds(), currentUserId);
        }

        // Refresh book from database to get all relationships loaded
        book = bookRepository.findById(book.getId()).orElseThrow();

        return bookMapper.toBookDetailDTO(book);
    }

    /**
     * Soft delete a book
     *
     * @param id The book ID
     * @throws ResourceNotFoundException if the book is not found or already deleted
     */
    @Transactional
    public void deleteBook(Long id) {
        Book book = bookRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", id));

        book.setIsDeleted(true);
        bookRepository.save(book);
    }

    /**
     * Associate categories with a book
     *
     * @param book The book
     * @param categoryIds List of category IDs
     * @param assignedBy ID of the user assigning categories
     * @throws ResourceNotFoundException if a category is not found
     */
    private void associateCategoriesWithBook(Book book, List<Long> categoryIds, Integer assignedBy) {
        List<BookCategory> bookCategories = new ArrayList<>();

        for (Long categoryId : categoryIds) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));

            BookCategory bookCategory = new BookCategory(assignedBy, book, category);
            bookCategories.add(bookCategory);
        }

        bookCategoryRepository.saveAll(bookCategories);
    }

    /**
     * Associate authors with a book
     *
     * @param book The book
     * @param authorIds List of author IDs
     * @param assignedBy ID of the user assigning authors
     * @throws ResourceNotFoundException if an author is not found
     */
    private void associateAuthorsWithBook(Book book, List<Long> authorIds, Integer assignedBy) {
        List<BookAuthor> bookAuthors = new ArrayList<>();

        int priority = authorIds.size();

        for (Long authorId : authorIds) {
            Author author = authorRepository.findById(authorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Author", "id", authorId));

            BookAuthor bookAuthor = new BookAuthor(priority, assignedBy, book, author);
            bookAuthors.add(bookAuthor);

            priority = priority - 1;
        }

        bookAuthorRepository.saveAll(bookAuthors);
    }
}
