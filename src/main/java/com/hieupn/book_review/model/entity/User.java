package com.hieupn.book_review.model.entity;

import com.hieupn.book_review.converter.StatusEnumConverter;
import com.hieupn.book_review.model.enums.UserStatusType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity class representing a user in the system.
 * Maps to the 'users' table in the database.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Access(AccessType.FIELD)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "bio", columnDefinition = "text")
    private String bio;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Convert(converter = StatusEnumConverter.class)
    @Column(name = "status", nullable = false, length = 50)
    private UserStatusType status;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // One-to-many relationship with reviews, but with unique constraint per book
    // A user can have multiple reviews but only one per book
    // This is enforced by the unique constraint in the database: user_book_unique (user_id, book_id)
    @OneToMany(mappedBy = "userId")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Review> reviews = new HashSet<>();

    // One-to-many relationship with comments
    @OneToMany(mappedBy = "userId")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Comment> comments = new HashSet<>();

    // One-to-many relationship with reactions
    @OneToMany(mappedBy = "userId")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Reaction> reactions = new HashSet<>();

    /**
     * Check if the user account is active
     *
     * @return true if user status is active, false otherwise
     */
    public boolean isActive() {
        return status.equals(UserStatusType.ACTIVE);
    }
}
