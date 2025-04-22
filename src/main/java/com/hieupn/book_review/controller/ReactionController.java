package com.hieupn.book_review.controller;

import com.hieupn.book_review.model.dto.CreateReactionDTO;
import com.hieupn.book_review.model.dto.ReactionDTO;
import com.hieupn.book_review.model.dto.ReactionSummaryDTO;
import com.hieupn.book_review.service.ReactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for reaction-related operations
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;

    /**
     * Toggle a reaction (add if not exists, remove if exists with same type, update if exists with different type)
     *
     * @param createReactionDTO Reaction data
     * @return Response indicating whether the reaction was added/updated or removed
     */
    @PostMapping("/reactions")
    public ResponseEntity<Map<String, Object>> toggleReaction(@Valid @RequestBody CreateReactionDTO createReactionDTO) {
        // TODO: Get current user ID from security context
        Integer currentUserId = 1; // For demonstration purposes

        boolean added = reactionService.toggleReaction(createReactionDTO, currentUserId);

        Map<String, Object> response = new HashMap<>();
        if (added) {
            response.put("status", "success");
            response.put("action", "added");
            response.put("message", "Reaction added/updated successfully");
        } else {
            response.put("status", "success");
            response.put("action", "removed");
            response.put("message", "Reaction removed successfully");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get reaction summary for a specific item
     *
     * @param reactableType Type of the item being reacted to (e.g., "comment")
     * @param reactableId   ID of the item being reacted to
     * @return ReactionSummaryDTO with counts by type
     */
    @GetMapping("/reactions/{reactableType}/{reactableId}")
    public ResponseEntity<ReactionSummaryDTO> getReactionSummary(
            @PathVariable String reactableType,
            @PathVariable Long reactableId) {

        ReactionSummaryDTO summary = reactionService.getReactionSummary(reactableId, reactableType);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get current user's reaction for a specific item
     *
     * @param reactableType Type of the item being reacted to (e.g., "comment")
     * @param reactableId   ID of the item being reacted to
     * @return ReactionDTO or 404 if no reaction exists
     */
    @GetMapping("/reactions/{reactableType}/{reactableId}/my-reaction")
    public ResponseEntity<ReactionDTO> getCurrentUserReaction(
            @PathVariable String reactableType,
            @PathVariable Long reactableId) {

        // TODO: Get current user ID from security context
        Integer currentUserId = 1; // For demonstration purposes

        ReactionDTO reaction = reactionService.getUserReaction(reactableId, reactableType, currentUserId);

        if (reaction == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(reaction);
    }
}
