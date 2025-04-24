// src/main/java/com/hieupn/book_review/model/entity/BookGenreId.java
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
 * Embedded ID class for the composite primary key of BookGenre
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookGenreId implements Serializable {

    @Column(name = "book_id")
    private Long bookId;

    @Column(name = "genre_id")
    private Long genreId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookGenreId id = (BookGenreId) o;
        return Objects.equals(bookId, id.bookId) &&
                Objects.equals(genreId, id.genreId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookId, genreId);
    }
}
