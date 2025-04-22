package com.hieupn.book_review.service;

import com.hieupn.book_review.mapper.ReactionMapper;
import com.hieupn.book_review.model.dto.CreateReactionDTO;
import com.hieupn.book_review.model.dto.ReactionDTO;
import com.hieupn.book_review.model.dto.ReactionSummaryDTO;
import com.hieupn.book_review.model.entity.Reaction;
import com.hieupn.book_review.repository.ReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing reactions (likes, etc.)
 */
@Service
@RequiredArgsConstructor
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final ReactionMapper reactionMapper;

    /**
     * Toggle a reaction (add if not exists, remove if exists)
     *
     * @param createReactionDTO Reaction data
     * @param userId           The ID of the user creating the reaction
     * @return true if reaction was added, false if it was removed
     */
    @Transactional
    public boolean toggleReaction(CreateReactionDTO createReactionDTO, Integer userId) {
        Optional<Reaction> existingReaction = reactionRepository.findByUserIdAndReactableIdAndReactableType(
                userId,
                createReactionDTO.getReactableId(),
                createReactionDTO.getReactableType()
        );

        if (existingReaction.isPresent()) {
            Reaction reaction = existingReaction.get();

            // If same reaction type, remove it (toggle off)
            if (reaction.getReactionType().equals(createReactionDTO.getReactionType())) {
                reactionRepository.delete(reaction);
                return false; // Reaction removed
            } else {
                // If different reaction type, update it
                reaction.setReactionType(createReactionDTO.getReactionType());
                reactionRepository.save(reaction);
                return true; // Reaction updated
            }
        } else {
            // Create new reaction
            Reaction reaction = Reaction.builder()
                    .userId(userId)
                    .reactableId(createReactionDTO.getReactableId())
                    .reactableType(createReactionDTO.getReactableType())
                    .reactionType(createReactionDTO.getReactionType())
                    .build();

            reactionRepository.save(reaction);
            return true; // Reaction added
        }
    }

    /**
     * Get reaction summary (count by type) for a specific item
     *
     * @param reactableId   The ID of the item being reacted to
     * @param reactableType The type of the item being reacted to
     * @return ReactionSummaryDTO with counts by type
     */
    public ReactionSummaryDTO getReactionSummary(Long reactableId, String reactableType) {
        List<Object[]> reactionCounts = reactionRepository.countReactionsByTypeForReactable(reactableId, reactableType);

        Map<String, Long> countsByType = new HashMap<>();
        long totalCount = 0;

        for (Object[] result : reactionCounts) {
            String type = (String) result[0];
            Long count = (Long) result[1];

            countsByType.put(type, count);
            totalCount += count;
        }

        return new ReactionSummaryDTO(reactableId, reactableType, countsByType, totalCount);
    }

    /**
     * Get a user's reaction for a specific item
     *
     * @param reactableId   The ID of the item being reacted to
     * @param reactableType The type of the item being reacted to
     * @param userId        The ID of the user
     * @return ReactionDTO or null if no reaction exists
     */
    public ReactionDTO getUserReaction(Long reactableId, String reactableType, Integer userId) {
        Optional<Reaction> reactionOpt = reactionRepository.findByUserIdAndReactableIdAndReactableType(
                userId, reactableId, reactableType);

        return reactionOpt.map(reactionMapper::toReactionDTO).orElse(null);
    }
}
