package games.moisoni.google_iab.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.ProductDetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import games.moisoni.google_iab.type.SkuProductType;

/**
 * Details of a product fetched from Google Play, passed to BillingEventListener.onProductsFetched()
 * <p>
 * Also available from PurchaseInfo.getProductInfo() when the details of the purchased product were fetched
 */
public class ProductInfo {

    private final SkuProductType skuProductType;
    private final ProductDetails productDetails;
    private final String product;
    private final String description;
    private final String title;
    private final String type;
    private final String name;
    private final String oneTimePurchaseOfferFormattedPrice;
    private final long oneTimePurchaseOfferPriceAmountMicros;
    private final String oneTimePurchaseOfferPriceCurrencyCode;
    private final List<SubscriptionOfferDetails> subscriptionOfferDetails;

    /**
     * Creates a ProductInfo from the product details returned by Play Billing
     */
    public ProductInfo(SkuProductType skuProductType, @NonNull ProductDetails productDetails) {
        this.skuProductType = skuProductType;
        this.productDetails = productDetails;
        this.product = productDetails.getProductId();
        this.description = productDetails.getDescription();
        this.title = productDetails.getTitle();
        this.type = productDetails.getProductType();
        this.name = productDetails.getName();

        ProductDetails.OneTimePurchaseOfferDetails offerDetails = productDetails.getOneTimePurchaseOfferDetails();
        if (offerDetails != null) {
            this.oneTimePurchaseOfferFormattedPrice = offerDetails.getFormattedPrice();
            this.oneTimePurchaseOfferPriceAmountMicros = offerDetails.getPriceAmountMicros();
            this.oneTimePurchaseOfferPriceCurrencyCode = offerDetails.getPriceCurrencyCode();
        } else {
            this.oneTimePurchaseOfferFormattedPrice = null;
            this.oneTimePurchaseOfferPriceAmountMicros = 0L;
            this.oneTimePurchaseOfferPriceCurrencyCode = null;
        }

        List<ProductDetails.SubscriptionOfferDetails> offerDetailsList = productDetails.getSubscriptionOfferDetails();
        this.subscriptionOfferDetails = new ArrayList<>();

        if (offerDetailsList != null) {
            for (ProductDetails.SubscriptionOfferDetails details : offerDetailsList) {
                SubscriptionOfferDetails newOfferDetails = createSubscriptionOfferDetails(details);
                this.subscriptionOfferDetails.add(newOfferDetails);
            }
        }
    }

    /**
     * Returns CONSUMABLE, NON_CONSUMABLE or SUBSCRIPTION, based on the product ID list the product was added to
     */
    public SkuProductType getSkuProductType() {
        return skuProductType;
    }

    /**
     * Returns the original Play Billing ProductDetails
     */
    public ProductDetails getProductDetails() {
        return productDetails;
    }

    /**
     * Returns the product ID
     */
    public String getProduct() {
        return product;
    }

    /**
     * Returns the description set in Play Console
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the product name followed by the app name in parentheses (e.g. "100 Coins (My App)")
     * <p>
     * Use getName() for the product name only
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the Play Billing product type: "inapp" (BillingClient.ProductType.INAPP) or "subs" (BillingClient.ProductType.SUBS)
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the product name set in Play Console, without the app name (e.g. "100 Coins")
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the formatted price of the one-time purchase offer
     * <p>
     * Null for subscriptions, which have no one-time purchase offer (see getSubscriptionOfferDetails())
     */
    @Nullable
    public String getOneTimePurchaseOfferFormattedPrice() {
        return oneTimePurchaseOfferFormattedPrice;
    }

    /**
     * Returns the price in micro-units of the one-time purchase offer, or 0 for subscriptions
     */
    public long getOneTimePurchaseOfferPriceAmountMicros() {
        return oneTimePurchaseOfferPriceAmountMicros;
    }

    /**
     * Returns the ISO 4217 currency code of the one-time purchase offer
     * <p>
     * Null for subscriptions, which have no one-time purchase offer (see getSubscriptionOfferDetails())
     */
    @Nullable
    public String getOneTimePurchaseOfferPriceCurrencyCode() {
        return oneTimePurchaseOfferPriceCurrencyCode;
    }

    /**
     * Returns the base plans and offers of a subscription that the user is eligible for (read-only), empty for in-app products
     * <p>
     * Google Play does not guarantee their order, use getBasePlanId() / getOfferId() to find a specific one
     */
    public List<SubscriptionOfferDetails> getSubscriptionOfferDetails() {
        return Collections.unmodifiableList(subscriptionOfferDetails);
    }

    @NonNull
    private SubscriptionOfferDetails createSubscriptionOfferDetails(@NonNull ProductDetails.SubscriptionOfferDetails offerDetails) {
        return new SubscriptionOfferDetails(offerDetails.getOfferId(), offerDetails.getPricingPhases().getPricingPhaseList(), offerDetails.getOfferTags(), offerDetails.getOfferToken(), offerDetails.getBasePlanId());
    }
}