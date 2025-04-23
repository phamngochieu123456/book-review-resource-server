package com.hieupn.book_review.controller;

import com.hieupn.book_review.exception.UnauthorizedException;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/books/{bookId}/reviews")
    @PreAuthorize("hasAuthority('READ_REVIEW')")
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

    @GetMapping("/books/{bookId}/my-review")
    @PreAuthorize("hasAuthority('READ_REVIEW')")
    public ResponseEntity<ReviewDTO> getCurrentUserReviewForBook(@PathVariable Long bookId) {
        // Get currentUserId from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();

        // Extract user_id from JWT token safely using Number.intValue()
        Number userIdNumber = jwt.getClaim("user_id");
        if (userIdNumber == null) {
            throw new UnauthorizedException("User ID not found in token");
        }

        Integer currentUserId = userIdNumber.intValue();

        ReviewDTO review = reviewService.getUserReviewForBook(currentUserId, bookId);

        if (review == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(review);
    }

    @GetMapping("/users/{userId}/reviews")
    @PreAuthorize("hasAuthority('READ_REVIEW')")
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

    @GetMapping("/reviews/{reviewId}")
    @PreAuthorize("hasAuthority('READ_REVIEW')")
    public ResponseEntity<ReviewDTO> getReviewById(@PathVariable Long reviewId) {
        ReviewDTO review = reviewService.getReviewById(reviewId);
        return ResponseEntity.ok(review);
    }

    @PostMapping("/reviews")
    @PreAuthorize("hasAuthority('WRITE_REVIEW')")
    public ResponseEntity<ReviewDTO> createOrUpdateReview(@Valid @RequestBody CreateReviewDTO createReviewDTO) {
        // Get currentUserId from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();

        // Extract user_id from JWT token safely using Number.intValue()
        Number userIdNumber = jwt.getClaim("user_id");
        if (userIdNumber == null) {
            throw new UnauthorizedException("User ID not found in token");
        }

        Integer currentUserId = userIdNumber.intValue();

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

    @PutMapping("/reviews/{reviewId}")
    @PreAuthorize("hasAuthority('WRITE_REVIEW')")
    public ResponseEntity<ReviewDTO> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewDTO updateReviewDTO) {

        // Get currentUserId from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();

        // Extract user_id from JWT token safely using Number.intValue()
        Number userIdNumber = jwt.getClaim("user_id");
        if (userIdNumber == null) {
            throw new UnauthorizedException("User ID not found in token");
        }

        Integer currentUserId = userIdNumber.intValue();

        ReviewDTO updatedReview = reviewService.updateReview(reviewId, updateReviewDTO, currentUserId);

        return ResponseEntity.ok(updatedReview);
    }

    @DeleteMapping("/reviews/{reviewId}")
    @PreAuthorize("hasAuthority('WRITE_REVIEW')")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        // Get currentUserId from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();

        // Extract user_id from JWT token safely using Number.intValue()
        Number userIdNumber = jwt.getClaim("user_id");
        if (userIdNumber == null) {
            throw new UnauthorizedException("User ID not found in token");
        }

        Integer currentUserId = userIdNumber.intValue();

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        reviewService.deleteReview(reviewId, currentUserId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/reviews/{reviewId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> adminDeleteReview(@PathVariable Long reviewId) {
        // Get currentUserId from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();

        // Extract user_id from JWT token safely using Number.intValue()
        Number userIdNumber = jwt.getClaim("user_id");
        if (userIdNumber == null) {
            throw new UnauthorizedException("User ID not found in token");
        }

        Integer currentUserId = userIdNumber.intValue();

        boolean isAdmin = true; // Admin role is required for this endpoint

        reviewService.deleteReview(reviewId, currentUserId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    private boolean isValidSortField(String field) {
        return field.equals("createdAt") ||
                field.equals("rating");
    }
}
