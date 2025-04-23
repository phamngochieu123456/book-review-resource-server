package com.hieupn.book_review.aspect;

import com.hieupn.book_review.exception.AccessDeniedException;
import com.hieupn.book_review.model.entity.Review;
import com.hieupn.book_review.repository.ReviewRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ReviewOwnershipAspect {

    private final ReviewRepository reviewRepository;

    public ReviewOwnershipAspect(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Around("execution(* com.hieupn.book_review.service.ReviewService.updateReview(..)) && args(reviewId, *, currentUserId)")
    public Object checkReviewOwnership(ProceedingJoinPoint joinPoint, Long reviewId, Integer currentUserId) throws Throwable {
        Review review = reviewRepository.findById(reviewId)
                .orElse(null);

        if (review != null && !review.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("You can only edit your own reviews");
        }

        return joinPoint.proceed();
    }
}
