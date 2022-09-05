package games.moisoni.google_iab.models;

import com.android.billingclient.api.ProductDetails;

import java.util.List;

import games.moisoni.google_iab.enums.SkuProductType;

public class ProductInfo {

    private final SkuProductType skuProductType;
    private final ProductDetails productDetails;

    private final String product;
    private final String price;
    private final String description;
    private final String title;
    private final String type;
    private final String name;
    private final ProductDetails.OneTimePurchaseOfferDetails oneTimePurchaseOfferDetails;
    private final List<ProductDetails.SubscriptionOfferDetails> subscriptionOfferDetails;


    public ProductInfo(SkuProductType skuProductType, ProductDetails productDetails) {
        this.skuProductType = skuProductType;
        this.productDetails = productDetails;
        this.product = productDetails.getProductId();
        this.price = productDetails.getDescription();
        this.description = productDetails.getDescription();
        this.title = productDetails.getTitle();
        this.type = productDetails.getProductType();
        this.name = productDetails.getName();
        this.oneTimePurchaseOfferDetails = productDetails.getOneTimePurchaseOfferDetails();
        this.subscriptionOfferDetails = productDetails.getSubscriptionOfferDetails();
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