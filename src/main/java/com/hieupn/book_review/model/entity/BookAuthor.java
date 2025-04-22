package com.hieupn.book_review.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity class representing the many-to-many relationship between books and authors.
 * Maps to the 'book_authors' table in the database.
 */
@Entity
@Table(name = "book_authors")
@Getter
@Setter
@NoArgsConstructor
@Access(AccessType.FIELD)
public class BookAuthor {

    @EmbeddedId
    private BookAuthorId id = new BookAuthorId();

    @Column(nullable = false)
    private Integer priority;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "assigned_by", nullable = false)
    private Integer assignedBy;

    @ManyToOne
    @JoinColumn(
            name = "book_id",
            insertable = false, updatable = false)
    private Book book;

    @ManyToOne
    @JoinColumn(
            name = "author_id",
            insertable = false, updatable = false)
    private Author author;

    /**
     * Constructor for creating a BookAuthor relationship
     *
     * @param priority The priority
     * @param assignedBy User ID who assigned this author to the book
     * @param book The book
     * @param author The author
     */
    public BookAuthor(Integer priority, Integer assignedBy, Book book, Author author) {
        // Set fields
        this.priority = priority;
        this.assignedBy = assignedBy;
        this.book = book;
        this.author = author;

        // Set identifier values
        this.id = new BookAuthorId(book.getId(), author.getId());

        // Ensure bidirectional integrity
        book.addBookAuthor(this);
        author.addBookAuthor(this);

        // Note: createdAt and updatedAt are managed by DB default values
    }
}
