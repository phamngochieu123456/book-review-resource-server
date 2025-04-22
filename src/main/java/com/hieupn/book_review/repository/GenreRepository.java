package com.hieupn.book_review.repository;

import com.hieupn.book_review.model.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Genre entity
 */
@Repository
public interface GenreRepository extends JpaRepository<Genre, Long>, QuerydslPredicateExecutor<Genre> {
}
