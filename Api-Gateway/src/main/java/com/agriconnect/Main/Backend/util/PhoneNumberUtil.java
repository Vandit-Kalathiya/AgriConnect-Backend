package com.agriconnect.Main.Backend.util;

import org.springframework.stereotype.Component;

@Component
public class PhoneNumberUtil {

    /**
     * Normalize phone number to 10-digit format
     * Removes +91, 91, spaces, dashes, and other formatting
     * 
     * Examples:
     *   +917990137814 -> 7990137814
     *   917990137814 -> 7990137814
     *   7990137814 -> 7990137814
     *   +91 7990137814 -> 7990137814
     */
    public static String normalize(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return phoneNumber;
        }

        // Remove all non-digit characters (spaces, dashes, parentheses, etc.)
        String digitsOnly = phoneNumber.replaceAll("[^0-9]", "");

        // Remove country code +91 or 91 if present
        if (digitsOnly.startsWith("91") && digitsOnly.length() == 12) {
            digitsOnly = digitsOnly.substring(2);
        }

        // Return 10-digit number
        return digitsOnly;
    }

    /**
     * Format phone number for Twilio (add +91 prefix)
     */
    public static String formatForTwilio(String phoneNumber) {
        String normalized = normalize(phoneNumber);
        if (normalized != null && !normalized.isEmpty()) {
            return "+91" + normalized;
        }
        return phoneNumber;
    }

    /**
     * Validate Indian phone number (10 digits starting with 6-9)
     */
    public static boolean isValid(String phoneNumber) {
        String normalized = normalize(phoneNumber);
        return normalized != null && 
               normalized.matches("^[6-9][0-9]{9}$");
    }
}
