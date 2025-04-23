package com.hieupn.book_review.aspect;

import com.hieupn.book_review.exception.AccessDeniedException;
import com.hieupn.book_review.model.entity.Comment;
import com.hieupn.book_review.repository.CommentRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CommentOwnershipAspect {

    private final CommentRepository commentRepository;

    public CommentOwnershipAspect(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @Around("execution(* com.hieupn.book_review.service.CommentService.updateComment(..)) && args(commentId, *, currentUserId)")
    public Object checkCommentOwnership(ProceedingJoinPoint joinPoint, Long commentId, Integer currentUserId) throws Throwable {
        Comment comment = commentRepository.findById(commentId)
                .orElse(null);

        if (comment != null && !comment.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("You can only edit your own comments");
        }

        return joinPoint.proceed();
    }
}
