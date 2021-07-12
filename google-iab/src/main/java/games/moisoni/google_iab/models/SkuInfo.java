package games.moisoni.google_iab.models;

import com.android.billingclient.api.SkuDetails;

import games.moisoni.google_iab.enums.SkuProductType;

public class SkuInfo {

    private final SkuProductType skuProductType;
    private final SkuDetails skuDetails;

    private final String sku;
    private final String price;
    private final String description;
    private final String title;
    private final String type;
    private final String iconUrl;
    private final String freeTrialPeriod;
    private final String introductoryPrice;
    private final String introductoryPricePeriod;
    private final String subscriptionPeriod;
    private final String priceCurrencyCode;
    private final String originalPrice;
    private final String originalJson;

    private final int introductoryPriceCycles;

    private final long priceAmountMicros;
    private final long originalPriceAmountMicros;
    private final long introductoryPriceAmountMicros;

    public SkuInfo(SkuProductType skuProductType, SkuDetails skuDetails) {
        this.skuProductType = skuProductType;
        this.skuDetails = skuDetails;
        this.sku = skuDetails.getSku();
        this.price = skuDetails.getPrice();
        this.description = skuDetails.getDescription();
        this.title = skuDetails.getTitle();
        this.type = skuDetails.getType();
        this.iconUrl = skuDetails.getIconUrl();
        this.freeTrialPeriod = skuDetails.getFreeTrialPeriod();
        this.introductoryPrice = skuDetails.getIntroductoryPrice();
        this.introductoryPricePeriod = skuDetails.getIntroductoryPricePeriod();
        this.subscriptionPeriod = skuDetails.getSubscriptionPeriod();
        this.priceCurrencyCode = skuDetails.getPriceCurrencyCode();
        this.originalPrice = skuDetails.getOriginalPrice();
        this.originalJson = skuDetails.getOriginalJson();
        this.introductoryPriceCycles = skuDetails.getIntroductoryPriceCycles();
        this.priceAmountMicros = skuDetails.getPriceAmountMicros();
        this.originalPriceAmountMicros = skuDetails.getOriginalPriceAmountMicros();
        this.introductoryPriceAmountMicros = skuDetails.getIntroductoryPriceAmountMicros();
    }

    public SkuProductType getSkuProductType() {
        return skuProductType;
    }

    public SkuDetails getSkuDetails() {
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

    public String getIconUrl() {
        return iconUrl;
    }

    public String getFreeTrialPeriod() {
        return freeTrialPeriod;
    }

    public String getIntroductoryPrice() {
        return introductoryPrice;
    }

    public String getIntroductoryPricePeriod() {
        return introductoryPricePeriod;
    }

    public String getSubscriptionPeriod() {
        return subscriptionPeriod;
    }

    public String getPriceCurrencyCode() {
        return priceCurrencyCode;
    }

    public String getOriginalPrice() {
        return originalPrice;
    }

    public String getOriginalJson() {
        return originalJson;
    }

    public int getIntroductoryPriceCycles() {
        return introductoryPriceCycles;
    }

    public long getPriceAmountMicros() {
        return priceAmountMicros;
    }

    public long getOriginalPriceAmountMicros() {
        return originalPriceAmountMicros;
    }

    public long getIntroductoryPriceAmountMicros() {
        return introductoryPriceAmountMicros;
    }
}