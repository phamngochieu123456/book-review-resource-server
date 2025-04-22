package com.hieupn.book_review.repository;

import com.hieupn.book_review.model.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Custom repository interface for Book-specific queries using QueryDSL
 */
public interface BookRepositoryCustom {

    /**
     * Find all non-deleted books with optional filtering by category ID, author ID, and search term
     *
     * @param categoryId Optional category ID to filter by
     * @param authorId Optional author ID to filter by
     * @param searchTerm Optional search term to filter title or description
     * @param pageable Pagination and sorting information
     * @return A page of books matching the criteria
     */
    Page<Book> findAllNonDeletedBooks(Long categoryId, Long authorId, String searchTerm, Pageable pageable);
}
