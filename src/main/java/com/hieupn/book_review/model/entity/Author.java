package com.hieupn.book_review.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity class representing an author in the system.
 * Maps to the 'authors' table in the database.
 */
@Entity
@Table(name = "authors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Access(AccessType.FIELD)
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "biography", columnDefinition = "text")
    private String biography;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    // Many-to-many relationship with Book through BookAuthor
    @OneToMany(mappedBy = "author")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<BookAuthor> bookAuthors = new HashSet<>();

    /**
     * Add a book-author relationship to this author
     * @param bookAuthor The book-author relationship to add
     */
    public void addBookAuthor(BookAuthor bookAuthor) {
        bookAuthors.add(bookAuthor);
    }
}
