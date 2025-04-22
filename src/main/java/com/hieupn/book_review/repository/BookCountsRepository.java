package com.hieupn.book_review.repository;

import com.hieupn.book_review.model.entity.BookCounts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for BookCounts entity
 */
@Repository
public interface BookCountsRepository extends JpaRepository<BookCounts, Integer> {

    /**
     * Find a book count by its name
     *
     * @param countName The name of the count to search for
     * @return Optional containing the book count if found, otherwise empty Optional
     */
    Optional<BookCounts> findByCountName(String countName);
}
