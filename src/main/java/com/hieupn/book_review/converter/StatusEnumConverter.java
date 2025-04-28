package com.hieupn.book_review.converter;

import com.hieupn.book_review.model.enums.UserStatusType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Converter
public class StatusEnumConverter implements AttributeConverter<UserStatusType, String> {

    private static final Logger logger = LoggerFactory.getLogger(StatusEnumConverter.class);

    @Override
    public String convertToDatabaseColumn(UserStatusType attribute) {
        // If enum is null, return null, otherwise convert to lowercase before saving
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public UserStatusType convertToEntityAttribute(String dbData) {
        // If the database data is null, return null
        if (dbData == null) {
            return null;
        }

        // Trim and convert to uppercase for safer comparison
        String normalizedValue = dbData.trim().toUpperCase();

        // Log the actual value from DB to help with debugging
        logger.debug("Converting DB status value: '{}' to enum", dbData);

        try {
            return UserStatusType.valueOf(normalizedValue);
        } catch (IllegalArgumentException e) {
            // Fallback to default value in case of error
            logger.warn("Could not convert '{}' to UserStatusType enum, defaulting to ACTIVE", dbData);
            return UserStatusType.ACTIVE; // Default to ACTIVE if conversion fails
        }
    }
}
