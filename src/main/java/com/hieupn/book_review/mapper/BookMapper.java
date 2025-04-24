// src/main/java/com/hieupn/book_review/mapper/BookMapper.java
package com.hieupn.book_review.mapper;

import com.hieupn.book_review.model.dto.*;
import com.hieupn.book_review.model.entity.Book;
import com.hieupn.book_review.model.entity.BookAuthor;
import com.hieupn.book_review.model.entity.BookGenre;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper interface for converting between Book entity and BookDTO objects
 */
@Mapper(componentModel = "spring", uses = {GenreMapper.class})
public interface BookMapper {

    BookMapper INSTANCE = Mappers.getMapper(BookMapper.class);

    /**
     * Convert Book entity to BookDetailDTO
     *
     * @param book The Book entity
     * @return The corresponding BookDetailDTO
     */
    @Mapping(target = "authors", ignore = true)
    @Mapping(target = "genres", ignore = true)
    BookDetailDTO toBookDetailDTO(Book book);

    /**
     * Convert BookDetailDTO to Book entity
     *
     * @param bookDetailDTO The BookDetailDTO
     * @return The corresponding Book entity
     */
    @Mapping(target = "bookAuthors", ignore = true)
    @Mapping(target = "bookGenres", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    @Mapping(target = "comments", ignore = true)
    Book toBook(BookDetailDTO bookDetailDTO);

    /**
     * Convert Book entity to BookSummaryDTO
     *
     * @param book The Book entity
     * @return The corresponding BookSummaryDTO
     */
    @Mapping(target = "authors", ignore = true)
    BookSummaryDTO toBookSummaryDTO(Book book);

    /**
     * After mapping from Book to BookDetailDTO, add genres
     *
     * @param book The source Book entity
     * @param bookDetailDTO The target BookDetailDTO
     */
    @AfterMapping
    default void mapGenresForBookDetailDTO(Book book, @MappingTarget BookDetailDTO bookDetailDTO) {
        Set<BookGenre> bookGenres = book.getBookGenres();
        if (bookGenres != null && !bookGenres.isEmpty()) {
            // No need to filter by bookGenre.isDeleted since it's a denormalized field from the book
            // The book entity itself is already filtered by isDeleted=false
            List<GenreDTO> genreDTOs = bookGenres.stream()
                    .map(bookGenre -> GenreMapper.INSTANCE.toGenreDTO(bookGenre.getGenre()))
                    .collect(Collectors.toList());
            bookDetailDTO.setGenres(genreDTOs);
        }
    }

    /**
     * After mapping from Book to BookDetailDTO, add authors
     *
     * @param book The source Book entity
     * @param bookDetailDTO The target BookDetailDTO
     */
    @AfterMapping
    default void mapAuthorsForBookDetailDTO(Book book, @MappingTarget BookDetailDTO bookDetailDTO) {
        // This will be injected by Spring when using the mapper
        AuthorMapper authorMapper = AuthorMapper.INSTANCE;

        Set<BookAuthor> bookAuthors = book.getBookAuthors();
        if (bookAuthors != null && !bookAuthors.isEmpty()) {
            List<AuthorSummaryDTO> authorDTOs = bookAuthors.stream()
                    .map(bookAuthor -> authorMapper.toAuthorSummaryDTO(bookAuthor.getAuthor()))
                    .collect(Collectors.toList());
            bookDetailDTO.setAuthors(authorDTOs);
        }
    }

    /**
     * After mapping from Book to BookSummaryDTO, add authors
     *
     * @param book The source Book entity
     * @param bookSummaryDTO The target BookSummaryDTO
     */
    @AfterMapping
    default void mapAuthorsForBookSummaryDTO(Book book, @MappingTarget BookSummaryDTO bookSummaryDTO) {
        // This will be injected by Spring when using the mapper
        AuthorMapper authorMapper = AuthorMapper.INSTANCE;

        Set<BookAuthor> bookAuthors = book.getBookAuthors();
        if (bookAuthors != null && !bookAuthors.isEmpty()) {
            List<AuthorSummaryDTO> authorDTOs = bookAuthors.stream()
                    .map(bookAuthor -> authorMapper.toAuthorSummaryDTO(bookAuthor.getAuthor()))
                    .collect(Collectors.toList());
            bookSummaryDTO.setAuthors(authorDTOs);
        }
    }
}
