// src/main/java/com/hieupn/book_review/model/entity/Book.java
package com.hieupn.book_review.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Entity class representing a book in the system.
 * Maps to the 'books' table in the database.
 */
@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Access(AccessType.FIELD)
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "isbn", length = 20, unique = true)
    private String isbn;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "publication_year")
    private Integer publicationYear;

    @Column(name = "publication_year_is_null", nullable = false)
    private Boolean publicationYearIsNull;

    // Many-to-many relationship with Author through BookAuthor
    @OneToMany(mappedBy = "book")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OrderBy("priority DESC") // Order by priority in descending order
    private Set<BookAuthor> bookAuthors = new LinkedHashSet<>();

    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    // One-to-many relationship with Review
    @OneToMany(mappedBy = "book")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Review> reviews = new HashSet<>();

    // One-to-many relationship with Comment
    @OneToMany(mappedBy = "book")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Comment> comments = new HashSet<>();

    // Many-to-many relationship with Genre through BookGenre
    @OneToMany(mappedBy = "book")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OrderBy("count DESC")
    private Set<BookGenre> bookGenres = new HashSet<>();

    /**
     * Add a review to this book
     * @param review The review to add
     */
    public void addReview(Review review) {
        reviews.add(review);
        review.setBook(this);
    }

    /**
     * Add a comment to this book
     * @param comment The comment to add
     */
    public void addComment(Comment comment) {
        comments.add(comment);
        comment.setBook(this);
    }

    /**
     * Add a book-author relationship to this book
     * @param bookAuthor The book-author relationship to add
     */
    public void addBookAuthor(BookAuthor bookAuthor) {
        bookAuthors.add(bookAuthor);
    }

    /**
     * Add a book-genre relationship to this book
     * @param bookGenre The book-genre relationship to add
     */
    public void addBookGenre(BookGenre bookGenre) {
        bookGenres.add(bookGenre);
    }
}