package com.hieupn.book_review.service;

import com.hieupn.book_review.exception.ResourceNotFoundException;
import com.hieupn.book_review.mapper.CommentMapper;
import com.hieupn.book_review.model.dto.CommentDTO;
import com.hieupn.book_review.model.dto.CreateCommentDTO;
import com.hieupn.book_review.model.dto.UpdateCommentDTO;
import com.hieupn.book_review.model.entity.Book;
import com.hieupn.book_review.model.entity.Comment;
import com.hieupn.book_review.repository.BookRepository;
import com.hieupn.book_review.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing book comments
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final BookRepository bookRepository;
    private final CommentMapper commentMapper;

    /**
     * Get top-level comments for a book with pagination
     *
     * @param bookId   The book ID
     * @param pageable Pagination information
     * @return Page of CommentDTOs
     */
    public Page<CommentDTO> getBookComments(Long bookId, Pageable pageable) {
        Page<Comment> comments = commentRepository.findByBookIdAndParentCommentIsNullAndIsDeletedFalse(bookId, pageable);
        return comments.map(comment -> {
            CommentDTO dto = commentMapper.toCommentDTO(comment);
            // Add child comments
            dto.setReplies(getChildComments(comment.getId()));
            return dto;
        });
    }

    /**
     * Get child comments (replies) for a parent comment
     *
     * @param parentCommentId The parent comment ID
     * @return List of CommentDTOs
     */
    public List<CommentDTO> getChildComments(Long parentCommentId) {
        List<Comment> childComments = commentRepository.findByParentCommentIdAndIsDeletedFalse(parentCommentId);
        return childComments.stream()
                .map(commentMapper::toCommentDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a comment by its ID
     *
     * @param commentId The comment ID
     * @return CommentDTO
     * @throws ResourceNotFoundException if comment not found or is deleted
     */
    public CommentDTO getCommentById(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        if (comment.getIsDeleted()) {
            throw new ResourceNotFoundException("Comment", "id", commentId);
        }

        CommentDTO dto = commentMapper.toCommentDTO(comment);

        // Add child comments if this is a parent comment
        if (comment.getParentComment() == null) {
            dto.setReplies(getChildComments(comment.getId()));
        }

        return dto;
    }

    /**
     * Create a new comment
     *
     * @param createCommentDTO Comment creation data
     * @param userId          The ID of the user creating the comment
     * @return The created CommentDTO
     * @throws ResourceNotFoundException if book or parent comment not found
     */
    @Transactional
    public CommentDTO createComment(CreateCommentDTO createCommentDTO, Integer userId) {
        // Check if book exists
        Book book = bookRepository.findByIdAndIsDeletedFalse(createCommentDTO.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", createCommentDTO.getBookId()));

        Comment.CommentBuilder commentBuilder = Comment.builder()
                .content(createCommentDTO.getContent())
                .userId(userId)
                .book(book)
                .isDeleted(false);

        // If this is a reply, set parent comment
        if (createCommentDTO.getParentCommentId() != null) {
            Comment parentComment = commentRepository.findById(createCommentDTO.getParentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Comment", "id", createCommentDTO.getParentCommentId()));

            // Ensure parent comment is not deleted
            if (parentComment.getIsDeleted()) {
                throw new ResourceNotFoundException("Parent Comment", "id", createCommentDTO.getParentCommentId());
            }

            commentBuilder.parentComment(parentComment);
        }

        Comment comment = commentBuilder.build();
        Comment savedComment = commentRepository.save(comment);

        return commentMapper.toCommentDTO(savedComment);
    }

    /**
     * Update an existing comment
     *
     * @param commentId        The comment ID
     * @param updateCommentDTO Comment update data
     * @param userId           The ID of the user updating the comment
     * @return The updated CommentDTO
     * @throws ResourceNotFoundException if comment not found or is deleted
     * @throws IllegalArgumentException  if user doesn't own the comment
     */
    @Transactional
    public CommentDTO updateComment(Long commentId, UpdateCommentDTO updateCommentDTO, Integer userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        // Check if comment is deleted
        if (comment.getIsDeleted()) {
            throw new ResourceNotFoundException("Comment", "id", commentId);
        }

        // Check if the user owns this comment
        if (!comment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only update your own comments");
        }

        // Update comment content
        comment.setContent(updateCommentDTO.getContent());
        Comment updatedComment = commentRepository.save(comment);

        return commentMapper.toCommentDTO(updatedComment);
    }

    /**
     * Soft delete a comment (mark as deleted)
     *
     * @param commentId The comment ID
     * @param userId    The ID of the user deleting the comment
     * @param isAdmin   Whether the user is an admin
     * @throws ResourceNotFoundException if comment not found or is already deleted
     * @throws IllegalArgumentException  if user doesn't own the comment and isn't an admin
     */
    @Transactional
    public void softDeleteComment(Long commentId, Integer userId, boolean isAdmin) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        // Check if comment is already deleted
        if (comment.getIsDeleted()) {
            throw new ResourceNotFoundException("Comment", "id", commentId);
        }

        // Check if the user owns this comment or is an admin
        if (!comment.getUserId().equals(userId) && !isAdmin) {
            throw new IllegalArgumentException("You can only delete your own comments");
        }

        // Soft delete by setting isDeleted flag
        comment.setIsDeleted(true);
        commentRepository.save(comment);
    }

    /**
     * Hard delete a comment (admin only)
     *
     * @param commentId The comment ID
     * @throws ResourceNotFoundException if comment not found
     */
    @Transactional
    public void hardDeleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        // Delete the comment
        commentRepository.deleteById(commentId);
    }
}
