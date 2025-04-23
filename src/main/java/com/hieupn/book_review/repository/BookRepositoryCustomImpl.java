package com.hieupn.book_review.repository;

import com.hieupn.book_review.model.entity.Book;
import com.hieupn.book_review.model.entity.BookCounts;
import com.hieupn.book_review.model.entity.QBook;
import com.hieupn.book_review.model.entity.QBookCategory;
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
    public Page<Book> findAllNonDeletedBooks(Long categoryId, Long authorId, String searchTerm, Pageable pageable) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QBook qBook = QBook.book;
        QBookCategory qBookCategory = QBookCategory.bookCategory;

        // Build common where conditions
        BooleanBuilder whereClause = new BooleanBuilder();
        whereClause.and(qBook.isDeleted.eq(false));

        // Apply filters to where clause if provided
        boolean hasFilters = false;

        // Add title search condition using LIKE if searchTerm is provided
        if (StringUtils.hasText(searchTerm)) {
            // Use likeIgnoreCase for case-insensitive search, starts with searchTerm
            whereClause.and(qBook.title.like(searchTerm + "%"));
            hasFilters = true;
        }

        // Apply category filter if provided
        if (categoryId != null) {
            whereClause.and(qBookCategory.category.id.eq(categoryId));
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
                JPAQuery<Long> countQuery = createCountQuery(queryFactory, qBook, qBookCategory, categoryId, whereClause);
                Long countResult = countQuery.fetchOne(); // fetchOne() returns null if no results
                total = (countResult != null) ? countResult : 0L;
            }
        } else {
            // --- Count Query for filtered results: ---
            JPAQuery<Long> countQuery = createCountQuery(queryFactory, qBook, qBookCategory, categoryId, whereClause);
            total = countQuery.fetchCount();
        }

        // --- Data Query: ---
        // Use fetch join to load associations
        JPAQuery<Book> query = queryFactory
                .selectFrom(qBook)
                .distinct()
                // Fetch join for bookCategories
                .leftJoin(qBookCategory).on(qBook.eq(qBookCategory.book)).fetchJoin()
                .where(whereClause);

        // Apply sorting based on pageable
        applySorting(query, qBook, pageable.getSort());

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
                                            QBookCategory qBookCategory,
                                            Long categoryId, BooleanBuilder whereClause) {
        // Create count query - no fetch join needed
        JPAQuery<Long> countQuery = queryFactory.select(qBook.id)
                .from(qBook);

        // Apply joins for filters if needed
        if (categoryId != null) {
            countQuery.join(qBookCategory).on(qBook.eq(qBookCategory.book)
                    .and(qBookCategory.category.id.eq(categoryId)));
        }

        // Apply where conditions
        countQuery.where(whereClause);

        // Use distinct to eliminate duplicates from joins
        return countQuery.distinct();
    }

    /**
     * Applies sorting based on Sort specification
     */
    private void applySorting(JPAQuery<Book> query, QBook qBook, Sort sort) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        if (sort.isEmpty()) {
            orderSpecifiers.add(qBook.createdAt.desc());
            orderSpecifiers.add(qBook.id.asc());
        } else {
            for (Sort.Order order : sort) {
                String property = order.getProperty();
                boolean isAscending = order.isAscending();

                switch (property) {
                    case "title":
                        orderSpecifiers.add(isAscending ? qBook.title.asc() : qBook.title.desc());
                        break;
                    case "averageRating":
                        orderSpecifiers.add(isAscending ? qBook.averageRating.asc() : qBook.averageRating.desc());
                        break;
                    case "publicationYear":
                        orderSpecifiers.add(isAscending ? qBook.publicationYear.asc() : qBook.publicationYear.desc());
                        break;
                    case "createdAt":
                        orderSpecifiers.add(isAscending ? qBook.createdAt.asc() : qBook.createdAt.desc());
                        break;
                    default:
                        // Handle or skip invalid fields
                        break;
                }
            }
            orderSpecifiers.add(qBook.id.asc()); // Always add a sort by id
        }

        query.orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]));
    }
}
