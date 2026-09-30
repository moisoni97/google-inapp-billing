package games.moisoni.google_iab.status;

/**
 * Result of BillingConnector.isPurchased()
 */
public enum PurchasedResult {
    /**
     * The billing client is not connected and purchases of the product's type were not fetched yet
     * (still connecting or reconnecting), or the BillingConnector was released
     */
    CLIENT_NOT_READY,
    /**
     * The billing client is connected but purchases of the product's type were not fetched yet
     * (the query is still running, or it failed and is retried on the next refresh)
     */
    PURCHASED_PRODUCTS_NOT_FETCHED_YET,
    /**
     * The product is owned and its purchase state is PURCHASED
     */
    YES,
    /**
     * The product is not owned, or its purchase is still PENDING
     */
    NO
}