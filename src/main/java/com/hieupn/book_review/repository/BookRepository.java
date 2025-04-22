package com.hieupn.book_review.repository;

import com.hieupn.book_review.model.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Book entity
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long>, QuerydslPredicateExecutor<Book>, BookRepositoryCustom {

    /**
     * Find a book by its ID where isDeleted is false
     *
     * @param id The book ID
     * @return Optional containing the book if found and not deleted, otherwise empty Optional
     */
    Optional<Book> findByIdAndIsDeletedFalse(Long id);

    /**
     * Find a book by its ISBN
     *
     * @param isbn The ISBN to search for
     * @return Optional containing the book if found, otherwise empty Optional
     */
    Optional<Book> findByIsbn(String isbn);

    /**
     * Check if a book exists with the given ISBN and is not the specified book
     *
     * @param isbn The ISBN to check
     * @param id The ID of the book to exclude from the check
     * @return true if a book with the given ISBN exists and is not the specified book, false otherwise
     */
    boolean existsByIsbnAndIdNot(String isbn, Long id);
}
