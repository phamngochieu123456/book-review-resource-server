package com.hieupn.book_review.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity class representing the many-to-many relationship between books and categories.
 * Maps to the 'book_categories' table in the database.
 */
@Entity
@Table(name = "book_categories")
@Getter
@Setter
@NoArgsConstructor
@Access(AccessType.FIELD)
public class BookCategory {

    @EmbeddedId
    private BookCategoryId id = new BookCategoryId();

    @Column(nullable = false)
    private Integer count;

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
            name = "category_id",
            insertable = false, updatable = false)
    private Category category;

    /**
     * Constructor for creating a BookCategory relationship
     *
     * @param assignedBy User ID who assigned this category to the book
     * @param book The book
     * @param category The category
     */
    public BookCategory(Integer assignedBy, Book book, Category category) {
        // Set fields
        this.assignedBy = assignedBy;
        this.book = book;
        this.category = category;
        this.count = 1; // Default count to 1

        // Set identifier values
        this.id = new BookCategoryId(book.getId(), category.getId());

        // Ensure bidirectional integrity
        book.addBookCategory(this);
        category.addBookCategory(this);

        // Note: createdAt and updatedAt are managed by DB default values
    }
}
