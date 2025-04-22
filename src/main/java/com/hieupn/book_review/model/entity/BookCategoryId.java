package com.hieupn.book_review.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/**
 * Embedded ID class for the composite primary key of BookCategory
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookCategoryId implements Serializable {

    @Column(name = "book_id")
    private Long bookId;

    @Column(name = "category_id")
    private Long categoryId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookCategoryId id = (BookCategoryId) o;
        return Objects.equals(bookId, id.bookId) &&
                Objects.equals(categoryId, id.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookId, categoryId);
    }
}
