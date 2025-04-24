// src/main/java/com/hieupn/book_review/model/entity/BookGenre.java
package com.hieupn.book_review.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity class representing the many-to-many relationship between books and genres.
 * Contains denormalized fields from Book entity for query optimization.
 * Maps to the 'book_genres' table in the database.
 */
@Entity
@Table(name = "book_genres")
@Getter
@Setter
@NoArgsConstructor
@Access(AccessType.FIELD)
public class BookGenre {

    @EmbeddedId
    private BookGenreId id = new BookGenreId();

    @Column(nullable = false)
    private Integer count;

    /**
     * Denormalized field from Book entity for query optimization
     */
    @Column(name = "title", nullable = false)
    private String title;

    /**
     * Denormalized field from Book entity for query optimization
     */
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    /**
     * Denormalized field from Book entity for query optimization
     */
    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;

    /**
     * Denormalized field from Book entity for query optimization
     */
    @Column(name = "publication_year")
    private Integer publicationYear;

    @Column(name = "publication_year_is_null", nullable = false)
    private Boolean publicationYearIsNull;

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
            name = "genre_id",
            insertable = false, updatable = false)
    private Genre genre;

    /**
     * Constructor for creating a BookGenre relationship
     *
     * @param count Number of associations (typically 1)
     * @param assignedBy User ID who assigned this genre to the book
     * @param book The book
     * @param genre The genre
     */
    public BookGenre(Integer count, Integer assignedBy, Book book, Genre genre) {
        // Set fields
        this.count = count;
        this.assignedBy = assignedBy;
        this.book = book;
        this.genre = genre;

        // Copy denormalized fields from book for query optimization
        this.title = book.getTitle();
        this.isDeleted = book.getIsDeleted();
        this.averageRating = book.getAverageRating();
        this.publicationYear = book.getPublicationYear();

        // Set identifier values
        this.id = new BookGenreId(book.getId(), genre.getId());

        // Ensure bidirectional integrity
        book.addBookGenre(this);
        genre.addBookGenre(this);

        // Note: createdAt and updatedAt are managed by DB default values
    }

    /**
     * Update denormalized fields from the associated book
     * Call this method when book data changes
     */
    public void updateDenormalizedFields() {
        if (book != null) {
            this.title = book.getTitle();
            this.isDeleted = book.getIsDeleted();
            this.averageRating = book.getAverageRating();
            this.publicationYear = book.getPublicationYear();
        }
    }
}
