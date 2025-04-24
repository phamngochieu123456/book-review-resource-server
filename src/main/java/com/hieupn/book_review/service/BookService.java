package com.hieupn.book_review.service;

import com.hieupn.book_review.exception.DuplicateResourceException;
import com.hieupn.book_review.exception.ResourceNotFoundException;
import com.hieupn.book_review.mapper.BookMapper;
import com.hieupn.book_review.model.dto.BookDetailDTO;
import com.hieupn.book_review.model.dto.BookSummaryDTO;
import com.hieupn.book_review.model.dto.CreateBookRequestDTO;
import com.hieupn.book_review.model.dto.UpdateBookRequestDTO;
import com.hieupn.book_review.model.entity.*;
import com.hieupn.book_review.repository.*;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final BookGenreRepository bookGenreRepository;
    private final BookAuthorRepository bookAuthorRepository;
    private final BookMapper bookMapper;

    @PersistenceContext
    private EntityManager entityManager;

    public Page<BookSummaryDTO> getAllBooks(Long genreId, Long authorId, String searchTerm, Pageable pageable) {
        // Genre filtering is handled by genreId parameter
        Page<Book> books = bookRepository.findAllNonDeletedBooks(genreId, authorId, searchTerm, pageable);
        return books.map(bookMapper::toBookSummaryDTO);
    }

    public BookDetailDTO getBookById(Long id) {
        Book book = bookRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", id));

        return bookMapper.toBookDetailDTO(book);
    }

    /**
     * Get books by author
     *
     * @param authorId The author ID
     * @param pageable Pagination information
     * @return Page of BookSummaryDTO objects
     * @throws ResourceNotFoundException if author not found
     */
    public Page<BookSummaryDTO> getBooksByAuthor(Long authorId, Pageable pageable) {
        // Check if author exists
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author", "id", authorId));

        // Create predicate to search for books by author
        QBook qBook = QBook.book;
        QBookAuthor qBookAuthor = QBookAuthor.bookAuthor;

        BooleanBuilder whereClause = new BooleanBuilder();
        whereClause.and(qBook.isDeleted.eq(false));
        whereClause.and(qBookAuthor.author.id.eq(authorId));

        // Create query
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        JPAQuery<Book> query = queryFactory
                .selectFrom(qBook)
                .join(qBookAuthor).on(qBook.eq(qBookAuthor.book))
                .where(whereClause)
                .distinct();

        // Apply pagination
        query.offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        // Apply sorting
        if (pageable.getSort().isSorted()) {
            List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

            pageable.getSort().forEach(order -> {
                OrderSpecifier<?> orderSpecifier = null;

                switch (order.getProperty()) {
                    case "title":
                        orderSpecifier = order.isAscending() ? qBook.title.asc() : qBook.title.desc();
                        break;
                    case "publicationYear":
                        // First sort by the is_null indicator to ensure nulls come last
                        orderSpecifiers.add(qBook.publicationYearIsNull.asc());
                        // Then sort by the actual year field
                        orderSpecifier = order.isAscending() ? qBook.publicationYear.asc() : qBook.publicationYear.desc();
                        break;
                    case "averageRating":
                        orderSpecifier = order.isAscending() ? qBook.averageRating.asc() : qBook.averageRating.desc();
                        break;
                    default:
                        // Default sort by average rating descending
                        orderSpecifier = qBook.averageRating.desc().nullsLast();
                }

                if (orderSpecifier != null) {
                    orderSpecifiers.add(orderSpecifier);
                }
            });

            if (!orderSpecifiers.isEmpty()) {
                query.orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]));
            }
        } else {
            // Default sort by average rating (highest first)
            query.orderBy(qBook.averageRating.desc().nullsLast());
        }

        // Execute query
        List<Book> books = query.fetch();

        // Get total count
        JPAQuery<Long> countQuery = queryFactory
                .select(qBook.countDistinct())
                .from(qBook)
                .join(qBookAuthor).on(qBook.eq(qBookAuthor.book))
                .where(whereClause);

        Long total = countQuery.fetchOne();

        if (total == null) {
            total = 0L;
        }

        // Map to DTOs
        List<BookSummaryDTO> bookDTOs = books.stream()
                .map(bookMapper::toBookSummaryDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(bookDTOs, pageable, total);
    }

    @Transactional
    public BookDetailDTO createBook(CreateBookRequestDTO createBookRequestDTO, Integer currentUserId) {
        // Check if ISBN already exists
        if (createBookRequestDTO.getIsbn() != null && !createBookRequestDTO.getIsbn().isEmpty()) {
            Optional<Book> existingBook = bookRepository.findByIsbn(createBookRequestDTO.getIsbn());
            if (existingBook.isPresent()) {
                throw new DuplicateResourceException("Book", "isbn", createBookRequestDTO.getIsbn());
            }
        }

        // Create book entity
        Book book = Book.builder()
                .title(createBookRequestDTO.getTitle())
                .description(createBookRequestDTO.getDescription())
                .isbn(createBookRequestDTO.getIsbn())
                .coverImageUrl(createBookRequestDTO.getCoverImageUrl())
                .publicationYear(createBookRequestDTO.getPublicationYear())
                .isDeleted(false)
                .averageRating(new BigDecimal("0.00"))
                .reviewCount(0)
                .build();

        // Save book to get an ID
        book = bookRepository.save(book);

        // Associate genres with proper denormalized fields
        associateGenresWithBook(book, createBookRequestDTO.getGenreIds(), currentUserId);

        // Associate authors
        associateAuthorsWithBook(book, createBookRequestDTO.getAuthorIds(), currentUserId);

        // Refresh book from database to get all relationships loaded
        book = bookRepository.findById(book.getId()).orElseThrow();

        return bookMapper.toBookDetailDTO(book);
    }

    @Transactional
    public BookDetailDTO updateBook(Long id, UpdateBookRequestDTO updateBookRequestDTO, Integer currentUserId) {
        // Find the book
        Book book = bookRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", id));

        // Check if new ISBN already exists for a different book
        if (updateBookRequestDTO.getIsbn() != null &&
                !updateBookRequestDTO.getIsbn().equals(book.getIsbn()) &&
                !updateBookRequestDTO.getIsbn().isEmpty()) {

            boolean existsWithIsbn = bookRepository.existsByIsbnAndIdNot(updateBookRequestDTO.getIsbn(), id);
            if (existsWithIsbn) {
                throw new DuplicateResourceException("Book", "isbn", updateBookRequestDTO.getIsbn());
            }
        }

        // Update authors if provided
        if (updateBookRequestDTO.getAuthorIds() != null) {
            // Remove existing authors
            bookAuthorRepository.deleteByBook(book);
            // Add new authors
            associateAuthorsWithBook(book, updateBookRequestDTO.getAuthorIds(), currentUserId);
        }

        // Store old values for comparison
        Boolean oldIsDeleted = book.getIsDeleted();
        BigDecimal oldAverageRating = book.getAverageRating();
        Integer oldPublicationYear = book.getPublicationYear();

        // Update basic book properties
        book.setTitle(updateBookRequestDTO.getTitle());
        book.setDescription(updateBookRequestDTO.getDescription());
        book.setIsbn(updateBookRequestDTO.getIsbn());
        book.setCoverImageUrl(updateBookRequestDTO.getCoverImageUrl());
        book.setPublicationYear(updateBookRequestDTO.getPublicationYear());

        // Save book changes
        book = bookRepository.save(book);

        // Check if denormalized fields have changed
        boolean denormalizedFieldsChanged =
                (oldIsDeleted != null && book.getIsDeleted() != null && !oldIsDeleted.equals(book.getIsDeleted())) ||
                        (oldAverageRating != null && book.getAverageRating() != null && !oldAverageRating.equals(book.getAverageRating())) ||
                        (oldPublicationYear != null && book.getPublicationYear() != null && !oldPublicationYear.equals(book.getPublicationYear())) ||
                        (oldIsDeleted == null && book.getIsDeleted() != null) ||
                        (oldAverageRating == null && book.getAverageRating() != null) ||
                        (oldPublicationYear == null && book.getPublicationYear() != null);

        // Update genres if provided
        if (updateBookRequestDTO.getGenreIds() != null) {
            // Remove existing genres
            bookGenreRepository.deleteByBook(book);
            // Add new genres
            associateGenresWithBook(book, updateBookRequestDTO.getGenreIds(), currentUserId);
        } else if (denormalizedFieldsChanged) {
            // If denormalized fields changed but genres weren't updated, we need to update the denormalized fields
            updateBookGenreDenormalizedFields(book);
        }

        // Refresh book from database to get all relationships loaded
        book = bookRepository.findById(book.getId()).orElseThrow();

        return bookMapper.toBookDetailDTO(book);
    }

    @Transactional
    public void deleteBook(Long id) {
        Book book = bookRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", id));

        book.setIsDeleted(true);
        Book savedBook = bookRepository.save(book);

        // Update the denormalized isDeleted field in book_genres
        updateBookGenreDenormalizedFields(savedBook);
    }

    private void associateGenresWithBook(Book book, List<Long> genreIds, Integer assignedBy) {
        List<BookGenre> bookGenres = new ArrayList<>();

        for (Long genreId : genreIds) {
            Genre genre = genreRepository.findById(genreId)
                    .orElseThrow(() -> new ResourceNotFoundException("Genre", "id", genreId));

            BookGenre bookGenre = new BookGenre(1, assignedBy, book, genre);

            // Set denormalized fields from book to BookGenre for query optimization
            bookGenre.setIsDeleted(book.getIsDeleted());
            bookGenre.setAverageRating(book.getAverageRating());
            bookGenre.setPublicationYear(book.getPublicationYear());

            bookGenres.add(bookGenre);
        }

        bookGenreRepository.saveAll(bookGenres);
    }

    private void associateAuthorsWithBook(Book book, List<Long> authorIds, Integer assignedBy) {
        List<BookAuthor> bookAuthors = new ArrayList<>();

        int priority = authorIds.size();

        for (Long authorId : authorIds) {
            Author author = authorRepository.findById(authorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Author", "id", authorId));

            BookAuthor bookAuthor = new BookAuthor(priority, assignedBy, book, author);
            bookAuthors.add(bookAuthor);

            priority = priority - 1;
        }

        bookAuthorRepository.saveAll(bookAuthors);
    }

    /**
     * Update denormalized fields in book_genres when book data changes
     * This keeps the denormalized fields in sync with the source data for query optimization
     */
    private void updateBookGenreDenormalizedFields(Book book) {
        List<BookGenre> bookGenres = bookGenreRepository.findByBook(book);
        for (BookGenre bookGenre : bookGenres) {
            bookGenre.setIsDeleted(book.getIsDeleted());
            bookGenre.setAverageRating(book.getAverageRating());
            bookGenre.setPublicationYear(book.getPublicationYear());
        }
        bookGenreRepository.saveAll(bookGenres);
    }
}
