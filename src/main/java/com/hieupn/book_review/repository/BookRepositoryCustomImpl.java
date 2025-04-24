// src/main/java/com/hieupn/book_review/repository/BookRepositoryCustomImpl.java
package com.hieupn.book_review.repository;

import com.hieupn.book_review.model.entity.Book;
import com.hieupn.book_review.model.entity.BookCounts;
import com.hieupn.book_review.model.entity.QBook;
import com.hieupn.book_review.model.entity.QBookGenre;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BookRepositoryCustomImpl implements BookRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private BookCountsRepository bookCountsRepository;

    @Override
    public Page<Book> findAllNonDeletedBooks(Long genreId, Long authorId, String searchTerm, Pageable pageable) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QBook qBook = QBook.book;
        QBookGenre qBookGenre = QBookGenre.bookGenre;

        // Build common where conditions
        BooleanBuilder whereClause = new BooleanBuilder();

        // Apply filters to where clause if provided
        boolean hasFilters = false;

        // Apply genre filter if provided
        if (genreId != null) {
            whereClause.and(qBookGenre.genre.id.eq(genreId));
            // We don't need to check bookGenre.isDeleted here since it's a denormalized field
            // Rather, we use the denormalized field for more efficient filtering
            whereClause.and(qBookGenre.isDeleted.eq(false)); // Use the denormalized field
            hasFilters = true;
        }
        else {
            whereClause.and(qBook.isDeleted.eq(false)); // This is the book's isDeleted flag
        }

        // Add title search condition using LIKE if searchTerm is provided
        if (StringUtils.hasText(searchTerm)) {
            // Use likeIgnoreCase for case-insensitive search, starts with searchTerm
            whereClause.and(qBookGenre.title.like(searchTerm + "%"));
            hasFilters = true;
        }

        // Determine total count based on whether filters are applied
        long total;

        // If no filters are applied, use the pre-calculated count from book_counts table
        if (!hasFilters) {
            // Use the book_counts table for total count when no filters are applied
            Optional<BookCounts> bookCountOpt = bookCountsRepository.findByCountName("active_books");
            if (bookCountOpt.isPresent()) {
                total = bookCountOpt.get().getCurrentCount();
            } else {
                JPAQuery<Long> countQuery = createCountQuery(queryFactory, qBook, qBookGenre, genreId, whereClause);
                Long countResult = countQuery.fetchOne(); // fetchOne() returns null if no results
                total = (countResult != null) ? countResult : 0L;
            }
        } else {
            // --- Count Query for filtered results: ---
            JPAQuery<Long> countQuery = createCountQuery(queryFactory, qBook, qBookGenre, genreId, whereClause);
            total = countQuery.fetchCount();
        }

        // --- Data Query: ---
        JPAQuery<Book> query = queryFactory
                .selectFrom(qBook)
                .distinct();

        // Apply joins based on filters
        if (genreId != null) {
            // Join with bookGenre for genre filtering
            query.join(qBookGenre).on(qBook.eq(qBookGenre.book)
                    .and(qBookGenre.genre.id.eq(genreId))
                    .and(qBookGenre.isDeleted.eq(false))); // Use the denormalized field
        }

        // Apply where clause
        query.where(whereClause);

        // Apply sorting based on pageable
        applySorting(query, qBook, qBookGenre, genreId, pageable.getSort());

        // Apply pagination
        query.offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        List<Book> books = query.fetch();

        return new PageImpl<>(books, pageable, total);
    }

    /**
     * Creates a count query with the appropriate joins and conditions
     */
    private JPAQuery<Long> createCountQuery(JPAQueryFactory queryFactory, QBook qBook,
                                            QBookGenre qBookGenre,
                                            Long genreId, BooleanBuilder whereClause) {
        // Create count query - no fetch join needed
        JPAQuery<Long> countQuery = queryFactory.select(qBook.id)
                .from(qBook);

        // Apply joins for filters if needed
        if (genreId != null) {
            countQuery.join(qBookGenre).on(qBook.eq(qBookGenre.book)
                    .and(qBookGenre.genre.id.eq(genreId))
                    .and(qBookGenre.isDeleted.eq(false))); // Use the denormalized field
        }

        // Apply where conditions
        countQuery.where(whereClause);

        // Use distinct to eliminate duplicates from joins
        return countQuery.distinct();
    }

    /**
     * Applies sorting based on Sort specification
     * For performance reasons, when filtering by genre and sorting by rating or publication year,
     * we use the denormalized fields in book_genres
     */
    private void applySorting(JPAQuery<Book> query, QBook qBook, QBookGenre qBookGenre,
                              Long genreId, Sort sort) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        if (sort.isEmpty()) {
            // Default sort by average rating descending
            if (genreId != null) {
                // When filtering by genre, use denormalized field for better performance
                orderSpecifiers.add(qBookGenre.averageRating.desc().nullsLast());
            } else {
                orderSpecifiers.add(qBook.averageRating.desc().nullsLast());
            }
            orderSpecifiers.add(qBook.id.asc()); // Secondary sort by ID
        } else {
            for (Sort.Order order : sort) {
                String property = order.getProperty();
                boolean isAscending = order.isAscending();

                // Use denormalized fields when possible for better query performance
                boolean useGenreTable = genreId != null &&
                        (property.equals("averageRating") || property.equals("publicationYear"));

                switch (property) {
                    case "title":
                        orderSpecifiers.add(isAscending ? qBook.title.asc() : qBook.title.desc());
                        break;
                    case "averageRating":
                        if (useGenreTable) {
                            // Use denormalized field in book_genres table
                            orderSpecifiers.add(isAscending ?
                                    qBookGenre.averageRating.asc().nullsLast() :
                                    qBookGenre.averageRating.desc().nullsLast());
                        } else {
                            orderSpecifiers.add(isAscending ?
                                    qBook.averageRating.asc().nullsLast() :
                                    qBook.averageRating.desc().nullsLast());
                        }
                        break;
                    case "publicationYear":
                        if (useGenreTable) {
                            // Use denormalized field in book_genres table
                            orderSpecifiers.add(isAscending ?
                                    qBookGenre.publicationYear.asc().nullsLast() :
                                    qBookGenre.publicationYear.desc().nullsLast());
                        } else {
                            orderSpecifiers.add(isAscending ?
                                    qBook.publicationYear.asc().nullsLast() :
                                    qBook.publicationYear.desc().nullsLast());
                        }
                        break;
                    case "createdAt":
                        orderSpecifiers.add(isAscending ? qBook.createdAt.asc() : qBook.createdAt.desc());
                        break;
                    default:
                        // Handle or skip invalid fields
                        break;
                }
            }

            // Always add a sort by id for consistent ordering
            orderSpecifiers.add(qBook.id.asc());
        }

        query.orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]));
    }
}
