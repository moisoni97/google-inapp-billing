package games.moisoni.google_iab.status;

/**
 * Result of BillingConnector.isSubscriptionSupported()
 */
public enum SupportState {
    /**
     * The device supports subscriptions
     */
    SUPPORTED,
    /**
     * The device doesn't support subscriptions (e.g. an outdated Play Store)
     */
    NOT_SUPPORTED,
    /**
     * The billing client is not connected yet, check again once connected
     */
    DISCONNECTED
}
