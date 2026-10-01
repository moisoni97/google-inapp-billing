package games.moisoni.google_iab.type;

/**
 * Type of the purchases passed to BillingEventListener.onPurchasedProductsFetched()
 */
public enum ProductType {
    /**
     * In-app products (consumables and non-consumables)
     */
    INAPP,
    /**
     * Subscriptions
     */
    SUBS,
    /**
     * Used internally for purchases from a purchase flow, never passed to onPurchasedProductsFetched()
     */
    COMBINED
}
