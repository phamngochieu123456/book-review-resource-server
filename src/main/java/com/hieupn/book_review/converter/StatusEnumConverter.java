package com.hieupn.book_review.converter;

import com.hieupn.book_review.model.enums.UserStatusType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class StatusEnumConverter implements AttributeConverter<UserStatusType, String> {

    @Override
    public String convertToDatabaseColumn(UserStatusType attribute) {
        // If enum is null, return null, otherwise convert to lowercase before saving
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public UserStatusType convertToEntityAttribute(String dbData) {
        // If the database data is null, return null, otherwise convert from lowercase to uppercase to match the enum
        return dbData == null ? null : UserStatusType.valueOf(dbData.toUpperCase());
    }
}
