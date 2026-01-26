package com.agriconnect.Contract.Farming.App.util;

import com.agriconnect.Contract.Farming.App.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Component
public class CursorUtil {

    private static final String DELIMITER = "|";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;

    /**
     * Encode cursor from date, time, and ID
     * Format: base64(date|time|id)
     */
    public String encodeCursor(LocalDate date, LocalTime time, String id) {
        if (date == null || time == null || id == null) {
            return null;
        }

        String cursorString = date.format(DATE_FORMATTER) + DELIMITER +
                              time.format(TIME_FORMATTER) + DELIMITER +
                              id;

        return Base64.getUrlEncoder().encodeToString(cursorString.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decode cursor to get date, time, and ID
     * Returns array: [date, time, id]
     */
    public CursorData decodeCursor(String cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return null;
        }

        try {
            String decodedString = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decodedString.split("\\" + DELIMITER);

            if (parts.length != 3) {
                throw new BadRequestException("Invalid cursor format");
            }

            LocalDate date = LocalDate.parse(parts[0], DATE_FORMATTER);
            LocalTime time = LocalTime.parse(parts[1], TIME_FORMATTER);
            String id = parts[2];

            return new CursorData(date, time, id);

        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid cursor: " + e.getMessage());
        }
    }

    public static class CursorData {
        private final LocalDate date;
        private final LocalTime time;
        private final String id;

        public CursorData(LocalDate date, LocalTime time, String id) {
            this.date = date;
            this.time = time;
            this.id = id;
        }

        public LocalDate getDate() {
            return date;
        }

        public LocalTime getTime() {
            return time;
        }

        public String getId() {
            return id;
        }
    }
}
