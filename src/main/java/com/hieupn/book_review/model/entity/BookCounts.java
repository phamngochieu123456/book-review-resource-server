package com.hieupn.book_review.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity class representing book counts in the system.
 * Maps to the 'book_counts' table in the database.
 */
@Entity
@Table(name = "book_counts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Access(AccessType.FIELD)
public class BookCounts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "count_name", nullable = false, unique = true)
    private String countName;

    @Column(name = "current_count")
    private Integer currentCount;
}
