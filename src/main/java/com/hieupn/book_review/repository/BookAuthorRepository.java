package com.hieupn.book_review.repository;

import com.hieupn.book_review.model.entity.Author;
import com.hieupn.book_review.model.entity.Book;
import com.hieupn.book_review.model.entity.BookAuthor;
import com.hieupn.book_review.model.entity.BookAuthorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for BookAuthor entity
 */
@Repository
public interface BookAuthorRepository extends JpaRepository<BookAuthor, BookAuthorId>,
        QuerydslPredicateExecutor<BookAuthor> {

    /**
     * Find all BookAuthor entries by book
     *
     * @param book The book to find authors for
     * @return List of BookAuthor entries for the given book
     */
    List<BookAuthor> findByBook(Book book);

    /**
     * Find all BookAuthor entries by author
     *
     * @param author The author to find books for
     * @return List of BookAuthor entries for the given author
     */
    List<BookAuthor> findByAuthor(Author author);

    /**
     * Delete all BookAuthor entries for a given book
     *
     * @param book The book to delete authors for
     */
    void deleteByBook(Book book);

    /**
     * Delete all BookAuthor entries for a given author
     *
     * @param author The author to delete books for
     */
    void deleteByAuthor(Author author);
}
