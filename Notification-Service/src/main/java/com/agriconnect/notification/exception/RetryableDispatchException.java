package com.agriconnect.notification.exception;

/**
 * Thrown by dispatchers for transient failures (SMTP timeout, Twilio rate-limit, FCM 5xx).
 * The consumer will retry using Spring-Retry's exponential backoff.
 * After exhausting retries the event is forwarded to the DLQ.
 */
public class RetryableDispatchException extends RuntimeException {

    public RetryableDispatchException(String message, Throwable cause) {
        super(message, cause);
    }

    public RetryableDispatchException(String message) {
        super(message);
    }
}
