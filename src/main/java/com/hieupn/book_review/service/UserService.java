package com.hieupn.book_review.service;

import com.hieupn.book_review.model.entity.User;
import com.hieupn.book_review.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for user-related operations
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Get a user by ID
     *
     * @param userId The user ID
     * @return Optional containing the user if found, otherwise empty Optional
     */
    public Optional<User> getUserById(Integer userId) {
        return userRepository.findById(userId);
    }

    /**
     * Get a user's username by ID
     *
     * @param userId The user ID
     * @return The username if found, otherwise null
     */
    public String getUsernameById(Integer userId) {
        return userRepository.findById(userId)
                .map(User::getUsername)
                .orElse(null);
    }

    /**
     * Get usernames for a list of user IDs
     *
     * @param userIds List of user IDs
     * @return Map of user ID to username
     */
    public Map<Integer, String> getUsernamesByIds(List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<User> users = userRepository.findByUserIdIn(userIds);

        return users.stream()
                .collect(Collectors.toMap(
                        User::getUserId,
                        User::getUsername,
                        (username1, username2) -> username1)); // In case of duplicates, keep the first one
    }

    /**
     * Populate usernames for a collection of DTOs with user information
     *
     * @param dtos Collection of DTOs that need username population
     * @param userIdGetter Function to extract the user ID from a DTO
     * @param usernameSetter Function to set the username in a DTO
     * @param <T> Type of DTO
     */
    public <T> void populateUsernames(Collection<T> dtos,
                                      Function<T, Integer> userIdGetter,
                                      BiConsumer<T, String> usernameSetter) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }

        // Extract unique user IDs
        List<Integer> userIds = dtos.stream()
                .map(userIdGetter)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return;
        }

        // Fetch usernames for those IDs
        Map<Integer, String> usernameMap = getUsernamesByIds(userIds);

        // Update usernames in the DTOs
        dtos.forEach(dto -> {
            Integer userId = userIdGetter.apply(dto);
            if (userId != null) {
                String username = usernameMap.get(userId);
                if (username != null) {
                    usernameSetter.accept(dto, username);
                }
            }
        });
    }
}
