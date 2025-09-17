package games.moisoni.google_iab.listeners;

import androidx.annotation.NonNull;

import java.util.List;

import games.moisoni.google_iab.BillingConnector;
import games.moisoni.google_iab.enums.ProductType;
import games.moisoni.google_iab.models.BillingResponse;
import games.moisoni.google_iab.models.ProductInfo;
import games.moisoni.google_iab.models.PurchaseInfo;

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
     * Callback will be triggered when a specific product ID is not found during a query
     * This is useful for identifying configuration errors in the Play Console
     *
     * @param productId - the product ID that was not found
     * @param response  - provides information about the error
     */
    void onProductQueryError(@NonNull String productId, @NonNull BillingResponse response);
}