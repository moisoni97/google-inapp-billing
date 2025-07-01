package games.moisoni.google_iab.models;

import androidx.annotation.NonNull;

import com.android.billingclient.api.ProductDetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import games.moisoni.google_iab.enums.SkuProductType;

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

    public SkuProductType getSkuProductType() {
        return skuProductType;
    }

    public ProductDetails getProductDetails() {
        return productDetails;
    }

    public String getProduct() {
        return product;
    }

    public String getDescription() {
        return description;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getOneTimePurchaseOfferFormattedPrice() {
        return oneTimePurchaseOfferFormattedPrice;
    }

    public long getOneTimePurchaseOfferPriceAmountMicros() {
        return oneTimePurchaseOfferPriceAmountMicros;
    }

    public String getOneTimePurchaseOfferPriceCurrencyCode() {
        return oneTimePurchaseOfferPriceCurrencyCode;
    }

    public List<SubscriptionOfferDetails> getSubscriptionOfferDetails() {
        return Collections.unmodifiableList(subscriptionOfferDetails);
    }

    @NonNull
    private SubscriptionOfferDetails createSubscriptionOfferDetails(@NonNull ProductDetails.SubscriptionOfferDetails offerDetails) {
        return new SubscriptionOfferDetails(offerDetails.getOfferId(), offerDetails.getPricingPhases().getPricingPhaseList(), offerDetails.getOfferTags(), offerDetails.getOfferToken(), offerDetails.getBasePlanId());
    }
}