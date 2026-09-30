package games.moisoni.google_iab.listener;

import androidx.annotation.NonNull;

import java.util.List;

import games.moisoni.google_iab.BillingConnector;
import games.moisoni.google_iab.type.ProductType;
import games.moisoni.google_iab.model.BillingResponse;
import games.moisoni.google_iab.model.ProductInfo;
import games.moisoni.google_iab.model.PurchaseInfo;

public interface BillingEventListener {
    /**
     * Callback will be triggered when products are queried for Play Console
     *
     * @param productDetails - a list with available products
     */
    void onProductsFetched(@NonNull List<ProductInfo> productDetails);

    /**
     * Callback will be triggered when purchased products are queried from Play Console
     *
     * @param purchases   - a list with owned products
     * @param productType - the type of the product, either IN_APP or SUBS
     */
    void onPurchasedProductsFetched(@NonNull ProductType productType, @NonNull List<PurchaseInfo> purchases);

    /**
     * Callback will be triggered when a product is purchased successfully
     *
     * @param purchases - a list with purchased products
     */
    void onProductsPurchased(@NonNull List<PurchaseInfo> purchases);

    /**
     * Callback will be triggered when a purchase is acknowledged
     *
     * @param purchase - specifier of acknowledged purchase
     */
    void onPurchaseAcknowledged(@NonNull PurchaseInfo purchase);

    /**
     * Callback will be triggered when a purchase is consumed
     *
     * @param purchase - specifier of consumed purchase
     */
    void onPurchaseConsumed(@NonNull PurchaseInfo purchase);

    /**
     * Callback will be triggered when error occurs
     *
     * @param response - provides information about the error
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