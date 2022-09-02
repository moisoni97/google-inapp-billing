package games.moisoni.google_iab.models;

import com.android.billingclient.api.ProductDetails;

import java.util.List;

import games.moisoni.google_iab.enums.SkuProductType;

public class ProductInfo {

    private final SkuProductType skuProductType;
    private final ProductDetails skuDetails;

    private final String sku;
    private final String price;
    private final String description;
    private final String title;
    private final String type;
    private final String name;
    private final ProductDetails.OneTimePurchaseOfferDetails oneTimePurchaseOfferDetails;
    private final List<ProductDetails.SubscriptionOfferDetails> subscriptionOfferDetails;


    public ProductInfo(SkuProductType skuProductType, ProductDetails skuDetails) {
        this.skuProductType = skuProductType;
        this.skuDetails = skuDetails;
        this.sku = skuDetails.getProductId();
        this.price = skuDetails.getDescription();
        this.description = skuDetails.getDescription();
        this.title = skuDetails.getTitle();
        this.type = skuDetails.getProductType();
        this.name = skuDetails.getProductType();
        this.oneTimePurchaseOfferDetails = skuDetails.getOneTimePurchaseOfferDetails();
        this.subscriptionOfferDetails = skuDetails.getSubscriptionOfferDetails();
    }

    public SkuProductType getSkuProductType() {
        return skuProductType;
    }

    public ProductDetails getSkuDetails() {
        return skuDetails;
    }

    public String getSku() {
        return sku;
    }

    public String getPrice() {
        return price;
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

    public ProductDetails.OneTimePurchaseOfferDetails getOneTimePurchaseOfferDetails() {
        return oneTimePurchaseOfferDetails;
    }

    public List<ProductDetails.SubscriptionOfferDetails> getSubscriptionOfferDetails() {
        return subscriptionOfferDetails;
    }
}