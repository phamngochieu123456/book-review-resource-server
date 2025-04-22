package com.hieupn.book_review.controller;

import com.hieupn.book_review.model.dto.CreateReviewDTO;
import com.hieupn.book_review.model.dto.ReviewDTO;
import com.hieupn.book_review.model.dto.UpdateReviewDTO;
import com.hieupn.book_review.model.response.PagedResponse;
import com.hieupn.book_review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * REST controller for review-related operations
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Get all reviews for a book
     *
     * @param bookId   Book ID
     * @param page     Page number (zero-based)
     * @param size     Items per page
     * @param sortBy   Field to sort by (createdAt, rating)
     * @param sortDir  Sort direction (asc or desc)
     * @return PagedResponse of ReviewDTOs
     */
    @GetMapping("/books/{bookId}/reviews")
    public ResponseEntity<PagedResponse<ReviewDTO>> getBookReviews(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        // Validate sortBy parameter
        if (!isValidSortField(sortBy)) {
            sortBy = "createdAt";
        }

        // Create sort and pageable objects
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        // Get reviews from service
        Page<ReviewDTO> reviews = reviewService.getBookReviews(bookId, pageable);

        // Wrap the Page in our custom PagedResponse
        PagedResponse<ReviewDTO> response = new PagedResponse<>(reviews);

        return ResponseEntity.ok(response);
    }

    /**
     * Get current user's review for a book
     *
     * @param bookId Book ID
     * @return ReviewDTO or 404 if no review exists
     */
    @GetMapping("/books/{bookId}/my-review")
    public ResponseEntity<ReviewDTO> getCurrentUserReviewForBook(@PathVariable Long bookId) {
        // TODO: Get current user ID from security context
        Integer currentUserId = 1; // For demonstration purposes

        ReviewDTO review = reviewService.getUserReviewForBook(currentUserId, bookId);

        if (review == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(review);
    }

    /**
     * Get all reviews by a user
     *
     * @param userId   User ID
     * @param page     Page number (zero-based)
     * @param size     Items per page
     * @param sortBy   Field to sort by (createdAt, rating)
     * @param sortDir  Sort direction (asc or desc)
     * @return PagedResponse of ReviewDTOs
     */
    @GetMapping("/users/{userId}/reviews")
    public ResponseEntity<PagedResponse<ReviewDTO>> getUserReviews(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        // Validate sortBy parameter
        if (!isValidSortField(sortBy)) {
            sortBy = "createdAt";
        }

        // Create sort and pageable objects
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        // Get reviews from service
        Page<ReviewDTO> reviews = reviewService.getUserReviews(userId, pageable);

        // Wrap the Page in our custom PagedResponse
        PagedResponse<ReviewDTO> response = new PagedResponse<>(reviews);

        return ResponseEntity.ok(response);
    }

    /**
     * Get a review by its ID
     *
     * @param reviewId Review ID
     * @return ReviewDTO
     */
    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewDTO> getReviewById(@PathVariable Long reviewId) {
        ReviewDTO review = reviewService.getReviewById(reviewId);
        return ResponseEntity.ok(review);
    }

    /**
     * Create or update a review
     * If the user already has a review for this book, it will be updated
     *
     * @param createReviewDTO Review creation data
     * @return ReviewDTO for the created/updated review
     */
    @PostMapping("/reviews")
    public ResponseEntity<ReviewDTO> createOrUpdateReview(@Valid @RequestBody CreateReviewDTO createReviewDTO) {
        // TODO: Get current user ID from security context
        Integer currentUserId = 1; // For demonstration purposes

        ReviewDTO createdReview = reviewService.createReview(createReviewDTO, currentUserId);

        // Check if this is a new review or an update
        boolean isUpdate = reviewService.getUserReviewForBook(currentUserId, createReviewDTO.getBookId()) != null;

        if (isUpdate) {
            // If it's an update, return 200 OK
            return ResponseEntity.ok(createdReview);
        } else {
            // If it's a new review, return 201 Created with location
            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(createdReview.getId())
                    .toUri();

            return ResponseEntity.created(location).body(createdReview);
        }
    }

    /**
     * Update an existing review
     *
     * @param reviewId       Review ID
     * @param updateReviewDTO Review update data
     * @return ReviewDTO for the updated review
     */
    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewDTO> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewDTO updateReviewDTO) {

        // TODO: Get current user ID from security context
        Integer currentUserId = 1; // For demonstration purposes

        ReviewDTO updatedReview = reviewService.updateReview(reviewId, updateReviewDTO, currentUserId);

        return ResponseEntity.ok(updatedReview);
    }

    /**
     * Delete a review
     *
     * @param reviewId Review ID
     * @return No content response
     */
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        // TODO: Get current user ID and roles from security context
        Integer currentUserId = 1; // For demonstration purposes
        boolean isAdmin = false; // For demonstration purposes

        reviewService.deleteReview(reviewId, currentUserId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    /**
     * Admin: Delete any review
     *
     * @param reviewId Review ID
     * @return No content response
     */
    @DeleteMapping("/admin/reviews/{reviewId}")
    public ResponseEntity<Void> adminDeleteReview(@PathVariable Long reviewId) {
        // TODO: Get current user ID from security context and check if admin
        Integer currentUserId = 1; // For demonstration purposes
        boolean isAdmin = true; // Admin role is required for this endpoint

        reviewService.deleteReview(reviewId, currentUserId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    /**
     * Check if the sort field is valid
     *
     * @param field The field to check
     * @return true if valid, false otherwise
     */
    private boolean isValidSortField(String field) {
        return field.equals("createdAt") ||
                field.equals("rating");
    }
}
