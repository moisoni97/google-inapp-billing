package games.moisoni.google_iab.models;

import com.android.billingclient.api.SkuDetails;

import games.moisoni.google_iab.enums.SkuProductType;

public class SkuInfo {

    private final String skuId;
    private final SkuProductType skuProductType;
    private final SkuDetails skuDetails;

    public SkuInfo(SkuProductType skuProductType, SkuDetails skuDetails) {
        this.skuProductType = skuProductType;
        this.skuDetails = skuDetails;
        this.skuId = skuDetails.getSku();
    }

    public String getSkuId() {
        return skuId;
    }

    public SkuProductType getSkuProductType() {
        return skuProductType;
    }

    public SkuDetails getSkuDetails() {
        return skuDetails;
    }
}