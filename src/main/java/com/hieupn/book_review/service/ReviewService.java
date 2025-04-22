package com.hieupn.book_review.service;

import com.hieupn.book_review.exception.ResourceNotFoundException;
import com.hieupn.book_review.mapper.ReviewMapper;
import com.hieupn.book_review.model.dto.CreateReviewDTO;
import com.hieupn.book_review.model.dto.ReviewDTO;
import com.hieupn.book_review.model.dto.UpdateReviewDTO;
import com.hieupn.book_review.model.entity.Book;
import com.hieupn.book_review.model.entity.Review;
import com.hieupn.book_review.model.entity.QReview;
import com.hieupn.book_review.repository.BookRepository;
import com.hieupn.book_review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Service for managing book reviews
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final ReviewMapper reviewMapper;

    /**
     * Get all reviews for a book with pagination
     *
     * @param bookId   The book ID
     * @param pageable Pagination information
     * @return Page of ReviewDTOs
     */
    public Page<ReviewDTO> getBookReviews(Long bookId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByBookId(bookId, pageable);
        return reviews.map(reviewMapper::toReviewDTO);
    }

    /**
     * Get all reviews by a user with pagination
     *
     * @param userId   The user ID
     * @param pageable Pagination information
     * @return Page of ReviewDTOs
     */
    public Page<ReviewDTO> getUserReviews(Integer userId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByUserId(userId, pageable);
        return reviews.map(reviewMapper::toReviewDTO);
    }

    /**
     * Get a review by its ID
     *
     * @param reviewId The review ID
     * @return ReviewDTO
     * @throws ResourceNotFoundException if review not found
     */
    public ReviewDTO getReviewById(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
        return reviewMapper.toReviewDTO(review);
    }

    /**
     * Get a user's review for a specific book
     *
     * @param userId The user ID
     * @param bookId The book ID
     * @return ReviewDTO or null if no review exists
     */
    public ReviewDTO getUserReviewForBook(Integer userId, Long bookId) {
        Optional<Review> reviewOpt = reviewRepository.findByUserIdAndBookId(userId, bookId);
        return reviewOpt.map(reviewMapper::toReviewDTO).orElse(null);
    }

    /**
     * Create or update a book review (a user can only have one review per book)
     *
     * @param createReviewDTO Review creation data
     * @param userId         The ID of the user creating the review
     * @return The created or updated ReviewDTO
     * @throws ResourceNotFoundException if book not found
     */
    @Transactional
    public ReviewDTO createReview(CreateReviewDTO createReviewDTO, Integer userId) {
        // Check if book exists
        Book book = bookRepository.findByIdAndIsDeletedFalse(createReviewDTO.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", createReviewDTO.getBookId()));

        // Check if user already has a review for this book
        Optional<Review> existingReview = reviewRepository.findByUserIdAndBookId(userId, createReviewDTO.getBookId());

        Review review;

        if (existingReview.isPresent()) {
            // Update existing review instead of creating a new one
            review = existingReview.get();
            review.setRating(createReviewDTO.getRating());
            review.setComment(createReviewDTO.getComment());
        } else {
            // Create new review
            review = Review.builder()
                    .book(book)
                    .userId(userId)
                    .rating(createReviewDTO.getRating())
                    .comment(createReviewDTO.getComment())
                    .build();
        }

        Review savedReview = reviewRepository.save(review);

        // Update book's average rating and review count
        updateBookRatingStatistics(book.getId());

        return reviewMapper.toReviewDTO(savedReview);
    }

    /**
     * Update an existing review
     *
     * @param reviewId       The review ID
     * @param updateReviewDTO Review update data
     * @param userId         The ID of the user updating the review
     * @return The updated ReviewDTO
     * @throws ResourceNotFoundException if review not found
     * @throws IllegalArgumentException if user doesn't own the review
     */
    @Transactional
    public ReviewDTO updateReview(Long reviewId, UpdateReviewDTO updateReviewDTO, Integer userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        // Check if the user owns this review
        if (!review.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only update your own reviews");
        }

        // Update review fields
        review.setRating(updateReviewDTO.getRating());
        review.setComment(updateReviewDTO.getComment());

        Review updatedReview = reviewRepository.save(review);

        // Update book's average rating
        updateBookRatingStatistics(review.getBook().getId());

        return reviewMapper.toReviewDTO(updatedReview);
    }

    /**
     * Delete a review
     *
     * @param reviewId The review ID
     * @param userId   The ID of the user deleting the review
     * @param isAdmin  Whether the user is an admin
     * @throws ResourceNotFoundException if review not found
     * @throws IllegalArgumentException if user doesn't own the review and isn't an admin
     */
    @Transactional
    public void deleteReview(Long reviewId, Integer userId, boolean isAdmin) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        // Check if the user owns this review or is an admin
        if (!review.getUserId().equals(userId) && !isAdmin) {
            throw new IllegalArgumentException("You can only delete your own reviews");
        }

        Long bookId = review.getBook().getId();

        // Delete the review
        reviewRepository.deleteById(reviewId);

        // Update book's average rating and review count
        updateBookRatingStatistics(bookId);
    }

    /**
     * Update a book's average rating and review count
     *
     * @param bookId The book ID
     */
    private void updateBookRatingStatistics(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", bookId));

        // Count total reviews
        long reviewCount = reviewRepository.countByBookId(bookId);

        // Calculate average rating
        BigDecimal averageRating = BigDecimal.ZERO;
        if (reviewCount > 0) {
            // Get all reviews for this book to calculate average
            Iterable<Review> reviews = reviewRepository.findAll(QReview.review.book.id.eq(bookId));

            int totalRating = 0;
            int count = 0;
            for (Review review : reviews) {
                totalRating += review.getRating();
                count++;
            }

            if (count > 0) {
                averageRating = new BigDecimal(totalRating).divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
            }
        }

        // Update book statistics
        book.setAverageRating(averageRating);
        book.setReviewCount((int) reviewCount);
        bookRepository.save(book);
    }
}
