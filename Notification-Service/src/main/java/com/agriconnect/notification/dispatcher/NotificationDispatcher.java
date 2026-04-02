package com.agriconnect.notification.dispatcher;

/**
 * Common contract for all notification delivery channels.
 * Each implementation is responsible for a single channel: EMAIL, SMS, PUSH, IN_APP.
 */
public interface NotificationDispatcher {

    /**
     * Returns the channel name this dispatcher handles, e.g. "EMAIL".
     */
    String channel();

    /**
     * Dispatches the notification described by the context.
     * Implementations must throw {@link com.agriconnect.notification.exception.RetryableDispatchException}
     * for transient failures so the retry policy can kick in.
     */
    void dispatch(DispatchContext context);
}
