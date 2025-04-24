// src/main/java/com/hieupn/book_review/repository/BookRepositoryCustomImpl.java
package com.hieupn.book_review.repository;

import com.hieupn.book_review.model.entity.Book;
import com.hieupn.book_review.model.entity.BookCounts;
import com.hieupn.book_review.model.entity.QBook;
import com.hieupn.book_review.model.entity.QBookGenre;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
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
 * Implementation of custom book repository methods using QueryDSL.
 * This class leverages denormalized fields in the book_genres table for optimized performance
 * when filtering and sorting books, especially by genre.
 */
public class BookRepositoryCustomImpl implements BookRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private BookCountsRepository bookCountsRepository;

    /**
     * Finds all non-deleted books with optional filtering by genre, author, and search term.
     *
     * Performance optimizations:
     * 1. Uses denormalized fields in book_genres table when filtering by genre
     * 2. Avoids joins when possible for better performance
     * 3. Utilizes pre-calculated counts for unfiltered queries
     * 4. Uses appropriate indexes for each query pattern
     *
     * @param genreId Optional genre ID to filter by
     * @param authorId Optional author ID to filter by
     * @param searchTerm Optional search term to filter title
     * @param pageable Pagination and sorting information
     * @return A page of books matching the criteria
     */
    @Override
    public Page<Book> findAllNonDeletedBooks(Long genreId, Long authorId, String searchTerm, Pageable pageable) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QBook qBook = QBook.book;
        QBookGenre qBookGenre = QBookGenre.bookGenre;

        // Build filtering criteria
        BooleanBuilder whereClause = buildFilteringCriteria(qBook, qBookGenre, genreId, authorId, searchTerm);

        // Determine if any filter is applied
        boolean hasFilters = genreId != null || authorId != null || StringUtils.hasText(searchTerm);

        // Get total count of matching books
        long totalCount = countMatchingBooks(queryFactory, qBook, qBookGenre, genreId, whereClause, hasFilters);

        // Fetch actual book data with proper pagination and sorting
        List<Book> books = fetchBooksWithFilters(queryFactory, qBook, qBookGenre, genreId, whereClause, pageable);

        return new PageImpl<>(books, pageable, totalCount);
    }

    /**
     * Builds filtering criteria based on provided parameters.
     *
     * @param qBook Book entity path for QueryDSL
     * @param qBookGenre BookGenre entity path for QueryDSL
     * @param genreId Optional genre ID to filter by
     * @param authorId Optional author ID to filter by
     * @param searchTerm Optional search term for title
     * @return BooleanBuilder with all filtering conditions
     */
    private BooleanBuilder buildFilteringCriteria(QBook qBook, QBookGenre qBookGenre,
                                                  Long genreId, Long authorId, String searchTerm) {
        BooleanBuilder whereClause = new BooleanBuilder();

        // Apply genre filter if provided
        if (genreId != null) {
            // When filtering by genre, use book_genres table with denormalized fields
            whereClause.and(qBookGenre.genre.id.eq(genreId));
            // Use the denormalized isDeleted field for better performance
            whereClause.and(qBookGenre.isDeleted.eq(false));
        } else {
            // When not filtering by genre, use the book table directly
            whereClause.and(qBook.isDeleted.eq(false));
        }

        // Add author filter if provided (handled by the join in the query execution)
        // Note: This is left for future implementation

        // Add title search condition if search term is provided
        if (StringUtils.hasText(searchTerm)) {

            if (genreId != null) {
                // When filtering by genre, use denormalized title in book_genres
                whereClause.and(qBookGenre.title.like(searchTerm + "%"));
            } else {
                // Otherwise use title from books table
                whereClause.and(qBook.title.like(searchTerm + "%"));
            }
        }

        return whereClause;
    }

    /**
     * Counts total number of books matching the filter criteria.
     * Uses optimization strategies:
     * 1. For unfiltered queries, uses pre-calculated counts from book_counts table
     * 2. For filtered queries, builds optimized count queries
     *
     * @param queryFactory QueryDSL factory for creating queries
     * @param qBook Book entity path
     * @param qBookGenre BookGenre entity path
     * @param genreId Optional genre ID filter
     * @param whereClause Filter conditions
     * @param hasFilters Whether any filters are applied
     * @return Total count of matching books
     */
    private long countMatchingBooks(JPAQueryFactory queryFactory, QBook qBook, QBookGenre qBookGenre,
                                    Long genreId, BooleanBuilder whereClause, boolean hasFilters) {
        // If no filters are applied, use the pre-calculated count for better performance
        if (!hasFilters) {
            Optional<BookCounts> bookCountOpt = bookCountsRepository.findByCountName("active_books");
            if (bookCountOpt.isPresent()) {
                return bookCountOpt.get().getCurrentCount();
            }
        }

        // Create and execute count query
        JPAQuery<Long> countQuery = createOptimizedCountQuery(queryFactory, qBook, qBookGenre, genreId, whereClause);
        Long countResult = countQuery.fetchOne();
        return (countResult != null) ? countResult : 0L;
    }

    /**
     * Creates an optimized count query based on filter criteria.
     * When filtering by genre, counts directly from book_genres table to avoid expensive JOINs.
     *
     * @param queryFactory QueryDSL factory
     * @param qBook Book entity path
     * @param qBookGenre BookGenre entity path
     * @param genreId Optional genre ID filter
     * @param whereClause Filter conditions
     * @return Optimized count query
     */
    private JPAQuery<Long> createOptimizedCountQuery(JPAQueryFactory queryFactory, QBook qBook,
                                                     QBookGenre qBookGenre, Long genreId, BooleanBuilder whereClause) {
        // When filtering by genre, count directly from book_genres table for better performance
        if (genreId != null) {
            return queryFactory
                    .select(qBookGenre.book.id.countDistinct())
                    .from(qBookGenre)
                    .where(whereClause);
        } else {
            // For non-genre filtering, count from books table
            return queryFactory
                    .select(qBook.id.countDistinct())
                    .from(qBook)
                    .where(whereClause);
        }
    }

    /**
     * Fetches books matching filter criteria with pagination and sorting.
     * Uses different query strategies based on filter combination:
     * 1. For genre filtering: uses denormalized fields and tuple queries
     * 2. For other filters: uses simpler queries directly against book table
     *
     * @param queryFactory QueryDSL factory
     * @param qBook Book entity path
     * @param qBookGenre BookGenre entity path
     * @param genreId Optional genre ID filter
     * @param whereClause Filter conditions
     * @param pageable Pagination and sorting information
     * @return List of books matching criteria
     */
    private List<Book> fetchBooksWithFilters(JPAQueryFactory queryFactory, QBook qBook, QBookGenre qBookGenre,
                                             Long genreId, BooleanBuilder whereClause, Pageable pageable) {
        // Special handling for genre filtering to leverage denormalized fields
        if (genreId != null) {
            return fetchBooksWithGenreFilter(queryFactory, qBook, qBookGenre, genreId, whereClause, pageable);
        } else {
            return fetchBooksWithoutGenreFilter(queryFactory, qBook, qBookGenre, whereClause, pageable);
        }
    }

    /**
     * Fetches books with genre filtering using denormalized fields for better performance.
     * Uses tuple queries to include sorting fields in the SELECT clause.
     *
     * @param queryFactory QueryDSL factory
     * @param qBook Book entity path
     * @param qBookGenre BookGenre entity path
     * @param genreId Genre ID to filter by
     * @param whereClause Filter conditions
     * @param pageable Pagination and sorting information
     * @return List of books matching criteria
     */
    private List<Book> fetchBooksWithGenreFilter(JPAQueryFactory queryFactory, QBook qBook, QBookGenre qBookGenre,
                                                 Long genreId, BooleanBuilder whereClause, Pageable pageable) {
        // Extract sort fields for use in both SELECT and ORDER BY clauses
        LinkedHashMap<String, Boolean> sortFields = extractSortFields(pageable.getSort());

        // Create tuple query that includes book entity and sorting fields
        JPAQuery<Tuple> tupleQuery = queryFactory
                .select(qBook, qBookGenre.id.bookId, qBookGenre.averageRating, qBookGenre.title,
                        qBookGenre.publicationYear, qBookGenre.publicationYearIsNull)
                .from(qBook)
                .join(qBookGenre).on(qBook.eq(qBookGenre.book)
                        .and(qBookGenre.genre.id.eq(genreId))
                        .and(qBookGenre.isDeleted.eq(false)))
                .where(whereClause)
                .distinct();

        // Apply sorting to the tuple query
        applySortingToTupleQuery(tupleQuery, qBook, qBookGenre, sortFields);

        // Apply pagination
        tupleQuery.offset(pageable.getOffset()).limit(pageable.getPageSize());

        // Execute query and convert results to Book entities
        List<Tuple> tuples = tupleQuery.fetch();
        return convertTuplesToBooks(tuples);
    }

    /**
     * Fetches books without genre filtering using standard book table queries.
     *
     * @param queryFactory QueryDSL factory
     * @param qBook Book entity path
     * @param qBookGenre BookGenre entity path (not used directly in this method)
     * @param whereClause Filter conditions
     * @param pageable Pagination and sorting information
     * @return List of books matching criteria
     */
    private List<Book> fetchBooksWithoutGenreFilter(JPAQueryFactory queryFactory, QBook qBook, QBookGenre qBookGenre,
                                                    BooleanBuilder whereClause, Pageable pageable) {
        // For queries without genre filtering, use simpler approach
        JPAQuery<Book> query = queryFactory
                .selectFrom(qBook)
                .where(whereClause)
                .distinct();

        // Apply sorting
        applyStandardSorting(query, qBook, pageable.getSort());

        // Apply pagination
        query.offset(pageable.getOffset()).limit(pageable.getPageSize());

        // Execute query
        return query.fetch();
    }

    /**
     * Applies standard sorting to a book query without using denormalized fields.
     *
     * @param query The book query to apply sorting to
     * @param qBook Book entity path for access to sort fields
     * @param sort Sort specification
     */
    private void applyStandardSorting(JPAQuery<Book> query, QBook qBook, Sort sort) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        if (sort.isEmpty()) {
            // Default sort by average rating descending, then by ID
            orderSpecifiers.add(qBook.averageRating.desc());
            orderSpecifiers.add(qBook.id.asc());
        } else {
            for (Sort.Order order : sort) {
                String property = order.getProperty();
                boolean isAscending = order.isAscending();

                switch (property) {
                    case "title":
                        orderSpecifiers.add(isAscending ?
                                qBook.title.asc() :
                                qBook.title.desc());
                        break;
                    case "averageRating":
                        orderSpecifiers.add(isAscending ?
                                qBook.averageRating.asc() :
                                qBook.averageRating.desc());
                        break;
                    case "publicationYear":
                        // Sort nulls last by first sorting on the null indicator field
                        orderSpecifiers.add(qBook.publicationYearIsNull.asc());
                        orderSpecifiers.add(isAscending ?
                                qBook.publicationYear.asc() :
                                qBook.publicationYear.desc());
                        break;
                    default:
                        // Skip invalid fields
                        break;
                }
            }

            // Always add ID sort for consistent ordering
            orderSpecifiers.add(qBook.id.asc());
        }

        query.orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]));
    }

    /**
     * Extracts sort fields and directions from Sort object.
     *
     * @param sort Sort specification
     * @return Map of field names to sort direction (true = ascending, false = descending)
     */
    private LinkedHashMap<String, Boolean> extractSortFields(Sort sort) {
        LinkedHashMap<String, Boolean> sortFields = new LinkedHashMap<>();

        if (sort.isEmpty()) {
            // Default sort: averageRating descending, ID ascending
            sortFields.put("averageRating", false); // false = descending
            sortFields.put("id", true); // true = ascending
        } else {
            for (Sort.Order order : sort) {
                sortFields.put(order.getProperty(), order.isAscending());
            }

            // Always add ID sort for consistency if not already included
            if (!sortFields.containsKey("id")) {
                sortFields.put("id", true);
            }
        }

        return sortFields;
    }

    /**
     * Applies sorting to a tuple query based on the sort fields map.
     * Uses denormalized fields from book_genres table for better performance.
     *
     * @param query The tuple query to apply sorting to
     * @param qBook Book entity path (not used directly in this method)
     * @param qBookGenre BookGenre entity path for access to denormalized sort fields
     * @param sortFields Map of field names to sort directions
     */
    private void applySortingToTupleQuery(JPAQuery<Tuple> query, QBook qBook, QBookGenre qBookGenre,
                                          LinkedHashMap<String, Boolean> sortFields) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        for (Map.Entry<String, Boolean> entry : sortFields.entrySet()) {
            String property = entry.getKey();
            boolean isAscending = entry.getValue();

            switch (property) {
                case "title":
                    orderSpecifiers.add(isAscending ?
                            qBookGenre.title.asc() :
                            qBookGenre.title.desc());
                    break;
                case "averageRating":
                    orderSpecifiers.add(isAscending ?
                            qBookGenre.averageRating.asc() :
                            qBookGenre.averageRating.desc());
                    break;
                case "publicationYear":
                    // Sort nulls last by first sorting on the null indicator field
                    orderSpecifiers.add(qBookGenre.publicationYearIsNull.asc());
                    // Then sort by the actual year field
                    orderSpecifiers.add(isAscending ?
                            qBookGenre.publicationYear.asc() :
                            qBookGenre.publicationYear.desc());
                    break;
                case "id":
                    orderSpecifiers.add(isAscending ?
                            qBookGenre.id.bookId.asc() :
                            qBookGenre.id.bookId.desc());
                    break;
                default:
                    // Skip invalid fields
                    break;
            }
        }

        if (!orderSpecifiers.isEmpty()) {
            query.orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]));
        }
    }

    /**
     * Converts a list of tuples to a list of Book entities.
     * The first element of each tuple is expected to be the Book entity.
     *
     * @param tuples List of tuples from the query result
     * @return List of Book entities
     */
    private List<Book> convertTuplesToBooks(List<Tuple> tuples) {
        List<Book> books = new ArrayList<>(tuples.size());

        for (Tuple tuple : tuples) {
            Book book = tuple.get(0, Book.class);
            if (book != null) {
                books.add(book);
            }
        }

        return books;
    }
}
