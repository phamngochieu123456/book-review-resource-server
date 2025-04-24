// src/main/java/com/hieupn/book_review/model/entity/Genre.java
package com.hieupn.book_review.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity class representing a genre in the system.
 * A genre is a special type of category used for filtering books.
 * Maps to the 'genres' table in the database.
 */
@Entity
@Table(name = "genres")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Access(AccessType.FIELD)
public class Genre {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    // Many-to-many relationship with Book through BookGenre
    @OneToMany(mappedBy = "genre")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<BookGenre> bookGenres = new HashSet<>();

    /**
     * Add a book-genre relationship to this genre
     * @param bookGenre The book-genre relationship to add
     */
    public void addBookGenre(BookGenre bookGenre) {
        bookGenres.add(bookGenre);
    }
}
