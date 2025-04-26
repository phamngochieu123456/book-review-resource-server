package com.hieupn.book_review.repository;

import com.hieupn.book_review.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * Find a user by username
     *
     * @param username The username to search for
     * @return Optional containing the user if found, otherwise empty Optional
     */
    Optional<User> findByUsername(String username);

    /**
     * Find users by their IDs
     *
     * @param userIds List of user IDs to find
     * @return List of found users
     */
    List<User> findByUserIdIn(List<Integer> userIds);
}
