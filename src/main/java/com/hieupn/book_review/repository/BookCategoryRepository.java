package com.hieupn.book_review.repository;

import com.hieupn.book_review.model.entity.Book;
import com.hieupn.book_review.model.entity.BookCategory;
import com.hieupn.book_review.model.entity.BookCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for BookCategory entity
 */
@Repository
public interface BookCategoryRepository extends JpaRepository<BookCategory, BookCategoryId>,
        QuerydslPredicateExecutor<BookCategory> {

    /**
     * Find all BookCategory entries by book
     *
     * @param book The book to find categories for
     * @return List of BookCategory entries for the given book
     */
    List<BookCategory> findByBook(Book book);

    /**
     * Delete all BookCategory entries for a given book
     *
     * @param book The book to delete categories for
     */
    void deleteByBook(Book book);
}
