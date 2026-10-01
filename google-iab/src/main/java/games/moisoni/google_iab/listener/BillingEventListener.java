package games.moisoni.google_iab.listener;

import androidx.annotation.NonNull;

import java.util.List;

import games.moisoni.google_iab.BillingConnector;
import games.moisoni.google_iab.type.ProductType;
import games.moisoni.google_iab.model.BillingResponse;
import games.moisoni.google_iab.model.ProductInfo;
import games.moisoni.google_iab.model.PurchaseInfo;

/**
 * Receives the events of a BillingConnector
 * <p>
 * All callbacks are delivered on the main thread. No callback is delivered after BillingConnector.release()
 */
public interface BillingEventListener {
    /**
     * Callback will be triggered when product details are fetched from Google Play
     * <p>
     * Triggered separately for in-app products and subscriptions, and again after the connection is re-established,
     * so replace previously fetched products (e.g. by product ID) instead of appending them
     *
     * @param productDetails - the fetched products of one type (never empty)
     */
    void onProductsFetched(@NonNull List<ProductInfo> productDetails);

    /**
     * Callback will be triggered when owned purchases are queried from Google Play
     * <p>
     * Triggered separately for INAPP and SUBS (SUBS only on devices that support subscriptions), after connecting
     * and again on every purchases refresh (each résumé when a Lifecycle was provided, or refreshPurchases())
     * Also triggered when no purchases are owned
     * <p>
     * The list also contains PENDING purchases and purchases that are not acknowledged yet (with autoAcknowledge() / autoConsume()
     * they are acknowledged or consumed right after this callback). Restore entitlements only for purchases where
     * isPurchased() and isAcknowledged() are true, in a way that is safe to repeat. Consumables are granted in onPurchaseConsumed()
     *
     * @param productType - INAPP (consumables and non-consumables) or SUBS
     * @param purchases   - the owned purchases of that type (can be empty)
     */
    void onPurchasedProductsFetched(@NonNull ProductType productType, @NonNull List<PurchaseInfo> purchases);

    /**
     * Callback will be triggered when a purchase flow finishes successfully
     * <p>
     * A purchase can still be PENDING here (e.g. a cash payment). Grant entitlement in onPurchaseAcknowledged()
     * or onPurchaseConsumed() instead, which are triggered once the purchase is PURCHASED and acknowledged / consumed
     *
     * @param purchases - the new purchases (never empty)
     */
    void onProductsPurchased(@NonNull List<PurchaseInfo> purchases);

    /**
     * Callback will be triggered when a non-consumable or subscription purchase is acknowledged
     * <p>
     * Grant entitlement here. Also triggered for purchases made earlier that were not acknowledged yet
     * (e.g. a PENDING payment that cleared, or the app was closed before the acknowledgment), so granting must be safe to repeat
     * <p>
     * purchase.isAcknowledged() still returns false here, it reflects the state before the acknowledgment
     *
     * @param purchase - the acknowledged purchase
     */
    void onPurchaseAcknowledged(@NonNull PurchaseInfo purchase);

    /**
     * Callback will be triggered when a consumable purchase is consumed
     * <p>
     * Grant entitlement here (purchase.getQuantity() items when multi-quantity purchases are enabled in Play Console)
     * The product can be purchased again
     *
     * @param purchase - the consumed purchase
     */
    void onPurchaseConsumed(@NonNull PurchaseInfo purchase);

    /**
     * Callback will be triggered when an error occurs
     * <p>
     * response.getErrorType() tells what failed (see ErrorType for when each type is reported)
     *
     * @param billingConnector - the BillingConnector that reported the error
     * @param response         - provides information about the error
     */
    void onBillingError(@NonNull BillingConnector billingConnector, @NonNull BillingResponse response);

    /**
     * Callback will be triggered when the details of a specific product ID are not returned by a query
     * This is useful for identifying configuration errors in the Play Console
     * <p>
     * The response message contains the reason reported by Google Play: the product was not found (not created or not active),
     * its ID has an invalid format, or the user is not eligible for any of its offers
     *
     * @param productId - the product ID that was not returned
     * @param response  - provides information about the error (ErrorType.PRODUCT_ID_QUERY_FAILED)
     */
    void onProductQueryError(@NonNull String productId, @NonNull BillingResponse response);
}