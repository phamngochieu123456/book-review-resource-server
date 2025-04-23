package com.hieupn.book_review.controller;

import com.hieupn.book_review.exception.UnauthorizedException;
import com.hieupn.book_review.model.dto.CreateReactionDTO;
import com.hieupn.book_review.model.dto.ReactionDTO;
import com.hieupn.book_review.model.dto.ReactionSummaryDTO;
import com.hieupn.book_review.service.ReactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;

    @PostMapping("/reactions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> toggleReaction(@Valid @RequestBody CreateReactionDTO createReactionDTO) {
        // Get currentUserId from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();

        // Extract user_id from JWT token safely using Number.intValue()
        Number userIdNumber = jwt.getClaim("user_id");
        if (userIdNumber == null) {
            throw new UnauthorizedException("User ID not found in token");
        }

        Integer currentUserId = userIdNumber.intValue();

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

    @GetMapping("/reactions/{reactableType}/{reactableId}")
    public ResponseEntity<ReactionSummaryDTO> getReactionSummary(
            @PathVariable String reactableType,
            @PathVariable Long reactableId) {

        ReactionSummaryDTO summary = reactionService.getReactionSummary(reactableId, reactableType);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/reactions/{reactableType}/{reactableId}/my-reaction")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReactionDTO> getCurrentUserReaction(
            @PathVariable String reactableType,
            @PathVariable Long reactableId) {

        // Get currentUserId from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();

        // Extract user_id from JWT token safely using Number.intValue()
        Number userIdNumber = jwt.getClaim("user_id");
        if (userIdNumber == null) {
            throw new UnauthorizedException("User ID not found in token");
        }

        Integer currentUserId = userIdNumber.intValue();

        ReactionDTO reaction = reactionService.getUserReaction(reactableId, reactableType, currentUserId);

        if (reaction == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(reaction);
    }
}
