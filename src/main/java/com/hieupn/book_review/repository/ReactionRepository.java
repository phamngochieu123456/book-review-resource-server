package com.hieupn.book_review.repository;

import com.hieupn.book_review.model.entity.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, Long>, QuerydslPredicateExecutor<Reaction> {
    // Find a user's reaction for a specific item
    Optional<Reaction> findByUserIdAndReactableIdAndReactableType(Integer userId, Long reactableId, String reactableType);

    // Count reactions by type for a specific reactable item
    @Query("SELECT r.reactionType, COUNT(r) FROM Reaction r WHERE r.reactableId = :reactableId AND r.reactableType = :reactableType GROUP BY r.reactionType")
    List<Object[]> countReactionsByTypeForReactable(Long reactableId, String reactableType);
}
