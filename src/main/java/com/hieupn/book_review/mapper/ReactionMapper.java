package com.hieupn.book_review.mapper;

import com.hieupn.book_review.model.dto.ReactionDTO;
import com.hieupn.book_review.model.entity.Reaction;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Mapper for converting between Reaction entity and ReactionDTO
 */
@Mapper(componentModel = "spring")
public interface ReactionMapper {

    ReactionMapper INSTANCE = Mappers.getMapper(ReactionMapper.class);

    /**
     * Convert Reaction entity to ReactionDTO
     *
     * @param reaction The Reaction entity
     * @return The corresponding ReactionDTO
     */
    @Mapping(target = "username", ignore = true) // Will be populated by UserService
    ReactionDTO toReactionDTO(Reaction reaction);

    /**
     * Convert a list of Reaction entities to a list of ReactionDTOs
     *
     * @param reactions The list of Reaction entities
     * @return The list of corresponding ReactionDTOs
     */
    List<ReactionDTO> toReactionDTOs(List<Reaction> reactions);
}
