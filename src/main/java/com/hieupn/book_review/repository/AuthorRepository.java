package com.hieupn.book_review.repository;

import com.hieupn.book_review.model.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Author entity
 */
@Repository
public interface AuthorRepository extends JpaRepository<Author, Long>, QuerydslPredicateExecutor<Author> {
}
