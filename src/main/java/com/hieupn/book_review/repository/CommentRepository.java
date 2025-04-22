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
    // Tìm tất cả bình luận của một cuốn sách
    Page<Comment> findByBookIdAndIsDeletedFalse(Long bookId, Pageable pageable);

    // Tìm tất cả bình luận trả lời của một bình luận
    List<Comment> findByParentCommentIdAndIsDeletedFalse(Long parentCommentId);

    // Tìm bình luận gốc (không có parent) của một cuốn sách
    Page<Comment> findByBookIdAndParentCommentIsNullAndIsDeletedFalse(Long bookId, Pageable pageable);
}
