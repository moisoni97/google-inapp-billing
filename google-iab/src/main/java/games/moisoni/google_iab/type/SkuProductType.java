package games.moisoni.google_iab.type;

/**
 * Type of product, based on the product ID list it was added to
 * (setConsumableIds(), setNonConsumableIds() or setSubscriptionIds())
 */
public enum SkuProductType {
    /**
     * Can be purchased again once consumed (e.g. coins, gems)
     */
    CONSUMABLE,
    /**
     * Purchased once and owned permanently (e.g. remove ads), acknowledged instead of consumed
     */
    NON_CONSUMABLE,
    /**
     * Subscription, acknowledged instead of consumed
     */
    SUBSCRIPTION
}
