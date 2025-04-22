package com.hieupn.book_review.repository;

import com.hieupn.book_review.model.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, QuerydslPredicateExecutor<Review> {
    // Tìm tất cả đánh giá của một cuốn sách
    Page<Review> findByBookId(Long bookId, Pageable pageable);

    // Tìm tất cả đánh giá của một người dùng
    Page<Review> findByUserId(Integer userId, Pageable pageable);

    // Tìm đánh giá của một người dùng cho một cuốn sách cụ thể
    Optional<Review> findByUserIdAndBookId(Integer userId, Long bookId);

    // Đếm số lượng đánh giá cho một cuốn sách
    long countByBookId(Long bookId);
}
