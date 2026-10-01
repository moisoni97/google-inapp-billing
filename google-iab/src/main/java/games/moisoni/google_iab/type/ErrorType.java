package games.moisoni.google_iab.type;

/**
 * Type of error reported through BillingEventListener.onBillingError() (or onProductQueryError())
 * <p>
 * BillingResponse.getResponseCode() is the Play Billing response code (BillingClient.BillingResponseCode),
 * or 99 when the error was detected by the library itself
 * <p>
 * A failed connection is retried automatically with exponential backoff (except PLAY_STORE_NOT_INSTALLED)
 */
public enum ErrorType {
    /**
     * purchase() / subscribe() was called before isReady() (not connected yet, or no product details fetched),
     * or consumePurchase() / acknowledgePurchase() was called while the billing client is not connected
     */
    CLIENT_NOT_READY,
    /**
     * The connection to the Google Play billing service was lost (the library reconnects automatically),
     * or Play Billing returned SERVICE_DISCONNECTED
     */
    CLIENT_DISCONNECTED,
    /**
     * purchase() / subscribe() was called with a product ID whose details were not fetched
     * (not in the product ID lists, missing from Play Console, or its details query failed)
     */
    PRODUCT_NOT_EXIST,
    /**
     * The details of a product were not returned by a query
     * <p>
     * Reported through onProductQueryError(), not onBillingError(). The message contains the reason reported by Google Play
     */
    PRODUCT_ID_QUERY_FAILED,
    /**
     * A consume request failed
     * <p>
     * With autoConsume(), the consumption is retried on the next purchases refresh
     */
    CONSUME_ERROR,
    /**
     * A consumable purchase can't be consumed yet because it is PENDING (e.g. a cash payment)
     * <p>
     * With autoConsume(), it is reported once per purchase and the purchase is consumed on a later refresh, once it is PURCHASED
     */
    CONSUME_WARNING,
    /**
     * An acknowledge request failed
     * <p>
     * With autoAcknowledge(), the acknowledgment is retried on the next purchases refresh
     */
    ACKNOWLEDGE_ERROR,
    /**
     * A non-consumable or subscription purchase can't be acknowledged yet because it is PENDING (e.g. a cash payment)
     * <p>
     * With autoAcknowledge(), it is reported once per purchase and the purchase is acknowledged on a later refresh, once it is PURCHASED
     */
    ACKNOWLEDGE_WARNING,
    /**
     * A query of owned purchases failed
     * <p>
     * It is retried on the next purchases refresh, and isPurchased() keeps answering from the last successful query
     */
    FETCH_PURCHASED_PRODUCTS_ERROR,
    /**
     * A product details query failed or returned no products, the connection could not be started,
     * the subscriptions page could not be opened, or Play Billing returned a response code without a dedicated type
     */
    BILLING_ERROR,
    /**
     * A network error occurred (Play Billing NETWORK_ERROR)
     */
    NETWORK_ERROR,
    /**
     * The user canceled the purchase flow (Play Billing USER_CANCELED)
     */
    USER_CANCELED,
    /**
     * The Google Play billing service is temporarily unavailable (Play Billing SERVICE_UNAVAILABLE)
     */
    SERVICE_UNAVAILABLE,
    /**
     * Billing is not available, e.g. the Play Store version is not supported
     * or billing is not available in the user's country (Play Billing BILLING_UNAVAILABLE)
     */
    BILLING_UNAVAILABLE,
    /**
     * The product is not available for purchase (Play Billing ITEM_UNAVAILABLE),
     * or subscribe() was called with an offer ID that doesn't exist or the user is not eligible for
     */
    ITEM_UNAVAILABLE,
    /**
     * Incorrect use of the API (Play Billing DEVELOPER_ERROR), or an invalid argument detected by the library:
     * a null or empty product ID or purchase token, a finishing Activity, an invalid offer index or base plan ID,
     * purchase() called with a subscription or subscribe(activity, productId, basePlanId, offerId) with an in-app product
     */
    DEVELOPER_ERROR,
    /**
     * A generic error returned by Play Billing (Play Billing ERROR)
     */
    ERROR,
    /**
     * The product is already owned (Play Billing ITEM_ALREADY_OWNED)
     * <p>
     * The library refreshes purchases afterward, so with autoConsume() an owned consumable that was not consumed yet gets consumed
     */
    ITEM_ALREADY_OWNED,
    /**
     * The product is not owned (Play Billing ITEM_NOT_OWNED)
     */
    ITEM_NOT_OWNED,
    /**
     * connect() found no Google Play Store on the device. The response code is BILLING_UNAVAILABLE
     * <p>
     * The connection is not retried
     */
    PLAY_STORE_NOT_INSTALLED,
    /**
     * The signature of a purchase doesn't match the license key passed to the BillingConnector constructor
     * <p>
     * The purchase is ignored (not listed, consumed or acknowledged), so Google refunds it after 3 days
     * Make sure the license key matches the one from Play Console
     */
    SIGNATURE_VERIFICATION_FAILED,
    /**
     * The requested feature is not supported by the Play Store on the device (Play Billing FEATURE_NOT_SUPPORTED)
     */
    FEATURE_NOT_SUPPORTED,
}