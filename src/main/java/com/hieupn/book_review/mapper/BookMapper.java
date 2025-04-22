package com.hieupn.book_review.mapper;

import com.hieupn.book_review.model.dto.*;
import com.hieupn.book_review.model.entity.Book;
import com.hieupn.book_review.model.entity.BookAuthor;
import com.hieupn.book_review.model.entity.BookCategory;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper interface for converting between Book entity and BookDTO objects
 */
@Mapper(componentModel = "spring", uses = {CategoryMapper.class})
public interface BookMapper {

    BookMapper INSTANCE = Mappers.getMapper(BookMapper.class);

    /**
     * Convert Book entity to BookDetailDTO
     *
     * @param book The Book entity
     * @return The corresponding BookDetailDTO
     */
    @Mapping(target = "authors", ignore = true)
    @Mapping(target = "categories", ignore = true)
    BookDetailDTO toBookDetailDTO(Book book);

    /**
     * Convert BookDetailDTO to Book entity
     *
     * @param bookDetailDTO The BookDetailDTO
     * @return The corresponding Book entity
     */
    @Mapping(target = "bookAuthors", ignore = true)
    @Mapping(target = "bookCategories", ignore = true)
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
     * After mapping from Book to BookDetailDTO, add categories
     *
     * @param book The source Book entity
     * @param bookDetailDTO The target BookDetailDTO
     */
    @AfterMapping
    default void mapCategoriesForBookDetailDTO(Book book, @MappingTarget BookDetailDTO bookDetailDTO) {
        Set<BookCategory> bookCategories = book.getBookCategories();
        if (bookCategories != null && !bookCategories.isEmpty()) {
            List<CategorySummaryDTO> categoryDTOs = bookCategories.stream()
                    .map(bookCategory -> CategoryMapper.INSTANCE.toCategorySummaryDTO(bookCategory.getCategory()))
                    .collect(Collectors.toList());
            bookDetailDTO.setCategories(categoryDTOs);
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
