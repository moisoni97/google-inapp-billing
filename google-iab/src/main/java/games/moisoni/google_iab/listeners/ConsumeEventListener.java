package games.moisoni.google_iab.listeners;

/**
 * Listener interface for purchase consumption events.
 * <p>
 * Used to receive callbacks when purchase consumption succeeds or fails
 * after retry attempts. This mirrors AcknowledgeEventListener exactly.
 */
public interface ConsumeEventListener {
    /**
     * Callback triggered when a purchase is successfully consumed
     * <p>
     * This indicates that Google Play has confirmed the consumption
     * and the product can be purchased again
     */
    void onSuccess();

    /**
     * Callback triggered when all consumption attempts have failed
     * <p>
     * Called after maximum retry attempts with no success
     */
    void onFailure();
}