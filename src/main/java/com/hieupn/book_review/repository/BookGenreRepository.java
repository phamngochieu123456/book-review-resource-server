// src/main/java/com/hieupn/book_review/repository/BookGenreRepository.java
package com.hieupn.book_review.repository;

import com.hieupn.book_review.model.entity.Book;
import com.hieupn.book_review.model.entity.BookGenre;
import com.hieupn.book_review.model.entity.BookGenreId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for BookGenre entity
 */
@Repository
public interface BookGenreRepository extends JpaRepository<BookGenre, BookGenreId>,
        QuerydslPredicateExecutor<BookGenre> {

    /**
     * Find all BookGenre entries by book
     *
     * @param book The book to find genres for
     * @return List of BookGenre entries for the given book
     */
    List<BookGenre> findByBook(Book book);

    /**
     * Delete all BookGenre entries for a given book
     *
     * @param book The book to delete genres for
     */
    void deleteByBook(Book book);
}
