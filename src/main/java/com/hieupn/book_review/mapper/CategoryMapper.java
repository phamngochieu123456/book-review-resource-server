package com.hieupn.book_review.mapper;

import com.hieupn.book_review.model.dto.CategorySummaryDTO;
import com.hieupn.book_review.model.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Mapper interface for converting between Category entity and CategoryDTO
 */
@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryMapper INSTANCE = Mappers.getMapper(CategoryMapper.class);

    /**
     * Convert Category entity to CategoryDTO
     *
     * @param category The Category entity
     * @return The corresponding CategoryDTO
     */
    CategorySummaryDTO toCategorySummaryDTO(Category category);

    /**
     * Convert CategoryDTO to Category entity
     *
     * @param categorySummaryDTO The CategoryDTO
     * @return The corresponding Category entity
     */
    @Mapping(target = "bookCategories", ignore = true)
    Category toCategory(CategorySummaryDTO categorySummaryDTO);


    /**
     * Convert a list of Category entities to a list of CategoryDTOs
     *
     * @param categories The list of Category entities
     * @return The list of corresponding CategoryDTOs
     */
    List<CategorySummaryDTO> toCategorySummaryDTOs(List<Category> categories);
}
