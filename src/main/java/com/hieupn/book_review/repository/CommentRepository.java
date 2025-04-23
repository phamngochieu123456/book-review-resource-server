package com.hieupn.book_review.repository;

import com.hieupn.book_review.model.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>, QuerydslPredicateExecutor<Comment> {
    // Find all comments for a book
    Page<Comment> findByBookIdAndIsDeletedFalse(Long bookId, Pageable pageable);

    // Find all replies to a comment
    List<Comment> findByParentCommentIdAndIsDeletedFalse(Long parentCommentId);

    // Find root comments (without parent) for a book
    Page<Comment> findByBookIdAndParentCommentIsNullAndIsDeletedFalse(Long bookId, Pageable pageable);
}
