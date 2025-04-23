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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final BookCategoryRepository bookCategoryRepository;
    private final BookAuthorRepository bookAuthorRepository;
    private final BookMapper bookMapper;

    @PersistenceContext
    private EntityManager entityManager;

    public Page<BookSummaryDTO> getAllBooks(Long categoryId, Long authorId, String searchTerm, Pageable pageable) {
        Page<Book> books = bookRepository.findAllNonDeletedBooks(categoryId, authorId, searchTerm, pageable);
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
                        orderSpecifier = order.isAscending() ? qBook.publicationYear.asc() : qBook.publicationYear.desc();
                        break;
                    case "averageRating":
                        orderSpecifier = order.isAscending() ? qBook.averageRating.asc() : qBook.averageRating.desc();
                        break;
                    case "createdAt":
                        orderSpecifier = order.isAscending() ? qBook.createdAt.asc() : qBook.createdAt.desc();
                        break;
                    default:
                        // Default sort by publication year descending
                        orderSpecifier = qBook.publicationYear.desc().nullsLast();
                }

                if (orderSpecifier != null) {
                    orderSpecifiers.add(orderSpecifier);
                }
            });

            if (!orderSpecifiers.isEmpty()) {
                query.orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]));
            }
        } else {
            // Default sort by publication year (newest first)
            query.orderBy(qBook.publicationYear.desc().nullsLast());
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
                .build();

        // Save book to get an ID
        book = bookRepository.save(book);

        // Associate categories
        associateCategoriesWithBook(book, createBookRequestDTO.getCategoryIds(), currentUserId);

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

        // Update basic book properties
        book.setTitle(updateBookRequestDTO.getTitle());
        book.setDescription(updateBookRequestDTO.getDescription());
        book.setIsbn(updateBookRequestDTO.getIsbn());
        book.setCoverImageUrl(updateBookRequestDTO.getCoverImageUrl());
        book.setPublicationYear(updateBookRequestDTO.getPublicationYear());

        // Save book changes
        book = bookRepository.save(book);

        // Update categories if provided
        if (updateBookRequestDTO.getCategoryIds() != null) {
            // Remove existing categories
            bookCategoryRepository.deleteByBook(book);
            // Add new categories
            associateCategoriesWithBook(book, updateBookRequestDTO.getCategoryIds(), currentUserId);
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
        bookRepository.save(book);
    }

    private void associateCategoriesWithBook(Book book, List<Long> categoryIds, Integer assignedBy) {
        List<BookCategory> bookCategories = new ArrayList<>();

        for (Long categoryId : categoryIds) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));

            BookCategory bookCategory = new BookCategory(assignedBy, book, category);
            bookCategories.add(bookCategory);
        }

        bookCategoryRepository.saveAll(bookCategories);
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
}
