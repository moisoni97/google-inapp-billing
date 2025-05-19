package games.moisoni.google_iab.listeners;

/**
 * Listener interface for purchase acknowledgment events
 * <p>
 * Used to receive callbacks when purchase acknowledgment succeeds or fails
 * after retry attempts. This is particularly useful for tracking the final
 * state of acknowledgment operations that include automatic retry logic
 */
public interface AcknowledgeEventListener {
    /**
     * Callback triggered when a purchase is successfully acknowledged
     * <p>
     * This indicates that Google Play has confirmed the acknowledgment
     * and the purchase will not be automatically refunded
     */
    void onSuccess();

    /**
     * Callback triggered when all acknowledgment attempts have failed
     * <p>
     * This will be called after the maximum number of retry attempts
     * have been exhausted without success. The purchase may still be
     * in an unacknowledged state and at risk of automatic refund
     */
    void onFailure();
}