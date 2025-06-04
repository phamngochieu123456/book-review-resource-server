// src/main/java/com/hieupn/book_review/repository/BookRepositoryCustomImpl.java
package com.hieupn.book_review.repository;

import com.hieupn.book_review.model.entity.Book;
import com.hieupn.book_review.model.entity.BookCounts;
import com.hieupn.book_review.model.entity.QBook;
import com.hieupn.book_review.model.entity.QBookGenre;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
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

import java.util.*;

/**
 * Optimized implementation of custom book repository methods using QueryDSL with cursor-based pagination.
 *
 * Performance Strategy:
 * 1. Uses cursor-based pagination to avoid OFFSET performance issues with large datasets
 * 2. Leverages denormalized fields in book_genres table for genre filtering
 * 3. Implements two-step query approach:
 *    - Step 1: Light query to find cursor position (only sorting fields)
 *    - Step 2: Full query using cursor conditions for optimal performance
 * 4. Reduces query time from ~90s to ~5s for large offsets
 */
public class BookRepositoryCustomImpl implements BookRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private BookCountsRepository bookCountsRepository;

    /**
     * Container class to hold cursor data for pagination optimization.
     * Encapsulates the values needed to construct WHERE conditions for cursor-based pagination.
     */
    private static class CursorData {
        private final Tuple cursorValues;
        private final List<OrderSpecifier<?>> orderSpecifiers;
        private final List<Expression<?>> selectExpressions;

        public CursorData(Tuple cursorValues, List<OrderSpecifier<?>> orderSpecifiers, List<Expression<?>> selectExpressions) {
            this.cursorValues = cursorValues;
            this.orderSpecifiers = orderSpecifiers;
            this.selectExpressions = selectExpressions;
        }

        public Tuple getCursorValues() { return cursorValues; }
        public List<OrderSpecifier<?>> getOrderSpecifiers() { return orderSpecifiers; }
        public List<Expression<?>> getSelectExpressions() { return selectExpressions; }
    }

    /**
     * Finds all non-deleted books with optional filtering by genre, author, and search term.
     * Uses cursor-based pagination for optimal performance with large datasets.
     *
     * @param genreId Optional genre ID to filter by
     * @param authorId Optional author ID to filter by (placeholder for future implementation)
     * @param searchTerm Optional search term to filter title
     * @param pageable Pagination and sorting information
     * @return A page of books matching the criteria
     */
    @Override
    public Page<Book> findAllNonDeletedBooks(Long genreId, Long authorId, String searchTerm, Pageable pageable) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        // Build filtering criteria
        BooleanBuilder whereClause = buildFilteringCriteria(genreId, authorId, searchTerm);

        // Determine if any filter is applied
        boolean hasFilters = genreId != null || authorId != null || StringUtils.hasText(searchTerm);

        // Get total count of matching books
        long totalCount = countMatchingBooks(queryFactory, genreId, whereClause, hasFilters);

        // Fetch actual book data using cursor-based pagination
        List<Book> books = fetchBooksWithCursorPagination(queryFactory, genreId, whereClause, pageable);

        return new PageImpl<>(books, pageable, totalCount);
    }

    /**
     * Builds filtering criteria based on provided parameters.
     * Automatically selects the appropriate table (books or book_genres) based on filter combination.
     *
     * @param genreId Optional genre ID to filter by
     * @param authorId Optional author ID to filter by (placeholder)
     * @param searchTerm Optional search term for title
     * @return BooleanBuilder with all filtering conditions
     */
    private BooleanBuilder buildFilteringCriteria(Long genreId, Long authorId, String searchTerm) {
        BooleanBuilder whereClause = new BooleanBuilder();

        if (genreId != null) {
            // When filtering by genre, use denormalized fields in book_genres table
            whereClause.and(QBookGenre.bookGenre.genre.id.eq(genreId));
            whereClause.and(QBookGenre.bookGenre.isDeleted.eq(false));

            // Apply title search on denormalized title field if provided
            if (StringUtils.hasText(searchTerm)) {
                whereClause.and(QBookGenre.bookGenre.title.like(searchTerm + "%"));
            }
        } else {
            // When not filtering by genre, use books table directly
            whereClause.and(QBook.book.isDeleted.eq(false));

            // Apply title search on books table if provided
            if (StringUtils.hasText(searchTerm)) {
                whereClause.and(QBook.book.title.like(searchTerm + "%"));
            }
        }

        // Author filtering placeholder - to be implemented when needed
        // if (authorId != null) { ... }

        return whereClause;
    }

    /**
     * Counts total number of books matching the filter criteria.
     * Uses optimization strategies for better performance.
     *
     * @param queryFactory QueryDSL factory for creating queries
     * @param genreId Optional genre ID filter
     * @param whereClause Filter conditions
     * @param hasFilters Whether any filters are applied
     * @return Total count of matching books
     */
    private long countMatchingBooks(JPAQueryFactory queryFactory, Long genreId,
                                    BooleanBuilder whereClause, boolean hasFilters) {
        // For unfiltered queries, use pre-calculated count for optimal performance
        if (!hasFilters) {
            Optional<BookCounts> bookCountOpt = bookCountsRepository.findByCountName("active_books");
            if (bookCountOpt.isPresent()) {
                return bookCountOpt.get().getCurrentCount();
            }
        }

        // Create optimized count query based on filter type
        JPAQuery<Long> countQuery = createOptimizedCountQuery(queryFactory, genreId, whereClause);
        Long countResult = countQuery.fetchOne();
        return (countResult != null) ? countResult : 0L;
    }

    /**
     * Creates an optimized count query based on filter criteria.
     * Chooses the most efficient table to count from based on filter combination.
     *
     * @param queryFactory QueryDSL factory
     * @param genreId Optional genre ID filter
     * @param whereClause Filter conditions
     * @return Optimized count query
     */
    private JPAQuery<Long> createOptimizedCountQuery(JPAQueryFactory queryFactory, Long genreId,
                                                     BooleanBuilder whereClause) {
        if (genreId != null) {
            // For genre filtering, count from book_genres table to avoid expensive JOINs
            return queryFactory
                    .select(QBookGenre.bookGenre.book.id.count())
                    .from(QBookGenre.bookGenre)
                    .where(whereClause);
        } else {
            // For non-genre filtering, count from books table
            return queryFactory
                    .select(QBook.book.id.count())
                    .from(QBook.book)
                    .where(whereClause);
        }
    }

    /**
     * Fetches books using cursor-based pagination for optimal performance.
     * Automatically delegates to appropriate implementation based on filter type.
     *
     * @param queryFactory QueryDSL factory
     * @param genreId Optional genre ID filter
     * @param whereClause Filter conditions
     * @param pageable Pagination and sorting information
     * @return List of books matching criteria
     */
    private List<Book> fetchBooksWithCursorPagination(JPAQueryFactory queryFactory, Long genreId,
                                                      BooleanBuilder whereClause, Pageable pageable) {
        if (genreId != null) {
            return fetchBooksWithGenreCursorPagination(queryFactory, whereClause, pageable);
        } else {
            return fetchBooksWithoutGenreCursorPagination(queryFactory, whereClause, pageable);
        }
    }

    /**
     * Fetches books without genre filtering using cursor-based pagination.
     * Uses books table directly for optimal performance when no genre filter is applied.
     *
     * @param queryFactory QueryDSL factory
     * @param whereClause Filter conditions
     * @param pageable Pagination and sorting information
     * @return List of books matching criteria
     */
    private List<Book> fetchBooksWithoutGenreCursorPagination(JPAQueryFactory queryFactory,
                                                              BooleanBuilder whereClause, Pageable pageable) {
        // Skip cursor optimization for first page to avoid unnecessary complexity
        if (pageable.getOffset() == 0) {
            return fetchBooksDirectly(queryFactory, whereClause, pageable, false);
        }

        // Step 1: Find cursor position using lightweight query with only sorting fields
        CursorData cursorData = findCursorPosition(queryFactory, whereClause, pageable, false);

        if (cursorData.getCursorValues() == null) {
            return Collections.emptyList();
        }

        // Step 2: Build cursor-based WHERE conditions
        BooleanBuilder cursorWhereClause = buildCursorWhereConditions(whereClause, cursorData, pageable.getSort(), false);

        // Step 3: Execute full query with cursor conditions and no offset
        Pageable cursorPageable = createCursorPageable(pageable);
        return fetchBooksDirectly(queryFactory, cursorWhereClause, cursorPageable, false);
    }

    /**
     * Fetches books with genre filtering using cursor-based pagination.
     * Uses denormalized fields in book_genres table for optimal performance.
     *
     * @param queryFactory QueryDSL factory
     * @param whereClause Filter conditions (including genre filter)
     * @param pageable Pagination and sorting information
     * @return List of books matching criteria
     */
    private List<Book> fetchBooksWithGenreCursorPagination(JPAQueryFactory queryFactory,
                                                           BooleanBuilder whereClause, Pageable pageable) {
        // Skip cursor optimization for first page
        if (pageable.getOffset() == 0) {
            return fetchBooksDirectly(queryFactory, whereClause, pageable, true);
        }

        // Step 1: Find cursor position using lightweight query with denormalized fields
        CursorData cursorData = findCursorPosition(queryFactory, whereClause, pageable, true);

        if (cursorData.getCursorValues() == null) {
            return Collections.emptyList();
        }

        // Step 2: Build cursor-based WHERE conditions using denormalized fields
        BooleanBuilder cursorWhereClause = buildCursorWhereConditions(whereClause, cursorData, pageable.getSort(), true);

        // Step 3: Execute full query with cursor conditions and no offset
        Pageable cursorPageable = createCursorPageable(pageable);
        return fetchBooksDirectly(queryFactory, cursorWhereClause, cursorPageable, true);
    }

    /**
     * Finds cursor position by executing a lightweight query with only sorting fields.
     * This dramatically reduces I/O compared to fetching full book records for cursor positioning.
     *
     * @param queryFactory QueryDSL factory
     * @param whereClause Filter conditions
     * @param pageable Pagination information
     * @param useGenreTable Whether to use book_genres table for denormalized fields
     * @return CursorData containing cursor values and query metadata
     */
    private CursorData findCursorPosition(JPAQueryFactory queryFactory, BooleanBuilder whereClause,
                                          Pageable pageable, boolean useGenreTable) {
        // Build lightweight query with only essential fields for cursor positioning
        List<Expression<?>> selectExpressions = buildCursorSelectExpressions(pageable.getSort(), useGenreTable);
        List<OrderSpecifier<?>> orderSpecifiers = buildOrderSpecifiers(pageable.getSort(), useGenreTable);

        // Create cursor positioning query
        JPAQuery<Tuple> cursorQuery = createCursorPositioningQuery(queryFactory, selectExpressions, whereClause, useGenreTable);

        // Apply sorting and pagination to find cursor position
        cursorQuery.orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]));
        cursorQuery.offset(pageable.getOffset()).limit(1);

        // Execute lightweight query to get cursor values
        Tuple cursorValues = cursorQuery.fetchOne();

        return new CursorData(cursorValues, orderSpecifiers, selectExpressions);
    }

    /**
     * Builds the list of expressions to select for cursor positioning.
     * Only includes fields necessary for sorting to minimize data transfer.
     *
     * @param sort Sort specification
     * @param useGenreTable Whether to use denormalized fields from book_genres
     * @return List of expressions for cursor query SELECT clause
     */
    private List<Expression<?>> buildCursorSelectExpressions(Sort sort, boolean useGenreTable) {
        List<Expression<?>> expressions = new ArrayList<>();

        // Always include common fields
        if (useGenreTable) {
            expressions.add(QBookGenre.bookGenre.isDeleted);
            expressions.add(QBookGenre.bookGenre.id.bookId);
        } else {
            expressions.add(QBook.book.isDeleted);
            expressions.add(QBook.book.id);
        }

        // Add sorting-specific fields
        if (sort.isEmpty()) {
            // Default sort: average rating desc, id asc
            if (useGenreTable) {
                expressions.add(QBookGenre.bookGenre.averageRating);
            } else {
                expressions.add(QBook.book.averageRating);
            }
        } else {
            for (Sort.Order order : sort) {
                switch (order.getProperty()) {
                    case "title":
                        expressions.add(useGenreTable ? QBookGenre.bookGenre.title : QBook.book.title);
                        break;
                    case "averageRating":
                        expressions.add(useGenreTable ? QBookGenre.bookGenre.averageRating : QBook.book.averageRating);
                        break;
                    case "publicationYear":
                        if (useGenreTable) {
                            expressions.add(QBookGenre.bookGenre.publicationYear);
                            expressions.add(QBookGenre.bookGenre.publicationYearIsNull);
                        } else {
                            expressions.add(QBook.book.publicationYear);
                            expressions.add(QBook.book.publicationYearIsNull);
                        }
                        break;
                    default:
                        // Default to averageRating for unknown properties
                        expressions.add(useGenreTable ? QBookGenre.bookGenre.averageRating : QBook.book.averageRating);
                        break;
                }
            }
        }

        return expressions;
    }

    /**
     * Creates the cursor positioning query with minimal field selection.
     *
     * @param queryFactory QueryDSL factory
     * @param selectExpressions Fields to select for cursor
     * @param whereClause Filter conditions
     * @param useGenreTable Whether to use book_genres table
     * @return Configured cursor positioning query
     */
    private JPAQuery<Tuple> createCursorPositioningQuery(JPAQueryFactory queryFactory, List<Expression<?>> selectExpressions,
                                                         BooleanBuilder whereClause, boolean useGenreTable) {
        if (useGenreTable) {
            // Use book_genres table with JOIN for genre filtering
            return queryFactory
                    .select(selectExpressions.toArray(new Expression[0]))
                    .from(QBookGenre.bookGenre)
                    .where(whereClause);
        } else {
            // Use books table directly for non-genre queries
            return queryFactory
                    .select(selectExpressions.toArray(new Expression[0]))
                    .from(QBook.book)
                    .where(whereClause);
        }
    }

    /**
     * Builds ORDER BY clauses based on sort specification.
     * Uses appropriate entity path (QBook vs QBookGenre) based on query type.
     *
     * @param sort Sort specification
     * @param useGenreTable Whether to use denormalized fields from book_genres
     * @return List of OrderSpecifier objects for query sorting
     */
    private List<OrderSpecifier<?>> buildOrderSpecifiers(Sort sort, boolean useGenreTable) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        if (sort.isEmpty()) {
            // Default sort: average rating desc, id asc
            if (useGenreTable) {
                orderSpecifiers.add(QBookGenre.bookGenre.averageRating.desc());
                orderSpecifiers.add(QBookGenre.bookGenre.id.bookId.asc());
            } else {
                orderSpecifiers.add(QBook.book.averageRating.desc());
                orderSpecifiers.add(QBook.book.id.asc());
            }
        } else {
            for (Sort.Order order : sort) {
                String property = order.getProperty();
                boolean isAscending = order.isAscending();

                switch (property) {
                    case "title":
                        if (useGenreTable) {
                            orderSpecifiers.add(isAscending ? QBookGenre.bookGenre.title.asc() : QBookGenre.bookGenre.title.desc());
                        } else {
                            orderSpecifiers.add(isAscending ? QBook.book.title.asc() : QBook.book.title.desc());
                        }
                        break;
                    case "averageRating":
                        if (useGenreTable) {
                            orderSpecifiers.add(isAscending ? QBookGenre.bookGenre.averageRating.asc() : QBookGenre.bookGenre.averageRating.desc());
                        } else {
                            orderSpecifiers.add(isAscending ? QBook.book.averageRating.asc() : QBook.book.averageRating.desc());
                        }
                        break;
                    case "publicationYear":
                        // Handle null values by sorting them last
                        if (useGenreTable) {
                            orderSpecifiers.add(QBookGenre.bookGenre.publicationYearIsNull.asc());
                            orderSpecifiers.add(isAscending ? QBookGenre.bookGenre.publicationYear.asc() : QBookGenre.bookGenre.publicationYear.desc());
                        } else {
                            orderSpecifiers.add(QBook.book.publicationYearIsNull.asc());
                            orderSpecifiers.add(isAscending ? QBook.book.publicationYear.asc() : QBook.book.publicationYear.desc());
                        }
                        break;
                    default:
                        // Default to averageRating for unknown properties
                        if (useGenreTable) {
                            orderSpecifiers.add(QBookGenre.bookGenre.averageRating.desc());
                        } else {
                            orderSpecifiers.add(QBook.book.averageRating.desc());
                        }
                        break;
                }
            }

            // Always add ID sort for consistent ordering
            if (useGenreTable) {
                orderSpecifiers.add(QBookGenre.bookGenre.id.bookId.asc());
            } else {
                orderSpecifiers.add(QBook.book.id.asc());
            }
        }

        return orderSpecifiers;
    }

    /**
     * Builds cursor-based WHERE conditions using values from cursor position.
     * Creates efficient range conditions that replace OFFSET for pagination.
     *
     * @param originalWhereClause Original filter conditions
     * @param cursorData Cursor values and metadata
     * @param sort Sort specification
     * @param useGenreTable Whether to use denormalized fields
     * @return Enhanced WHERE clause with cursor conditions
     */
    private BooleanBuilder buildCursorWhereConditions(BooleanBuilder originalWhereClause, CursorData cursorData,
                                                      Sort sort, boolean useGenreTable) {
        BooleanBuilder cursorWhereClause = new BooleanBuilder(originalWhereClause);
        Tuple cursorValues = cursorData.getCursorValues();

        if (sort.isEmpty()) {
            // Default sort: average rating desc, id asc
            if (useGenreTable) {
                cursorWhereClause.and(
                        (QBookGenre.bookGenre.title.eq(cursorValues.get(QBookGenre.bookGenre.title))
                                .and(QBookGenre.bookGenre.id.bookId.goe(cursorValues.get(QBookGenre.bookGenre.id.bookId))))
                                .or(QBookGenre.bookGenre.title.lt(cursorValues.get(QBookGenre.bookGenre.title)))
                );
            }
            else {
                cursorWhereClause.and(
                        (QBook.book.title.eq(cursorValues.get(QBook.book.title))
                                .and(QBook.book.id.goe(cursorValues.get(QBook.book.id))))
                                .or(QBook.book.title.lt(cursorValues.get(QBook.book.title)))
                );
            }
        } else {
            for (Sort.Order order : sort) {
                String property = order.getProperty();
                boolean isAscending = order.isAscending();

                switch (property) {
                    case "title":
                        if (useGenreTable) {
                            String titleValue = cursorValues.get(QBookGenre.bookGenre.title);
                            cursorWhereClause.and(isAscending ?
                                    QBookGenre.bookGenre.title.goe(titleValue) : QBookGenre.bookGenre.title.loe(titleValue));

                            if (isAscending) {
                                cursorWhereClause.and(
                                        (QBookGenre.bookGenre.title.eq(cursorValues.get(QBookGenre.bookGenre.title))
                                                .and(QBookGenre.bookGenre.id.bookId.goe(cursorValues.get(QBookGenre.bookGenre.id.bookId))))
                                                .or(QBookGenre.bookGenre.title.gt(cursorValues.get(QBookGenre.bookGenre.title)))
                                );
                            }
                            else {
                                cursorWhereClause.and(
                                        (QBookGenre.bookGenre.title.eq(cursorValues.get(QBookGenre.bookGenre.title))
                                                .and(QBookGenre.bookGenre.id.bookId.goe(cursorValues.get(QBookGenre.bookGenre.id.bookId))))
                                                .or(QBookGenre.bookGenre.title.lt(cursorValues.get(QBookGenre.bookGenre.title)))
                                );
                            }
                        }
                        else {
                            if (isAscending) {
                                cursorWhereClause.and(
                                        (QBook.book.title.eq(cursorValues.get(QBook.book.title))
                                                .and(QBook.book.id.goe(cursorValues.get(QBook.book.id))))
                                                .or(QBook.book.title.gt(cursorValues.get(QBook.book.title)))
                                );
                            }
                            else {
                                cursorWhereClause.and(
                                        (QBook.book.title.eq(cursorValues.get(QBook.book.title))
                                                .and(QBook.book.id.goe(cursorValues.get(QBook.book.id))))
                                                .or(QBook.book.title.lt(cursorValues.get(QBook.book.title)))
                                );
                            }
                        }
                        break;
                    case "averageRating":
                        if (useGenreTable) {
                            if (isAscending) {
                                cursorWhereClause.and(
                                        (QBookGenre.bookGenre.averageRating.eq(cursorValues.get(QBookGenre.bookGenre.averageRating))
                                                .and(QBookGenre.bookGenre.id.bookId.goe(cursorValues.get(QBookGenre.bookGenre.id.bookId))))
                                                .or(QBookGenre.bookGenre.averageRating.gt(cursorValues.get(QBookGenre.bookGenre.averageRating)))
                                );
                            }
                            else {
                                cursorWhereClause.and(
                                        (QBookGenre.bookGenre.averageRating.eq(cursorValues.get(QBookGenre.bookGenre.averageRating))
                                                .and(QBookGenre.bookGenre.id.bookId.goe(cursorValues.get(QBookGenre.bookGenre.id.bookId))))
                                                .or(QBookGenre.bookGenre.averageRating.lt(cursorValues.get(QBookGenre.bookGenre.averageRating)))
                                );
                            }
                        }
                        else {
                            if (isAscending) {
                                cursorWhereClause.and(
                                    (QBook.book.averageRating.eq(cursorValues.get(QBook.book.averageRating))
                                        .and(QBook.book.id.goe(cursorValues.get(QBook.book.id))))
                                    .or(QBook.book.averageRating.gt(cursorValues.get(QBook.book.averageRating)))
                                );
                            }
                            else {
                                cursorWhereClause.and(
                                    (QBook.book.averageRating.eq(cursorValues.get(QBook.book.averageRating))
                                        .and(QBook.book.id.goe(cursorValues.get(QBook.book.id))))
                                    .or(QBook.book.averageRating.lt(cursorValues.get(QBook.book.averageRating)))
                                );
                            }
                        }
                        break;
                    case "publicationYear":
                        if (useGenreTable) {
                            if (isAscending) {
                                cursorWhereClause.and(
                                        (QBookGenre.bookGenre.publicationYearIsNull.eq(cursorValues.get(QBookGenre.bookGenre.publicationYearIsNull))
                                                .and(QBookGenre.bookGenre.publicationYear.eq(cursorValues.get(QBookGenre.bookGenre.publicationYear)))
                                                .and(QBookGenre.bookGenre.id.bookId.goe(cursorValues.get(QBookGenre.bookGenre.id.bookId))))
                                                .or(QBookGenre.bookGenre.publicationYearIsNull.eq(cursorValues.get(QBookGenre.bookGenre.publicationYearIsNull))
                                                        .and(QBookGenre.bookGenre.publicationYear.gt(cursorValues.get(QBookGenre.bookGenre.publicationYear))))
                                                .or(QBookGenre.bookGenre.publicationYearIsNull.gt(cursorValues.get(QBookGenre.bookGenre.publicationYearIsNull)))
                                );
                            }
                            else {
                                cursorWhereClause.and(
                                        (QBookGenre.bookGenre.publicationYearIsNull.eq(cursorValues.get(QBookGenre.bookGenre.publicationYearIsNull))
                                                .and(QBookGenre.bookGenre.publicationYear.eq(cursorValues.get(QBookGenre.bookGenre.publicationYear)))
                                                .and(QBookGenre.bookGenre.id.bookId.goe(cursorValues.get(QBookGenre.bookGenre.id.bookId))))
                                                .or(QBookGenre.bookGenre.publicationYearIsNull.eq(cursorValues.get(QBookGenre.bookGenre.publicationYearIsNull))
                                                        .and(QBookGenre.bookGenre.publicationYear.lt(cursorValues.get(QBookGenre.bookGenre.publicationYear))))
                                                .or(QBookGenre.bookGenre.publicationYearIsNull.gt(cursorValues.get(QBookGenre.bookGenre.publicationYearIsNull)))
                                );
                            }
                        }
                        else {
                            if (isAscending) {
                                cursorWhereClause.and(
                                    (QBook.book.publicationYearIsNull.eq(cursorValues.get(QBook.book.publicationYearIsNull))
                                            .and(QBook.book.publicationYear.eq(cursorValues.get(QBook.book.publicationYear)))
                                            .and(QBook.book.id.goe(cursorValues.get(QBook.book.id))))
                                            .or(QBook.book.publicationYearIsNull.eq(cursorValues.get(QBook.book.publicationYearIsNull))
                                                    .and(QBook.book.publicationYear.gt(cursorValues.get(QBook.book.publicationYear))))
                                            .or(QBook.book.publicationYearIsNull.gt(cursorValues.get(QBook.book.publicationYearIsNull)))
                                );
                            }
                            else {
                                cursorWhereClause.and(
                                    (QBook.book.publicationYearIsNull.eq(cursorValues.get(QBook.book.publicationYearIsNull))
                                            .and(QBook.book.publicationYear.eq(cursorValues.get(QBook.book.publicationYear)))
                                            .and(QBook.book.id.goe(cursorValues.get(QBook.book.id))))
                                            .or(QBook.book.publicationYearIsNull.eq(cursorValues.get(QBook.book.publicationYearIsNull))
                                                    .and(QBook.book.publicationYear.lt(cursorValues.get(QBook.book.publicationYear))))
                                            .or(QBook.book.publicationYearIsNull.gt(cursorValues.get(QBook.book.publicationYearIsNull)))
                                );
                            }
                        }
                        break;
                    default:
                        // Default to averageRating
                        if (useGenreTable) {
                            cursorWhereClause.and(QBookGenre.bookGenre.averageRating.loe(cursorValues.get(QBookGenre.bookGenre.averageRating)));
                        } else {
                            cursorWhereClause.and(QBook.book.averageRating.loe(cursorValues.get(QBook.book.averageRating)));
                        }
                        break;
                }
            }
        }

        return cursorWhereClause;
    }

    /**
     * Creates a new Pageable with offset reset to 0 for cursor-based queries.
     * Maintains the same page size and sort configuration.
     *
     * @param pageable Original pagination configuration
     * @return New Pageable with offset = 0 for cursor-based querying
     */
    private Pageable createCursorPageable(Pageable pageable) {
        return org.springframework.data.domain.PageRequest.of(0, pageable.getPageSize(), pageable.getSort());
    }

    /**
     * Executes the final book query to fetch complete book entities.
     * Handles both genre-filtered and non-genre-filtered queries.
     *
     * @param queryFactory QueryDSL factory
     * @param whereClause Complete WHERE conditions (including cursor conditions)
     * @param pageable Pagination configuration
     * @param useGenreTable Whether this is a genre-filtered query
     * @return List of complete Book entities
     */
    private List<Book> fetchBooksDirectly(JPAQueryFactory queryFactory, BooleanBuilder whereClause,
                                          Pageable pageable, boolean useGenreTable) {
        JPAQuery<Book> query;

        if (useGenreTable) {
            // For genre queries, join with book_genres and fetch complete book entities
            query = queryFactory
                    .selectFrom(QBook.book)
                    .join(QBookGenre.bookGenre).on(QBook.book.eq(QBookGenre.bookGenre.book))
                    .where(whereClause);
        } else {
            // For non-genre queries, select directly from books table
            query = queryFactory
                    .selectFrom(QBook.book)
                    .where(whereClause);
        }

        // Apply sorting using appropriate entity path
        applyQuerySorting(query, pageable.getSort(), useGenreTable);

        // Apply pagination
        query.offset(pageable.getOffset()).limit(pageable.getPageSize());

        return query.fetch();
    }

    /**
     * Applies sorting to the final book query.
     * Uses the appropriate entity path based on whether genre filtering is applied.
     *
     * @param query Book query to apply sorting to
     * @param sort Sort specification
     * @param useGenreTable Whether to use denormalized fields for sorting
     */
    private void applyQuerySorting(JPAQuery<Book> query, Sort sort, boolean useGenreTable) {
        List<OrderSpecifier<?>> orderSpecifiers = buildOrderSpecifiers(sort, useGenreTable);
        query.orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]));
    }
}
